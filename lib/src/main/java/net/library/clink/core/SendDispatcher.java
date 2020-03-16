package net.library.clink.core;

import java.io.Closeable;
import java.io.InputStream;

/**
 * 发送数据的调度者
 * 缓存所有需要发送的数据，通过队列对数据进行发送
 * 并且在发送数据时，实现对数据的基本包装
 */
public interface SendDispatcher extends Closeable {

    /**
     * 发送数据
     *
     * @param packet 数据
     */
    void send(SendPacket<? extends InputStream> packet);

    /**
     * 取消发送数据
     *
     * @param packet 数据
     */
    void cancel(SendPacket<? extends InputStream> packet);

}