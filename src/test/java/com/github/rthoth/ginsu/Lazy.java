package com.github.rthoth.ginsu;

import java.util.function.Supplier;

public class Lazy<T> {

    private static final Object UNDEFINED = new Object();

    private final Supplier<T> supplier;
    private Object value = UNDEFINED;

    public Lazy(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    @SuppressWarnings("unchecked")
    public T get() {
        if (value == UNDEFINED)
            value = supplier.get();

        return (T) value;
    }
}
