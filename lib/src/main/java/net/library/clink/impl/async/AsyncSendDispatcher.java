package net.library.clink.impl.async;

import net.library.clink.core.IOArgs;
import net.library.clink.core.SendDispatcher;
import net.library.clink.core.SendPacket;
import net.library.clink.core.Sender;
import net.library.clink.util.CloseUtil;

import java.io.IOException;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncSendDispatcher implements SendDispatcher {

    private final Sender sender;

    private final Queue<SendPacket> sendPacketQueue;

    private final AtomicBoolean isSending;

    private final AtomicBoolean isClosed;

    private IOArgs ioArgs;

    private SendPacket tempPacket;

    private int total;

    private int position;

    public AsyncSendDispatcher(Sender sender) {
        this.sender = sender;
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
        if (Objects.nonNull(tempPacket)) {
            CloseUtil.close(tempPacket);
        }
        tempPacket = takePacket();
        if (Objects.isNull(tempPacket)) {
            // 队列为空，取消状态发送
            isSending.set(false);
            return;
        }
        total = tempPacket.getLength();
        position = 0;
        sendCurrentPacket();
    }

    private void sendCurrentPacket() {
        // 开始， 清理
        ioArgs.startWriting();
        if (position >= total) {
            sendNextPacket();
            return;
        } else if (position == 0) {
            // 首包，需要携带长度信息
            ioArgs.writeLength(total);
        }
        byte[] bytes = tempPacket.bytes();
        // 将bytes的数据写入到IOArgs
        int count = ioArgs.readFrom(bytes, position);
        position += count;
        // 完成封装
        ioArgs.finishWriting();

        try {
            sender.sendAsync(ioArgs, ioArgsEventListener);
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
            if (Objects.nonNull(tempPacket)) {
                CloseUtil.close(tempPacket);
            }
        }
    }

    private final IOArgs.IOArgsEventListener ioArgsEventListener = new IOArgs.IOArgsEventListener() {
        @Override
        public void onStarted(IOArgs args) {

        }

        @Override
        public void onCompleted(IOArgs args) {
            // 继续发送当前包
            sendCurrentPacket();
        }

    };

}
