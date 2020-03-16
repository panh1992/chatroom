package net.library.clink.core;

import java.io.InputStream;

/**
 * 发送的包定义
 */
public abstract class SendPacket<Stream extends InputStream> extends Packet<Stream> {

    private boolean isCanceled;

    public boolean isCanceled() {
        return isCanceled;
    }

}
