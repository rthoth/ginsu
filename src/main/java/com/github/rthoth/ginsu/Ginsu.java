package com.github.rthoth.ginsu;

import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.DoubleFunction;

public abstract class Ginsu {

    public static final double DEFAULT_OFFSET = 1e-9;

    public static void isAscendant(double[] values, double offset) {
        offset *= 2;

        if (values.length > 0) {
            var previous = values[0];
            for (var i = 1; i < values.length; i++) {
                var current = values[i];
                if (current - previous > offset)
                    previous = current;
                else
                    throw new GinsuException.InvalidArgument("Invalid values sequence!");
            }
        }
    }

    @SuppressWarnings("unused")
    public static <T> T next(Iterator<T> iterator) {
        if (iterator.hasNext())
            return iterator.next();
        else
            throw new NoSuchElementException();
    }

    public static <T> PVector<T> toVector(double[] values, DoubleFunction<T> function) {
        var vector = TreePVector.<T>empty();
        for (var value : values)
            vector = vector.plus(function.apply(value));

        return vector;
    }
}
