package cn.sunrain.SDG.server.resource.registry;


import cn.sunrain.SDG.server.config.ServerConfig;
import cn.sunrain.SDG.server.lease.Lease;
import cn.sunrain.SDG.server.lease.RecentlyChangedItem;
import cn.sunrain.SDG.server.util.MeasuredRate;
import cn.sunrain.SDG.share.entity.Application;
import cn.sunrain.SDG.share.entity.Applications;
import cn.sunrain.SDG.share.entity.InstanceInfo;
import cn.sunrain.SDG.share.entity.LeaseInfo;
import cn.sunrain.SDG.share.enums.InstanceStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author lin
 * @date 2021/2/22 13:39
 */
@Component
public  class AbstractInstanceRegistry
        implements InstanceRegistry {

    private static final Logger logger = LoggerFactory.getLogger(AbstractInstanceRegistry.class);
    private static final String[] EMPTY_STR_ARRAY = new String[0];

    //实际缓存  存储所有InstanceInfo
    //第一层Map中 key 是InstanceInfo的Name    第二层Map中 key是InstanceInfo的Id
    private static final ConcurrentHashMap<String, Map<String, Lease<InstanceInfo>>> registry
            = new ConcurrentHashMap<String, Map<String, Lease<InstanceInfo>>>();

    /**  保存最近变更的实例信息
     *   如果每次心跳都全量获取实例信息，集群庞大的话，效率较低，存在大量重复的信息，
     *   浪费带宽，所以通过增量获取已变更的实例信息会是一个更好的选择，
     * */
    private ConcurrentLinkedQueue<RecentlyChangedItem> recentlyChangedQueue = new ConcurrentLinkedQueue<RecentlyChangedItem>();
    private final static Object lock = new Object();

    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock read = readWriteLock.readLock();
    private final Lock write = readWriteLock.writeLock();

    /** 所有已知的服务端地址 */
    protected String[] allKnownRemoteRegions = EMPTY_STR_ARRAY;
    /**自我保护阀值  = 服务总数 * 每分钟续约数 * 自我保护续约百分比阀值因子。*/
    protected volatile int numberOfRenewsPerMinThreshold;
    /**服务总数  期待客户端发送的最大续约数*/
    protected volatile int expectedNumberOfClientsSendingRenews;

    private final MeasuredRate renewsLastMin;
    /** 定时清理增量队列中的数据 */
    private Timer deltaRetentionTimer = new Timer("DeltaRetentionTimer", true);
    /** 定时剔除下线服务 */
    private Timer evictionTimer = new Timer("EvictionTimer", true);
    /** 定时计算自我保护阈值 */
    private Timer scheduleRenewalThresholdUpdateTimer = new Timer("ReplicaAwareInstanceRegistry - RenewalThresholdUpdater", true);
    private final AtomicReference<EvictionTask> evictionTaskRef = new AtomicReference<EvictionTask>();


    private ServerConfig serverConfig;


    public AbstractInstanceRegistry(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
        this.renewsLastMin = new MeasuredRate(1000 * 60 * 1);
        scheduleRenewalThresholdUpdateTask();
        this.deltaRetentionTimer.schedule(getDeltaRetentionTask(),
                serverConfig.getDeltaRetentionTimerIntervalInMs(),
                serverConfig.getDeltaRetentionTimerIntervalInMs());
    }

    protected void postInit() {
        renewsLastMin.start();
        if (evictionTaskRef.get() != null) {
            evictionTaskRef.get().cancel();
        }
        evictionTaskRef.set(new EvictionTask());
        evictionTimer.schedule(evictionTaskRef.get(),
                serverConfig.getEvictionIntervalTimerInMs(),
                serverConfig.getEvictionIntervalTimerInMs());
    }

    @Override
    public void openForTraffic(int count) {
        this.expectedNumberOfClientsSendingRenews = count;
        updateRenewsPerMinThreshold();
    }

    @Override
    public void shutdown() {

    }

    @Override
    public void register(InstanceInfo registrant, int leaseDuration, boolean isReplication) {
        try {
            synchronized (lock) {
                Map<String, Lease<InstanceInfo>> gMap = registry.get(registrant.getAppName());
                if (gMap == null) {
                    final ConcurrentHashMap<String, Lease<InstanceInfo>> gNewMap = new ConcurrentHashMap<String, Lease<InstanceInfo>>();
                    gMap = registry.putIfAbsent(registrant.getAppName(), gNewMap);
                    if (gMap == null) {
                        gMap = gNewMap;
                    }
                }

                Lease<InstanceInfo> existingLease = gMap.get(registrant.getId());
                //如果存在名字与ID相同的服务实例 那么对该服务实的修改时间与注册服务的修改时间进行判断
                //防止多线程操作时出现线程安全问题
                if (existingLease != null && (existingLease.getHolder() != null)) {
                    //已经存在服务的上一次修改时间
                    Long existingLastDirtyTimestamp = existingLease.getHolder().getLastDirtyTimestamp();
                    //现要注册服务的上一次修改时间
                    Long registrationLastDirtyTimestamp = registrant.getLastDirtyTimestamp();
                    if (existingLastDirtyTimestamp > registrationLastDirtyTimestamp) {
                        logger.warn("已存在的租约时间戳 {} 比 现注册租约的脏时间戳 {}  更大", existingLastDirtyTimestamp, registrationLastDirtyTimestamp);
                        logger.warn("所以仍然使用已有的租约，而不是现要注册的租约");
                        registrant = existingLease.getHolder();
                    }
                } else {
                    //如果这个服务实例并不存在过 那么代表这是完全新的服务实例
                    if (this.expectedNumberOfClientsSendingRenews > 0) {
                        // 因为注册了一个完全新的服务实例 那么服务的总个数就要加一 并重新计算自我保护阈值
                        this.expectedNumberOfClientsSendingRenews = this.expectedNumberOfClientsSendingRenews + 1;
                        updateRenewsPerMinThreshold();
                    }
                }
                Lease<InstanceInfo> lease = new Lease<InstanceInfo>(registrant, leaseDuration);
                if (existingLease != null) {
                    //如果已存在一个租约 那么需要注册的服务实例的上线时间 就是已存在实例的上线时间
                    lease.setServiceUpTimestamp(existingLease.getServiceUpTimestamp());
                }
                gMap.put(registrant.getId(), lease);

                // If the lease is registered with UP status, set lease service up timestamp
                if (InstanceStatus.UP.equals(registrant.getStatus())) {
                    lease.serviceUp();
                }
                registrant.setActionType(InstanceInfo.ActionType.ADDED);
                recentlyChangedQueue.add(new RecentlyChangedItem(lease));
                registrant.setLastUpdatedTimestamp();
                logger.info("注册 服务实例 {}/{} 服务状态为 :  {} (是否是来自其他服务节点的实例={})",
                        registrant.getAppName(), registrant.getId(), registrant.getStatus(), isReplication);
            }
        } catch (Exception e) {
            logger.error("服务实例注册失败，出现系统故障，故障问题如下:");
            logger.error(e.getLocalizedMessage());
        }
    }


    @Override
    public boolean cancel(String appName, String id, boolean isReplication) {
        return internalCancel(appName, id, isReplication);
    }

    protected boolean internalCancel(String appName, String id, boolean isReplication) {
        try {
            synchronized (lock) {
                Map<String, Lease<InstanceInfo>> gMap = registry.get(appName);
                Lease<InstanceInfo> leaseToCancel = null;
                if (gMap != null) {
                    leaseToCancel = gMap.remove(id);
                }
                if (leaseToCancel == null) {
                    logger.warn("DS: 注册表：取消失败，因为没有该注册租约: {}/{}", appName, id);
                    return false;
                } else {
                    leaseToCancel.cancel();
                    InstanceInfo instanceInfo = leaseToCancel.getHolder();
                    if (instanceInfo != null) {
                        instanceInfo.setActionType(InstanceInfo.ActionType.DELETED);
                        recentlyChangedQueue.add(new RecentlyChangedItem(leaseToCancel));
                        instanceInfo.setLastUpdatedTimestamp();
                    }
                }
                logger.info("服务下线 {}/{} (replication={})", appName, id, isReplication);
                return true;
            }
        } catch (Exception e) {
            logger.error("服务实例下线失败，出现系统故障，故障问题如下:");
            logger.error(e.getLocalizedMessage());
            return false;
        }
    }


    @Override
    public boolean renew(String appName, String id, boolean isReplication) {
        try {
            Map<String, Lease<InstanceInfo>> gMap = registry.get(appName);
            Lease<InstanceInfo> leaseToRenew = null;
            if (gMap != null) {
                leaseToRenew = gMap.get(id);
            }
            if (leaseToRenew == null) {
                logger.warn("DS: 注册表续约失败，因为没有该注册租约: {} - {}", appName, id);
                return false;
            } else {
                renewsLastMin.increment();
                leaseToRenew.renew();
                return true;
            }
        } catch (Exception e) {
            logger.error("服务实例续约失败，出现系统故障，故障问题如下:");
            logger.error(e.getLocalizedMessage());
            return false;
        }
    }


    @Override
    public void evict() {
        evict(0l);
    }

    /**
     * @param additionalLeaseMs 补偿时间 比如gc消耗的时间，或者时钟偏差
     *                          判断过期是  当前时间是否大于 上一次续约时间+心跳间隔时间+补偿时间
     */
    public void evict(long additionalLeaseMs) {
        logger.debug("Running the evict task");

        if (!isLeaseExpirationEnabled()) {
            logger.debug("DS: lease expiration is currently disabled.");
            return;
        }

        /*
         *首先收集所有过期的实例集合，然后以随机的顺序清除，
         *如果不这么做的话，当大批量清除时，还没等到自我保护起作用，
         *可能就已经把所有应用都剔除掉啦，随机化逐出，
         *将影响均匀分摊在了整个应用里
         */
        List<Lease<InstanceInfo>> expiredLeases = new ArrayList<>();
        for (Map.Entry<String, Map<String, Lease<InstanceInfo>>> groupEntry : registry.entrySet()) {
            Map<String, Lease<InstanceInfo>> leaseMap = groupEntry.getValue();
            if (leaseMap != null) {
                for (Map.Entry<String, Lease<InstanceInfo>> leaseEntry : leaseMap.entrySet()) {
                    Lease<InstanceInfo> lease = leaseEntry.getValue();
                    //找出过期的节点，最后一次续约时间加上过期时间再加上补偿时间是否小于当前时间，小则过期啦
                    if (lease.isExpired(additionalLeaseMs) && lease.getHolder() != null) {
                        expiredLeases.add(lease);
                    }
                }
            }
        }


        // To compensate for GC pauses or drifting local time, we need to use current registry size as a base for
        // triggering self-preservation. Without that we would wipe out full registry.
        //为了补偿GC暂停或本地时间漂移，我们需要使用当前注册表大小作为触发自我保护。否则，我们将消灭全部注册。
        int registrySize = (int) getLocalRegistrySize();
        int registrySizeThreshold = (int) (registrySize * serverConfig.getRenewalPercentThreshold());
        int evictionLimit = registrySize - registrySizeThreshold;
        int toEvict = Math.min(expiredLeases.size(), evictionLimit);
        if (toEvict > 0) {
            logger.info("Evicting {} items (expired={}, evictionLimit={})", toEvict, expiredLeases.size(), evictionLimit);

            //下面就是随机化删除的体现！！
            Random random = new Random(System.currentTimeMillis());
            for (int i = 0; i < toEvict; i++) {
                // Pick a random item (Knuth shuffle algorithm)
                int next = i + random.nextInt(expiredLeases.size() - i);
                Collections.swap(expiredLeases, i, next);
                Lease<InstanceInfo> lease = expiredLeases.get(i);

                String appName = lease.getHolder().getAppName();
                String id = lease.getHolder().getId();

                logger.warn("DS: Registry: expired lease for {}/{}", appName, id);
                internalCancel(appName, id, false);
            }
        }
    }

    /**
     * 检查是否启用租约到期。
     */
    @Override
    public boolean isLeaseExpirationEnabled() {
        //类似Eureka 是否开启保护机制，
        //如果没有开启，那么直接返回true 直接开始清理续约过期的服务
        if (!isSelfPreservationModeEnabled()) {
            // The self preservation mode is disabled, hence allowing the instances to expire.
            return true;
        }
        //如果保护模式打开啦，则如果最后一分钟续约的数量大于计算的阈值，则允许，否则不允许清除
        return numberOfRenewsPerMinThreshold > 0 && getNumOfRenewsInLastMin() > numberOfRenewsPerMinThreshold;
    }

    @Override
    public boolean isSelfPreservationModeEnabled() {
        return serverConfig.shouldEnableSelfPreservation();
    }

    protected void updateRenewsPerMinThreshold() {
        //自我保护阀值 = 服务总数 * 每分钟续约数 * 自我保护续约百分比阀值因子。
        this.numberOfRenewsPerMinThreshold = (int) (this.expectedNumberOfClientsSendingRenews
                * (60.0 / serverConfig.getExpectedClientRenewalIntervalSeconds())
                * serverConfig.getRenewalPercentThreshold());
    }

    @Override
    public long getNumOfRenewsInLastMin() {
        return renewsLastMin.getCount();
    }

    /**
     * 获取缓存中的服务实例个数
     * @return
     */
    public long getLocalRegistrySize() {
        long total = 0;
        for (Map<String, Lease<InstanceInfo>> entry : registry.values()) {
            total += entry.size();
        }
        return total;
    }


    public Applications getApplications() {
        boolean disableTransparentFallback = serverConfig.disableTransparentFallbackToOtherRegion();
        if (disableTransparentFallback) {
            return getApplicationsFromLocalRegionOnly();
        } else {
            return getApplicationsFromAllRemoteRegions();  // Behavior of falling back to remote region can be disabled.
        }
    }

    public Applications getApplicationsFromAllRemoteRegions() {
        return getApplicationsFromMultipleRegions(allKnownRemoteRegions);
    }
    @Override
    public Applications getApplicationsFromLocalRegionOnly() {
        return getApplicationsFromMultipleRegions(EMPTY_STR_ARRAY);
    }

    public Applications getApplicationsFromMultipleRegions(String[] remoteRegions) {
        //判断是否需要去其他服务端获取实例
        boolean includeRemoteRegion = null != remoteRegions && remoteRegions.length != 0;

        logger.debug("Fetching applications registry with remote regions: {}, Regions argument {}",
                includeRemoteRegion, remoteRegions);
        Applications apps = new Applications();
        //先获取本地缓存中的实例
        for (Map.Entry<String, Map<String, Lease<InstanceInfo>>> entry : registry.entrySet()) {
            Application app = null;
            if (entry.getValue() != null) {
                for (Map.Entry<String, Lease<InstanceInfo>> stringLeaseEntry : entry.getValue().entrySet()) {
                    Lease<InstanceInfo> lease = stringLeaseEntry.getValue();
                    if (app == null) {
                        app = new Application(lease.getHolder().getAppName());
                    }
                    app.addInstance(decorateInstanceInfo(lease));
                }
            }
            if (app != null) {
                apps.addApplication(app);
            }
        }

        return apps;
    }


    private InstanceInfo decorateInstanceInfo(Lease<InstanceInfo> lease) {
        InstanceInfo info = lease.getHolder();

        // client app settings
        int renewalInterval = LeaseInfo.DEFAULT_LEASE_RENEWAL_INTERVAL;
        int leaseDuration = LeaseInfo.DEFAULT_LEASE_DURATION;

        // TODO: clean this up
        if (info.getLeaseInfo() != null) {
            renewalInterval = info.getLeaseInfo().getRenewalIntervalInSecs();
            leaseDuration = info.getLeaseInfo().getDurationInSecs();
        }

        info.setLeaseInfo(LeaseInfo.builder()
                .registrationTimestamp(lease.getRegistrationTimestamp())
                .lastRenewalTimestamp(lease.getLastRenewalTimestamp())
                .serviceUpTimestamp(lease.getServiceUpTimestamp())
                .renewalIntervalInSecs(renewalInterval)
                .durationInSecs(leaseDuration)
                .evictionTimestamp(lease.getEvictionTimestamp()).build());

        return info;
    }


    public Applications getApplicationDeltas() {
        Applications apps = new Applications();
        Map<String, Application> applicationInstancesMap = new HashMap<String, Application>();
        try {
            write.lock();
            Iterator<RecentlyChangedItem> iter = this.recentlyChangedQueue.iterator();
            logger.debug("The number of elements in the delta queue is : {}",
                    this.recentlyChangedQueue.size());
            while (iter.hasNext()) {
                Lease<InstanceInfo> lease = iter.next().getLeaseInfo();
                InstanceInfo instanceInfo = lease.getHolder();
                logger.debug(
                        "The instance id {} is found with status {} and actiontype {}",
                        instanceInfo.getId(), instanceInfo.getStatus().name(), instanceInfo.getActionType().name());
                Application app = applicationInstancesMap.get(instanceInfo
                        .getAppName());
                if (app == null) {
                    app = new Application(instanceInfo.getAppName());
                    applicationInstancesMap.put(instanceInfo.getAppName(), app);
                    apps.addApplication(app);
                }
                InstanceInfo instance = new InstanceInfo();
                BeanUtils.copyProperties(instance,decorateInstanceInfo(lease));
                app.addInstance(instance);
            }

            boolean disableTransparentFallback = serverConfig.disableTransparentFallbackToOtherRegion();

            if (!disableTransparentFallback) {

            }
            return apps;
        } finally {
            write.unlock();
        }
    }


    @Override
    public List<Application> getSortedApplications() {
        List<Application> apps = new ArrayList<Application>(getApplications().getRegisteredApplications());
        apps.sort(Comparator.comparing(Application::getName));
        return apps;
    }


    @Override
    public Application getApplication(String appName, boolean includeRemoteRegion) {
        Application app = null;
        //先查询本地缓存的实例
        Map<String, Lease<InstanceInfo>> leaseMap = registry.get(appName);
        if (leaseMap != null && leaseMap.size() > 0) {
            for (Map.Entry<String, Lease<InstanceInfo>> entry : leaseMap.entrySet()) {
                if (app == null) {
                    app = new Application(appName);
                }
                app.addInstance(decorateInstanceInfo(entry.getValue()));
            }
        }else if (includeRemoteRegion) {}
        return app;
    }

    @Override
    public InstanceInfo getInstanceByAppAndId(String appName, String id) {
        return this.getInstanceByAppAndId(appName, id, true);
    }

    @Override
    public InstanceInfo getInstanceByAppAndId(String appName, String id, boolean includeRemoteRegions) {
        Map<String, Lease<InstanceInfo>> leaseMap = registry.get(appName);
        Lease<InstanceInfo> lease = null;
        if (leaseMap != null) {
            lease = leaseMap.get(id);
        }
        if (lease != null
                && (!isLeaseExpirationEnabled() || !lease.isExpired())) {
            return decorateInstanceInfo(lease);
        } else if (includeRemoteRegions) {

        }
        return null;
    }



    public boolean statusUpdate(String appName, String id,
                                InstanceStatus newStatus, String lastDirtyTimestamp,
                                boolean isReplication) {
        try {
            synchronized (lock){
                Map<String, Lease<InstanceInfo>> gMap = registry.get(appName);
                Lease<InstanceInfo> lease = null;
                if (gMap != null) {
                    lease = gMap.get(id);
                }
                if (lease == null) {
                    return false;
                } else {
                    lease.renew();
                    InstanceInfo info = lease.getHolder();
                    if (info == null) {
                        logger.error("Found Lease without a holder for instance id {}", id);
                    }
                    if ((info != null) && !(info.getStatus().equals(newStatus))) {
                        if (InstanceStatus.UP.equals(newStatus)) {
                            lease.serviceUp();
                        }
                        info.setStatus(newStatus);
                        long replicaDirtyTimestamp = 0;
                        if (lastDirtyTimestamp != null) {
                            replicaDirtyTimestamp = Long.parseLong(lastDirtyTimestamp);
                        }
                        //如果复制的脏时间戳大于现有时间戳，就需更新。
                        if (replicaDirtyTimestamp > info.getLastDirtyTimestamp()) {
                            info.setLastDirtyTimestamp(replicaDirtyTimestamp);
                        }
                        info.setActionType(InstanceInfo.ActionType.MODIFIED);
                        recentlyChangedQueue.add(new RecentlyChangedItem(lease));
                        info.setLastUpdatedTimestamp();
                    }
                    return true;
                }
            }
        }catch (Exception e){
            logger.error("服务实例下线失败，出现系统故障，故障问题如下:");
            logger.error(e.getLocalizedMessage());
            return false;
        }
    }




    private TimerTask getDeltaRetentionTask() {
        return new TimerTask() {

            @Override
            public void run() {
                Iterator<RecentlyChangedItem> it = recentlyChangedQueue.iterator();
                while (it.hasNext()) {
                    //移除超过3分钟的数据
                    if (it.next().getLastUpdateTime() <
                            System.currentTimeMillis() - serverConfig.getRetentionTimeInMSInDeltaQueue()) {
                        it.remove();
                    } else {
                        break;
                    }
                }
            }
        };
    }

    private void scheduleRenewalThresholdUpdateTask() {
        scheduleRenewalThresholdUpdateTimer.schedule(new TimerTask() {
                           @Override
                           public void run() {
                               updateRenewalThreshold();
                           }
                       }, serverConfig.getRenewalThresholdUpdateIntervalMs(),
                serverConfig.getRenewalThresholdUpdateIntervalMs());
    }
    private void updateRenewalThreshold() {
        try {
            Applications apps = getApplications();
            int count = 0;
            for (Application app : apps.getRegisteredApplications()) {
                for (InstanceInfo instance : app.getInstances()) {
                    ++count;
                }
            }
            synchronized (lock) {
                // Update threshold only if the threshold is greater than the
                // current expected threshold or if self preservation is disabled.
                if ((count) > (serverConfig.getRenewalPercentThreshold() * expectedNumberOfClientsSendingRenews)
                        || (!this.isSelfPreservationModeEnabled())) {
                    this.expectedNumberOfClientsSendingRenews = count;
                    updateRenewsPerMinThreshold();
                }
            }
            logger.info("Current renewal threshold is : {}", numberOfRenewsPerMinThreshold);
        } catch (Throwable e) {
            logger.error("Cannot update renewal threshold", e);
        }
    }

    /**
     * 过期节点清理任务
     */
    class EvictionTask extends TimerTask {

        private final AtomicLong lastExecutionNanosRef = new AtomicLong(0L);

        @Override
        public void run() {
            try {
                //获取补偿时间，比如gc消耗的时间，或者时钟偏差
                long compensationTimeMs = getCompensationTimeMs();
                logger.info("Running the evict task with compensationTime {}ms", compensationTimeMs);
                evict(compensationTimeMs);
            } catch (Throwable e) {
                logger.error("Could not run the evict task", e);
            }
        }

        /**
         * compute a compensation time defined as the actual time this task was executed since the prev iteration,
         * vs the configured amount of time for execution. This is useful for cases where changes in time (due to
         * clock skew or gc for example) causes the actual eviction task to execute later than the desired time
         * according to the configured cycle.
         */
        long getCompensationTimeMs() {
            long currNanos = getCurrentTimeNano();
            //最后一次执行时间
            long lastNanos = lastExecutionNanosRef.getAndSet(currNanos);
            if (lastNanos == 0L) {
                return 0L;
            }
            //当前时间与最后一次时间的时间差
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(currNanos - lastNanos);
            //时间差与执行时间间隔的差值即为需要补偿的时间
            long compensationTime = elapsedMs - serverConfig.getEvictionIntervalTimerInMs();
            return Math.max(compensationTime, 0L);
        }

        long getCurrentTimeNano() {  // for testing
            return System.nanoTime();
        }

    }


    public static ConcurrentHashMap<String, Map<String, Lease<InstanceInfo>>> getRegistry(){
        return registry;
    }
}

