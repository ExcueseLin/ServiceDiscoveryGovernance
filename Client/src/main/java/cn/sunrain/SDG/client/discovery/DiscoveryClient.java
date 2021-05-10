package cn.sunrain.SDG.client.discovery;

import cn.sunrain.SDG.client.config.ClientConfig;
import cn.sunrain.SDG.client.http.SDGHttpResponse;
import cn.sunrain.SDG.client.http.SdgHttpclient;
import cn.sunrain.SDG.share.entity.Application;
import cn.sunrain.SDG.share.entity.Applications;
import cn.sunrain.SDG.share.entity.InstanceInfo;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PreDestroy;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author lin
 * @date 2021/3/4 13:26
 */
public class DiscoveryClient implements Discovery {

    private static final Logger logger = LoggerFactory.getLogger(DiscoveryClient.class);

    private static final String VALUE_DELIMITER = ",";
    private static final String PREFIX = "DiscoveryClient_";
    private String appPathIdentifier;

    private volatile int registrySize = 0;
    private volatile long lastSuccessfulRegistryFetchTimestamp = -1;
    private volatile long lastSuccessfulHeartbeatTimestamp = -1;

    protected final ClientConfig clientConfig;
    //单调增加生成计数器，以确保过时的线程不会将注册表重置为旧版本
    //每次拉取成功+1
    private final AtomicLong fetchRegistryGeneration;
    //服务中心地址
    private final AtomicReference<String> serverUrls;
    private final AtomicReference<String[]> serverUrlsRef;

    private final AtomicReference<String> remoteRegionsToFetch;
    private final AtomicReference<String[]> remoteRegionsRef;

    // 定时任务的 线程池
    private final ScheduledExecutorService scheduler;
    // 心跳检查 线程池
    private final ThreadPoolExecutor heartbeatExecutor;
    // 服务获取 线程池
    private final ThreadPoolExecutor cacheRefreshExecutor;

    //客户端本地缓存  存储所有的客户端同步的服务实例
    private final AtomicReference<Applications> localRegionApps = new AtomicReference<Applications>();

    // 服务本身
    private final InstanceInfo instanceInfo;

    private final AtomicBoolean isShutdown = new AtomicBoolean(false);



    private SdgHttpclient sdgHttpclient;

    public DiscoveryClient(ClientConfig config, InstanceInfo instanceInfo, RestTemplate restTemplate) {
        // 服务初始化时间戳
        long initTimestampMs;

        this.clientConfig = config;

        this.instanceInfo = instanceInfo;
        appPathIdentifier = instanceInfo.getAppName() + "/" + instanceInfo.getId();


        serverUrls = new AtomicReference<>(clientConfig.getServerUrls());
        serverUrlsRef = new AtomicReference<>(serverUrls.get() == null ? null : serverUrls.get().split(","));


        remoteRegionsToFetch = new AtomicReference<String>(clientConfig.getFetchRemoteRegionsRegistry());
        remoteRegionsRef = new AtomicReference<>(remoteRegionsToFetch.get() == null ? null : remoteRegionsToFetch.get().split(","));


        sdgHttpclient = new SdgHttpclient(restTemplate,serverUrlsRef.get());

        fetchRegistryGeneration = new AtomicLong(0);
        localRegionApps.set(new Applications());

        //判断客户端是否需要注册到服务中心中   shouldRegisterWithEureka
        //判断客户端是否需要获取服务中心的其它实例  shouldFetchRegistry
        if (!config.shouldRegisterWithEureka() && !config.shouldFetchRegistry()) {
            logger.info("Client configured to neither register nor query for data.");
            scheduler = null;
            heartbeatExecutor = null;
            cacheRefreshExecutor = null;

            initTimestampMs = System.currentTimeMillis();
            logger.info("Discovery Client initialized at timestamp {} with initial instances count: {}",
                    initTimestampMs, this.getApplications().size());

            return;  // no need to setup up an network tasks and we are done
        }

        try {
            // 初始化三个线程池
            scheduler = Executors.newScheduledThreadPool(2,
                    new ThreadFactoryBuilder()
                            .setNameFormat("DiscoveryClient-%d")
                            .setDaemon(true)
                            .build());

            heartbeatExecutor = new ThreadPoolExecutor(
                    1, clientConfig.getHeartbeatExecutorThreadPoolSize(), 0, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>(),
                    new ThreadFactoryBuilder()
                            .setNameFormat("DiscoveryClient-HeartbeatExecutor-%d")
                            .setDaemon(true)
                            .build()
            );  // use direct handoff

            cacheRefreshExecutor = new ThreadPoolExecutor(
                    1, clientConfig.getCacheRefreshExecutorThreadPoolSize(), 0, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>(),
                    new ThreadFactoryBuilder()
                            .setNameFormat("DiscoveryClient-CacheRefreshExecutor-%d")
                            .setDaemon(true)
                            .build()
            );  // use direct handoff

        }catch (Throwable e) {
            throw new RuntimeException("Failed to initialize DiscoveryClient!", e);
        }

        //服务注册
        //首先查看服务是否需要注册到注册中心中  然后查看是否需要在启动时注册！
        if (clientConfig.shouldRegisterWithEureka() && clientConfig.shouldEnforceRegistrationAtInit()) {
            try {
                if (!register() ) {
                    throw new IllegalStateException("Registration error at startup. Invalid server response.");
                }
            } catch (Throwable th) {
                logger.error("Registration error at startup: {}", th.getMessage());
                throw new IllegalStateException(th);
            }
        }


        //服务发现
        //如果需要拉去服务中心中的服务 那么初始化时 就拉取一次
        //全局拉取基本就这一次！  后面都是增量拉取
        if (clientConfig.shouldFetchRegistry() ) {
            fetchRegistry(false);
        }



        initScheduledTasks();

        initTimestampMs = System.currentTimeMillis();

        logger.info("Discovery Client initialized at timestamp {} with initial instances count: {}",
                initTimestampMs, this.getApplications().size());
    }

