package net.server;

import lombok.extern.log4j.Log4j2;
import net.common.constant.UDPConstant;
import net.library.clink.util.ByteUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.UUID;

@Log4j2
class UDPProvider {

    public static Provider PROVIDER_INSTANCE;

    public static void start(int port) {
        stop();
        String sn = UUID.randomUUID().toString();
        Provider provider = new Provider(sn.getBytes(), port);
        provider.start();
        PROVIDER_INSTANCE = provider;
    }

    public static void stop() {
        if (Objects.nonNull(PROVIDER_INSTANCE)) {
            PROVIDER_INSTANCE.exit();
        }
    }

    @Log4j2
    private static class Provider extends Thread {

        private final byte[] sn;

        private final int port;

        private boolean done = false;

        private DatagramSocket datagramSocket;

        final byte[] buffer;

        public Provider(byte[] sn, int port) {
            this.sn = sn;
            this.port = port;
            this.buffer = new byte[128];
        }

        @Override
        public void run() {
            log.info("UDPProvider Started.");

            try {
                datagramSocket = new DatagramSocket(UDPConstant.PORT_SERVER);
                // 接收消息的Packet
                DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                while (!done) {
                    // 接收数据
                    datagramSocket.receive(receivePacket);

                    // 打印接收到的信息与发送者的信息
                    String clientIp = receivePacket.getAddress().getHostAddress();
                    int clientPort = receivePacket.getPort();
                    int clientDataLength = receivePacket.getLength();
                    byte[] clientData = receivePacket.getData();
                    boolean isValid = clientDataLength >= (UDPConstant.HEADER.length + 2 + 4)
                            && ByteUtil.startsWith(clientData, UDPConstant.HEADER);

                    log.info("ServerProvider receive from ip:{}\tport:{}\tdataValid:{}", clientIp,
                            clientPort, isValid);

                    if (!isValid) {
                        // 无效继续
                        continue;
                    }
                    // 解析命令与回送端口
                    int index = UDPConstant.HEADER.length;
                    short cmd = (short) ((clientData[index++] << 8) | (clientData[index++] & 0xFF));
                    int responsePort = (clientData[index++] << 24) | ((clientData[index++] & 0xFF) << 16) |
                            ((clientData[index++] & 0xFF) << 8) | (clientData[index] & 0xFF);
                    // 判断合法性
                    if (cmd == 1 && responsePort > 0) {
                        // 构建回送数据
                        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
                        byteBuffer.put(UDPConstant.HEADER);
                        byteBuffer.putShort((short) 2);
                        byteBuffer.putInt(port);
                        byteBuffer.put(sn);
                        int len = byteBuffer.position();
                        // 直接根据发送者构建一份回送数据
                        DatagramPacket responsePacket = new DatagramPacket(buffer, len, receivePacket.getAddress(),
                                responsePort);
                        datagramSocket.send(responsePacket);
                        log.info("ServerProvider response to ip:{}\tport:{}", clientIp, responsePort);
                    } else {
                        log.info("ServerProvider receive cmd nonsupport; cmd:{}\tport:{}", cmd, responsePort);
                    }
                }
            } catch (IOException ignored) {
            } finally {
                close();
            }
            log.info("UDPProvider Finished.");
        }

        private void close() {
            if (Objects.nonNull(datagramSocket)) {
                datagramSocket.close();
            }
        }

        void exit() {
            done = true;
            close();
        }

    }

}
