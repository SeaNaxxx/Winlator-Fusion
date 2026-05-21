package com.winlator.fusion.xserver;

public interface XLock extends AutoCloseable {
    @Override
    void close();
}
