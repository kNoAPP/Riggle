package com.knoban.multiplayer.utils;

/**
 * This is meant to replace JavaFX which is being annoying buggy in Linux distros as of late.
 * @author Alden Bansemer (kNoAPP)
 */
public class Pair<T, U> {

    private T t;
    private U u;

    public Pair(T t, U u) {
        this.t = t;
        this.u = u;
    }

    public T getKey() {
        return t;
    }

    public U getValue() {
        return u;
    }
}
