package net.common;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class Common {

    private static final String CHAT_CACHE_DIR = "chat_cache";

    public static File getCacheDir(String dir) {
        String path = String.join(File.separator, System.getProperty("user.dir"), CHAT_CACHE_DIR, dir);
        File file = new File(path);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                throw new RuntimeException("Create path error:" + path);
            }
        }
        return file;
    }

    public static File createRandomTemp(File parent) {
        String fileName = UUID.randomUUID().toString() + ".tmp";
        File file = new File(parent, fileName);
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

}
