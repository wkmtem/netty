package com.nettyrpc.client;

import com.nettyrpc.client.proxy.IAsyncObjectProxy;
import com.nettyrpc.client.proxy.AsyncObjectProxyImpl;
import com.nettyrpc.registry.ServiceDiscovery;

import java.lang.reflect.Proxy;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * RPC Client（Create RPC proxy）
 * @author wkm
 */
public class RpcClient {

    private ServiceDiscovery serviceDiscovery;
    private static ThreadPoolExecutor threadPoolExecutor = 
    		new ThreadPoolExecutor(16, 16, 600L, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(65536));

    // 未发现带参serverAddress的实例化 2017-7-22 wkm注释
    /*private String serverAddress;
    public RpcClient(String serverAddress) {
        this.serverAddress = serverAddress;
    }*/

    public RpcClient(ServiceDiscovery serviceDiscovery) {
        this.serviceDiscovery = serviceDiscovery;
    }

    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> interfaceClass) {
        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                new AsyncObjectProxyImpl<T>(interfaceClass)
        );
    }

    public static <T> IAsyncObjectProxy createAsync(Class<T> interfaceClass) {
        return new AsyncObjectProxyImpl<T>(interfaceClass);
    }

    public static void submit(Runnable task){
        threadPoolExecutor.submit(task);
    }

    public void stop() {
        threadPoolExecutor.shutdown();
        serviceDiscovery.stop();
        ConnectManage.getInstance().stop();
    }
}

