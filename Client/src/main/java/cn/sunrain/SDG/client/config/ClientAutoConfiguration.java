package cn.sunrain.SDG.client.config;

import cn.sunrain.SDG.client.discovery.Discovery;
import cn.sunrain.SDG.client.discovery.DiscoveryClient;
import cn.sunrain.SDG.share.entity.InstanceInfo;
import cn.sunrain.SDG.share.entity.LeaseInfo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertyResolver;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * @author lin
 * @date 2021/4/28 12:15
 */
@Configuration()
public class ClientAutoConfiguration {


    private ConfigurableEnvironment env;

    public ClientAutoConfiguration(ConfigurableEnvironment env) {
        this.env = env;
    }

    private String getProperty(String property) {
        return this.env.containsProperty(property) ? this.env.getProperty(property) : "";
    }


    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }


    @Bean
    @ConditionalOnMissingBean(value = ClientConfig.class,
            search = SearchStrategy.CURRENT)
    public ClientConfig clientConfig(ConfigurableEnvironment env) {
        ClientConfig client = new ClientConfig();
        if ("bootstrap".equals(this.env.getProperty("spring.config.name"))) {
            // We don't register during bootstrap by default, but there will be another
            // chance later.
            client.setRegisterWithEureka(false);
        }
        return client;
    }


    @Bean
    @ConditionalOnMissingBean(value = InstanceConfigBean.class,
            search = SearchStrategy.CURRENT)
    public InstanceConfigBean eurekaInstanceConfigBean(){
        String hostname = getProperty("sdg.instance.hostname");

        String ipAddress = getProperty("sdg.instance.ip-address");

        int serverPort = Integer.parseInt(
                env.getProperty("server.port", env.getProperty("port", "8080")));

        InstanceConfigBean instance = new InstanceConfigBean();
        if (StringUtils.hasText(ipAddress)) {
            instance.setIpAddress(ipAddress);
        }
        if (StringUtils.hasText(hostname)) {
            instance.setHostname(hostname);
        }
        instance.setPort(serverPort);
        instance.setInstanceId(getDefaultInstanceId(env,instance.getHostName()));

        return instance;
    }




    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(value = Discovery.class,
            search = SearchStrategy.CURRENT)
    public DiscoveryClient eurekaClient(ClientConfig config, InstanceConfigBean instanceConfigBean, RestTemplate restTemplate) {

        LeaseInfo leaseInfo = LeaseInfo.builder()
                .renewalIntervalInSecs(instanceConfigBean.getLeaseRenewalIntervalInSeconds())
                .durationInSecs(instanceConfigBean.getLeaseExpirationDurationInSeconds())
                .build();

        InstanceInfo instanceInfo = InstanceInfo.builder()
                .instanceId(instanceConfigBean.getInstanceId())
                .appName(instanceConfigBean.getAppname())
                .hostName(instanceConfigBean.getHostName())
                .ipAddr(instanceConfigBean.getIpAddress())
                .port(instanceConfigBean.getPort())
                .status(instanceConfigBean.getInitialStatus())
                .leaseInfo(leaseInfo)
                .build();

        return new DiscoveryClient( config, instanceInfo,
                restTemplate);
    }


    /**
     * 生成instanceId
     * 如果自定义则不需要修改
     * 如果没有自定义 则是 hostname : applicationName : port
     * @param resolver
     * @param hostname
     * @return
     */
    public static String getDefaultInstanceId(PropertyResolver resolver,String hostname ) {
        String InstanceId = resolver.getProperty("sdg.instance.instance-id");
        if (StringUtils.hasText(InstanceId)) {
            return InstanceId;
        } else {
            String appName = resolver.getProperty("spring.application.name");
            String namePart = combineParts(hostname, ":", appName);
            String indexPart = resolver.getProperty("server.port");
            return combineParts(namePart, ":", indexPart);
        }
    }
    public static String combineParts(String firstPart, String separator, String secondPart) {
        String combined = null;
        if (firstPart != null && secondPart != null) {
            combined = firstPart + separator + secondPart;
        } else if (firstPart != null) {
            combined = firstPart;
        } else if (secondPart != null) {
            combined = secondPart;
        }

        return combined;
    }
}
