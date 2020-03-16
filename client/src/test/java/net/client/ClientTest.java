package net.client;

import lombok.extern.log4j.Log4j2;
import net.client.bean.ServerInfo;
import net.common.Common;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Log4j2
class ClientTest {

    private static boolean done = false;

    @Test
    public void test() throws IOException {
        ServerInfo serverInfo = UDPSearcher.searchServer(10000);
        log.info("Server:{}", serverInfo);
        if (Objects.isNull(serverInfo)) {
            return;
        }
        // 当前连接数量
        int size = 0;
        final List<TCPClient> tcpClients = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            try {
                File cachePath = Common.getCacheDir("client/text" + i);
                TCPClient tcpClient = TCPClient.startWith(serverInfo, cachePath);
                if (Objects.isNull(tcpClient)) {
                    log.error("连接异常");
                    continue;
                }
                tcpClients.add(tcpClient);
                log.info("连接成功：{}", ++size);
            } catch (IOException e) {
                log.error("连接异常: {}", e.getMessage());
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException ignored) {
            }
        }
        Thread thread = new Thread(() -> {
            while (!done) {
                tcpClients.forEach(tcpClient -> tcpClient.send("Hello~~"));
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
            }
        });
        thread.start();
        System.in.read();
        done = true;
        // 等待线程完成
        try {
            thread.join();
        } catch (InterruptedException ignored) {
        }
        // 客户端结束操作
        tcpClients.forEach(TCPClient::exit);
    }

}