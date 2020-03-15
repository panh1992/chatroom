package net.library.clink.impl.async;

import lombok.extern.log4j.Log4j2;
import net.library.clink.core.IOArgs;
import net.library.clink.core.SendDispatcher;
import net.library.clink.core.SendPacket;
import net.library.clink.core.Sender;
import net.library.clink.util.CloseUtil;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

@Log4j2
public class AsyncSendDispatcher implements SendDispatcher, IOArgs.IOArgsEventProcessor {

    private final Sender sender;

    private final Queue<SendPacket> sendPacketQueue;

    private final AtomicBoolean isSending;

    private final AtomicBoolean isClosed;

    private IOArgs ioArgs;

    private SendPacket<?> packet;

    private ReadableByteChannel packetChannel;

    private long total;

    private long position;

    public AsyncSendDispatcher(Sender sender) {
        this.sender = sender;
        this.sender.setSendListener(this);
        this.ioArgs = new IOArgs();
        this.sendPacketQueue = new ConcurrentLinkedDeque<>();
        this.isSending = new AtomicBoolean();
        this.isClosed = new AtomicBoolean(false);
    }

    @Override
    public void send(SendPacket packet) {
        sendPacketQueue.offer(packet);
        if (isSending.compareAndSet(false, true)) {
            sendNextPacket();
        }
    }

    @Override
    public void cancel(SendPacket packet) {

    }

    private SendPacket takePacket() {
        SendPacket packet = sendPacketQueue.poll();
        if (Objects.nonNull(packet) && packet.isCanceled()) {
            // 已取消，不用发送
            return takePacket();
        }
        return packet;
    }

    private void sendNextPacket() {
        if (Objects.nonNull(packet)) {
            CloseUtil.close(packet);
        }
        packet = takePacket();
        if (Objects.isNull(packet)) {
            // 队列为空，取消状态发送
            isSending.set(false);
            return;
        }
        total = packet.length();
        position = 0;
        sendCurrentPacket();
    }

    private void sendCurrentPacket() {
        if (position >= total) {
            completePacket(position == total);
            sendNextPacket();
            return;
        }
        try {
            sender.postSendAsync();
        } catch (IOException e) {
            closeAndNotify();
        }
    }

    /**
     * 完成Packet发送
     *
     * @param isSucceed 是否成功
     */
    private void completePacket(boolean isSucceed) {
        if (Objects.isNull(packet)) {
            return;
        }
        CloseUtil.close(packet, packetChannel);
        packet = null;
        packetChannel = null;
        total = 0;
        position = 0;
    }

    private void closeAndNotify() {
        CloseUtil.close(this);
    }

    @Override
    public void close() {
        if (isClosed.compareAndSet(false, true)) {
            isSending.set(false);
            // 异常关闭导致的完成
            completePacket(false);
        }
    }

    @Override
    public IOArgs provideIOArgs() {
        // 开始， 清理
        ioArgs.startWriting();
        if (Objects.isNull(packetChannel)) {
            packetChannel = Channels.newChannel(packet.open());
            ioArgs.limit(4);
            ioArgs.writeLength((int) packet.length());
        } else {
            ioArgs.limit((int) Math.min(ioArgs.capacity(), total - position));
            try {
                int count = ioArgs.readFrom(packetChannel);
                position += count;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return ioArgs;
    }

    @Override
    public void onConsumeCompleted(IOArgs args) {
        // 继续发送当前的包
        sendCurrentPacket();
    }

    @Override
    public void onConsumeFailed(IOArgs args, Exception ex) {
        log.error(ex);
    }

}
