package cn.sunrain.SDG.share.entity;

import com.alibaba.fastjson.annotation.JSONCreator;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.Setter;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 应用表 Application
 * 一个应用有多个实例InstanceInfo
 * @author lin
 */
@Setter
public class Application implements Serializable {

    /** 应用名称 （现设计为 instanceInfo中的应用名）*/
    @JSONField(name = "name")
    private String name = "UNKNOW";

    /**存储实例们的Set集合（自带去重，实例id相同代表同一实例）*/
    @JSONField(name = "instances")
    private final Set<InstanceInfo> instances;

    /** 缓存 key是实例id，value是对应的实例对象 */
    @JSONField(serialize = false)
    private final Map<String, InstanceInfo> instancesMap;


    public Application() {
        instances = new LinkedHashSet<>();
        instancesMap = new ConcurrentHashMap<>();
    }
    public Application(String name) {
        this();
        this.name = name;
    }

    /**
     * 反序列化时通过此构造器来生成一个Application应用
     */
    @JSONCreator
    public Application(String name,@JSONField(name = "application") List<InstanceInfo> instances) {
        this(name);
        if (instances != null && instances.size() > 0){
            for (InstanceInfo instanceInfo : instances) {
                addInstance(instanceInfo);
            }
        }

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<InstanceInfo> getInstances() {
        synchronized (instances) {
            return new ArrayList<>(this.instances);
        }
    }


    /**
     *  添加一个实例（放在Set最上面。这是先remove后add的目的）
     * */
    public void addInstance(InstanceInfo i) {
        instancesMap.put(i.getId(),i);
        synchronized (instances) {
            instances.remove(i);
            instances.add(i);
        }
    }

    /**
     * 删除一个实例
     */
    public void removeInstance(InstanceInfo i) {
        instancesMap.remove(i.getId());
        synchronized (instances) {
            instances.remove(i);
        }
    }


    /**
     *  根据id拿到指定的Instance实例集合
     */
    public InstanceInfo getByInstanceId(String id) {
        return instancesMap.get(id);
    }

    /**
     * 获取所有实例总数
     */
    public int size() {
        return instances.size();
    }


}
