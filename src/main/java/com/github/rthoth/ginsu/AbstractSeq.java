package com.github.rthoth.ginsu;

import org.pcollections.HashTreePMap;
import org.pcollections.PMap;
import org.pcollections.PVector;

import java.util.Optional;

public abstract class AbstractSeq<T> {

    private final PVector<T> vector;
    private PMap<T, N<T>> map = HashTreePMap.empty();

    public AbstractSeq(PVector<T> vector, boolean isClosed) {
        final var iterator = vector.iterator();
        this.vector = vector;

        if (iterator.hasNext()) {
            final var first = new N<>(iterator.next());
            var previous = first;
            map = map.plus(first.value, first);

            while (iterator.hasNext()) {
                var current = new N<>(iterator.next());
                previous.next = current.value;
                current.previous = previous.value;
                map = map.plus(current.value, current);
                previous = current;
            }

            if (isClosed) {
                if (map.size() > 1) {
                    first.previous = previous.value;
                    previous.next = first.value;
                } else {
                    throw new GinsuException.IllegalState("Invalid sequence, a closed sequence should has more than 1 element!");
                }
            }
        }
    }

    public T get(int index) {
        return vector.get(index);
    }

    public PVector<T> getVector() {
        return vector;
    }

    public boolean isEmpty() {
        return vector.isEmpty();
    }

    public Optional<T> next(T value) {
        final var n = map.get(value);
        return n != null ? Optional.ofNullable(n.next) : Optional.empty();
    }

    public boolean nonEmpty() {
        return !vector.isEmpty();
    }

    public Optional<T> previous(T value) {
        final var n = map.get(value);
        return n != null ? Optional.ofNullable(n.previous) : Optional.empty();
    }

    public int size() {
        return vector.size();
    }

    private static class N<T> {
        final T value;
        T previous;
        T next;

        private N(T value) {
            this.value = value;
        }
    }
}
