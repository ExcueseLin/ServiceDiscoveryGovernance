package cn.sunrain.SDG.server.resource;

import cn.sunrain.SDG.server.config.ServerConfig;
import cn.sunrain.SDG.server.lease.Lease;
import cn.sunrain.SDG.server.resource.registry.AbstractInstanceRegistry;
import cn.sunrain.SDG.share.entity.Application;
import cn.sunrain.SDG.share.entity.InstanceInfo;
import cn.sunrain.SDG.share.http.Response;
import cn.sunrain.SDG.share.http.HttpNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author lin
 * @date 2021/2/22 10:41
 */
@RestController
@RequestMapping("/app")
public class ApplicationResource {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationResource.class);

    private static final ConcurrentHashMap<String, Map<String, Lease<InstanceInfo>>> registry = AbstractInstanceRegistry.getRegistry();

    private final ServerConfig serverConfig;
    private final AbstractInstanceRegistry instanceRegistry;


    @Autowired
    public ApplicationResource(ServerConfig serverConfig ,
                        AbstractInstanceRegistry instanceRegistry) {
        this.serverConfig = serverConfig;
        this.instanceRegistry = instanceRegistry;
    }

    /**
     * 获取服务名下所有的服务实例
     * @return
     */
    @GetMapping("/{appName}")
    public Response getApplication(@PathVariable("appName") String appName) {
        if (isBlank(appName)){
            return new Response(Response.Status.NOT_FOUND,"Missing-appName");
        }
        Application application = instanceRegistry.getApplication(appName, true);

        if (application == null){
            logger.debug("Not Found : {} ",appName);
            return new Response(Response.Status.NOT_FOUND,"Not Found");
        }else {
            logger.debug("Found Success  : {} ",appName);
            return new Response(Response.Status.OK,"Found Success",application);
        }
    }



    @PostMapping("/add")
    public Response addInstance(@RequestBody InstanceInfo info,
                                @RequestHeader(HttpNode.HEADER_REPLICATION) String isReplication ){
        if (isBlank(info.getId())){
            return new Response(Response.Status.NOT_FOUND,"Missing instanceId");
        }else if(isBlank(info.getAppName())){
            return new Response(Response.Status.NOT_FOUND,"Missing appName");
        }else if(isBlank(info.getIpAddr())){
            return new Response(Response.Status.NOT_FOUND,"Missing IpAddr");
        }else if(isBlank(info.getHostName())){
            return new Response(Response.Status.NOT_FOUND,"Missing HostName");
        }
        instanceRegistry.register(info,Lease.DEFAULT_DURATION_IN_SECS ,"true".equals(isReplication));
        //状态204 向后兼容
        return new Response(Response.Status.NO_CONTENT,"Register Success");
    }

    private boolean isBlank(String str) {
        return str == null || str.isEmpty();
    }
}
