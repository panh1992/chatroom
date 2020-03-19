package net.library.clink.frame;

import net.library.clink.core.Frame;
import net.library.clink.core.IOArgs;
import net.library.clink.core.SendPacket;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.util.Objects;

/**
 * 发送数据内容
 */
public class SendEntityFrame extends AbsSendPacketFrame {

    private final long unConsumeEntityLength;

    private final ReadableByteChannel channel;

    SendEntityFrame(short identifier, long entityLength, ReadableByteChannel channel,
                    SendPacket<? extends InputStream> packet) {
        super((int) Math.min(entityLength, MAX_CAPACITY), TYPE_PACKET_ENTITY, FLAG_NONE, identifier, packet);
        this.unConsumeEntityLength = entityLength - bodyRemaining;
        this.channel = channel;
    }

    @Override
    protected int consumeBody(IOArgs args) throws IOException {
        if (Objects.isNull(packet)) {
            // 已终止当前帧，则填充空数据
            return args.fillEmpty(bodyRemaining);
        }
        return args.readFrom(channel);
    }

    @Override
    public Frame buildNextFrame() {
        if (unConsumeEntityLength == 0) {
            return null;
        }
        return new SendEntityFrame(getBodyIdentifier(), unConsumeEntityLength, channel, packet);
    }

}
