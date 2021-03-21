package cn.sunrain.SDG.client.discovery;


import cn.sunrain.SDG.client.config.ClientConfig;
import cn.sunrain.SDG.share.entity.Application;
import cn.sunrain.SDG.share.entity.Applications;
import cn.sunrain.SDG.share.entity.InstanceInfo;

import java.util.List;
import java.util.Set;

/**
 * @author lin
 * @date 2021/3/4 13:11
 */
public interface Discovery {

    Applications getApplications();

    Application getApplication(String appName);

    List<InstanceInfo> getInstancesById(String id);

    public Applications getApplicationsForARegion(String region);


    /**
     * 获取所特定服务中心上的所有应用实例
     * @param serviceUrl  服务中心地址
     * @return
     */
    public Applications getApplications(String serviceUrl);

    /**
     * @return 以字符串形式显示此客户端可以访问的所有区域（本地+远程）
     */
    public Set<String> getAllKnownRegions();


    public void shutdown();

    public ClientConfig getEurekaClientConfig();
}
