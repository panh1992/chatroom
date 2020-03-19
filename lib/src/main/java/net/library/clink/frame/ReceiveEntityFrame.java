package net.library.clink.frame;

import net.library.clink.core.IOArgs;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;

/**
 * 接收实体帧
 */
public class ReceiveEntityFrame extends AbsReceiveFrame {

    private WritableByteChannel writableByteChannel;

    public ReceiveEntityFrame(byte[] header) {
        super(header);
    }

    public void bindPacketChannel(WritableByteChannel writableByteChannel) {
        this.writableByteChannel = writableByteChannel;
    }

    @Override
    protected int consumeBody(IOArgs args) throws IOException {
        return Objects.isNull(writableByteChannel) ? args.setEmpty(bodyRemaining) : args.writeTo(writableByteChannel);
    }
}
