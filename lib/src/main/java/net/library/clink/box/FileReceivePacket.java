package net.library.clink.box;

import lombok.extern.log4j.Log4j2;
import net.library.clink.core.ReceivePacket;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

@Log4j2
public class FileReceivePacket extends ReceivePacket<FileOutputStream, File> {

    private File file;

    public FileReceivePacket(long length, File file) {
        super(length);
        this.file = file;
    }

    @Override
    public byte type() {
        return TYPE_STREAM_FILE;
    }

    /**
     * 从流转变为对应的实体时直接返回创建时传入的File文件
     *
     * @param stream 文件传输流
     * @return File
     */
    @Override
    protected File buildEntity(FileOutputStream stream) {
        return file;
    }

    @Override
    protected FileOutputStream createStream() {
        try {
            return new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            log.error("{}文件沒有找到，发生异常: {}", file.getAbsolutePath(), e.getMessage());
            return null;
        }
    }

}
