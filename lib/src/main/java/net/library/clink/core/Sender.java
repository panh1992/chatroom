package net.library.clink.core;

import java.io.Closeable;
import java.io.IOException;

public interface Sender extends Closeable {

    void setSendListener(IOArgs.IOArgsEventProcessor processor);

    boolean postSendAsync() throws IOException;

}
