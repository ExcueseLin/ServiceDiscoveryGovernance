package cn.sunrain.SDG.client.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import static cn.sunrain.SDG.share.constants.SDGConstants.DEFAULT_PREFIX;

/**
 * @author lin
 * @date 2021/3/4 13:22
 */
@ConfigurationProperties(ClientConfig.PREFIX)
public class ClientConfig {

    public static final String PREFIX = "sdg.client";

    /**
     * Default Eureka URL.
     */
    public static final String DEFAULT_URL = "http://localhost:9001" + DEFAULT_PREFIX
            + "/";

    /**
     * Default availability zone if none is resolved based on region.
     */
    public static final String DEFAULT_ZONE = "defaultZone";

    /** 实例是否在eureka服务器上注册自己的信息以供其他服务发现，默认为true  */
    private boolean registerWithEureka = true;

    /** 此客户端是否获取eureka服务器注册表上的注册信息，默认为true */
    private boolean fetchRegistry = true;

    /** 心跳执行程序线程池的大小 默认为2 */
    private int heartbeatExecutorThreadPoolSize = 2;

    /** 执行程序缓存刷新线程池的大小，默认为2 */
    private int cacheRefreshExecutorThreadPoolSize = 2;

    /** 是否在初始化过程中注册服务。 */
    private boolean shouldEnforceRegistrationAtInit = true;

    /** 定时从Eureka Server拉取服务注册信息的间隔时间 */
    private int registryFetchIntervalSeconds = 30;
    /** 服务拉取任务（cacheRefreshExecutord）再次执行的最大延迟倍数。默认最大延迟倍数为10 */
    private int cacheRefreshExecutorExponentialBackOffBound = 10;

    /** 心跳执行器 (scheduler) 在续约过程中超时后的再次执行续约的最大延迟倍数。默认最大延迟倍数10 */
    private int heartbeatExecutorExponentialBackOffBound = 10;

    /** 增量信息是否可以提供给客户端 。而应求助于获取完整的注册表信息 。 默认false */
    private boolean disableDelta =  false;

    /** 应用实例开启关闭时下线开关。默认为 true  */
    private boolean shouldUnregisterOnShutdown = true;


    private String fetchRemoteRegionsRegistry;


    private String serverUrls;

    public String getServerUrls() {
        return serverUrls;
    }
    public void setServerUrls(String serverUrls) {
        this.serverUrls = serverUrls;
    }

    public boolean shouldRegisterWithEureka() {
        return this.registerWithEureka;
    }
    public boolean isRegisterWithEureka() {
        return registerWithEureka;
    }
    public void setRegisterWithEureka(boolean registerWithEureka) {
        this.registerWithEureka = registerWithEureka;
    }

    public boolean shouldFetchRegistry() {
        return this.fetchRegistry;
    }
    public boolean isFetchRegistry() {
        return fetchRegistry;
    }
    public void setFetchRegistry(boolean fetchRegistry) {
        this.fetchRegistry = fetchRegistry;
    }


    public int getHeartbeatExecutorThreadPoolSize() {
        return heartbeatExecutorThreadPoolSize;
    }
    public void setHeartbeatExecutorThreadPoolSize(int heartbeatExecutorThreadPoolSize) {
        this.heartbeatExecutorThreadPoolSize = heartbeatExecutorThreadPoolSize;
    }

    public int getCacheRefreshExecutorThreadPoolSize() {
        return cacheRefreshExecutorThreadPoolSize;
    }
    public void setCacheRefreshExecutorThreadPoolSize(int cacheRefreshExecutorThreadPoolSize) {
        this.cacheRefreshExecutorThreadPoolSize = cacheRefreshExecutorThreadPoolSize;
    }

    public boolean shouldEnforceRegistrationAtInit() {
        return this.shouldEnforceRegistrationAtInit;
    }
    public boolean isShouldEnforceRegistrationAtInit() {
        return shouldEnforceRegistrationAtInit;
    }
    public void setShouldEnforceRegistrationAtInit(boolean shouldEnforceRegistrationAtInit) {
        this.shouldEnforceRegistrationAtInit = shouldEnforceRegistrationAtInit;
    }

    public int getRegistryFetchIntervalSeconds() {
        return registryFetchIntervalSeconds;
    }
    public void setRegistryFetchIntervalSeconds(int registryFetchIntervalSeconds) {
        this.registryFetchIntervalSeconds = registryFetchIntervalSeconds;
    }

    public int getCacheRefreshExecutorExponentialBackOffBound() {
        return cacheRefreshExecutorExponentialBackOffBound;
    }
    public void setCacheRefreshExecutorExponentialBackOffBound(int cacheRefreshExecutorExponentialBackOffBound) {
        this.cacheRefreshExecutorExponentialBackOffBound = cacheRefreshExecutorExponentialBackOffBound;
    }

    public int getHeartbeatExecutorExponentialBackOffBound() {
        return heartbeatExecutorExponentialBackOffBound;
    }

    public void setHeartbeatExecutorExponentialBackOffBound(int heartbeatExecutorExponentialBackOffBound) {
        this.heartbeatExecutorExponentialBackOffBound = heartbeatExecutorExponentialBackOffBound;
    }

    public boolean shouldDisableDelta() {
        return this.disableDelta;
    }
    public boolean isDisableDelta() {
        return disableDelta;
    }
    public void setDisableDelta(boolean disableDelta) {
        this.disableDelta = disableDelta;
    }

    public boolean shouldUnregisterOnShutdown() {
        return this.shouldUnregisterOnShutdown;
    }
    public boolean isShouldUnregisterOnShutdown() {
        return shouldUnregisterOnShutdown;
    }
    public void setShouldUnregisterOnShutdown(boolean shouldUnregisterOnShutdown) {
        this.shouldUnregisterOnShutdown = shouldUnregisterOnShutdown;
    }


    public String getFetchRemoteRegionsRegistry() {
        return fetchRemoteRegionsRegistry;
    }
    public void setFetchRemoteRegionsRegistry(String fetchRemoteRegionsRegistry) {
        this.fetchRemoteRegionsRegistry = fetchRemoteRegionsRegistry;
    }

}
