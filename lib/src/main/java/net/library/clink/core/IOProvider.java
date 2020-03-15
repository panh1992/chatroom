package net.library.clink.core;

import java.io.Closeable;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;

/**
 * IO 提供者
 */
public interface IOProvider extends Closeable {

    boolean registerInput(SocketChannel socketChannel, HandleInputCallBack callBack);

    void unRegisterInput(SocketChannel socketChannel);

    boolean registerOutput(SocketChannel socketChannel, HandleOutputCallback callBack);

    void unRegisterOutput(SocketChannel socketChannel);

    abstract class HandleInputCallBack implements Runnable {

        @Override
        public final void run() {
            canProviderInput();
        }

        protected abstract void canProviderInput();

    }

    abstract class HandleOutputCallback implements Runnable {

        @Override
        public final void run() {
            canProviderOutput();
        }

        protected abstract void canProviderOutput();

    }

}
