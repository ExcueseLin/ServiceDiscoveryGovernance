package cn.sunrain.SDG.share.entity;

import com.alibaba.fastjson.annotation.JSONCreator;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.Setter;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * 注册表 Applications
 * 一个注册表中有多个应用Application
 *
 * Apps< Name , App>  +  App< Id, InstanceInfo >
 *   =  Apps <Name , < Id, InstanceInfo>...>
 * @author Lin
 */
@Setter
public class Applications implements Serializable {

    /**
     * 包含的所有Application应用们。使用AbstractQueue装载，
     * 实际是个ConcurrentLinkedQueue队列
     * （特点：FIFO）
     */
    @JSONField(name = "applications")
    private final AbstractQueue<Application> applications;

    /**
     * map缓存。key是应用名，value是应用实例本身。
     */
    @JSONField(name = "appNameApplicationMap")
    private final Map<String, Application> appNameApplicationMap;

    public Applications() {
        this.applications = new ConcurrentLinkedQueue<Application>();
        this.appNameApplicationMap = new ConcurrentHashMap<String, Application>();
    }


    @JSONCreator
    public Applications(@JSONField(name = "application") List<Application> registeredApplications) {
        this();
        if (registeredApplications!=null && registeredApplications.size() > 0){
            for (Application app : registeredApplications) {
                this.addApplication(app);
            }
        }

    }

    /**
     * 添加一个应用
     */
    public void addApplication(Application app) {
        appNameApplicationMap.put(app.getName().toUpperCase(Locale.ROOT), app);

        applications.add(app);
    }


    /**
     * 移除一个Application应用
     */
    public void removeApplication(Application app) {
        this.appNameApplicationMap.remove(app.getName().toUpperCase(Locale.ROOT));
        this.applications.remove(app);
    }


    /**
     * 得到已经注册的Applications应用们
     */
    public List<Application> getRegisteredApplications() {
        return new ArrayList<>(this.applications);
    }

    /**
     * 根据应用名获取应用
     */
    public Application getRegisteredApplications(String appName) {
        return appNameApplicationMap.get(appName.toUpperCase(Locale.ROOT));
    }


    public int size() {
        return applications.stream().mapToInt(Application::size).sum();
    }



}

