package cn.sunrain.SDG.client.http;

import cn.sunrain.SDG.share.entity.Application;
import cn.sunrain.SDG.share.entity.Applications;
import cn.sunrain.SDG.share.entity.InstanceInfo;
import cn.sunrain.SDG.share.enums.InstanceStatus;
import cn.sunrain.SDG.share.http.Response;
import cn.sunrain.SDG.share.utils.JsonConverter;
import cn.sunrain.SDG.share.utils.StringUtil;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static cn.sunrain.SDG.client.http.SDGHttpResponse.anSDGHttpResponse;


public abstract class RestTemplateSdgHttpClient {

    private static final Logger logger = LoggerFactory.getLogger(RestTemplateSdgHttpClient.class);

    private final RestTemplate restTemplate;



    public RestTemplateSdgHttpClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;

    }

    public SDGHttpResponse<Void> register(InstanceInfo info,String serviceUrl) {

            String urlPath = serviceUrl + "app/add";

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.ACCEPT_ENCODING, "gzip");
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

            ResponseEntity<Void> response = restTemplate.exchange(urlPath, HttpMethod.POST,
                    new HttpEntity<>(info, headers), Void.class);

            return anSDGHttpResponse(response.getStatusCodeValue())
                    .headers(headersOf(response)).build();
    }



    public SDGHttpResponse<Void> cancel(String appName, String id,String serviceUrl) {
        String urlPath = serviceUrl + "instance?appName=" + appName + "&id=" + id;

        ResponseEntity<Void> response = restTemplate.exchange(urlPath, HttpMethod.DELETE,
                null, Void.class);

        return anSDGHttpResponse(response.getStatusCodeValue())
                .headers(headersOf(response)).build();
    }

    public SDGHttpResponse<InstanceInfo> sendHeartBeat(String appName, String id,
                                                          InstanceInfo info, InstanceStatus overriddenStatus,
                                                        String serviceUrl) {
        String urlPath = serviceUrl + "instance?appName= " + appName + "&id=" + id
                + "&status=" + info.getStatus().toString()
                + "&lastDirtyTimestamp=" + info.getLastDirtyTimestamp()
                + (overriddenStatus != null ? "&overriddenstatus=" + overriddenStatus.name() : "");

        ResponseEntity<Response> response = restTemplate.exchange(urlPath,
                HttpMethod.PUT, null, Response.class);

        String str = response.hasBody() && response.getBody().getEntity() != null
                ? JSON.toJSONString(response.getBody().getEntity())  : null;

        InstanceInfo instanceInfo =  str ==null ? null :JSON.parseObject(str,InstanceInfo.class);

        return anSDGHttpResponse(response.getStatusCodeValue(),
                response.getStatusCode().value()== HttpStatus.OK.value()
                        && instanceInfo != null ? instanceInfo  : null)
                .headers(headersOf(response)).build();
    }

    public SDGHttpResponse<Applications> getApplications(String serviceUrl,String... regions) {
        return getApplicationsInternal("apps/", serviceUrl , regions);
    }

    public SDGHttpResponse<Applications> getDelta(String serviceUrl,String... regions) {
        return getApplicationsInternal("apps/delta", serviceUrl, regions);
}


    private SDGHttpResponse<Applications> getApplicationsInternal(String urlPath,String serviceUrl,
                                                                     String[] regions) {
        String url = serviceUrl + urlPath;

        if (regions != null && regions.length > 0) {
            url = url + (urlPath.contains("?") ? "&" : "?") + "regions="
                    + StringUtil.join(regions);
        }

        ResponseEntity<Response> response = restTemplate.exchange(url,
                HttpMethod.GET, null, Response.class);

        String str = response.hasBody() && response.getBody().getEntity() != null
                ? JSON.toJSONString(response.getBody().getEntity())  : null;

        Applications applications =  str ==null ? null : JsonConverter.strToApplications(str);
        return anSDGHttpResponse(response.getStatusCodeValue(),
                response.getStatusCode().value()== HttpStatus.OK.value()
                        && applications != null ? applications : null)
                .headers(headersOf(response)).build();
    }


    public SDGHttpResponse<InstanceInfo> getInstance(String appName, String id , String serviceUrl) {
        return getInstanceInternal("instance"+"?appName=" + appName + "&id=" + id,serviceUrl);
    }




    private SDGHttpResponse<InstanceInfo> getInstanceInternal(String urlPath,String serviceUrl) {
        urlPath = serviceUrl + urlPath;

        ResponseEntity<InstanceInfo> response = restTemplate.exchange(urlPath,
                HttpMethod.GET, null, InstanceInfo.class);

        return anSDGHttpResponse(response.getStatusCodeValue(),
                response.getStatusCodeValue() == HttpStatus.OK.value()
                        && response.hasBody() ? response.getBody() : null)
                .headers(headersOf(response)).build();
    }






    private static Map<String, String> headersOf(ResponseEntity<?> response) {
        HttpHeaders httpHeaders = response.getHeaders();
        if (httpHeaders == null || httpHeaders.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> headers = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : httpHeaders.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                headers.put(entry.getKey(), entry.getValue().get(0));
            }
        }
        return headers;
    }


}
