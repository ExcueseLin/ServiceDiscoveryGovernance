package cn.sunrain.SDG.server.resource;

import cn.sunrain.SDG.server.config.ServerConfig;
import cn.sunrain.SDG.server.resource.registry.AbstractInstanceRegistry;
import cn.sunrain.SDG.share.entity.InstanceInfo;
import cn.sunrain.SDG.share.http.Response;
import cn.sunrain.SDG.share.enums.InstanceStatus;
import cn.sunrain.SDG.share.http.HttpNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author lin
 * @date 2021/2/22 11:00
 */
@RestController
@RequestMapping("/instance")
public class InstanceInfoResource {

    private static final Logger logger = LoggerFactory.getLogger(InstanceInfoResource.class);


    private final ServerConfig serverConfig;
    private final AbstractInstanceRegistry registry;


    @Autowired
    public InstanceInfoResource(ServerConfig serverConfig ,
                               AbstractInstanceRegistry instanceRegistry) {
        this.serverConfig = serverConfig;
        this.registry = instanceRegistry;
    }

    @GetMapping("")
    public Response getInstanceInfo(@RequestParam("appName") String appName,
                                    @RequestParam("id") String id) {
        InstanceInfo appInfo = registry
                .getInstanceByAppAndId(appName, id);
        if (appInfo != null) {
            logger.debug("Found: {} - {}", appName, id);
            return new Response(Response.Status.OK,"Found Success",appInfo);
        } else {
            logger.debug("Not Found: {} - {}", appName, id);
            return new Response(Response.Status.NOT_FOUND,"Not Found ");
        }
    }


    @PutMapping()
    public Response renewLease(
            @RequestHeader(HttpNode.HEADER_REPLICATION) String isReplication,
            @RequestParam("status") String status,
            @RequestParam("lastDirtyTimestamp") String lastDirtyTimestamp,
            @RequestParam("appName") String appName,
            @RequestParam("id") String id) {
        boolean isFromReplicaNode = "true".equals(isReplication);
        boolean isSuccess = registry.renew(appName, id, isFromReplicaNode);

        // Not found in the registry, immediately ask for a register
        if (!isSuccess) {
            logger.warn("Not Found (Renew): {} - {}", appName, id);
            return new Response(Response.Status.NOT_FOUND,"Not Found (Renew) ");
        }

        //续约成功
        logger.debug("Found (Renew): {} - {}", appName, id);
        return new Response(Response.Status.OK,"Renew Success");
    }

    @PutMapping("status")
    public Response statusUpdate(
            @RequestParam("appName") String appName,
            @RequestParam("id") String id,
            @RequestParam("value") String newStatus,
            @RequestHeader(HttpNode.HEADER_REPLICATION) String isReplication,
            @RequestParam("lastDirtyTimestamp") String lastDirtyTimestamp) {
        try {
            if (registry.getInstanceByAppAndId(appName, id) == null) {
                logger.warn("Instance not found: {}/{}", appName, id);
                return new Response(Response.Status.NOT_FOUND,"Instance Not Found");
            }
            boolean isSuccess = registry.statusUpdate(appName, id,
                    InstanceStatus.valueOf(newStatus), lastDirtyTimestamp,
                    "true".equals(isReplication));

            if (isSuccess) {
                logger.info("Status updated: {} - {} - {}", appName, id, newStatus);
                return new Response(Response.Status.OK,"Instance Status Updated Success");
            } else {
                logger.warn("Unable to update status: {} - {} - {}",appName, id, newStatus);
                return new Response(Response.Status.INTERNAL_SERVER_ERROR,"Unable to update status");
            }
        } catch (Throwable e) {
            logger.error("Error updating instance {} for status {}", id,
                    newStatus);
            return new Response(Response.Status.INTERNAL_SERVER_ERROR,"Error updating instance");
        }
    }


    @DeleteMapping
    public Response cancelLease(
            @RequestParam("appName") String appName,
            @RequestParam("id") String id,
            @RequestHeader(HttpNode.HEADER_REPLICATION) String isReplication) {
        try {
            boolean isSuccess = registry.cancel(appName, id,
                    "true".equals(isReplication));

            if (isSuccess) {
                logger.debug("Found (Cancel): {} - {}", appName, id);
                return new Response(Response.Status.OK,"Instance Cancel Success");
            } else {
                logger.info("Not Found (Cancel): {} - {}", appName, id);
                return new Response(Response.Status.NOT_FOUND,"Instance Not Found (Cancel)");
            }
        } catch (Throwable e) {
            logger.error("Error (cancel): {} - {}",appName, id, e);
            return new Response(Response.Status.INTERNAL_SERVER_ERROR,"Error cancel instance");
        }

    }




    private Response validateDirtyTimestamp(String appName,String id,
                                            Long lastDirtyTimestamp, boolean isReplication) {
        InstanceInfo appInfo = registry.getInstanceByAppAndId(appName, id, false);
        if (appInfo != null) {
            if ((lastDirtyTimestamp != null) && (!lastDirtyTimestamp.equals(appInfo.getLastDirtyTimestamp()))) {
                Object[] args = {id, appInfo.getLastDirtyTimestamp(), lastDirtyTimestamp, isReplication};

                if (lastDirtyTimestamp > appInfo.getLastDirtyTimestamp()) {
                    logger.debug(
                            "Time to sync, since the last dirty timestamp differs -"
                                    + " ReplicationInstance id : {},Registry : {} Incoming: {} Replication: {}",
                            args);
                    return new Response(Response.Status.NOT_FOUND, "Time Expired");
                } else if (appInfo.getLastDirtyTimestamp() > lastDirtyTimestamp) {
                    // In the case of replication, send the current instance info in the registry for the
                    // replicating node to sync itself with this one.
                    if (isReplication) {
                        logger.debug(
                                "Time to sync, since the last dirty timestamp differs -"
                                        + " ReplicationInstance id : {},Registry : {} Incoming: {} Replication: {}",
                                args);
                        return new Response(Response.Status.CONFLICT, "Time Conflict", appInfo);
                    } else {
                        return new Response(Response.Status.OK, "");
                    }
                }
            }
        }
        return new Response(Response.Status.OK, "");
    }




}
