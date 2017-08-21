package com.nettyrpc.service;

import com.nettyrpc.pojo.po.Person;

/**
 * 服务接口(发布为jar，server与client都需要使用)
 */
public interface HelloService {
    String hello(String name);

    String hello(Person person);
}
