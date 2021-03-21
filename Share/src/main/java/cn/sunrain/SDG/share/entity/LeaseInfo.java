package cn.sunrain.SDG.share.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.sun.xml.internal.ws.developer.Serialization;
import lombok.*;

import java.io.Serializable;

/**
 * 续租信息
 * 服务主要根据续租信息中的信息对服务进行状态的判断
 * @author lin
 */

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LeaseInfo implements Serializable {
    /** 默认值 */
    public static final int DEFAULT_LEASE_RENEWAL_INTERVAL = 30;
    public static final int DEFAULT_LEASE_DURATION = 90;

    /** 续租间隔时间（多长时间续约一次），默认是30s。 */
    private int renewalIntervalInSecs = DEFAULT_LEASE_RENEWAL_INTERVAL;
    /**续约持续时间（过期时间），默认是90s。90s倒计时，期间没有收到续约就会执行对应动作 */
    private int durationInSecs = DEFAULT_LEASE_DURATION;

    /** 租约的注册时间 */
    @JSONField(name = "registrationTimestamp")
    private long registrationTimestamp;
    /** 最近一次的续约时间（服务端记录，用于倒计时的起始值） */
    @JSONField(name = "lastRenewalTimestamp")
    private long lastRenewalTimestamp;
    /** 下线时间（服务的上、下线属于比较频繁的操作。但是此时服务实例并未T除去） */
    @JSONField(name = "evictionTimestamp")
    private long evictionTimestamp;
    /** 上线时间 */
    @JSONField(name = "serviceUpTimestamp")
    private long serviceUpTimestamp;

}
