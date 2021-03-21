package cn.sunrain.SDG.server.lease;

import cn.sunrain.SDG.share.entity.InstanceInfo;

/**
 * @author lin
 * @date 2021/2/23 11:48
 */
public class RecentlyChangedItem {

    private long lastUpdateTime;
    private Lease<InstanceInfo> leaseInfo;

    public RecentlyChangedItem(Lease<InstanceInfo> lease) {
        this.leaseInfo = lease;
        lastUpdateTime = System.currentTimeMillis();
    }

    public long getLastUpdateTime() {
        return this.lastUpdateTime;
    }

    public Lease<InstanceInfo> getLeaseInfo() {
        return this.leaseInfo;
    }

}
