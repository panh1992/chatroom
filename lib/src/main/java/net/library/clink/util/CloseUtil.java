package net.library.clink.util;

import lombok.extern.log4j.Log4j2;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;

@Log4j2
public class CloseUtil {

    public static void close(Closeable... closeables) {
        if (Objects.isNull(closeables)) {
            return;
        }
        for (Closeable closeable : closeables) {
            if (Objects.nonNull(closeable)) {
                try {
                    closeable.close();
                } catch (IOException e) {
                    log.error(e);
                }
            }
        }
    }

}
