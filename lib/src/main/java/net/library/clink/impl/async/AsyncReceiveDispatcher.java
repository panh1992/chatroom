package net.library.clink.impl.async;

import lombok.extern.log4j.Log4j2;
import net.library.clink.core.IOArgs;
import net.library.clink.core.ReceiveDispatcher;
import net.library.clink.core.ReceivePacket;
import net.library.clink.core.Receiver;
import net.library.clink.util.CloseUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

@Log4j2
public class AsyncReceiveDispatcher implements ReceiveDispatcher, IOArgs.IOArgsEventProcessor,
        AsyncPacketWriter.PacketProvider {

    private final AtomicBoolean isClosed;

    private final Receiver receiver;

    private final ReceiveRacketCallback receiveRacketCallback;

    private final AsyncPacketWriter writer;

    {
        this.isClosed = new AtomicBoolean(false);
        this.writer = new AsyncPacketWriter(this);
    }

    public AsyncReceiveDispatcher(Receiver receiver, ReceiveRacketCallback receiveRacketCallback) {
        this.receiver = receiver;
        this.receiver.setReceiveListener(this);
        this.receiveRacketCallback = receiveRacketCallback;
    }

    @Override
    public void start() {
        registerReceive();
    }

    @Override
    public void stop() {

    }

    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false, true)) {
            writer.close();
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

    @Override
    public IOArgs provideIOArgs() {
        return writer.takeTOArgs();
    }

    @Override
    public void onConsumeCompleted(IOArgs args) {
        if (isClosed.get()) {
            return;
        }
        do {
            writer.consumeIOArgs(args);
        } while (args.remained() && !isClosed.get());
        // 继续接收下一条数据
        registerReceive();
    }

    @Override
    public void onConsumeFailed(IOArgs args, Exception ex) {
        log.error(ex);
    }

    @Override
    public ReceivePacket<? extends OutputStream, ?> takePacket(byte type, long length, byte[] headerInfo) {
        return receiveRacketCallback.onArrivedNewPacket(type, length);
    }

    @Override
    public void completedPacket(ReceivePacket<? extends OutputStream, ?> packet, boolean isSucceed) {
        CloseUtil.close(packet);
        receiveRacketCallback.onReceivePacketCompleted(packet);
    }

}
