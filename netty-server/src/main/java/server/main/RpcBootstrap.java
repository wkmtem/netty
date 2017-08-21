package server.main;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class RpcBootstrap {

    @SuppressWarnings("resource")
	public static void main(String[] args) {
        new ClassPathXmlApplicationContext("classpath:server-spring.xml");
    }
}
