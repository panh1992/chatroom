package net.library.clink.impl.async;

import lombok.extern.log4j.Log4j2;
import net.library.clink.core.IOArgs;
import net.library.clink.core.SendDispatcher;
import net.library.clink.core.SendPacket;
import net.library.clink.core.Sender;
import net.library.clink.util.CloseUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

@Log4j2
public class AsyncSendDispatcher implements SendDispatcher, IOArgs.IOArgsEventProcessor, AsyncPacketReader.PacketProvider {

    private final Sender sender;

    private final Queue<SendPacket<? extends InputStream>> sendPacketQueue;

    private final AtomicBoolean isSending;

    private final AtomicBoolean isClosed;

    private final AsyncPacketReader reader;

    private final Object queueLock;

    {
        this.sendPacketQueue = new ConcurrentLinkedDeque<>();
        this.isSending = new AtomicBoolean();
        this.isClosed = new AtomicBoolean(false);
        this.reader = new AsyncPacketReader(this);
        this.queueLock = new Object();
    }

    public AsyncSendDispatcher(Sender sender) {
        this.sender = sender;
        this.sender.setSendListener(this);
    }

    @Override
    public void send(SendPacket<? extends InputStream> packet) {
        synchronized (queueLock) {
            sendPacketQueue.offer(packet);
            if (isSending.compareAndSet(false, true)) {
                if (reader.requestTakePacket()) {
                    requestSend();
                }
            }
        }
    }

    @Override
    public void cancel(SendPacket<? extends InputStream> packet) {
        boolean isRemove;
        synchronized (queueLock) {
            isRemove = sendPacketQueue.remove(packet);
        }
        if (isRemove) {
            packet.cancel();
            return;
        }

        reader.cancel(packet);
    }

    @Override
    public SendPacket<? extends InputStream> takePacket() {
        SendPacket<? extends InputStream> packet;
        synchronized (queueLock) {
            packet = sendPacketQueue.poll();
            if (Objects.isNull(packet)) {
                // 队列为空，取消发送状态
                isSending.set(false);
                return null;
            }
        }
        if (packet.isCanceled()) {
            // 已取消，不用发送
            return takePacket();
        }
        return packet;
    }

    /**
     * 完成Packet发送
     *
     * @param isSucceed 是否成功
     */
    @Override
    public void completedPacket(SendPacket<? extends InputStream> packet, boolean isSucceed) {
        CloseUtil.close(packet);
    }

    /**
     * 请求网络进行数据发送
     */
    private void requestSend() {
        try {
            sender.postSendAsync();
        } catch (IOException e) {
            closeAndNotify();
        }
    }

    private void closeAndNotify() {
        CloseUtil.close(this);
    }

    @Override
    public void close() {
        if (isClosed.compareAndSet(false, true)) {
            isSending.set(false);
            // reader关闭
            reader.close();
        }
    }

    @Override
    public IOArgs provideIOArgs() {
        return reader.fillData();
    }

    @Override
    public void onConsumeCompleted(IOArgs args) {
        // 继续发送当前的包
        if (reader.requestTakePacket()) {
            requestSend();
        }
    }

    @Override
    public void onConsumeFailed(IOArgs args, Exception ex) {
        if (Objects.nonNull(args)) {
            log.error(ex);
        } else {
            // TODO 进行其他操作
        }
    }

}
