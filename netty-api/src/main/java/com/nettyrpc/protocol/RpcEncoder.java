package com.nettyrpc.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * RPC Encoder
 * @author wkm
 * @param <I>
 */
@SuppressWarnings("rawtypes")
public class RpcEncoder extends MessageToByteEncoder {

    private Class<?> genericClass;

    public RpcEncoder(Class<?> genericClass) {
        this.genericClass = genericClass;
    }

    /**
     * 实现Netty编码抽象方法
     */
    @Override
    public void encode(ChannelHandlerContext ctx, Object in, ByteBuf out) throws Exception {
        if (genericClass.isInstance(in)) {
            byte[] data = SerializationUtil.serialize(in);
            // Not use this, have some bugs
            //byte[] data = JsonUtil.serialize(in);
            out.writeInt(data.length);
            out.writeBytes(data);
        }
    }
}
