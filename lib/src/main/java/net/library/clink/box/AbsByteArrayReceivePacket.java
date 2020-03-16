package net.library.clink.box;

import net.library.clink.core.ReceivePacket;

import java.io.ByteArrayOutputStream;

/**
 * 定义最基础的基与{@link ByteArrayOutputStream}的输出接收包
 *
 * @param <Entity> 对应的实体泛型，需定义{@link ByteArrayOutputStream}最终转化为什么实体
 */
public abstract class AbsByteArrayReceivePacket<Entity> extends ReceivePacket<ByteArrayOutputStream, Entity> {

    public AbsByteArrayReceivePacket(long length) {
        super(length);
    }

    /**
     * 创建流操作直接返回一个{@link ByteArrayOutputStream}流
     *
     * @return {@link ByteArrayOutputStream}
     */
    @Override
    protected final ByteArrayOutputStream createStream() {
        return new ByteArrayOutputStream((int) length);
    }

}
