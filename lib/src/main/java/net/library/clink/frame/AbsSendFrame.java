package net.library.clink.frame;

import net.library.clink.core.Frame;
import net.library.clink.core.IOArgs;

import java.io.IOException;

public abstract class AbsSendFrame extends Frame {

    volatile byte headerRemaining = FRAME_HEADER_LENGTH;

    volatile int bodyRemaining;

    public AbsSendFrame(int length, byte type, byte flag, short identifier) {
        super(length, type, flag, identifier);
        this.bodyRemaining = length;
    }

    @Override
    public synchronized boolean handle(IOArgs args) throws IOException {
        try {
            args.limit(headerRemaining + bodyRemaining);
            args.startWriting();
            if (headerRemaining > 0 && args.remained()) {
                headerRemaining -= consumeHeader(args);
            }
            if (headerRemaining == 0 && args.remained() && bodyRemaining > 0) {
                bodyRemaining -= consumeBody(args);
            }
            return headerRemaining == 0 && bodyRemaining == 0;
        } finally {
            args.finishWriting();
        }
    }

    private byte consumeHeader(IOArgs args) {
        int offset = header.length - headerRemaining;
        return (byte) args.readFrom(header, offset, headerRemaining);
    }

    @Override
    public int getConsumableLength() {
        return headerRemaining + bodyRemaining;
    }

    protected abstract int consumeBody(IOArgs args) throws IOException;

    /**
     * 是否已经处于发送数据中，如果已经发送了部分数据则返回True
     * 只要头部数据已经开始消费，则肯定已经处于发送数据中
     *
     * @return True，已发送部分数据
     */
    protected synchronized boolean isSending() {
        return headerRemaining < FRAME_HEADER_LENGTH;
    }

}
