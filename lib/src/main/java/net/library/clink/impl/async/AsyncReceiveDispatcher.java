package net.library.clink.impl.async;

import net.library.clink.box.StringReceivePacket;
import net.library.clink.core.IOArgs;
import net.library.clink.core.ReceiveDispatcher;
import net.library.clink.core.ReceivePacket;
import net.library.clink.core.Receiver;
import net.library.clink.util.CloseUtil;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncReceiveDispatcher implements ReceiveDispatcher {

    private final AtomicBoolean isClosed;

    private final Receiver receiver;

    private final ReceiveRacketCallback receiveRacketCallback;

    private IOArgs ioArgs;

    private ReceivePacket tempPacket;

    private byte[] buffer;

    private int total;

    private int position;

    public AsyncReceiveDispatcher(Receiver receiver, ReceiveRacketCallback receiveRacketCallback) {
        this.receiver = receiver;
        this.receiver.serReceiveListener(new IOArgs.IOArgsEventListener() {

            @Override
            public void onStarted(IOArgs args) {
                int receiveSize;
                if (Objects.isNull(tempPacket)) {
                    receiveSize = 4;
                } else {
                    receiveSize = Math.min(total - position, args.capacity());
                }
                //  设置本次接收数据大小
                args.limit(receiveSize);
            }

            @Override
            public void onCompleted(IOArgs args) {
                assemblePacket(args);
                // 继续接收下一条数据
                registerReceive();
            }

        });
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
            if (Objects.nonNull(tempPacket)) {
                CloseUtil.close(tempPacket);
            }
        }
    }

    private void closeAndNotify() {
        CloseUtil.close(this);
    }

    private void registerReceive() {
        try {
            receiver.receiveAsync(ioArgs);
        } catch (IOException e) {
            closeAndNotify();
        }
    }

    /**
     * 解析数据到Packet
     */
    private void assemblePacket(IOArgs args) {
        if (Objects.isNull(tempPacket)) {
            int length = args.readLength();
            tempPacket = new StringReceivePacket(length);
            buffer = new byte[length];
            total = length;
            position = 0;
        }
        int count = args.writeTo(buffer, 0);
        if (count > 0) {
            tempPacket.save(buffer, count);
            position += count;
            // 检查是否完成一份Packet数据
            if (position == total) {
                completePacket();
                tempPacket = null;
            }
        }
    }

    private void completePacket() {
        CloseUtil.close(this.tempPacket);
        receiveRacketCallback.onReceivePacketCompleted(tempPacket);
    }

}
