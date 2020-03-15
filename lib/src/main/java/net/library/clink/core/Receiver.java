package net.library.clink.core;

import java.io.Closeable;
import java.io.IOException;

public interface Receiver extends Closeable {

    void serReceiveListener(IOArgs.IOArgsEventListener listener);

    boolean receiveAsync(IOArgs ioArgs) throws IOException;

}
