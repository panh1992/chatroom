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

    {
        this.sendPacketQueue = new ConcurrentLinkedDeque<>();
        this.isSending = new AtomicBoolean();
        this.isClosed = new AtomicBoolean(false);
        this.reader = new AsyncPacketReader(this);
    }

    public AsyncSendDispatcher(Sender sender) {
        this.sender = sender;
        this.sender.setSendListener(this);
    }

    @Override
    public void send(SendPacket<? extends InputStream> packet) {
        sendPacketQueue.offer(packet);
        requestSend();
    }

    @Override
    public void cancel(SendPacket<? extends InputStream> packet) {
        if (sendPacketQueue.remove(packet)) {
            packet.cancel();
            return;
        }

        reader.cancel(packet);
    }

    @Override
    public SendPacket<? extends InputStream> takePacket() {
        SendPacket<? extends InputStream> packet = sendPacketQueue.poll();
        if (Objects.isNull(packet)) {
            return null;
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
        synchronized (isSending) {
            if (isSending.get() || isClosed.get()) {
                return;
            }
            // 返回true代表当前有数据需要发送
            if (reader.requestTakePacket()) {
                try {
                    boolean isSucceed = sender.postSendAsync();
                    if (isSucceed) {
                        isSending.set(true);
                    }
                } catch (IOException e) {
                    closeAndNotify();
                }
            }
        }
    }

    private void closeAndNotify() {
        CloseUtil.close(this);
    }

    @Override
    public void close() {
        if (isClosed.compareAndSet(false, true)) {
            // reader关闭
            reader.close();
            sendPacketQueue.clear();
            synchronized (isSending) {
                isSending.set(false);
            }
        }
    }

    @Override
    public IOArgs provideIOArgs() {
        return isClosed.get() ? null : reader.fillData();
    }

    @Override
    public void onConsumeCompleted(IOArgs args) {
        synchronized (isSending) {
            isSending.set(false);
        }
        requestSend();
    }

    @Override
    public void onConsumeFailed(IOArgs args, Exception ex) {
        log.error(ex);
        synchronized (isSending) {
            isSending.set(false);
        }
        requestSend();
    }

}
