package net.client;

import lombok.extern.log4j.Log4j2;
import net.client.bean.ServerInfo;
import net.common.Common;
import net.library.clink.core.Connector;
import net.library.clink.core.Packet;
import net.library.clink.core.ReceivePacket;
import net.library.clink.util.CloseUtil;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

@Log4j2
public class TCPClient extends Connector {

    private final File cachePath;

    public TCPClient(SocketChannel socketChannel, File cachePath) throws IOException {
        this.cachePath = cachePath;
        setup(socketChannel);
    }

    public static TCPClient startWith(ServerInfo serverInfo, File cachePath) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        // 链接到服务器
        socketChannel.connect(new InetSocketAddress(Inet4Address.getByName(serverInfo.getAddress()), serverInfo.getPort()));

        log.info("已发起服务器连接，并进入后续流程");
        log.info("客户端信息: {}", socketChannel.getLocalAddress());
        log.info("服务端信息: {}", socketChannel.getRemoteAddress());

        try {
            return new TCPClient(socketChannel, cachePath);
        } catch (Exception e) {
            log.error("连接异常，", e);
            CloseUtil.close(socketChannel);
        }
        return null;
    }

    @Override
    public void onChannelClosed(SocketChannel socketChannel) {
        super.onChannelClosed(socketChannel);
        log.info("连接已关闭，无法读取数据");
    }

    @Override
    protected File createNewReceiveFile() {
        return Common.createRandomTemp(cachePath);
    }

    @Override
    protected void onReceivedPacket(ReceivePacket<?, ?> packet) {
        super.onReceivedPacket(packet);
        if (packet.type() == Packet.TYPE_MEMORY_STRING) {
            log.info("{}: {}", key, packet.entity());
        }
    }

    public void exit() {
        CloseUtil.close(this);
    }

}
