package com.nettyrpc.client.proxy;

import com.nettyrpc.client.RPCFuture;

/**
 * 异步对象代理接口
 * Created by wkm on 2016/3/16.
 */
public interface IAsyncObjectProxy {
    public RPCFuture call(String funcName, Object... args);
}