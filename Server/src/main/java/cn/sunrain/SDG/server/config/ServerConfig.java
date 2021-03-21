package cn.sunrain.SDG.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author lin
 * @date 2021/2/22 16:38
 */
@Component
@ConfigurationProperties(ServerConfig.PREFIX)
public class ServerConfig {

    public static final String PREFIX = "sunrain.sdg.server";
    private static final int MINUTES = 60 * 1000;

    /** 是否开启自我保护 */
    private boolean enableSelfPreservation = true;
    /**  自我保护阈值更新的时间间隔，单位为毫秒 */
    private int renewalThresholdUpdateIntervalMs = 15 * MINUTES;
    /** 客户端续约间隔 */
    private int expectedClientRenewalIntervalSeconds = 30;
    /** 自我保护续约百分比阀值因子 */
    private double renewalPercentThreshold = 0.85;

    /** 客户端保持增量信息缓存的时间，从而保证不会丢失这些信息，单位为毫秒 */
    private long retentionTimeInMSInDeltaQueue = 3 * MINUTES;

    /**  如果在远程区域本地没有实例运行，对于应用程序回退的旧行为是否被禁用， 默认为false */
    private boolean disableTransparentFallbackToOtherRegion;
    /**  过期实例应该启动并运行的时间间隔，单位为毫秒 */
    private long evictionIntervalTimerInMs = 60 * 1000;
    /**  清理任务程序被唤醒的时间间隔，清理过期的增量信息，单位为毫秒 */
    private long deltaRetentionTimerIntervalInMs = 30 * 1000;

    /** 当时间变化实例是否跟着同步，默认为true */
    private boolean syncWhenTimestampDiffers = true;


    public int getExpectedClientRenewalIntervalSeconds() {
        return this.expectedClientRenewalIntervalSeconds;
    }
    public void setExpectedClientRenewalIntervalSeconds(
            int expectedClientRenewalIntervalSeconds) {
        this.expectedClientRenewalIntervalSeconds = expectedClientRenewalIntervalSeconds;
    }


    public double getRenewalPercentThreshold() {
        return renewalPercentThreshold;
    }
    public void setRenewalPercentThreshold(double renewalPercentThreshold) {
        this.renewalPercentThreshold = renewalPercentThreshold;
    }

    public boolean shouldEnableSelfPreservation() {
        return this.enableSelfPreservation;
    }
    public boolean isEnableSelfPreservation() {
        return enableSelfPreservation;
    }
    public void setEnableSelfPreservation(boolean enableSelfPreservation) {
        this.enableSelfPreservation = enableSelfPreservation;
    }


    public long getRetentionTimeInMSInDeltaQueue() {
        return retentionTimeInMSInDeltaQueue;
    }
    public void setRetentionTimeInMSInDeltaQueue(long retentionTimeInMSInDeltaQueue) {
        this.retentionTimeInMSInDeltaQueue = retentionTimeInMSInDeltaQueue;
    }


    public boolean disableTransparentFallbackToOtherRegion() {
        return this.disableTransparentFallbackToOtherRegion;
    }
    public boolean isDisableTransparentFallbackToOtherRegion() {
        return disableTransparentFallbackToOtherRegion;
    }
    public void setDisableTransparentFallbackToOtherRegion(
            boolean disableTransparentFallbackToOtherRegion) {
        this.disableTransparentFallbackToOtherRegion = disableTransparentFallbackToOtherRegion;
    }


    public long getEvictionIntervalTimerInMs() {
        return evictionIntervalTimerInMs;
    }
    public void setEvictionIntervalTimerInMs(long evictionIntervalTimerInMs) {
        this.evictionIntervalTimerInMs = evictionIntervalTimerInMs;
    }


    public long getDeltaRetentionTimerIntervalInMs() {
        return deltaRetentionTimerIntervalInMs;
    }

    public void setDeltaRetentionTimerIntervalInMs(long deltaRetentionTimerIntervalInMs) {
        this.deltaRetentionTimerIntervalInMs = deltaRetentionTimerIntervalInMs;
    }


    public int getRenewalThresholdUpdateIntervalMs() {
        return renewalThresholdUpdateIntervalMs;
    }
    public void setRenewalThresholdUpdateIntervalMs(
            int renewalThresholdUpdateIntervalMs) {
        this.renewalThresholdUpdateIntervalMs = renewalThresholdUpdateIntervalMs;
    }

    public boolean shouldSyncWhenTimestampDiffers() {
        return this.syncWhenTimestampDiffers;
    }
    public boolean isSyncWhenTimestampDiffers() {
        return syncWhenTimestampDiffers;
    }
    public void setSyncWhenTimestampDiffers(boolean syncWhenTimestampDiffers) {
        this.syncWhenTimestampDiffers = syncWhenTimestampDiffers;
    }
}
