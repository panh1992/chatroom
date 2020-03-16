package net.server.handler;

import lombok.extern.log4j.Log4j2;
import net.common.Common;
import net.library.clink.core.Connector;
import net.library.clink.core.Packet;
import net.library.clink.core.ReceivePacket;
import net.library.clink.util.CloseUtil;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SocketChannel;

@Log4j2
public class ClientHandler extends Connector {

    private final String clientInfo;

    private final File cachePath;

    private final CloseNotifyCallBack closeNotifyCallBack;

    public ClientHandler(SocketChannel socketChannel, CloseNotifyCallBack closeNotifyCallBack, File cachePath) throws IOException {
        this.clientInfo = socketChannel.getRemoteAddress().toString();
        this.cachePath = cachePath;
        this.closeNotifyCallBack = closeNotifyCallBack;
        log.info("新客户端连接: {}", clientInfo);
        setup(socketChannel);
    }

    public void exit() {
        CloseUtil.close(this);
        log.info("客户端已退出: {}", clientInfo);
    }

    @Override
    public void onChannelClosed(SocketChannel socketChannel) {
        super.onChannelClosed(socketChannel);
        exitBySelf();
    }

    @Override
    protected File createNewReceiveFile() {
        return Common.createRandomTemp(cachePath);
    }

    @Override
    protected void onReceivedPacket(ReceivePacket<?, ?> packet) {
        super.onReceivedPacket(packet);
        if (packet.type() == Packet.TYPE_MEMORY_STRING) {
            String entity = packet.entity().toString();
            log.info("{}: {}", key, entity);
            closeNotifyCallBack.onNewMessageArrived(this, entity);
        }
    }

    private void exitBySelf() {
        exit();
        closeNotifyCallBack.onSelfClosed(this);
    }

    public interface CloseNotifyCallBack {

        // 自身关闭通知
        void onSelfClosed(ClientHandler handler);

        // 收到消息时通知
        void onNewMessageArrived(ClientHandler handler, String msg);

    }

}
