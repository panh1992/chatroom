package net.library.clink.impl;

import net.library.clink.core.IOArgs;
import net.library.clink.core.IOProvider;
import net.library.clink.core.Receiver;
import net.library.clink.core.Sender;
import net.library.clink.util.CloseUtil;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

public class SocketChannelAdapter implements Sender, Receiver {

    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    private final SocketChannel socketChannel;

    private final IOProvider ioProvider;

    private final OnChannelStatusChangedListener listener;

    private IOArgs.IOArgsEventProcessor receiveIOEventProcessor;

    private IOArgs.IOArgsEventProcessor sendIOEventProcessor;

    public SocketChannelAdapter(SocketChannel socketChannel, IOProvider ioProvider,
                                OnChannelStatusChangedListener listener) throws IOException {
        this.socketChannel = socketChannel;
        this.ioProvider = ioProvider;
        this.listener = listener;
        this.socketChannel.configureBlocking(false);
    }

    @Override
    public void setReceiveListener(IOArgs.IOArgsEventProcessor processor) {
        this.receiveIOEventProcessor = processor;
    }

    @Override
    public boolean postReceiveAsync() throws IOException {
        if (isClosed.get()) {
            throw new IOException("Current channel is closed!");
        }
        return ioProvider.registerInput(socketChannel, inputCallBack);
    }

    @Override
    public void setSendListener(IOArgs.IOArgsEventProcessor processor) {
        this.sendIOEventProcessor = processor;
    }

    @Override
    public boolean postSendAsync() throws IOException {
        if (isClosed.get()) {
            throw new IOException("Current channel is closed!");
        }
        return ioProvider.registerOutput(socketChannel, outputCallBack);
    }

    private IOProvider.HandleInputCallBack inputCallBack = new IOProvider.HandleInputCallBack() {
        @Override
        protected void canProviderInput() {
            if (isClosed.get()) {
                return;
            }
            IOArgs ioArgs = receiveIOEventProcessor.provideIOArgs();
            try {
                // 具体的读取操作
                if (ioArgs.readFrom(socketChannel) > 0) {
                    // 读取完成回调
                    receiveIOEventProcessor.onConsumeCompleted(ioArgs);
                } else {
                    receiveIOEventProcessor.onConsumeFailed(ioArgs, new IOException("Cannot read any data!"));
                }
            } catch (IOException ignored) {
                CloseUtil.close(SocketChannelAdapter.this);
            }
        }
    };

    private IOProvider.HandleOutputCallback outputCallBack = new IOProvider.HandleOutputCallback() {

        @Override
        protected void canProviderOutput() {
            if (isClosed.get()) {
                return;
            }
            IOArgs ioArgs = sendIOEventProcessor.provideIOArgs();
            try {
                // 具体的发送操作
                if (ioArgs.writeTo(socketChannel) > 0) {
                    // 发送完成回调
                    sendIOEventProcessor.onConsumeCompleted(ioArgs);
                } else {
                    sendIOEventProcessor.onConsumeFailed(ioArgs, new IOException("Cannot write any data!"));
                }
            } catch (IOException ignored) {
                CloseUtil.close(SocketChannelAdapter.this);
            }
        }

    };

    @Override
    public void close() {
        if (isClosed.compareAndSet(false, true)) {
            // 解除注册回调
            ioProvider.unRegisterInput(socketChannel);
            ioProvider.unRegisterOutput(socketChannel);
            // 关闭
            CloseUtil.close(socketChannel);
            // 回调当前Channel已关闭
            listener.onChannelClosed(socketChannel);
        }
    }

    public interface OnChannelStatusChangedListener {

        void onChannelClosed(SocketChannel socketChannel);

    }

}