    /**
     * 初始化三个线程池 执行的任务
     */
    private void initScheduledTasks() {
        //客户端是否需要获取服务中心的所有实例
        if (clientConfig.shouldFetchRegistry()) {
            //Server拉取服务注册信息的间隔时间
            int registryFetchIntervalSeconds = clientConfig.getRegistryFetchIntervalSeconds();
            //服务拉取任务 再次执行的最大延迟倍数
            int expBackOffBound = clientConfig.getCacheRefreshExecutorExponentialBackOffBound();
            scheduler.schedule(
                    new TimedSupervisorTask(
                            scheduler,
                            cacheRefreshExecutor,
                            registryFetchIntervalSeconds,
                            TimeUnit.SECONDS,
                            expBackOffBound,
                            new CacheRefreshThread()
                    ),
                    registryFetchIntervalSeconds, TimeUnit.SECONDS);
        }

        //客户端是否需要注册到注册中心中
        if (clientConfig.shouldRegisterWithEureka()) {
            //续租间隔时间
            int renewalIntervalInSecs = instanceInfo.getLeaseInfo().getRenewalIntervalInSecs();
            //心跳执行器 在续约过程中超时后的再次执行续约的最大延迟倍数。
            int expBackOffBound = clientConfig.getHeartbeatExecutorExponentialBackOffBound();
            logger.info("Starting heartbeat executor: " + "renew interval is: {}", renewalIntervalInSecs);

            // Heartbeat timer
            //维持心跳线程
            scheduler.schedule(
                    new TimedSupervisorTask(
                            scheduler,
                            heartbeatExecutor,
                            renewalIntervalInSecs,
                            TimeUnit.SECONDS,
                            expBackOffBound,
                            new HeartbeatThread()
                    ),
                    renewalIntervalInSecs, TimeUnit.SECONDS);
        }else {
            logger.info("Not registering with Eureka server per configuration");
        }

    }






    private boolean fetchRegistry(boolean forceFullRegistryFetch) {
        try {
            // If the delta is disabled or if it is the first time, get all
            // applications
            Applications applications = getApplications();
            if (clientConfig.shouldDisableDelta()  //如果增量被禁止 则 全量更新
                    || forceFullRegistryFetch
                    || (applications == null)
                    || (applications.getRegisteredApplications().size() == 0))
            {
                logger.info("Disable delta property : {}", clientConfig.shouldDisableDelta());
                logger.info("Force full registry fetch : {}", forceFullRegistryFetch);
                logger.info("Application is null : {}", (applications == null));
                logger.info("Registered Applications size is zero : {}",
                        (applications.getRegisteredApplications().size() == 0));
                //全量拉取
                getAndStoreFullRegistry();
            }else {
                //增量拉取
                getAndUpdateDelta(applications);
            }
        }catch (Throwable e){
            logger.error( "fetchRegistry was unable to refresh its cache! status = {}",e.getMessage(), e);
            return false;
        }

        return true;
    }


