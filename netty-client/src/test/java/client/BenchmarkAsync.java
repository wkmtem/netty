package client;

import com.nettyrpc.client.RPCFuture;
import com.nettyrpc.client.RpcClient;
import com.nettyrpc.client.proxy.IAsyncObjectProxy;
import com.nettyrpc.registry.ServiceDiscovery;
import com.nettyrpc.service.HelloService;

import java.util.concurrent.TimeUnit;

/**
 * Created by luxiaoxun on 2016/3/16.
 */
public class BenchmarkAsync {
    public static void main(String[] args) throws InterruptedException {
        ServiceDiscovery serviceDiscovery = new ServiceDiscovery("192.168.0.200:2181");
        final RpcClient rpcClient = new RpcClient(serviceDiscovery);

        int threadNum = 10;
        final int requestNum = 20;
        Thread[] threads = new Thread[threadNum];

        long startTime = System.currentTimeMillis();
        //benchmark for async call
        for (int i = 0; i < threadNum; ++i) {
            threads[i] = new Thread(new Runnable() {
            	/**
            	 * 客户端请求异步处理，不需要同步等待
            	 * 发送一个异步请求，返回Feature，通过Feature的callback机制获取结果
            	 */
                @Override
                public void run() {
                    for (int i = 0; i < requestNum; i++) {
                        try {
                            IAsyncObjectProxy client = rpcClient.createAsync(HelloService.class);
                            RPCFuture helloFuture = client.call("hello", Integer.toString(i));
                            String result = (String) helloFuture.get(3000, TimeUnit.MILLISECONDS);
                            //System.out.println(result);
                            if (!result.equals("Hello! " + i))
                                System.out.println("error = " + result);
                        } catch (Exception e) {
                            System.out.println(e);
                        }
                    }
                }
            });
            threads[i].start();
        }
        for (int i = 0; i < threads.length; i++) {
            threads[i].join();
        }
        long timeCost = (System.currentTimeMillis() - startTime);
        String msg = String.format("Async call total-time-cost:%sms, req/s=%s", timeCost, ((double) (requestNum * threadNum)) / timeCost * 1000);
        System.out.println(msg);

        rpcClient.stop();

    }
}
