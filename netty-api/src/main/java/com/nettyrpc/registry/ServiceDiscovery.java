package com.nettyrpc.registry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;

import com.nettyrpc.client.ConnectManage;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 从Zookeeper上发现服务，获取服务地址，添加watch事件
 * @author wkm

 */
public class ServiceDiscovery {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceDiscovery.class);

    private String registryAddress;
    private ZooKeeper zookeeper;
    
    /**
     * 这里每次使用代理远程调用服务，从Zookeeper上获取可用的服务地址，通过RpcClient send一个Request，等待该Request的Response返回。
     * 这里原代码有个比较严重的bug，在原给出的简单的Test中是很难测出来的，原使用了obj的wait和notifyAll来等待Response返回，
     * 会出现“假死等待”的情况：一个Request发送出去后，在obj.wait()调用之前可能Response就返回了，
     * 这时候在channelRead0里已经拿到了Response并且obj.notifyAll()已经在obj.wait()之前调用了，
     * 此时send后再obj.wait()就出现了假死等待，客户端就一直等待在这里。
     * 使用CountDownLatch可以解决这个问题：1个同步辅助类，在完成一组正在其他线程中执行的操作之前，它允许1个或多个线程一直等待。
     */
    private CountDownLatch latch = new CountDownLatch(1);

    private volatile List<String> dataList = new ArrayList<>();

    public ServiceDiscovery(String registryAddress) {
        this.registryAddress = registryAddress;
        zookeeper = connectServer();
        if (zookeeper != null) {
            this.watchNode(zookeeper);
        }
    }

    public String discover() {
        String data = null;
        int size = dataList.size();
        if (size > 0) {
            if (size == 1) {
                data = dataList.get(0);
                LOGGER.debug("using only data: {}", data);
            } else {
                data = dataList.get(ThreadLocalRandom.current().nextInt(size));
                LOGGER.debug("using random data: {}", data);
            }
        }
        return data;
    }

    private ZooKeeper connectServer() {
        ZooKeeper zk = null;
        try {
            zk = new ZooKeeper(registryAddress, Constant.ZK_SESSION_TIMEOUT, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    if (event.getState() == Event.KeeperState.SyncConnected) {
                        // 当前线程调用此方法，则计数减一
                    	latch.countDown();
                    }
                }
            });
            // 此方法会一直阻塞当前线程，直到计时器的值为0
            latch.await();
        } catch (IOException | InterruptedException e) {
            LOGGER.error("", e);
        }
        return zk;
    }

    /**
     * 每次服务地址节点发生变化，都需要再次watchNode，获取新的服务地址列表
     * Watch是一次性的，每次都需要重新注册
     */
    private void watchNode(final ZooKeeper zk) {
        try {
            List<String> nodeList = zk.getChildren(Constant.ZK_REGISTRY_PATH, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    if (event.getType() == Event.EventType.NodeChildrenChanged) {
                        watchNode(zk);
                    }
                }
            });
            List<String> dataList = new ArrayList<>();
            for (String node : nodeList) {
                byte[] bytes = zk.getData(Constant.ZK_REGISTRY_PATH + "/" + node, false, null);
                dataList.add(new String(bytes));
            }
            LOGGER.debug("node data: {}", dataList);
            this.dataList = dataList;

            LOGGER.debug("Service discovery triggered updating connected server node.");
            this.UpdateConnectedServer();
        } catch (KeeperException | InterruptedException e) {
            LOGGER.error("", e);
        }
    }

    private void UpdateConnectedServer(){
        ConnectManage.getInstance().updateConnectedServer(this.dataList);
    }

    public void stop(){
        if(zookeeper!=null){
            try {
                zookeeper.close();
            } catch (InterruptedException e) {
                LOGGER.error("", e);
            }
        }
    }
}
