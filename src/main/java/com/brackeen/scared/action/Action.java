package com.brackeen.scared.action;

public interface Action {

    void tick();

    void unload();

    boolean isFinished();
}
