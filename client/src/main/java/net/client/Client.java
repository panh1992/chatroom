package net.client;

import lombok.extern.log4j.Log4j2;
import net.client.bean.ServerInfo;
import net.common.Common;
import net.library.clink.box.FileSendPacket;
import net.library.clink.core.IOContext;
import net.library.clink.impl.IOSelectorProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;

@Log4j2
public class Client {

    private static final int TIME_OUT = 10000;

    public static void main(String[] args) throws IOException {
        File cachePath = Common.getCacheDir("client");
        IOContext.setup().ioProvider(new IOSelectorProvider()).start();

        ServerInfo serverInfo = UDPSearcher.searchServer(TIME_OUT);

        log.info("Server:{}", serverInfo);

        if (Objects.nonNull(serverInfo)) {
            TCPClient tcpClient = null;
            try {
                tcpClient = TCPClient.startWith(serverInfo, cachePath);
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
            String line = input.readLine();
            if ("00bye00".equalsIgnoreCase(line)) {
                break;
            }
            // 发送文件 --f url
            if (line.startsWith("--f")) {
                String[] array = line.split(" ");
                if (array.length >= 2) {
                    String filePath = array[1];
                    File sendFile = new File(filePath);
                    if (sendFile.exists() && sendFile.isFile()) {
                        FileSendPacket packet = new FileSendPacket(sendFile);
                        tcpClient.send(packet);
                    }
                }
            } else {
                // 发送到服务器
                tcpClient.send(line);
            }
        } while (true);
        // 释放资源
        input.close();
    }

}
