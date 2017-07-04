package com.balsdon.bleexample;


public interface Command<T> {
    void execute(T data);
}
