package net.library.clink.core;

import java.io.Closeable;
import java.io.IOException;

public interface Sender extends Closeable {

    boolean sendAsync(IOArgs args, IOArgs.IOArgsEventListener listener) throws IOException;

}
