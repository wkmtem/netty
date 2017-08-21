package server.service.impl;

import com.nettyrpc.pojo.po.Person;
import com.nettyrpc.server.RpcService;
import com.nettyrpc.service.HelloService;

/**
 * 使用注解标注要发布的服务(仅server使用)
 */
@RpcService(HelloService.class)
public class HelloServiceImpl implements HelloService {

    @Override
    public String hello(String name) {
        return "Hello! " + name;
    }

    @Override
    public String hello(Person person) {
        return "Hello! " + person.getFirstName() + " " + person.getLastName();
    }
}