    /**
     * 全量拉取
     * @throws Throwable
     */
    private void getAndStoreFullRegistry() throws Throwable {
        long currentUpdateGeneration = fetchRegistryGeneration.get();

        logger.info("Getting all instance registry info from the eureka server");

        Applications apps = null;
        SDGHttpResponse<Applications> httpResponse = sdgHttpclient.getApplications(remoteRegionsRef.get());
        if (httpResponse.getStatusCode() == Response.Status.OK.getStatusCode()) {
            apps = httpResponse.getEntity();
        }
        logger.info("The response status is {}", httpResponse.getStatusCode());

        if (apps == null) {
            logger.error("The application is null for some reason. Not storing this information");
        }
        //多线程调用时可以保证只有一个线程可以将拉取的app存入本地缓存中！
        else if (fetchRegistryGeneration.compareAndSet(currentUpdateGeneration, currentUpdateGeneration + 1)) {
            //eureka 在放入缓存前， 将所的到的结果进行了洗牌
            localRegionApps.set(apps);
            logger.debug("Got full registry with apps ");
        } else {
            logger.warn("Not updating applications as another thread is updating it already");
        }
    }





    boolean register() throws Throwable {
        logger.info(PREFIX + "{}: registering service...", appPathIdentifier);
        SDGHttpResponse<Void> httpResponse;
        try {
            httpResponse = sdgHttpclient.register(instanceInfo);
        } catch (Exception e) {
            logger.warn(PREFIX + "{} - registration failed {}", appPathIdentifier, e.getMessage(), e);
            throw e;
        }
        if (logger.isInfoEnabled()) {
            logger.info(PREFIX + "{} - registration status: {}", appPathIdentifier, httpResponse.getStatusCode());
        }
        return httpResponse.getStatusCode() == Response.Status.OK.getStatusCode();
    }



    /**
     * 增量拉取
     * @param applications
     * @throws Throwable
     */
    private void getAndUpdateDelta(Applications applications) throws Throwable {
        long currentUpdateGeneration = fetchRegistryGeneration.get();

        Applications delta = null;
        SDGHttpResponse<Applications> httpResponse = sdgHttpclient.getDelta(remoteRegionsRef.get());
        if (httpResponse.getStatusCode() == Response.Status.OK.getStatusCode()) {
            delta = httpResponse.getEntity();
        }

        //如果拉取失败 那么进行一次全量拉取
        if (delta == null) {
            logger.warn("The server does not allow the delta revision to be applied because it is not safe. "
                    + "Hence got the full registry.");
            getAndStoreFullRegistry();
        } else if (fetchRegistryGeneration.compareAndSet(currentUpdateGeneration, currentUpdateGeneration + 1)) {
            logger.debug("Got delta update with apps hashcode ");
            synchronized (Discovery.class){
                updateDelta(delta);
            }
        } else {
            logger.warn("Not updating application delta as another thread is updating it already");
            logger.debug("Ignoring delta update  as another thread is updating it already");
        }
    }

    /**
     * 增量拉取时  保存到本地缓存
     * @param delta
     */
    private void updateDelta(Applications delta) {
        int deltaCount = 0;
        //遍历所有服务
        for (Application app : delta.getRegisteredApplications()) {
            //遍历当前服务的所有实例
            for (InstanceInfo instance : app.getInstances()) {
                //取出缓存的所有服务列表，用于合并
                Applications applications = getApplications();

                ++deltaCount;
                //对新增的实例的处理
                if (InstanceInfo.ActionType.ADDED.equals(instance.getActionType())) {
                    Application existingApp = applications.getRegisteredApplications(instance.getAppName());
                    if (existingApp == null) {
                        applications.addApplication(app);
                    }
                    logger.debug("Added instance {} to the existing apps in region ", instance.getId());
                    applications.getRegisteredApplications(instance.getAppName()).addInstance(instance);
                }
                //对修改的实例的处理
                else if (InstanceInfo.ActionType.MODIFIED.equals(instance.getActionType())) {
                    Application existingApp = applications.getRegisteredApplications(instance.getAppName());
                    if (existingApp == null) {
                        applications.addApplication(app);
                    }
                    logger.debug("Modified instance {} to the existing apps ", instance.getId());

                    applications.getRegisteredApplications(instance.getAppName()).addInstance(instance);

                }
                //对删除的实例的处理
                else if (InstanceInfo.ActionType.DELETED.equals(instance.getActionType())) {
                    Application existingApp = applications.getRegisteredApplications(instance.getAppName());
                    if (existingApp != null) {
                        logger.debug("Deleted instance {} to the existing apps ", instance.getId());
                        existingApp.removeInstance(instance);
                        /*
                         * We find all instance list from application(The status of instance status is not only the status is UP but also other status)
                         * if instance list is empty, we remove the application.
                         */
                        //如果删除完以后  application 为空了  那么就可以将整个 application删除
                        if (existingApp.getInstances().isEmpty()) {
                            applications.removeApplication(existingApp);
                        }
                    }
                }
            }
        }
        logger.debug("The total number of instances fetched by the delta processor : {}", deltaCount);


    }



