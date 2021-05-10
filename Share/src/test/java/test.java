import cn.sunrain.SDG.share.entity.Application;
import cn.sunrain.SDG.share.entity.Applications;
import cn.sunrain.SDG.share.entity.InstanceInfo;
import cn.sunrain.SDG.share.entity.LeaseInfo;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author lin
 * @date 2021/4/29 14:54
 */
public class test {


    @Test
    public  void test() {

        LeaseInfo leaseInfo = LeaseInfo.builder()
                .durationInSecs(12222)
                .lastRenewalTimestamp(1231)
                .renewalIntervalInSecs(2123)
                .build();

        LeaseInfo leaseInfo2 = LeaseInfo.builder()
                .durationInSecs(213123)
                .lastRenewalTimestamp(123123123)
                .renewalIntervalInSecs(123132)
                .build();

        InstanceInfo instanceInfo = InstanceInfo.builder()
                .ipAddr("1234")
                .instanceId("123")
                .appName("test")
                .hostName("hostname")
                .leaseInfo(leaseInfo)
                .build();

        InstanceInfo instanceInfo2= InstanceInfo.builder()
                .ipAddr("4564564")
                .instanceId("45646456")
                .appName("4564564")
                .hostName("45645645645")
                .leaseInfo(leaseInfo2)
                .build();



        System.out.println(JSONObject.toJSONString(instanceInfo));


        String s = "{\"appName\":\"test\",\"hostName\":\"hostname\",\"id\":\"123\",\"instanceId\":\"123\",\"instanceInfoDirty\":false,\"ipAddr\":\"1234\",\"leaseInfo\":{\"durationInSecs\":12222,\"evictionTimestamp\":0,\"lastRenewalTimestamp\":1231,\"registrationTimestamp\":0,\"renewalIntervalInSecs\":2123,\"serviceUpTimestamp\":0}}";


        InstanceInfo in = JSON.parseObject(s,InstanceInfo.class);
        System.out.println(JSONObject.toJSONString(in));
       // System.out.println(JSONObject.toJSONString(strToInstanceInfo(s)));

        System.out.println();

        Application application = new Application();
        application.addInstance(instanceInfo);
        application.addInstance(instanceInfo2);
        System.out.println(JSONObject.toJSONString(application));

        String str1 = "{\"instance\":[{\"appName\":\"test\",\"hostName\":\"hostname\",\"id\":\"123\",\"instanceId\":\"123\",\"instanceInfoDirty\":false,\"ipAddr\":\"1234\",\"leaseInfo\":{\"durationInSecs\":12222,\"evictionTimestamp\":0,\"lastRenewalTimestamp\":1231,\"registrationTimestamp\":0,\"renewalIntervalInSecs\":2123,\"serviceUpTimestamp\":0}}],\"name\":\"UNKNOW\"}\n";

        String str11 = "{\"instance\":[{\"appName\":\"test\",\"hostName\":\"hostname\",\"id\":\"123\",\"instanceId\":\"123\",\"instanceInfoDirty\":false,\"ipAddr\":\"1234\",\"leaseInfo\":{\"durationInSecs\":12222,\"evictionTimestamp\":0,\"lastRenewalTimestamp\":1231,\"registrationTimestamp\":0,\"renewalIntervalInSecs\":2123,\"serviceUpTimestamp\":0}},{\"appName\":\"4564564\",\"hostName\":\"45645645645\",\"id\":\"45646456\",\"instanceId\":\"45646456\",\"instanceInfoDirty\":false,\"ipAddr\":\"4564564\",\"leaseInfo\":{\"durationInSecs\":213123,\"evictionTimestamp\":0,\"lastRenewalTimestamp\":123123123,\"registrationTimestamp\":0,\"renewalIntervalInSecs\":123132,\"serviceUpTimestamp\":0}}],\"name\":\"UNKNOW\"}";

        //System.out.println(JSONObject.toJSONString(application1));
        Application application1 = strToApplication(str11);
        System.out.println(JSONObject.toJSONString(application1));
        System.out.println();

        Applications apps = new Applications();
        apps.addApplication(application);
        System.out.println(JSONObject.toJSONString(apps));


        String str2 = "{\"registeredApplications\":[{\"instance\":[{\"appName\":\"test\",\"hostName\":\"hostname\",\"id\":\"123\",\"instanceId\":\"123\",\"instanceInfoDirty\":false,\"ipAddr\":\"1234\",\"leaseInfo\":{\"durationInSecs\":12222,\"evictionTimestamp\":0,\"lastRenewalTimestamp\":1231,\"registrationTimestamp\":0,\"renewalIntervalInSecs\":2123,\"serviceUpTimestamp\":0}}],\"name\":\"UNKNOW\"}]}";
        String str22 = "{\"registeredApplications\":[{\"instance\":[{\"appName\":\"test\",\"hostName\":\"hostname\",\"id\":\"123\",\"instanceId\":\"123\",\"instanceInfoDirty\":false,\"ipAddr\":\"1234\",\"leaseInfo\":{\"durationInSecs\":12222,\"evictionTimestamp\":0,\"lastRenewalTimestamp\":1231,\"registrationTimestamp\":0,\"renewalIntervalInSecs\":2123,\"serviceUpTimestamp\":0}},{\"appName\":\"4564564\",\"hostName\":\"45645645645\",\"id\":\"45646456\",\"instanceId\":\"45646456\",\"instanceInfoDirty\":false,\"ipAddr\":\"4564564\",\"leaseInfo\":{\"durationInSecs\":213123,\"evictionTimestamp\":0,\"lastRenewalTimestamp\":123123123,\"registrationTimestamp\":0,\"renewalIntervalInSecs\":123132,\"serviceUpTimestamp\":0}}],\"name\":\"UNKNOW\"}]}";
        Applications applications = strToApplications(str22);
        System.out.println(JSONObject.toJSONString(applications));
        System.out.println();

        String s1 = "{\n" +
                "        \"registeredApplications\": [\n" +
                "            {\n" +
                "                \"name\": \"client-test\",\n" +
                "                \"instances\": [\n" +
                "                    {\n" +
                "                        \"instanceId\": \"DESKTOP-A84G64I:client-test:9002\",\n" +
                "                        \"appName\": \"client-test\",\n" +
                "                        \"ipAddr\": \"192.168.31.76\",\n" +
                "                        \"port\": 9002,\n" +
                "                        \"hostName\": \"DESKTOP-A84G64I\",\n" +
                "                        \"status\": \"UP\",\n" +
                "                        \"lastDirtyTimestamp\": null,\n" +
                "                        \"lastUpdatedTimestamp\": 1619699514988,\n" +
                "                        \"leaseInfo\": {\n" +
                "                            \"renewalIntervalInSecs\": 0,\n" +
                "                            \"durationInSecs\": 90,\n" +
                "                            \"registrationTimestamp\": 1619699514988,\n" +
                "                            \"lastRenewalTimestamp\": 1619699514988,\n" +
                "                            \"evictionTimestamp\": 0,\n" +
                "                            \"serviceUpTimestamp\": 1619699514988\n" +
                "                        },\n" +
                "                        \"actionType\": \"ADDED\",\n" +
                "                        \"id\": \"DESKTOP-A84G64I:client-test:9002\",\n" +
                "                        \"instanceInfoDirty\": false\n" +
                "                    }\n" +
                "                ]\n" +
                "            }\n" +
                "        ]\n" +
                "    }";

        System.out.println(JSONObject.toJSONString(strToApplications(s1)));


    }



