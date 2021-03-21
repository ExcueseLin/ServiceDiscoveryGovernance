package cn.sunrain.SDG.server.resource.registry;

/**
 * @author lin
 * @date 2021/2/22 11:37
 */
public interface LeaseManager<T> {

    void register(T r, int leaseDuration, boolean isReplication);


    boolean cancel(String appName, String id, boolean isReplication);


    boolean renew(String appName, String id, boolean isReplication);


    void evict();

}
