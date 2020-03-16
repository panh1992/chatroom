package net.library.clink.core;

import lombok.extern.log4j.Log4j2;
import net.library.clink.box.BytesReceivePacket;
import net.library.clink.box.FileReceivePacket;
import net.library.clink.box.StringReceivePacket;
import net.library.clink.box.StringSendPacket;
import net.library.clink.impl.SocketChannelAdapter;
import net.library.clink.impl.async.AsyncReceiveDispatcher;
import net.library.clink.impl.async.AsyncSendDispatcher;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SocketChannel;
import java.util.UUID;

@Log4j2
public abstract class Connector implements SocketChannelAdapter.OnChannelStatusChangedListener, Closeable {

    protected UUID key = UUID.randomUUID();

    private SocketChannel socketChannel;

    private Sender sender;

    private Receiver receiver;

    private SendDispatcher sendDispatcher;

    private ReceiveDispatcher receiveDispatcher;

    public void setup(SocketChannel socketChannel) throws IOException {
        this.socketChannel = socketChannel;

        IOContext context = IOContext.get();
        SocketChannelAdapter adapter = new SocketChannelAdapter(socketChannel, context.getIoProvider(), this);

        this.sender = adapter;
        this.receiver = adapter;

        this.sendDispatcher = new AsyncSendDispatcher(sender);
        this.receiveDispatcher = new AsyncReceiveDispatcher(receiver, new ReceiveDispatcher.ReceiveRacketCallback() {
            @Override
            public ReceivePacket<?, ?> onArrivedNewPacket(byte type, long length) {
                switch (type) {
                    case Packet.TYPE_MEMORY_BYTES:
                        return new BytesReceivePacket(length);
                    case Packet.TYPE_MEMORY_STRING:
                        return new StringReceivePacket(length);
                    case Packet.TYPE_STREAM_FILE:
                        return new FileReceivePacket(length, createNewReceiveFile());
                    case Packet.TYPE_STREAM_DIRECT:
                        return new BytesReceivePacket(length);
                    default:
                        throw new UnsupportedOperationException("Unsupported packet type:" + type);
                }
            }

            @Override
            public void onReceivePacketCompleted(ReceivePacket<?, ?> packet) {
                onReceivedPacket(packet);
            }
        });

        // 启动接收
        this.receiveDispatcher.start();
    }

    public void send(String msg) {
        SendPacket<ByteArrayInputStream> sendPacket = new StringSendPacket(msg);
        sendDispatcher.send(sendPacket);
    }

    public void send(SendPacket<? extends InputStream> packet) {
        sendDispatcher.send(packet);
    }

    @Override
    public void close() throws IOException {
        receiveDispatcher.close();
        sendDispatcher.close();
        sender.close();
        receiver.close();
        socketChannel.close();
    }

    @Override
    public void onChannelClosed(SocketChannel socketChannel) {

    }

    protected void onReceivedPacket(ReceivePacket<?, ?> packet) {
        log.info("{}: [New Packet]-Type:{}, Length:{}", key, packet.type(), packet.length());
    }

    protected abstract File createNewReceiveFile();

}
