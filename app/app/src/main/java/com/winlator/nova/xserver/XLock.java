package com.winlator.nova.xserver;

public interface XLock extends AutoCloseable {
    @Override
    void close();
}
