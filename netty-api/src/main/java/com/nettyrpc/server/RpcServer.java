package com.nettyrpc.server;

import com.nettyrpc.protocol.RpcDecoder;
import com.nettyrpc.protocol.RpcEncoder;
import com.nettyrpc.protocol.RpcRequest;
import com.nettyrpc.protocol.RpcResponse;
import com.nettyrpc.registry.ServiceRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Netty 支持 NIO(Socket tcp/udp) 的 RPC 服务器
 * spring自动把上下文环境对象调用ApplicationContextAware接口中的setApplicationContext方法
 * 实现Netty NIO RPC 向Zookeeper注册服务
 * @author wkm
 */
public class RpcServer implements ApplicationContextAware, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcServer.class);

    private String serverAddress;
    private ServiceRegistry serviceRegistry;
    // 存放接口名与服务对象之间的映射关系
    private Map<String, Object> handlerMap = new HashMap<>();

    private static ThreadPoolExecutor threadPoolExecutor;

    public RpcServer(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public RpcServer(String serverAddress, ServiceRegistry serviceRegistry) {
        this.serverAddress = serverAddress;
        this.serviceRegistry = serviceRegistry;
    }

    /**
     * ApplicationContextAware接口方法：服务在启动的时候spring扫描得到所有的服务接口及其实现
     */
    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
    	// 获取所有带有 RpcService 注解的 Spring Bean
    	Map<String, Object> serviceBeanMap = 
        		ctx.getBeansWithAnnotation(RpcService.class);
        if (MapUtils.isNotEmpty(serviceBeanMap)) {
            for (Object serviceBean : serviceBeanMap.values()) {
                String interfaceName = serviceBean.getClass().getAnnotation(RpcService.class).value().getName();
                handlerMap.put(interfaceName, serviceBean);
            }
        }
    }

    /**
     * InitializingBean接口方法：spring初始化bean
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
        	// 启动netty
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                    	
                        @Override
                        public void initChannel(SocketChannel channel) throws Exception {
                            channel.pipeline()
                            		/**
                            		 * 由于处理的是TCP消息，添加了TCP的粘包处理Handler
                            		 * 消息编解码时，开始4个字节表示消息的长度，即：先写消息的长度，再写消息
                            		 */
                                    .addLast(new LengthFieldBasedFrameDecoder(65536,0,4,0,0))
                                    .addLast(new RpcDecoder(RpcRequest.class)) // 将RPC处理请求进行解码
                                    .addLast(new RpcEncoder(RpcResponse.class)) // 将RPC返回响应进行编码
                                    .addLast(new RpcHandler(handlerMap)); // 处理RPC请求
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            String[] array = serverAddress.split(":");
            String host = array[0];
            int port = Integer.parseInt(array[1]);

            ChannelFuture future = bootstrap.bind(host, port).sync();
            LOGGER.debug("Server started on port {}", port);

            if (serviceRegistry != null) {
            	// 注册服务地址
                serviceRegistry.register(serverAddress);
            }
            future.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public static void submit(Runnable task){
        if(threadPoolExecutor == null){
            synchronized (RpcServer.class) {
                if(threadPoolExecutor == null){
                    threadPoolExecutor = new ThreadPoolExecutor(16, 16, 600L, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(65536));
                }
            }
        }
        threadPoolExecutor.submit(task);
    }
}
