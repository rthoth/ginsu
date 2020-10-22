package com.github.rthoth.ginsu;

import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.util.Iterator;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class Grid<T> {

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

    private Grid(int width, int height) {
        this.width = width;
        this.height = height;
        data = TreePVector.empty();
    }

    protected T _get(int x, int y) {
        return data.get(mapToIndex(x, y));
    }

    public <V, M> Grid<M> combine(Grid<V> grid, BiFunction<T, V, M> mapper) {
        return new View<>(this, (x, y, value) -> mapper.apply(value, grid._get(x, y)));
    }

    public abstract Grid<T> copy();

    public Entry<Optional<T>> get(int x, int y) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            return new Entry<>(x, y, Optional.ofNullable(_get(x, y)));
        } else {
            throw new GinsuException.IllegalArgument(x + ", " + y);
        }
    }

    @SuppressWarnings("unused")
    public final Iterable<Entry<T>> iterable() {
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

    protected abstract int mapToIndex(int x, int y);

    public String toWKT() {
        var iterator = iterable().iterator();
        var wkt = new StringBuilder();
        wkt.append("GEOMETRYCOLLECTION (");

        while (iterator.hasNext()) {
            wkt.append(iterator.next().value);
            if (iterator.hasNext())
                wkt.append(", ");
        }

        return wkt.append(")").toString();
    }

    public <M> Grid<M> view(Function<T, M> mapper) {
        return new View<>(this, (x, y, value) -> mapper.apply(value));
    }

    private interface ViewMapper<T, M> {

        M apply(int x, int y, T value);
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

        public <M> Entry<M> copy(M value) {
            return new Entry<>(x, y, value);
        }

        public <M> Entry<M> map(Function<T, M> mapper) {
            return new Entry<>(x, y, mapper.apply(value));
        }

        @Override
        public String toString() {
            return "Entry(" + x + ", " + y + ", " + value + ")";
        }
    }

    private static class View<T, M> extends Grid<M> {

        private final Grid<T> grid;
        private final ViewMapper<T, M> mapper;

        private View(Grid<T> grid, ViewMapper<T, M> mapper) {
            super(grid.width, grid.height);
            this.mapper = mapper;
            this.grid = grid;
        }

        @Override
        protected M _get(int x, int y) {
            return mapper.apply(x, y, grid._get(x, y));
        }

        @Override
        public Grid<M> copy() {
            var data = Ginsu.map(iterable(), entry -> entry.value);
            return new YX<>(width, height, data);
        }

        @Override
        protected int mapToIndex(int x, int y) {
            return grid.mapToIndex(x, y);
        }
    }

    public static class XY<T> extends Grid<T> {

        public XY(int width, int height, PVector<T> data) {
            super(width, height, data);
        }

        @Override
        public Grid<T> copy() {
            return new XY<>(width, height, data);
        }

        @Override
        protected int mapToIndex(int x, int y) {
            return x * height + y;
        }
    }

    public static class YX<T> extends Grid<T> {

        public YX(int width, int height, PVector<T> data) {
            super(width, height, data);
        }

        @Override
        public Grid<T> copy() {
            return new YX<>(width, height, data);
        }

        @Override
        protected int mapToIndex(int x, int y) {
            return y * width + x;
        }
    }
}