    /**
     * The heartbeat task that renews the lease in the given intervals.
     */
    private class HeartbeatThread implements Runnable {

        public void run() {
            if (renew()) {
                lastSuccessfulHeartbeatTimestamp = System.currentTimeMillis();
            }
        }
    }

    boolean renew() {
        SDGHttpResponse<InstanceInfo> httpResponse;
        try {
            httpResponse = sdgHttpclient.sendHeartBeat(instanceInfo.getAppName(), instanceInfo.getId(), instanceInfo, null);
            logger.debug(PREFIX + "{} - Heartbeat status: {}", appPathIdentifier, httpResponse.getStatusCode());
            if (httpResponse.getStatusCode() == Response.Status.NOT_FOUND.getStatusCode()) {
                logger.info(PREFIX + "{} - Re-registering apps/{}", appPathIdentifier, instanceInfo.getAppName());
                long timestamp = instanceInfo.setIsDirtyWithTime();
                boolean success = register();
                if (success) {
                    instanceInfo.unsetIsDirty(timestamp);
                }
                return success;
            }
            return httpResponse.getStatusCode() == Response.Status.OK.getStatusCode();
        } catch (Throwable e) {
            logger.error(PREFIX + "{} - was unable to send heartbeat!", appPathIdentifier, e);
            return false;
        }
    }



    class CacheRefreshThread implements Runnable {
        public void run() {
            refreshRegistry();
        }
    }


    void refreshRegistry() {
        try {
            boolean success = fetchRegistry(false);
            if (success) {
                registrySize = localRegionApps.get().size();
                lastSuccessfulRegistryFetchTimestamp = System.currentTimeMillis();
                logger.debug("Completed cache refresh task for discovery. ");
            }

        } catch (Throwable e) {
            logger.error("Cannot fetch registry from server", e);
        }
    }



    @Override
    public Applications getApplications() {
        return localRegionApps.get();
    }

    @Override
    public Application getApplication(String appName) {
        return getApplications().getRegisteredApplications(appName);
    }

    @Override
    public List<InstanceInfo> getInstancesById(String id) {
        List<InstanceInfo> instancesList = new ArrayList<InstanceInfo>();
        for (Application app : this.getApplications()
                .getRegisteredApplications()) {
            InstanceInfo instanceInfo = app.getByInstanceId(id);
            if (instanceInfo != null) {
                instancesList.add(instanceInfo);
            }
        }
        return instancesList;
    }


    @Override
    public ClientConfig getEurekaClientConfig() {
        return clientConfig;
    }

    @PreDestroy
    @Override
    public void shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            logger.info("Shutting down DiscoveryClient ...");

            cancelScheduledTasks();
            // If APPINFO was registered
            if ( clientConfig.shouldRegisterWithEureka()
                    && clientConfig.shouldUnregisterOnShutdown()) {
                unregister();
            }

            logger.info("Completed shut down of DiscoveryClient");
        }
    }


    private void cancelScheduledTasks() {
        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdownNow();
        }
        if (cacheRefreshExecutor != null) {
            cacheRefreshExecutor.shutdownNow();
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    void unregister() {
        // It can be null if shouldRegisterWithEureka == false
        try {
            logger.info("Unregistering ...");
            SDGHttpResponse<Void> httpResponse = sdgHttpclient.cancel(instanceInfo.getAppName(), instanceInfo.getId());
            logger.info(PREFIX + "{} - deregister  status: {}", appPathIdentifier, httpResponse.getStatusCode());
        } catch (Exception e) {
            logger.error(PREFIX + "{} - de-registration failed{}", appPathIdentifier, e.getMessage(), e);
        }
    }
}
