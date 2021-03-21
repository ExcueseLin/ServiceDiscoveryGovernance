package cn.sunrain.SDG.server.resource;

import cn.sunrain.SDG.server.config.ServerConfigBean;
import cn.sunrain.SDG.server.lease.Lease;
import cn.sunrain.SDG.server.resource.registry.AbstractInstanceRegistry;
import cn.sunrain.SDG.share.entity.Applications;
import cn.sunrain.SDG.share.entity.InstanceInfo;
import cn.sunrain.SDG.share.http.Response;
import cn.sunrain.SDG.share.http.HttpNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author lin
 * @date 2021/2/22 10:41
 */
@RestController
@RequestMapping("/apps")
public class ApplicationsResource {


    private static final Logger logger = LoggerFactory.getLogger(ApplicationsResource.class);

    private static final ConcurrentHashMap<String, Map<String, Lease<InstanceInfo>>> registry = AbstractInstanceRegistry.getRegistry();

    private final ServerConfigBean serverConfig;
    private final AbstractInstanceRegistry instanceRegistry;


    @Autowired
    public ApplicationsResource(ServerConfigBean serverConfig ,
                        AbstractInstanceRegistry instanceRegistry) {
        this.serverConfig = serverConfig;
        this.instanceRegistry = instanceRegistry;
    }

    /**
     *
     * @param acceptHeader
     * @param acceptEncoding
     * @param request
     * @param regionsStr  分区，代表其他服务中心的地址
     * @return
     */
    @GetMapping()
    public Response getContainers(@RequestParam(HttpNode.HEADER_ACCEPT) String acceptHeader,
                                  @RequestParam(HttpNode.HEADER_ACCEPT_ENCODING) String acceptEncoding,
                                  HttpServletRequest request,
                                  @RequestParam("regions") String regionsStr) {
        boolean isRemoteRegionRequested = null != regionsStr && !regionsStr.isEmpty();
        String[] regions = null;
        if (isRemoteRegionRequested){
            regions = regionsStr.toLowerCase().split(",");
            //我们不会对不同顺序查询的相同区域使用不同的缓存。
            Arrays.sort(regions);
        }

        Applications applications = instanceRegistry.getApplications();
        if (applications == null){
            logger.debug("Applications is Empty");
            return new Response(Response.Status.NOT_FOUND,"Applications is Empty");
        }
        logger.debug("Found Applications Success");
        return new Response(Response.Status.OK,"Found Applications Success",applications);

    }


    @GetMapping("/delta")
    public Response getContainerDifferential(@RequestParam(HttpNode.HEADER_ACCEPT) String acceptHeader,
                                             @RequestParam(HttpNode.HEADER_ACCEPT_ENCODING) String acceptEncoding,
                                             HttpServletRequest request,
                                             @RequestParam("regions") String regionsStr) {
        boolean isRemoteRegionRequested = null != regionsStr && !regionsStr.isEmpty();
        String[] regions = null;
        if (isRemoteRegionRequested){
            regions = regionsStr.toLowerCase().split(",");
            //我们不会对不同顺序查询的相同区域使用不同的缓存。
            Arrays.sort(regions);
        }

        Applications applications = instanceRegistry.getApplicationDeltas();
        if (applications == null){
            logger.debug("Applications is Empty");
            return new Response(Response.Status.NOT_FOUND,"Applications is Empty");
        }
        logger.debug("Found Applications Success");
        return new Response(Response.Status.OK,"Found Applications Success",applications);
    }

}
