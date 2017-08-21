package com.nettyrpc.service;

import java.util.List;

import com.nettyrpc.pojo.po.Person;

/**
 * Created by luxiaoxun on 2016-03-10.
 */
public interface PersonService {
    List<Person> GetTestPerson(String name, int num);
}