    @Test
    public void test2(){
        String s1 = "{\n" +
                "        \"registeredApplications\": [\n" +
                "            {\n" +
                "                \"name\": \"client-test\",\n" +
                "                \"instances\": [\n" +
                "                    {\n" +
                "                        \"instanceId\": \"DESKTOP-A84G64I:client-test:9002\",\n" +
                "                        \"appName\": \"client-test\",\n" +
                "                        \"ipAddr\": \"192.168.31.76\",\n" +
                "                        \"port\": 9002,\n" +
                "                        \"hostName\": \"DESKTOP-A84G64I\",\n" +
                "                        \"status\": \"UP\",\n" +
                "                        \"lastDirtyTimestamp\": null,\n" +
                "                        \"lastUpdatedTimestamp\": 1619699514988,\n" +
                "                        \"leaseInfo\": {\n" +
                "                            \"renewalIntervalInSecs\": 0,\n" +
                "                            \"durationInSecs\": 90,\n" +
                "                            \"registrationTimestamp\": 1619699514988,\n" +
                "                            \"lastRenewalTimestamp\": 1619699514988,\n" +
                "                            \"evictionTimestamp\": 0,\n" +
                "                            \"serviceUpTimestamp\": 1619699514988\n" +
                "                        },\n" +
                "                        \"actionType\": \"ADDED\",\n" +
                "                        \"id\": \"DESKTOP-A84G64I:client-test:9002\",\n" +
                "                        \"instanceInfoDirty\": false\n" +
                "                    }\n" +
                "                ]\n" +
                "            }\n" +
                "        ]\n" +
                "    }";

        System.out.println(JSONObject.toJSONString(strToApplications(s1)));
    }

