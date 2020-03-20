package net.library.clink.impl.async;

import lombok.extern.log4j.Log4j2;
import net.library.clink.core.Frame;
import net.library.clink.core.IOArgs;
import net.library.clink.core.ReceivePacket;
import net.library.clink.frame.AbsReceiveFrame;
import net.library.clink.frame.CancelReceiveFrame;
import net.library.clink.frame.ReceiveEntityFrame;
import net.library.clink.frame.ReceiveFrameFactory;
import net.library.clink.frame.ReceiveHeaderFrame;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 写数据到Packet中
 */
@Log4j2
class AsyncPacketWriter implements Closeable {

    private final PacketProvider provider;

    private final Map<Short, PacketModel> packetModelMap;

    private final IOArgs args;

    private volatile Frame tempFrame;

    {
        this.packetModelMap = new HashMap<>();
        this.args = new IOArgs();
    }

    public AsyncPacketWriter(PacketProvider provider) {
        this.provider = provider;
    }

    /**
     * 构建一份数据容纳封装
     */
    synchronized IOArgs takeTOArgs() {
        args.limit(Objects.isNull(tempFrame) ? Frame.FRAME_HEADER_LENGTH :
                tempFrame.getConsumableLength());
        return args;
    }

    /**
     * 消费IOArgs中的数据
     */
    synchronized void consumeIOArgs(IOArgs args) {
        if (Objects.isNull(tempFrame)) {
            Frame temp;
            do {
                temp = buildNewFrame(args);
            } while (Objects.isNull(temp) && args.remained());
            if (Objects.isNull(temp)) {
                // 最终消费数据完成，但没有可消费区间，则直接返回
                return;
            }
            tempFrame = temp;
            if (!args.remained()) {
                return;
            }
        }

        do {
            try {
                if (tempFrame.handle(args)) {
                    if (tempFrame instanceof ReceiveHeaderFrame) {
                        ReceiveHeaderFrame headerFrame = (ReceiveHeaderFrame) tempFrame;
                        ReceivePacket<? extends OutputStream, ?> packet = provider.takePacket(headerFrame.getPacketType(),
                                headerFrame.getPacketLength(), headerFrame.getPacketHeaderInfo());
                        appendNewPacket(headerFrame.getBodyIdentifier(), packet);
                    } else if (tempFrame instanceof ReceiveEntityFrame) {
                        completeEntityFrame((ReceiveEntityFrame) tempFrame);
                    }
                    tempFrame = null;
                    break;
                }
            } catch (IOException e) {
                log.error(e);
            }
        } while (args.remained());

    }

    private void completeEntityFrame(ReceiveEntityFrame frame) {
        synchronized (packetModelMap) {
            short identifier = frame.getBodyIdentifier();
            int length = frame.getBodyLength();
            PacketModel model = packetModelMap.get(identifier);
            if (Objects.isNull(model)) {
                return;
            }
            model.unreceivedLength -= length;
            if (model.unreceivedLength <= 0) {
                provider.completedPacket(model.packet, true);
                packetModelMap.remove(identifier);
            }
        }
    }

    private void appendNewPacket(short identifier, ReceivePacket<? extends OutputStream, ?> packet) {
        synchronized (packetModelMap) {
            PacketModel model = new PacketModel(packet);
            packetModelMap.put(identifier, model);
        }
    }

    private Frame buildNewFrame(IOArgs args) {
        AbsReceiveFrame frame = ReceiveFrameFactory.createInstance(args);
        if (frame instanceof CancelReceiveFrame) {
            cancelReceivePacket(frame.getBodyIdentifier());
            return null;
        } else if (frame instanceof ReceiveEntityFrame) {
            WritableByteChannel channel = getPacketChannel(frame.getBodyIdentifier());
            ((ReceiveEntityFrame) frame).bindPacketChannel(channel);
        }
        return frame;
    }

    private WritableByteChannel getPacketChannel(short identifier) {
        synchronized (packetModelMap) {
            PacketModel model = packetModelMap.get(identifier);
            return Objects.isNull(model) ? null : model.writableByteChannel;
        }
    }

    private void cancelReceivePacket(short identifier) {
        synchronized (packetModelMap) {
            PacketModel model = packetModelMap.get(identifier);
            if (Objects.nonNull(model)) {
                ReceivePacket<? extends OutputStream, ?> packet = model.packet;
                provider.completedPacket(packet, false);
            }
        }
    }

    /**
     * 关闭操作， 关闭时若当前还有正在接收的Packet，则尝试停止对应的Packet接收
     */
    @Override
    public synchronized void close() {
        synchronized (packetModelMap) {
            packetModelMap.values().forEach(model ->
                    provider.completedPacket(model.packet, false));
            packetModelMap.clear();
        }
    }

    /**
     * Packet提供者
     */
    public interface PacketProvider {

        ReceivePacket<? extends OutputStream, ?> takePacket(byte type, long length, byte[] headerInfo);

        void completedPacket(ReceivePacket<? extends OutputStream, ?> packet, boolean isSucceed);

    }

    /**
     * 对于接收包的简单封装
     * 用以提供Packet、通道、未接收数据长度信息存储
     */
    static class PacketModel {

        final ReceivePacket<? extends OutputStream, ?> packet;

        final WritableByteChannel writableByteChannel;

        volatile long unreceivedLength;

        PacketModel(ReceivePacket<? extends OutputStream, ?> packet) {
            this.packet = packet;
            this.writableByteChannel = Channels.newChannel(packet.open());
            this.unreceivedLength = packet.length();
        }

    }

}
