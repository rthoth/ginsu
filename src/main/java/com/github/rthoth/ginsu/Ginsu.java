package com.github.rthoth.ginsu;

import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.DoubleFunction;
import java.util.function.Function;

public class Ginsu {

    public static final double DEFAULT_OFFSET = 1e-8D;
    public static final double DEFAULT_EXTRUSION = 0D;

    private Ginsu() {

    }

    public static <I, M> PVector<M> collect(Iterable<I> iterable, Function<I, Optional<M>> predicate) {
        var filtered = TreePVector.<M>empty();
        for (var element : iterable) {
            var optional = predicate.apply(element);
            if (optional.isPresent())
                filtered = filtered.plus(optional.get());
        }

        return filtered;
    }

    public static int compare(double reference, double offset, double value) {
        return Math.abs(reference - value) > offset ? Double.compare(reference, value) : 0;
    }

    public static <M extends Mergeable<M>, V extends PVector<M>> PVector<M> flatten(PVector<V> vector) {
        if (!vector.isEmpty()) {
            var previous = new ArrayList<>(vector.get(0));
            var expectedSize = previous.size();

            for (var i = 1; i < vector.size(); i++) {
                var current = vector.get(i);
                if (current.size() == expectedSize) {
                    for (var j = 0; j < expectedSize; j++) {
                        previous.set(j, previous.get(j).plus(current.get(j)));
                    }
                } else {
                    throw new GinsuException.IllegalArgument("All elements of vector must be the same size!");
                }
            }

            return TreePVector.from(previous);
        } else {
            return TreePVector.empty();
        }
    }

    public static <I, M> PVector<M> map(Iterable<I> input, Function<I, M> mapper) {
        return map(input.iterator(), mapper);
    }

    public static <I, M> PVector<M> map(Iterator<I> input, Function<I, M> mapper) {
        var vector = TreePVector.<M>empty();
        while (input.hasNext())
            vector = vector.plus(mapper.apply(input.next()));

        return vector;
    }

    public static <T> PVector<T> map(double[] array, DoubleFunction<T> mapper) {
        var vector = TreePVector.<T>empty();
        for (var element : array)
            vector = vector.plus(mapper.apply(element));

        return vector;
    }

    public static <T> T next(Iterator<T> iterator) {
        if (iterator.hasNext())
            return iterator.next();
        else
            throw new NoSuchElementException();
    }

    public static <T> PVector<T> toVector(Iterable<T> iterable) {
        if (!(iterable instanceof PVector)) {
            var vector = TreePVector.<T>empty();
            for (var element : iterable)
                vector = vector.plus(element);

            return vector;
        } else
            return (PVector<T>) iterable;
    }

    public static <T> Iterable<IndexEntry<T>> zipWithIndex(Iterable<T> iterable) {
        return () -> new Iterator<>() {

            private final Iterator<T> iterator = iterable.iterator();
            private int index = 0;

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public IndexEntry<T> next() {
                return new IndexEntry<>(iterator.next(), index++);
            }
        };
    }

    public static class IndexEntry<T> {

        public final T value;
        public final int index;

        public IndexEntry(T value, int index) {
            this.value = value;
            this.index = index;
        }

        public <M> IndexEntry<M> copy(M value) {
            return new IndexEntry<>(value, index);
        }
    }
}
