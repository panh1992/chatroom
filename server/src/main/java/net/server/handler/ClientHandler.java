package net.server.handler;

import lombok.extern.log4j.Log4j2;
import net.library.clink.core.Connector;
import net.library.clink.util.CloseUtil;

import java.io.IOException;
import java.nio.channels.SocketChannel;

@Log4j2
public class ClientHandler extends Connector {

    private final CloseNotifyCallBack closeNotifyCallBack;

    private final String clientInfo;

    public ClientHandler(SocketChannel socketChannel, CloseNotifyCallBack closeNotifyCallBack) throws IOException {
        this.closeNotifyCallBack = closeNotifyCallBack;
        setup(socketChannel);
        this.clientInfo = socketChannel.getRemoteAddress().toString();
        log.info("新客户端连接: {}", clientInfo);
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

    private void exitBySelf() {
        exit();
        closeNotifyCallBack.onSelfClosed(this);
    }

    @Override
    protected void onReceiveNewMessage(String str) {
        super.onReceiveNewMessage(str);
        closeNotifyCallBack.onNewMessageArrived(this, str);
    }

    public interface CloseNotifyCallBack {

        // 自身关闭通知
        void onSelfClosed(ClientHandler handler);

        // 收到消息时通知
        void onNewMessageArrived(ClientHandler handler, String msg);

    }

}
