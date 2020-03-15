package net.library.clink.core;

import lombok.extern.log4j.Log4j2;
import net.library.clink.box.StringReceivePacket;
import net.library.clink.box.StringSendPacket;
import net.library.clink.impl.SocketChannelAdapter;
import net.library.clink.impl.async.AsyncReceiveDispatcher;
import net.library.clink.impl.async.AsyncSendDispatcher;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Objects;
import java.util.UUID;

@Log4j2
public class Connector implements SocketChannelAdapter.OnChannelStatusChangedListener, Closeable {

    private UUID key = UUID.randomUUID();

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
        this.receiveDispatcher = new AsyncReceiveDispatcher(receiver, receiveRacketCallback);

        // 启动接收
        this.receiveDispatcher.start();
    }

    public void send(String msg) {
        SendPacket sendPacket = new StringSendPacket(msg);
        sendDispatcher.send(sendPacket);
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

    protected void onReceiveNewMessage(String str) {
        log.info("{}: {}", key.toString(), str);
    }

    private ReceiveDispatcher.ReceiveRacketCallback receiveRacketCallback = packet -> {
        if (packet instanceof StringReceivePacket) {
            String msg = ((StringReceivePacket) packet).string();
            onReceiveNewMessage(msg);
        }
    };

}
