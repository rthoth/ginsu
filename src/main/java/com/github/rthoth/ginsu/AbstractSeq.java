package com.github.rthoth.ginsu;

import org.pcollections.HashTreePMap;
import org.pcollections.PMap;
import org.pcollections.PVector;

import java.util.Optional;

public abstract class AbstractSeq<T> {

    private final PVector<T> pvector;
    private PMap<T, N<T>> pmap = HashTreePMap.empty();

    public AbstractSeq(PVector<T> pvector, boolean closed) {
        final var iterator = pvector.iterator();
        this.pvector = pvector;

        if (iterator.hasNext()) {
            final var first = new N<>(iterator.next());
            var previous = first;
            pmap = pmap.plus(first.value, first);

            while (iterator.hasNext()) {
                var current = new N<>(iterator.next());
                previous.next = current.value;
                current.previous = previous.value;
                pmap = pmap.plus(current.value, current);
                previous = current;
            }

            if (closed) {
                if (pmap.size() > 1) {
                    first.previous = previous.value;
                    previous.next = first.value;
                } else {
                    throw new GinsuException.IllegalState("Invalid sequence, a closed sequence should has more than 1 element!");
                }
            }
        }
    }

    public T get(int index) {
        return pvector.get(index);
    }

    public PVector<T> getPVector() {
        return pvector;
    }

    public boolean isEmpty() {
        return pvector.isEmpty();
    }

    public Optional<T> next(T value) {
        final var n = pmap.get(value);
        return n != null ? Optional.ofNullable(n.next) : Optional.empty();
    }

    public boolean nonEmpty() {
        return !pvector.isEmpty();
    }

    public Optional<T> previous(T value) {
        final var n = pmap.get(value);
        return n != null ? Optional.ofNullable(n.previous) : Optional.empty();
    }

    public int size() {
        return pvector.size();
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
