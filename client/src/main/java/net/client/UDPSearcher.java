package net.client;

import lombok.extern.log4j.Log4j2;
import net.client.bean.ServerInfo;
import net.foo.constant.UDPConstant;
import net.library.clink.util.ByteUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


@Log4j2
public class UDPSearcher {

    private static final int LISTEN_PORT = UDPConstant.PORT_CLIENT_RESPONSE;

    public static ServerInfo searchServer(int timeout) {
        log.info("UDPSearcher Started.");
        // 成功收到回送的栅栏
        CountDownLatch receiveLatch = new CountDownLatch(1);
        Listener listener = null;
        try {
            listener = listen(receiveLatch);
            sendBroadcast();
            receiveLatch.await(timeout, TimeUnit.MILLISECONDS);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        log.info("UDPSearcher Finished.");
        if (Objects.isNull(listener)) {
            return null;
        }
        List<ServerInfo> devices = listener.getServerAndClose();
        if (Objects.nonNull(devices) && devices.size() > 0) {
            return devices.get(0);
        }
        return null;
    }

    private static void sendBroadcast() throws IOException {
        log.info("UDPSearcher sendBroadcast started.");
        // 作为搜索方，让系统自动分配端口
        DatagramSocket datagramSocket = new DatagramSocket();
        // 构建一份请求数据
        ByteBuffer byteBuffer = ByteBuffer.allocate(128);
        // 头部
        byteBuffer.put(UDPConstant.HEADER);
        // CMD命令
        byteBuffer.putShort((short) 1);
        // 回送端口信息
        byteBuffer.putInt(LISTEN_PORT);
        // 直接构建packet
        DatagramPacket requestPacket = new DatagramPacket(byteBuffer.array(), byteBuffer.position() + 1);
        // 设置广播地址
        requestPacket.setAddress(InetAddress.getByName(UDPConstant.BROADCAST_HOST));
        // 设置服务器端口
        requestPacket.setPort(UDPConstant.PORT_SERVER);
        // 发送数据
        datagramSocket.send(requestPacket);
        datagramSocket.close();
        log.info("UDPSearcher sendBroadcast finished.");
    }

    private static Listener listen(CountDownLatch receiveLatch) throws InterruptedException {
        log.info("UDPSearcher start listen.");
        CountDownLatch startDownLatch = new CountDownLatch(1);
        Listener listener = new Listener(LISTEN_PORT, startDownLatch, receiveLatch);
        listener.start();
        startDownLatch.await();
        return listener;
    }

    private static class Listener extends Thread {

        private final int listenPort;

        private final CountDownLatch startDownLatch;

        private final CountDownLatch receiveDownLatch;

        private final List<ServerInfo> serverInfoList;

        private final byte[] buffer;

        private final int minLength;

        private boolean done = false;

        private DatagramSocket datagramSocket;

        public Listener(int listenPort, CountDownLatch startDownLatch, CountDownLatch receiveDownLatch) {
            this.listenPort = listenPort;
            this.startDownLatch = startDownLatch;
            this.receiveDownLatch = receiveDownLatch;
            this.serverInfoList = new ArrayList<>();
            this.buffer = new byte[128];
            this.minLength = UDPConstant.HEADER.length + 2 + 4;
        }

        @Override
        public void run() {
            // 通知已启动
            startDownLatch.countDown();
            try {
                datagramSocket = new DatagramSocket(listenPort);
                // 构建接收实体
                DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                while (!done) {
                    // 接收数据
                    datagramSocket.receive(receivePacket);
                    // 打印接收到的信息与发送者的信息
                    String ip = receivePacket.getAddress().getHostAddress();
                    int port = receivePacket.getPort();
                    int dataLength = receivePacket.getLength();
                    byte[] data = receivePacket.getData();
                    boolean isValid = dataLength >= minLength && ByteUtil.startsWith(data, UDPConstant.HEADER);
                    log.info("UDPSearcher receive form ip:{}\tport:{}\tdataValid:{}", ip, port, isValid);
                    if (!isValid) {
                        // 无效继续
                        continue;
                    }
                    ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, UDPConstant.HEADER.length, dataLength);
                    final short cmd = byteBuffer.getShort();
                    final int serverPort = byteBuffer.getInt();
                    if (cmd != 2 || serverPort <= 0) {
                        log.info("UDPSearcher receive cmd:{}\tserverPort:{}", cmd, serverPort);
                        continue;
                    }
                    String sn = new String(buffer, minLength, dataLength - minLength);
                    ServerInfo serverInfo = new ServerInfo(sn, serverPort, ip);
                    serverInfoList.add(serverInfo);
                    // 成功接收
                    receiveDownLatch.countDown();
                }
            } catch (IOException ignored) {
            } finally {
                close();
            }
            log.info("UDPSearcher Listener finished.");
        }

        private void close() {
            if (Objects.nonNull(datagramSocket)) {
                datagramSocket.close();
            }
        }

        public List<ServerInfo> getServerAndClose() {
            done = true;
            close();
            return this.serverInfoList;
        }

    }

}
