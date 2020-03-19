package net.library.clink.frame;

import net.library.clink.core.IOArgs;

import java.io.IOException;

/**
 * 取消发送数据, 接收实现
 */
public class CancelReceiveFrame extends AbsReceiveFrame {

    CancelReceiveFrame(byte[] header) {
        super(header);
    }

    @Override
    protected int consumeBody(IOArgs args) throws IOException {
        return 0;
    }

}
