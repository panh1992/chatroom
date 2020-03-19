package net.library.clink.frame;

import net.library.clink.core.Frame;
import net.library.clink.core.IOArgs;

import java.io.IOException;

/**
 * 取消发送数据
 */
public class CancelSendFrame extends AbsSendFrame {

    public CancelSendFrame(short identifier) {
        super(0, TYPE_PACKET_SEND_CANCEL, FLAG_NONE, identifier);
    }

    @Override
    protected int consumeBody(IOArgs args) throws IOException {
        return 0;
    }

    @Override
    public Frame nextFrame() {
        return null;
    }
}
