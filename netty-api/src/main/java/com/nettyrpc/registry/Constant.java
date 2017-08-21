package com.nettyrpc.registry;

/**
 * ZooKeeper constant
 *
 * @author wkm
 */
public interface Constant {

	// zookeeper会话超时
    int ZK_SESSION_TIMEOUT = 5000;

    // zookeeper注册路径
    String ZK_REGISTRY_PATH = "/registry";
    
    // zookeeper数据路径
    String ZK_DATA_PATH = ZK_REGISTRY_PATH + "/data";
}
