package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.Geometry;
import org.pcollections.PVector;

import java.util.Iterator;
import java.util.Optional;
import java.util.function.Function;

public abstract class Grid<T extends Geometry> {

    protected final int width;
    protected final int height;
    protected final PVector<T> data;

    private Grid(int width, int height, PVector<T> data) {
        if (width >= 0 && height >= 0) {
            this.width = width != 0 ? width : 1;
            this.height = height != 0 ? height : 1;

            if (this.width * this.height == data.size())
                this.data = data;
            else
                throw new GinsuException.IllegalArgument("Invalid data size!");
        } else {
            throw new GinsuException.IllegalArgument("Invalid grid size!");
        }
    }

    public Entry<Optional<T>> get(int x, int y) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            return new Entry<>(x, y, Optional.ofNullable(_get(x, y)));
        } else {
            throw new GinsuException.IllegalArgument(x + ", " + y);
        }
    }

    private T _get(int x, int y) {
        return data.get(map(x, y));
    }

    protected abstract int map(int x, int y);

    @SuppressWarnings("unused")
    public Iterable<Entry<T>> iterable() {
        return () -> new Iterator<>() {

            private int ix = 0;
            private int iy = 0;

            @Override
            public boolean hasNext() {
                return ix < width && iy < height;
            }

            @Override
            public Entry<T> next() {
                if (ix < width && iy < height) {
                    final var entry = new Entry<>(ix, iy, _get(ix, iy));
                    if (++ix == width) {
                        ix = 0;
                        iy++;
                    }

                    return entry;
                } else {
                    throw new GinsuException.IllegalState("It is out!");
                }
            }
        };
    }

    @SuppressWarnings("unused")
    public static class Entry<T> {

        public final int x;
        public final int y;
        public final T value;

        public Entry(int x, int y, T value) {

            this.x = x;
            this.y = y;
            this.value = value;
        }

        public <M> Entry<M> map(Function<T, M> mapper) {
            return new Entry<>(x, y, mapper.apply(value));
        }

        public <M> Entry<M> copy(M value) {
            return new Entry<>(x, y, value);
        }

        @Override
        public String toString() {
            return "Entry(" + x + ", " + y + ", " + value + ")";
        }
    }

    public static class XY<T extends Geometry> extends Grid<T> {

        public XY(int width, int height, PVector<T> data) {
            super(width, height, data);
        }

        @Override
        protected int map(int x, int y) {
            return x * height + y;
        }
    }

    public static class YX<T extends Geometry> extends Grid<T> {

        public YX(int width, int height, PVector<T> data) {
            super(width, height, data);
        }

        @Override
        protected int map(int x, int y) {
            return y * width + x;
        }
    }
}
