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




    public void shutdown();

    public ClientConfig getEurekaClientConfig();
}
