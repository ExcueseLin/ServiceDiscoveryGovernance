package cn.sunrain.SDG.server.resource.registry;

import cn.sunrain.SDG.share.entity.Application;
import cn.sunrain.SDG.share.entity.Applications;
import cn.sunrain.SDG.share.entity.InstanceInfo;
import cn.sunrain.SDG.share.enums.InstanceStatus;

import java.util.List;

/**
 * @author lin
 * @date 2021/2/22 11:27
 */
public interface InstanceRegistry extends LeaseManager<InstanceInfo>{

    void openForTraffic( int count);

    void shutdown();

    boolean statusUpdate(String appName, String id, InstanceStatus newStatus,
                         String lastDirtyTimestamp, boolean isReplication);

    Applications getApplicationsFromLocalRegionOnly();

    List<Application> getSortedApplications();

    Application getApplication(String appName, boolean includeRemoteRegion);

    InstanceInfo getInstanceByAppAndId(String appName, String id);

    InstanceInfo getInstanceByAppAndId(String appName, String id, boolean includeRemoteRegions);


    /**
     * 获取上一分钟的续约数
     * */
    long getNumOfRenewsInLastMin();

    /**
     * 是否清理续约超时的服务
     * @return
     */
    boolean isLeaseExpirationEnabled();

    boolean isSelfPreservationModeEnabled();

}
