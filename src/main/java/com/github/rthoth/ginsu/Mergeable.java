package com.github.rthoth.ginsu;

public interface Mergeable<T extends Mergeable<T>> {

    T plus(T other);
}
