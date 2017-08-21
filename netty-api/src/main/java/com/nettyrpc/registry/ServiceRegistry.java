package com.nettyrpc.registry;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 连接Zookeeper注册中心，注册服务地址，创建服务注册目录
 *
 * @author wkm
 */
public class ServiceRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceRegistry.class);

    private String registryAddress;
    // 1个同步辅助类，在完成一组正在其他线程中执行的操作之前，它允许1个或多个线程一直等待。
    private CountDownLatch latch = new CountDownLatch(1);

    public ServiceRegistry(String registryAddress) {
        this.registryAddress = registryAddress;
    }

    public void register(String data) {
        if (data != null) {
            ZooKeeper zk = connectServer();
            if (zk != null) {
                AddRootNode(zk); // Add root node if not exist
                createNode(zk, data);
            }
        }
    }

    private ZooKeeper connectServer() {
        ZooKeeper zk = null;
        try {
            zk = new ZooKeeper(registryAddress, Constant.ZK_SESSION_TIMEOUT, new Watcher() {
            	/**
            	 * 判断是否已连接ZK,连接后计数器递减
            	 */
                @Override
                public void process(WatchedEvent event) {
                    if (event.getState() == Event.KeeperState.SyncConnected) {
                        latch.countDown();
                    }
                }
            });
            // 若计数器不为0,则等待
            latch.await();
        } catch (IOException e) {
            LOGGER.error("", e);
        }
        catch (InterruptedException ex){
            LOGGER.error("", ex);
        }
        return zk;
    }

    /**
     * 判断服务父节点是否存在，如果不存在则添加一个PERSISTENT的服务父节点。
     * 启动服务时多了点判断，但不需要手动命令添加服务父节点。
     */
    private void AddRootNode(ZooKeeper zk){
        try {
            Stat s = zk.exists(Constant.ZK_REGISTRY_PATH, false);
            if (s == null) {
                zk.create(Constant.ZK_REGISTRY_PATH, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (KeeperException e) {
            LOGGER.error(e.toString());
        } catch (InterruptedException e) {
            LOGGER.error(e.toString());
        }
    }

    private void createNode(ZooKeeper zk, String data) {
        try {
            byte[] bytes = data.getBytes();
            String path = zk.create(Constant.ZK_DATA_PATH, bytes, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            LOGGER.debug("create zookeeper node ({} => {})", path, data);
        } catch (KeeperException e) {
            LOGGER.error("", e);
        }
        catch (InterruptedException ex){
            LOGGER.error("", ex);
        }
    }
}