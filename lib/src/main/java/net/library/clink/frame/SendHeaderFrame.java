package net.library.clink.frame;

import net.library.clink.core.Frame;
import net.library.clink.core.IOArgs;
import net.library.clink.core.SendPacket;

import java.io.InputStream;
import java.nio.channels.Channels;
import java.util.Objects;

/**
 * 发送头部数据
 */
public class SendHeaderFrame extends AbsSendPacketFrame {

    public static final int PACKET_HEADER_FRAME_MIN_LENGTH = 6;

    private final byte[] body;

    public SendHeaderFrame(short identifier, SendPacket<? extends InputStream> packet) {
        super(PACKET_HEADER_FRAME_MIN_LENGTH, TYPE_PACKET_HEADER, FLAG_NONE, identifier, packet);
        final long packetLength = packet.length();
        final byte packetType = packet.type();
        final byte[] packetHeaderInfo = packet.headerInfo();
        this.body = new byte[bodyRemaining];
        this.body[0] = (byte) (packetLength >> 32);
        this.body[1] = (byte) (packetLength >> 24);
        this.body[2] = (byte) (packetLength >> 16);
        this.body[3] = (byte) (packetLength >> 8);
        this.body[4] = (byte) packetLength;

        this.body[5] = packetType;

        if (Objects.nonNull(packetHeaderInfo)) {
            System.arraycopy(packetHeaderInfo, 0, body, PACKET_HEADER_FRAME_MIN_LENGTH, packetHeaderInfo.length);
        }
    }

    @Override
    protected int consumeBody(IOArgs args) {
        int offset = body.length - bodyRemaining;
        return args.readFrom(body, offset, bodyRemaining);
    }

    @Override
    public Frame buildNextFrame() {
        return new SendEntityFrame(getBodyIdentifier(), packet.length(), Channels.newChannel(packet.open()), packet);
    }

}
