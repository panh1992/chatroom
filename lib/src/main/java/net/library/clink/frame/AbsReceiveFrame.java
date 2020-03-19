package net.library.clink.frame;

import net.library.clink.core.Frame;
import net.library.clink.core.IOArgs;

import java.io.IOException;

public abstract class AbsReceiveFrame extends Frame {

    // 帧体可读写区域大小
    volatile int bodyRemaining;

    public AbsReceiveFrame(byte[] header) {
        super(header);
        this.bodyRemaining = getBodyLength();
    }

    @Override
    public synchronized boolean handle(IOArgs args) throws IOException {
        if (bodyRemaining == 0) {
            // 已读取所有数据
            return true;
        }
        bodyRemaining -= consumeBody(args);
        return bodyRemaining == 0;
    }

    @Override
    public int getConsumableLength() {
        return bodyRemaining;
    }

    @Override
    public Frame nextFrame() {
        return null;
    }

    protected abstract int consumeBody(IOArgs args) throws IOException;

}
