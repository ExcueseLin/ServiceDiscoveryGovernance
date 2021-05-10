package cn.sunrain.SDG.share.entity;

import cn.sunrain.SDG.share.enums.InstanceStatus;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.*;

import java.io.Serializable;

/**
 * 实例
 * @author lin
 */
@Builder
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class InstanceInfo implements Serializable {

    /** 实例ID
     *  eureka.instance.instance-id
     *  = ${spring.cloud.client.ipAddress}:${spring.application.name}:${server.port}:@project.version@
     * （在eureka项目本身的配置是：eureka.instanceId = xxxxxxx） 这个只能是唯一的！ */
    @JSONField(name = "instanceId")
    private volatile String instanceId;
    /** 应用名
     * （同一应用可以有N多个实例） */
    @JSONField(name = "appName")
    private volatile String appName;

    /** 实例IP地址 */
    @JSONField(name = "ipAddr")
    private volatile String ipAddr;

    /** 实例端口号 */
    @JSONField(name = "port",serialize = false)
    private volatile int port;

    /** 主机名 */
    @JSONField(name = "hostName")
    private volatile String hostName;

    /** 实例状态 */
    @JSONField(name = "status")
    private volatile InstanceStatus status;

    /** 标注实例数据是否是脏的（client和server端对比）
     true：表示 InstanceInfo 在 Eureka-Client 和 Eureka-Server 数据不一致，需要注册。
     每次 InstanceInfo 发生属性变化时，以及InstanceInfo 刚被创建时，会标记此值是true
     当符合条件时，InstanceInfo 不会立即向 Eureka-Server 注册，
     而是后台线程定时注册（当然若开启了eureka.shouldOnDemandUpdateStatusChange = true时是立即注册）
     false：表示一致，不需要做额外动作
     */
    private volatile boolean isInstanceInfoDirty;

    /** 上次标记为Dirty的时间 */
    @JSONField(name = "lastDirtyTimestamp")
    private volatile Long lastDirtyTimestamp;

    /** 上一次修改时间 */
    @JSONField(name = "lastUpdatedTimestamp")
    private volatile Long lastUpdatedTimestamp;

    /** 续租信息 */
    @JSONField(name = "leaseInfo")
    private volatile LeaseInfo leaseInfo;

    private volatile ActionType actionType;

    public String getId() {
        if (instanceId != null && !instanceId.isEmpty()) {
            return instanceId;
        }
        return hostName;
    }

    public LeaseInfo getLeaseInfo() {
        return leaseInfo;
    }

    /**
     * Sets the lease information regarding when it expires.
     *
     * @param info the lease information of this instance.
     */
    public void setLeaseInfo(LeaseInfo info) {
        leaseInfo = info;
    }

    @Override
    public int hashCode() {
        String id = getId();
        return (id == null) ? 31 : (id.hashCode() + 31);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        InstanceInfo other = (InstanceInfo) obj;
        String id = getId();
        if (id == null) {
            if (other.getId() != null) {
                return false;
            }
        } else if (!id.equals(other.getId())) {
            return false;
        }
        return true;
    }

    public void setLastUpdatedTimestamp() {
        this.lastUpdatedTimestamp = System.currentTimeMillis();
    }


    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }


    /**
     * 设置dirty标志，并返回isDirty事件的时间戳
     * @return
     */
    public synchronized long setIsDirtyWithTime() {
        setIsDirty();
        return lastDirtyTimestamp;
    }


    /**
     * 如果unsetDirtyTimestamp与lastDirtyTimestamp匹配，则取消设置脏标志。
     * 如果lastDirtyTimestamp>unsetDirtyTimestamp，则无操作
     * @param unsetDirtyTimestamp
     */
    public synchronized void unsetIsDirty(long unsetDirtyTimestamp) {
        if (lastDirtyTimestamp <= unsetDirtyTimestamp) {
            isInstanceInfoDirty = false;
        } else {
        }
    }

    public synchronized void setIsDirty() {
        isInstanceInfoDirty = true;
        lastDirtyTimestamp = System.currentTimeMillis();
    }

    public enum ActionType {
        ADDED, // Added in the discovery server
        MODIFIED, // Changed in the discovery server
        DELETED
        // Deleted from the discovery server
    }




}
