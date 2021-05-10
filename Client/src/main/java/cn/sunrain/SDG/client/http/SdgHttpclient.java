package cn.sunrain.SDG.client.http;

import cn.sunrain.SDG.share.entity.Applications;
import cn.sunrain.SDG.share.entity.InstanceInfo;
import cn.sunrain.SDG.share.enums.InstanceStatus;
import org.springframework.web.client.RestTemplate;
import static cn.sunrain.SDG.client.http.SDGHttpResponse.anSDGHttpResponse;

/**
 * @author lin
 * @date 2021/4/27 14:02
 */
public class SdgHttpclient extends RestTemplateSdgHttpClient {

    private final static int OK = 200;

    private String[] serviceUrls;


    public SdgHttpclient(RestTemplate restTemplate,String[] serverUrls) {
        super(restTemplate);
        this.serviceUrls = serverUrls;
        for (int i = 0; i < serverUrls.length; i++) {
            if (!serviceUrls[i].endsWith("/")) {
                serviceUrls[i] = serviceUrls[i] + "/";
            }
        }
    }


    public SDGHttpResponse<Void> register(InstanceInfo info) {
        SDGHttpResponse<Void> register = anSDGHttpResponse(500).build();

        if (serviceUrlsIsEmpty()){
            return register;
        }
        for (String serviceUrl : serviceUrls) {
            register = super.register(info, serviceUrl);
            if(register.getStatusCode() == OK){
                return register;
            }
        }
        return register;
    }

    public SDGHttpResponse<InstanceInfo> sendHeartBeat(String appName, String id, InstanceInfo info, InstanceStatus overriddenStatus) {
        SDGHttpResponse<InstanceInfo> register =  anSDGHttpResponse(500,InstanceInfo.class).build();
        if (serviceUrlsIsEmpty()){
            return register;
        }
        for (String serviceUrl : serviceUrls) {
            register = super.sendHeartBeat(appName, id, info, overriddenStatus, serviceUrl);
            if(register.getStatusCode() == OK){
                return register;
            }
        }
        return register;
    }

    public SDGHttpResponse<Void> cancel(String appName, String id) {
        SDGHttpResponse<Void> register = anSDGHttpResponse(500).build();
        if (serviceUrlsIsEmpty()){
            return register;
        }
        for (String serviceUrl : serviceUrls) {
            register =  super.cancel(appName, id, serviceUrl);
            if(register.getStatusCode() == OK){
                return register;
            }
        }
        return register;
    }

    public SDGHttpResponse<Applications> getApplications(String... regions) {
        SDGHttpResponse<Applications> register = anSDGHttpResponse(500,Applications.class).build();
        if (serviceUrlsIsEmpty()){
            return register;
        }
        for (String serviceUrl : serviceUrls) {
            register =  super.getApplications(serviceUrl, regions);
            if(register.getStatusCode() == OK){
                return register;
            }
        }
        return register;
    }

    public SDGHttpResponse<Applications> getDelta(String... regions) {
        SDGHttpResponse<Applications> register = anSDGHttpResponse(500,Applications.class).build();
        if (serviceUrlsIsEmpty()){
            return register;
        }
        for (String serviceUrl : serviceUrls) {
            register =  super.getDelta(serviceUrl, regions);
            if(register.getStatusCode() == OK){
                return register;
            }
        }
        return register;

    }

    private boolean serviceUrlsIsEmpty(){
        return serviceUrls == null || serviceUrls.length == 0;
    }

}
