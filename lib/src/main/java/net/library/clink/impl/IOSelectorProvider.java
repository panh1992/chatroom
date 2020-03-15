package net.library.clink.impl;

import lombok.extern.log4j.Log4j2;
import net.library.clink.core.IOProvider;
import net.library.clink.util.CloseUtil;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
public class IOSelectorProvider implements IOProvider {

    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    private final AtomicBoolean isRegisterInput = new AtomicBoolean(false);

    private final AtomicBoolean isRegisterOutput = new AtomicBoolean(false);

    private final Selector readSelector;

    private final Selector writeSelector;

    private final Map<SelectionKey, Runnable> inputCallBackMap;

    private final Map<SelectionKey, Runnable> outputCallBackMap;

    private final ExecutorService inputHandlerPool;

    private final ExecutorService outputHandlerPool;

    public IOSelectorProvider() throws IOException {
        this.readSelector = Selector.open();
        this.writeSelector = Selector.open();
        this.inputCallBackMap = new HashMap<>();
        this.outputCallBackMap = new HashMap<>();
        int cpuNum = Runtime.getRuntime().availableProcessors();
        this.inputHandlerPool = Executors.newFixedThreadPool(cpuNum,
                new IoProviderThreadFactory("IoProvider-Input-Thread-"));
        this.outputHandlerPool = Executors.newFixedThreadPool(cpuNum,
                new IoProviderThreadFactory("IoProvider-Output-Thread-"));
        // 开始输出输入的监听
        startRead();
        startWrite();
    }

    private void startRead() {
        Thread thread = new Thread("Clink IOSelectorProvider ReadSelector Thread") {
            @Override
            public void run() {
                while (!isClosed.get()) {
                    try {
                        if (readSelector.select() == 0) {
                            waitSelection(isRegisterInput);
                            continue;
                        }
                    } catch (IOException e) {
                        log.error(e);
                    }
                    Set<SelectionKey> selectionKeys = readSelector.selectedKeys();
                    for (SelectionKey selectionKey : selectionKeys) {
                        if (selectionKey.isValid()) {
                            handleSelection(selectionKey, SelectionKey.OP_READ, inputCallBackMap, inputHandlerPool);
                        }
                    }
                    selectionKeys.clear();
                }
            }
        };
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    private void startWrite() {
        Thread thread = new Thread("Clink IOSelectorProvider WriteSelector Thread") {
            @Override
            public void run() {
                while (!isClosed.get()) {
                    try {
                        if (writeSelector.select() == 0) {
                            waitSelection(isRegisterOutput);
                            continue;
                        }
                    } catch (IOException e) {
                        log.error(e);
                    }
                    Set<SelectionKey> selectionKeys = writeSelector.selectedKeys();
                    for (SelectionKey selectionKey : selectionKeys) {
                        if (selectionKey.isValid()) {
                            handleSelection(selectionKey, SelectionKey.OP_WRITE, outputCallBackMap, outputHandlerPool);
                        }
                    }
                    selectionKeys.clear();
                }
            }
        };
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    @Override
    public boolean registerInput(SocketChannel socketChannel, HandleInputCallBack callBack) {
        return Objects.nonNull(registerSelection(socketChannel, readSelector, SelectionKey.OP_READ, isRegisterInput,
                inputCallBackMap, callBack));
    }

    @Override
    public void unRegisterInput(SocketChannel socketChannel) {
        unRegisterSelection(socketChannel, readSelector, inputCallBackMap);
    }

    @Override
    public boolean registerOutput(SocketChannel socketChannel, HandleOutputCallback callBack) {
        return Objects.nonNull(registerSelection(socketChannel, writeSelector, SelectionKey.OP_WRITE, isRegisterOutput,
                outputCallBackMap, callBack));
    }

    @Override
    public void unRegisterOutput(SocketChannel socketChannel) {
        unRegisterSelection(socketChannel, writeSelector, outputCallBackMap);
    }

    @Override
    public void close() {
        if (isClosed.compareAndSet(false, true)) {
            inputHandlerPool.shutdown();
            outputHandlerPool.shutdown();
            inputCallBackMap.clear();
            outputCallBackMap.clear();
            readSelector.wakeup();
            writeSelector.wakeup();
            CloseUtil.close(readSelector, writeSelector);
        }
    }

    private static void waitSelection(final AtomicBoolean locker) {
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (locker) {
            if (locker.get()) {
                try {
                    locker.wait();
                } catch (InterruptedException e) {
                    log.error(e);
                }
            }
        }
    }

    private static SelectionKey registerSelection(SocketChannel socketChannel, Selector selector, int registerOps,
                                                  AtomicBoolean locker, Map<SelectionKey, Runnable> runnableMap,
                                                  Runnable runnable) {
        // noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (locker) {
            // 设置锁定状态
            locker.set(true);
            try {
                // 唤醒当前的selector，让selector不处于select()状态
                selector.wakeup();
                SelectionKey selectionKey = null;
                if (socketChannel.isRegistered()) {
                    // 查询是否已经注册过
                    selectionKey = socketChannel.keyFor(selector);
                    if (Objects.nonNull(selectionKey)) {
                        selectionKey.interestOps(selectionKey.readyOps() | registerOps);
                    }
                }
                if (Objects.isNull(selectionKey)) {
                    // 注册selector得到selectionKey
                    selectionKey = socketChannel.register(selector, registerOps);
                    // 注册回调
                    runnableMap.put(selectionKey, runnable);
                }
                return selectionKey;
            } catch (ClosedChannelException e) {
                return null;
            } finally {
                // 解除锁定状态
                locker.set(false);
                try {
                    // 通知
                    locker.notify();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static void unRegisterSelection(SocketChannel socketChannel, Selector selector,
                                            Map<SelectionKey, Runnable> runnableMap) {
        if (socketChannel.isRegistered()) {
            SelectionKey selectionKey = socketChannel.keyFor(selector);
            if (Objects.nonNull(selectionKey)) {
                // 取消监听的方法， 取消所有事件
                selectionKey.cancel();
                runnableMap.remove(selectionKey);
                selector.wakeup();
            }
        }
    }

    private static void handleSelection(SelectionKey selectionKey, int keyOps,
                                        Map<SelectionKey, Runnable> inputCallBackMap, ExecutorService inputHandlerPool) {
        // 取消继续对keyOps的监听
        selectionKey.interestOps(selectionKey.readyOps() & ~keyOps);
        Runnable runnable = inputCallBackMap.get(selectionKey);
        if (Objects.nonNull(runnable) && !inputHandlerPool.isShutdown()) {
            // 异步调度
            inputHandlerPool.execute(runnable);
        }
    }

    private static class IoProviderThreadFactory implements ThreadFactory {

        private final ThreadGroup group;

        private final AtomicInteger threadNumber = new AtomicInteger(1);

        private final String namePrefix;

        IoProviderThreadFactory(String namePrefix) {
            SecurityManager s = System.getSecurityManager();
            this.group = s != null ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            this.namePrefix = namePrefix;
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(this.group, r, this.namePrefix + this.threadNumber.getAndIncrement(), 0L);
            if (t.isDaemon()) {
                t.setDaemon(false);
            }

            if (t.getPriority() != 5) {
                t.setPriority(5);
            }

            return t;
        }
    }

}
