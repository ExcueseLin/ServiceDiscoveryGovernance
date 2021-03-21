package cn.sunrain.SDG.client.config;

/**
 * @author lin
 * @date 2021/3/4 13:22
 */
public class ClientConfig {

    /** 实例是否在eureka服务器上注册自己的信息以供其他服务发现，默认为true  */
    private boolean registerWithEureka = true;

    /** 此客户端是否获取eureka服务器注册表上的注册信息，默认为true */
    private boolean fetchRegistry = true;

    /** 心跳执行程序线程池的大小 默认为2 */
    private int heartbeatExecutorThreadPoolSize = 2;

    /** 执行程序缓存刷新线程池的大小，默认为2 */
    private int cacheRefreshExecutorThreadPoolSize = 2;

    /** 是否在初始化过程中注册服务。 */
    private boolean shouldEnforceRegistrationAtInit = false;

    /** 定时从Eureka Server拉取服务注册信息的间隔时间 */
    private int registryFetchIntervalSeconds = 30;
    /** 服务拉取任务（cacheRefreshExecutord）再次执行的最大延迟倍数。默认最大延迟倍数为10 */
    private int cacheRefreshExecutorExponentialBackOffBound = 10;

    /** 心跳执行器 (scheduler) 在续约过程中超时后的再次执行续约的最大延迟倍数。默认最大延迟倍数10 */
    private int heartbeatExecutorExponentialBackOffBound = 10;


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

}
