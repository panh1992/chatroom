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

    private IOArgs.IOArgsEventListener receiveIOEventListener;

    private IOArgs.IOArgsEventListener sendIOEventListener;

    private IOArgs tempReceiveArgs;

    public SocketChannelAdapter(SocketChannel socketChannel, IOProvider ioProvider,
                                OnChannelStatusChangedListener listener) throws IOException {
        this.socketChannel = socketChannel;
        this.ioProvider = ioProvider;
        this.listener = listener;
        this.socketChannel.configureBlocking(false);
    }

    private IOProvider.HandleInputCallBack inputCallBack = new IOProvider.HandleInputCallBack() {
        @Override
        protected void canProviderInput() {
            if (isClosed.get()) {
                return;
            }
            receiveIOEventListener.onStarted(tempReceiveArgs);

            try {
                // 具体的读取操作
                if (tempReceiveArgs.readFrom(socketChannel) > 0) {
                    // 读取完成回调
                    receiveIOEventListener.onCompleted(tempReceiveArgs);
                } else {
                    throw new IOException("Cannot read any data!");
                }
            } catch (IOException ignored) {
                CloseUtil.close(SocketChannelAdapter.this);
            }
        }
    };

    private IOProvider.HandleOutputCallback outputCallBack = new IOProvider.HandleOutputCallback() {
        @Override
        protected void canProviderOutput(Object attach) {
            if (isClosed.get()) {
                return;
            }
            IOArgs ioArgs = getAttach();
            sendIOEventListener.onStarted(ioArgs);
            try {
                // 具体的发送操作
                if (ioArgs.writeTo(socketChannel) > 0) {
                    // 发送完成回调
                    sendIOEventListener.onCompleted(ioArgs);
                } else {
                    throw new IOException("Cannot write any data!");
                }
            } catch (IOException ignored) {
                CloseUtil.close(SocketChannelAdapter.this);
            }
        }
    };

    @Override
    public void serReceiveListener(IOArgs.IOArgsEventListener listener) {
        receiveIOEventListener = listener;
    }

    @Override
    public boolean receiveAsync(IOArgs ioArgs) throws IOException {
        if (isClosed.get()) {
            throw new IOException("Current channel is closed!");
        }
        this.tempReceiveArgs = ioArgs;
        return ioProvider.registerInput(socketChannel, inputCallBack);
    }

    @Override
    public boolean sendAsync(IOArgs args, IOArgs.IOArgsEventListener listener) throws IOException {
        if (isClosed.get()) {
            throw new IOException("Current channel is closed!");
        }
        sendIOEventListener = listener;
        // 当前发送的数据附加到回调中
        outputCallBack.setAttach(args);
        return ioProvider.registerOutput(socketChannel, outputCallBack);
    }

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
