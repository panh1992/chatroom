package net.library.clink.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;

public class CloseUtil {

    public static void close(Closeable... closeables) {
        if (Objects.isNull(closeables)) {
            return;
        }
        for (Closeable closeable : closeables) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
