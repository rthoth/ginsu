package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public abstract class Ginsu {

    public static final double DEFAULT_OFFSET = 1e-9;
    private static final Object EMPTY = new Object();

    public static <T> T[] array(int length, IntFunction<T[]> constructor, IntFunction<T> initializer) {
        assert length > 0;
        var values = constructor.apply(length);
        for (var i = 0; i < length; i++) {
            values[i] = initializer.apply(i);
        }

        return values;
    }

    public static <I, O> PVector<O> collect(Iterable<I> iterable, Function<I, Optional<O>> collector) {
        return collect(iterable.iterator(), collector);
    }

    public static <I, O> PVector<O> collect(Iterator<I> iterator, Function<I, Optional<O>> collector) {
        var values = TreePVector.<O>empty();
        while (iterator.hasNext()) {
            var opt = collector.apply(iterator.next());
            if (opt.isPresent())
                values = values.plus(opt.get());
        }

        return values;
    }

    public static int compare(double value, double reference, double offset) {
        return Math.abs(reference - value) > offset ? Double.compare(value, reference) : 0;
    }

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

    public static <T extends Geometry> Iterable<T> iterable(GeometryCollection collection) {
        return () -> new Iterator<>() {

            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < collection.getNumGeometries();
            }

            @SuppressWarnings("unchecked")
            @Override
            public T next() {
                return (T) collection.getGeometryN(index++);
            }
        };
    }

    public static Iterable<LineString> iterable(Polygon polygon) {
        return () -> new Iterator<>() {

            private int index = -1;

            @Override
            public boolean hasNext() {
                return index < 0 || index < polygon.getNumInteriorRing();
            }

            @Override
            public LineString next() {
                return index++ != -1 ? polygon.getInteriorRingN(index) : polygon.getExteriorRing();
            }
        };
    }

    public static <T> Lazy<T> lazy(Supplier<T> supplier) {
        return new Lazy<>(supplier);
    }

    public static final <E, M> PVector<M> map(PVector<E> vector, Function<E, M> function) {
        var mapped = TreePVector.<M>empty();
        for (var element : vector)
            mapped = mapped.plus(function.apply(element));

        return mapped;
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

    public static <I, T> PVector<T> toVector(I[] values, Function<I, T> function) {
        var vector = TreePVector.<T>empty();
        for (var value : values)
            vector = vector.plus(function.apply(value));

        return vector;
    }

    public static <T> PVector<T> vector(int size, IntFunction<T> function) {
        var result = TreePVector.<T>empty();
        for (var i = 0; i < size; i++)
            result = result.plus(function.apply(i));
        return result;
    }

    public static <T> Iterable<IndexedEntry<T>> withIndex(Iterable<T> iterable) {
        return () -> new Iterator<>() {

            private final Iterator<T> iterator = iterable.iterator();
            private int i = -1;

            @Override
            public boolean hasNext() {
                if (iterator.hasNext()) {
                    i++;
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public IndexedEntry<T> next() {
                return new IndexedEntry<>(iterator.next(), i);
            }
        };
    }

    public static class IndexedEntry<T> {

        public final T value;
        public final int index;

        public IndexedEntry(T value, int index) {
            this.value = value;
            this.index = index;
        }
    }

    public static class Lazy<T> {

        private final Supplier<T> supplier;
        private Object value = EMPTY;

        public Lazy(Supplier<T> supplier) {
            this.supplier = supplier;
        }

        @SuppressWarnings("unchecked")
        public T get() {
            if (value == EMPTY) {
                synchronized (this) {
                    if (value == EMPTY)
                        value = supplier.get();
                }

            }
            return (T) value;
        }
    }
}
