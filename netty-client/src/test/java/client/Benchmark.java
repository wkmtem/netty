package client;

import com.nettyrpc.client.RpcClient;
import com.nettyrpc.registry.ServiceDiscovery;
import com.nettyrpc.service.HelloService;

/**
 * Created by luxiaoxun on 2016-03-11.
 */
public class Benchmark {

    public static void main(String[] args) throws InterruptedException {

        ServiceDiscovery serviceDiscovery = new ServiceDiscovery("192.168.0.200:2181");
        final RpcClient rpcClient = new RpcClient(serviceDiscovery);

        int threadNum = 10; // 线程数
        final int requestNum = 100; // RPC请求服务次数
        Thread[] threads = new Thread[threadNum];

        long startTime = System.currentTimeMillis();
        //benchmark for sync call
        for(int i = 0; i < threadNum; ++ i){
            threads[i] = new Thread(new Runnable(){
                @Override
                public void run() {
                    for (int i = 0; i < requestNum; i++) {
                        final HelloService syncClient = rpcClient.create(HelloService.class);
                        String result = syncClient.hello("Java " + i);
                        System.out.println(">>>>>>>>>> result : " + result);
                        if (!result.equals("Hello! Java " + i))
                            System.out.println("error = " + result);
                    }
                }
            });
            threads[i].start();
        }
        
        // 所有线程执行完毕后，再继续执行Thread.join()后面的代码
        for(int i = 0; i < threads.length; ++ i){
            threads[i].join();
        }
        System.out.println(">>>>>>>>>> 线程执行完毕 <<<<<<<<<<");
        long timeCost = (System.currentTimeMillis() - startTime);
        String msg = String.format("Sync call total-time-cost:%sms, req/s=%s",timeCost,((double)(requestNum * threadNum)) / timeCost * 1000);
        System.out.println(msg);

        rpcClient.stop();
    }
}
