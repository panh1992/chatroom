package net.server;

import lombok.extern.log4j.Log4j2;
import net.library.clink.util.CloseUtil;
import net.server.handler.ClientHandler;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Log4j2
public class TCPServer implements ClientHandler.CloseNotifyCallBack {

    private final int port;

    private final File cachePath;

    private ClientListener clientListener;

    private List<ClientHandler> clientHandlerList;

    private ExecutorService forwardingThreadPoolExecutor;

    private Selector selector;

    private ServerSocketChannel serverSocketChannel;

    public TCPServer(int port, File cachePath) {
        this.port = port;
        this.cachePath = cachePath;
        this.clientHandlerList = new ArrayList<>();
        // 转发线程池
        this.forwardingThreadPoolExecutor = Executors.newSingleThreadExecutor();
    }

    public boolean start() {
        try {
            selector = Selector.open();
            serverSocketChannel = ServerSocketChannel.open();
            // 设置非阻塞
            serverSocketChannel.configureBlocking(false);
            // 绑定本地端口
            serverSocketChannel.socket().bind(new InetSocketAddress(port));
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            log.info("服务器信息: {}", serverSocketChannel.getLocalAddress());

            // 启动客户端监听
            clientListener = new ClientListener();
            clientListener.start();
        } catch (IOException e) {
            log.error(e);
            return false;
        }
        return true;
    }

    public void stop() {
        if (Objects.nonNull(clientListener)) {
            clientListener.exit();
        }
        CloseUtil.close(serverSocketChannel);
        CloseUtil.close(selector);
        synchronized (TCPServer.this) {
            clientHandlerList.forEach(ClientHandler::exit);
            clientHandlerList.clear();
        }
        forwardingThreadPoolExecutor.shutdown();
    }

    /**
     * 发送广播
     * @param line 广播消息
     */
    public synchronized void broadcast(String line) {
        clientHandlerList.forEach(clientHandler -> clientHandler.send(line));
    }

    @Override
    public synchronized void onSelfClosed(ClientHandler handler) {
        clientHandlerList.remove(handler);
    }

    @Override
    public void onNewMessageArrived(final ClientHandler handler, final String msg) {
        // 异步提交转发任务
        forwardingThreadPoolExecutor.execute(() -> {
            synchronized (TCPServer.this) {
                for (ClientHandler clientHandler : clientHandlerList) {
                    // 跳过自己
                    if (clientHandler.equals(handler)) {
                        continue;
                    }
                    // 对其他客户端发送消息
                    clientHandler.send(msg);
                }
            }
        });
    }

    private class ClientListener extends Thread {

        private boolean done = false;

        @Override
        public void run() {
            log.info("服务器准备就绪");
            // 等待客户端连接
            do {
                try {
                    if (selector.select() == 0) {
                        if (done) {
                            break;
                        }
                        continue;
                    }
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        if (done) {
                            break;
                        }
                        SelectionKey key = iterator.next();
                        iterator.remove();
                        // 检查当前key的状态是否是关注的状态
                        if (key.isAcceptable()) {
                            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                            // 非阻塞状态拿到客户端连接
                            SocketChannel socketChannel = serverSocketChannel.accept();
                            try {
                                ClientHandler clientHandler = new ClientHandler(socketChannel,
                                        TCPServer.this, TCPServer.this.cachePath);
                                synchronized (TCPServer.this) {
                                    clientHandlerList.add(clientHandler);
                                    log.info("当前客户端连接数量: {}", clientHandlerList.size());
                                }
                            } catch (IOException e) {
                                log.error("客户端连接异常，", e);
                            }
                        }
                    }
                } catch (IOException e) {
                    log.error(e);
                }
            } while (!done);
            log.info("服务器已关闭");
        }

        public void exit() {
            done = true;
            // 唤醒当前阻塞
            selector.wakeup();
        }

    }

}
