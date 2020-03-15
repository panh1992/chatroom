package net.library.clink.core;

import java.io.Closeable;
import java.io.IOException;

public interface Receiver extends Closeable {

    void setReceiveListener(IOArgs.IOArgsEventProcessor processor);

    boolean postReceiveAsync() throws IOException;

}
