package net.library.clink.box;

import lombok.extern.log4j.Log4j2;
import net.library.clink.core.SendPacket;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

@Log4j2
public class FileSendPacket extends SendPacket<FileInputStream> {

    private final File file;

    public FileSendPacket(File file) {
        this.file = file;
        this.length = file.length();
    }

    @Override
    public byte type() {
        return TYPE_STREAM_FILE;
    }

    /**
     * 使用File构建文件读取流，用以读取本地的文件数据进行发送
     *
     * @return 文件读取流
     */
    @Override
    protected FileInputStream createStream() {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            log.error("{}文件沒有找到，发生异常: {}", file.getAbsolutePath(), e.getMessage());
            return null;
        }
    }

}
