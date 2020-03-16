package net.library.clink.core;

import java.io.Closeable;

/**
 * 接受的数据调度封装
 * 把一份或者多份IOArgs组合成一份Packet
 */
public interface ReceiveDispatcher extends Closeable {

    void start();

    void stop();

    interface ReceiveRacketCallback {

        ReceivePacket<?, ?> onArrivedNewPacket(byte type, long length);

        void onReceivePacketCompleted(ReceivePacket<?, ?> packet);

    }

}