    @Test
    public void test3(){
        String s22 = " {\n" +
                "                        \"instanceId\": \"DESKTOP-A84G64I:client-test:9002\",\n" +
                "                        \"appName\": \"client-test\",\n" +
                "                        \"ipAddr\": \"192.168.31.76\",\n" +
                "                        \"port\": 9002,\n" +
                "                        \"hostName\": \"DESKTOP-A84G64I\",\n" +
                "                        \"status\": \"UP\",\n" +
                "                        \"lastDirtyTimestamp\": null,\n" +
                "                        \"lastUpdatedTimestamp\": 1619699514988,\n" +
                "                        \"leaseInfo\": {\n" +
                "                            \"renewalIntervalInSecs\": 0,\n" +
                "                            \"durationInSecs\": 90,\n" +
                "                            \"registrationTimestamp\": 1619699514988,\n" +
                "                            \"lastRenewalTimestamp\": 1619699514988,\n" +
                "                            \"evictionTimestamp\": 0,\n" +
                "                            \"serviceUpTimestamp\": 1619699514988\n" +
                "                        },\n" +
                "                        \"actionType\": \"ADDED\",\n" +
                "                        \"id\": \"DESKTOP-A84G64I:client-test:9002\",\n" +
                "                        \"instanceInfoDirty\": false\n" +
                "                    }";
        InstanceInfo instanceInfo1 = JSON.parseObject(s22,InstanceInfo.class);
        System.out.println(JSON.toJSONString(instanceInfo1));
    }


    public Application strToApplication(String str){
        JSONObject json = JSON.parseObject(str);
        List<InstanceInfo> list = JSONObject.parseArray(json.getString("instance"),InstanceInfo.class);
        return new Application(json.getString("name"),list);
    }

    public Applications strToApplications(String str){
        JSONObject json = JSON.parseObject(str);
        JSONArray jsonArray = JSONArray.parseArray(json.getString("registeredApplications"));
        if (jsonArray == null) return null;
        List<Application> list = new ArrayList<>(json.size());
        for (int i = 0; i < jsonArray.size(); i++) {
            list.add(strToApplication(JSON.toJSONString(jsonArray.get(i))));
        }
        return new Applications(list);
    }



    @Test
    public void test4(){
       ConcurrentHashMap<String,String> map = new ConcurrentHashMap<>();

       map.put("client-test","sadasd");

        System.out.println(map.get("client-test"));

    }


    @Test
    public void test5(){
        ConcurrentLinkedQueue<Map<String,String>> recentlyChangedQueue = new ConcurrentLinkedQueue<>();
        ConcurrentHashMap<String,String> map = new ConcurrentHashMap<>();
        map.put("client-test","sadasd");
        recentlyChangedQueue.add(map);

        System.out.println(recentlyChangedQueue);

        Iterator<Map<String, String>> iterator = recentlyChangedQueue.iterator();
        while (iterator.hasNext()){
            Map<String, String> next = iterator.next();
            iterator.remove();
        }
        System.out.println(recentlyChangedQueue);



    }


}
