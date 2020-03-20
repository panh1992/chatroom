package net.server;

import lombok.extern.log4j.Log4j2;
import net.common.Common;
import net.common.constant.TCPConstant;
import net.library.clink.core.IOContext;
import net.library.clink.impl.IOSelectorProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;

@Log4j2
public class Server {

    public static void main(String[] args) throws IOException {
        File cachePath = Common.getCacheDir("server");
        IOContext.setup().ioProvider(new IOSelectorProvider()).start();
        TCPServer tcpServer = new TCPServer(TCPConstant.PORT_SERVER, cachePath);
        boolean isSucceed = tcpServer.start();
        if (!isSucceed) {
            log.error("Start TCP server failed");
            return;
        }

        UDPProvider.start(TCPConstant.PORT_SERVER);

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        String line;
        do {
            line = bufferedReader.readLine();
            if (Objects.isNull(line) || line.length() == 0 || "00bye00".equalsIgnoreCase(line)) {
                break;
            }
            tcpServer.broadcast(line);
        } while (true);

        UDPProvider.stop();
        tcpServer.stop();

        IOContext.close();
    }

}
