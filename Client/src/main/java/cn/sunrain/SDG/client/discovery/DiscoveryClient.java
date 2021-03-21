package cn.sunrain.SDG.client.discovery;

import cn.sunrain.SDG.client.config.ClientConfig;
import cn.sunrain.SDG.share.entity.Applications;
import cn.sunrain.SDG.share.entity.InstanceInfo;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author lin
 * @date 2021/3/4 13:26
 */
public class DiscoveryClient implements Discovery {

    private static final Logger logger = LoggerFactory.getLogger(DiscoveryClient.class);

    private static final String VALUE_DELIMITER = ",";


    protected final ClientConfig clientConfig;

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

    // 服务初始化时间戳
    private final long initTimestampMs;

    DiscoveryClient(ClientConfig config,InstanceInfo instanceInfo) {
        this.clientConfig = config;

        this.instanceInfo = instanceInfo;

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

        //服务发现
        //如果需要拉去服务中心中的服务 那么初始化时 就拉取一次
        //全局拉取基本就这一次！  后面都是增量拉取
        if (clientConfig.shouldFetchRegistry() ) {
            fetchRegistry(false);
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
            int registryFetchIntervalSeconds = clientConfig.getRegistryFetchIntervalSeconds();
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
            int renewalIntervalInSecs = instanceInfo.getLeaseInfo().getRenewalIntervalInSecs();
            int expBackOffBound = clientConfig.getHeartbeatExecutorExponentialBackOffBound();
            logger.info("Starting heartbeat executor: " + "renew interval is: {}", renewalIntervalInSecs);

            // Heartbeat timer
            scheduler.schedule(
                    new TimedSupervisorTask(
                            "heartbeat",
                            scheduler,
                            heartbeatExecutor,
                            renewalIntervalInSecs,
                            TimeUnit.SECONDS,
                            expBackOffBound,
                            new HeartbeatThread()
                    ),
                    renewalIntervalInSecs, TimeUnit.SECONDS);
        }

    }

}
