package cn.sunrain.SDG.client.config;

import cn.sunrain.SDG.share.enums.InstanceStatus;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author lin
 * @date 2021/3/4 14:02
 */
@ConfigurationProperties("sdg.instance")
public class InstanceConfigBean implements EnvironmentAware {

    private static final String UNKNOWN = "unknown";


    private String appname = UNKNOWN;

    /** 客户需要多长时间发送心跳给eureka服务器，表明它仍然活着,默认为30 秒 */
    private int leaseRenewalIntervalInSeconds = 30;
    /** 服务器在接收到实例的最后一次发出的心跳后，需要等待多久才可以将此实例删除，默认为90秒*/
    private int leaseExpirationDurationInSeconds = 90;

    private String instanceId;
    private String ipAddress;
    private int port = 80;
    private String hostname;
    private InstanceStatus initialStatus = InstanceStatus.UP;

    private Map<String, String> metadataMap = new HashMap<>();


    private Environment environment;


    public InstanceConfigBean(){
        try {
            InetAddress addr= InetAddress.getLocalHost();
            this.ipAddress = addr.getHostAddress();
            this.hostname = addr.getHostName();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
        // set some defaults from the environment, but allow the defaults to use relaxed
        // binding
        String springAppName = this.environment.getProperty("spring.application.name",
                "");
        if (StringUtils.hasText(springAppName)) {
            setAppname(springAppName);
        }
    }
    public Environment getEnvironment() {
        return environment;
    }



    public String getInstanceId() {
        if (this.instanceId == null && this.metadataMap != null) {
            return this.metadataMap.get("instanceId");
        }
        return this.instanceId;
    }
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }


    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
    public String getHostName() {
        return  this.hostname;
    }


    public String getAppname() {
        return appname;
    }
    public void setAppname(String appname) {
        this.appname = appname;
    }


    public int getLeaseRenewalIntervalInSeconds() {
        return leaseRenewalIntervalInSeconds;
    }
    public void setLeaseRenewalIntervalInSeconds(int leaseRenewalIntervalInSeconds) {
        this.leaseRenewalIntervalInSeconds = leaseRenewalIntervalInSeconds;
    }

    public int getLeaseExpirationDurationInSeconds() {
        return leaseExpirationDurationInSeconds;
    }
    public void setLeaseExpirationDurationInSeconds(
            int leaseExpirationDurationInSeconds) {
        this.leaseExpirationDurationInSeconds = leaseExpirationDurationInSeconds;
    }

    public String getIpAddress() {
        return ipAddress;
    }
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public InstanceStatus getInitialStatus() {
        return initialStatus;
    }
    public void setInitialStatus(InstanceStatus initialStatus) {
        this.initialStatus = initialStatus;
    }

    public Map<String, String> getMetadataMap() {
        return metadataMap;
    }
    public void setMetadataMap(Map<String, String> metadataMap) {
        this.metadataMap = metadataMap;
    }

    public int getPort() {
        return port;
    }
    public void setPort(int port) {
        this.port = port;
    }

}
