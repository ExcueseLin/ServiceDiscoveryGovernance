package cn.sunrain.SDG.client.http;

import cn.hutool.http.HttpUtil;
import cn.sunrain.SDG.share.entity.Applications;
import cn.sunrain.SDG.share.entity.InstanceInfo;
import cn.sunrain.SDG.share.enums.InstanceStatus;
import cn.sunrain.SDG.share.utils.StringUtil;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.sunrain.SDG.client.http.SDGHttpResponse.anSDGHttpResponse;


public class RestTemplateSdgHttpClient {

    private RestTemplate restTemplate;

    private String serviceUrl;

    public RestTemplateSdgHttpClient(RestTemplate restTemplate, String serviceUrl) {
        this.restTemplate = restTemplate;
        this.serviceUrl = serviceUrl;
        if (!serviceUrl.endsWith("/")) {
            this.serviceUrl = this.serviceUrl + "/";
        }
    }

    public SDGHttpResponse<Void> register(InstanceInfo info) {

            String urlPath = serviceUrl + "app/add" + info.getAppName();

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.ACCEPT_ENCODING, "gzip");
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

            ResponseEntity<Void> response = restTemplate.exchange(urlPath, HttpMethod.POST,
                    new HttpEntity<>(info, headers), Void.class);

            return anSDGHttpResponse(response.getStatusCodeValue())
                    .headers(headersOf(response)).build();
    }



    public SDGHttpResponse<Void> cancel(String appName, String id) {
        String urlPath = serviceUrl + "instance?appName=" + appName + "&id=" + id;

        ResponseEntity<Void> response = restTemplate.exchange(urlPath, HttpMethod.DELETE,
                null, Void.class);

        return anSDGHttpResponse(response.getStatusCodeValue())
                .headers(headersOf(response)).build();
    }

    public SDGHttpResponse<InstanceInfo> sendHeartBeat(String appName, String id,
                                                          InstanceInfo info, InstanceStatus overriddenStatus) {
        String urlPath = serviceUrl + "instance?appName= " + appName + "&id=" + id + "&status="
                + info.getStatus().toString() + "&lastDirtyTimestamp="
                + info.getLastDirtyTimestamp().toString() + (overriddenStatus != null
                ? "&overriddenstatus=" + overriddenStatus.name() : "");

        ResponseEntity<InstanceInfo> response = restTemplate.exchange(urlPath,
                HttpMethod.PUT, null, InstanceInfo.class);

        SDGHttpResponse.SDGHttpResponseBuilder<InstanceInfo> eurekaResponseBuilder = anSDGHttpResponse(
                response.getStatusCodeValue(), InstanceInfo.class)
                .headers(headersOf(response));

        if (response.hasBody()) {
            eurekaResponseBuilder.entity(response.getBody());
        }

        return eurekaResponseBuilder.build();
    }

    public SDGHttpResponse<Applications> getApplications(String... regions) {
        return getApplicationsInternal("apps/", regions);
    }

    public SDGHttpResponse<Applications> getDelta(String... regions) {
        return getApplicationsInternal("apps/delta", regions);
    }


    private SDGHttpResponse<Applications> getApplicationsInternal(String urlPath,
                                                                     String[] regions) {
        String url = serviceUrl + urlPath;

        if (regions != null && regions.length > 0) {
            url = url + (urlPath.contains("?") ? "&" : "?") + "regions="
                    + StringUtil.join(regions);
        }

        ResponseEntity<Applications> response = restTemplate.exchange(url,
                HttpMethod.GET, null, Applications.class);

        return anSDGHttpResponse(response.getStatusCodeValue(),
                response.getStatusCode().value() == HttpStatus.OK.value()
                        && response.hasBody() ? (Applications) response.getBody() : null)
                .headers(headersOf(response)).build();
    }


    public SDGHttpResponse<InstanceInfo> getInstance(String appName, String id) {
        return getInstanceInternal("instance"+"?appName=" + appName + "&id=" + id);
    }




    private SDGHttpResponse<InstanceInfo> getInstanceInternal(String urlPath) {
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
