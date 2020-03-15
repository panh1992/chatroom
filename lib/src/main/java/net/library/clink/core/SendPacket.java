package net.library.clink.core;

import java.io.InputStream;

/**
 * 发送的包定义
 */
public abstract class SendPacket<T extends InputStream> extends Packet<T> {

    private boolean isCanceled;

    public boolean isCanceled() {
        return isCanceled;
    }

}
