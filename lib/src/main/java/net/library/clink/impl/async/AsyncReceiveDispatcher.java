package net.library.clink.impl.async;

import lombok.extern.log4j.Log4j2;
import net.library.clink.core.IOArgs;
import net.library.clink.core.Packet;
import net.library.clink.core.ReceiveDispatcher;
import net.library.clink.core.ReceivePacket;
import net.library.clink.core.Receiver;
import net.library.clink.util.CloseUtil;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

@Log4j2
public class AsyncReceiveDispatcher implements ReceiveDispatcher, IOArgs.IOArgsEventProcessor {

    private final AtomicBoolean isClosed;

    private final Receiver receiver;

    private final ReceiveRacketCallback receiveRacketCallback;

    private IOArgs ioArgs;

    private ReceivePacket<?, ?> packet;

    private WritableByteChannel packetChannel;

    private long total;

    private long position;

    public AsyncReceiveDispatcher(Receiver receiver, ReceiveRacketCallback receiveRacketCallback) {
        this.receiver = receiver;
        this.receiver.setReceiveListener(this);
        this.receiveRacketCallback = receiveRacketCallback;
        this.isClosed = new AtomicBoolean(false);
        this.ioArgs = new IOArgs();
    }

    @Override
    public void start() {
        registerReceive();
    }

    @Override
    public void stop() {

    }

    @Override
    public void close() {
        if (isClosed.compareAndSet(false, true)) {
            completePacket(false);
        }
    }

    private void closeAndNotify() {
        CloseUtil.close(this);
    }

    private void registerReceive() {
        try {
            receiver.postReceiveAsync();
        } catch (IOException e) {
            closeAndNotify();
        }
    }

    /**
     * 解析数据到Packet
     */
    private void assemblePacket(IOArgs args) {
        if (Objects.isNull(packet)) {
            int length = args.readLength();
            // TODO: 临时解决方案
            byte type = length > 200 ? Packet.TYPE_STREAM_FILE : Packet.TYPE_MEMORY_STRING;
            packet = receiveRacketCallback.onArrivedNewPacket(type, length);
            packetChannel = Channels.newChannel(packet.open());
            total = length;
            position = 0;
        }
        try {
            int count = args.writeTo(packetChannel);
            position += count;
            // 检查是否完成一份Packet数据
            if (position == total) {
                completePacket(true);
            }
        } catch (IOException e) {
            log.error(e);
            completePacket(false);
        }
    }

    private void completePacket(boolean isSucceed) {
        CloseUtil.close(packet, packetChannel);
        if (Objects.nonNull(packet)) {
            receiveRacketCallback.onReceivePacketCompleted(packet);
        }
        packet = null;
        packetChannel = null;
    }

    @Override
    public IOArgs provideIOArgs() {
        int receiveSize;
        if (Objects.isNull(packet)) {
            receiveSize = 4;
        } else {
            receiveSize = (int) Math.min(total - position, ioArgs.capacity());
        }
        //  设置本次接收数据大小
        ioArgs.limit(receiveSize);
        return ioArgs;
    }

    @Override
    public void onConsumeCompleted(IOArgs args) {
        assemblePacket(args);
        // 继续接收下一条数据
        registerReceive();
    }

    @Override
    public void onConsumeFailed(IOArgs args, Exception ex) {
        log.error(ex);
    }

}
