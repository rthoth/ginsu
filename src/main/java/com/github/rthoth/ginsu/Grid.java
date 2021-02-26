package com.github.rthoth.ginsu;

import org.pcollections.PVector;

import java.util.Iterator;

public abstract class Grid<T> {

    public final int width;
    public final int height;

    public Grid(int width, int height) {
        assert width > 0;
        assert height > 0;
        this.width = width;
        this.height = height;
    }

    public static <T> Grid<T> of(int width, int height, T value) {
        return new Fixed<>(width, height, value);
    }

    public static <T> Grid<T> xy(int width, int height, PVector<T> vector) {
        return new XY<>(width, height, vector);
    }

    protected abstract T _get(int x, int y);

    public Iterable<Entry<T>> entries() {
        return () -> new Iterator<>() {
            private int x = 0;
            private int y = -1;

            @Override
            public boolean hasNext() {
                if (x < width) {
                    y++;
                    if (y < height)
                        return true;
                    y = 0;
                    return ++x < width;
                } else {
                    return false;
                }
            }

            @Override
            public Entry<T> next() {
                return new Entry<>(x, y, get(x, y));
            }
        };
    }

    public T get(int x, int y) {
        if (x >= 0 && x < width && y >= 0 && y < height)
            return _get(x, y);
        else
            throw new GinsuException.InvalidPosition("");
    }

    public SyncView<T> sync() {
        return new SyncView<>(this);
    }

    public static class Entry<T> {

        public final int x;
        public final int y;
        public final T value;

        public Entry(int x, int y, T value) {
            this.x = x;
            this.y = y;
            this.value = value;
        }

        @Override
        public String toString() {
            return String.format("Entry(%d, %d, %s)", x, y, value);
        }
    }

    private static class Fixed<T> extends Grid<T> {

        private final T value;

        public Fixed(int width, int height, T value) {
            super(width, height);
            this.value = value;
        }

        @Override
        protected T _get(int x, int y) {
            return value;
        }
    }

    private static class XY<T> extends Grid<T> {

        private final PVector<T> vector;

        public XY(int width, int height, PVector<T> vector) {
            super(width, height);
            assert vector.size() == width * height;
            this.vector = vector;
        }

        @Override
        protected T _get(int x, int y) {
            return vector.get((x * y) + y);
        }
    }
}
