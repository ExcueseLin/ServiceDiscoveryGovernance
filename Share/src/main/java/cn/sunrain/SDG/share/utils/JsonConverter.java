package cn.sunrain.SDG.share.utils;

import cn.sunrain.SDG.share.entity.Application;
import cn.sunrain.SDG.share.entity.Applications;
import cn.sunrain.SDG.share.entity.InstanceInfo;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lin
 * @date 2021/4/29 20:21
 */
public class JsonConverter {


    public static Application strToApplication(String str){
        JSONObject json = JSON.parseObject(str);
        List<InstanceInfo> list = JSONObject.parseArray(json.getString("instances"),InstanceInfo.class);
        return new Application(json.getString("name"),list);
    }

    public static Applications strToApplications(String str){
        JSONObject json = JSON.parseObject(str);
        JSONArray jsonArray = JSONArray.parseArray(json.getString("registeredApplications"));
        if (jsonArray == null) return null;
        List<Application> list = new ArrayList<>(json.size());
        for (int i = 0; i < jsonArray.size(); i++) {
            list.add(strToApplication(JSON.toJSONString(jsonArray.get(i))));
        }
        return new Applications(list);
    }




}
