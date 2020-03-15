package net.client;

import lombok.extern.log4j.Log4j2;
import net.client.bean.ServerInfo;
import net.library.clink.core.IOContext;
import net.library.clink.impl.IOSelectorProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;

@Log4j2
public class Client {

    private static final int TIME_OUT = 10000;

    public static void main(String[] args) throws IOException {
        IOContext.setup().ioProvider(new IOSelectorProvider()).start();

        ServerInfo serverInfo = UDPSearcher.searchServer(TIME_OUT);

        log.info("Server:{}", serverInfo);

        if (Objects.nonNull(serverInfo)) {
            TCPClient tcpClient = null;
            try {
                tcpClient = TCPClient.startWith(serverInfo);
                if (Objects.isNull(tcpClient)) {
                    return;
                }
                write(tcpClient);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (Objects.nonNull(tcpClient)) {
                    tcpClient.exit();
                }
            }
        }
        IOContext.close();
    }

    private static void write(TCPClient tcpClient) throws IOException {
        // 构建键盘输入，并转换为BufferedReader
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        do {
            // 键盘读取一行
            String line = input.readLine() + "\n";
            // 发送到服务器
            tcpClient.send(line);
            if ("00bye00".equalsIgnoreCase(line)) {
                break;
            }
        } while (true);
        // 释放资源
        input.close();
    }

}
