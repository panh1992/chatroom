package net.server;

import lombok.extern.log4j.Log4j2;
import net.foo.constant.TCPConstant;
import net.library.clink.core.IOContext;
import net.library.clink.impl.IOSelectorProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Log4j2
public class Server {

    public static void main(String[] args) throws IOException {
        IOContext.setup().ioProvider(new IOSelectorProvider()).start();
        TCPServer tcpServer = new TCPServer(TCPConstant.PORT_SERVER);
        boolean isSucceed = tcpServer.start();
        if (!isSucceed) {
            log.error("Start TCP server failed");
            return;
        }

        UDPProvider.start(TCPConstant.PORT_SERVER);

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        String line;
        do {
            line = bufferedReader.readLine() + "\n";
            tcpServer.broadcast(line);
        } while (!"00bye00".equalsIgnoreCase(line));

        UDPProvider.stop();
        tcpServer.stop();

        IOContext.close();
    }

}
