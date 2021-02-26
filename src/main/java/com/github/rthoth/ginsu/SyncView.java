package com.github.rthoth.ginsu;

import org.pcollections.TreePVector;

public class SyncView<T> {

    private final View<T> view;

    public SyncView(Grid<T> source) {
        this(new Source<>(source));
    }

    private SyncView(View<T> view) {
        this.view = view;
    }

    public Grid<T> end() {
        return view.grid();
    }

    public <O> SyncView<O> map(Mapper<T, O> mapper) {
        return new SyncView<>(new Mapped<>(view, mapper));
    }

    public <I, O> SyncView<O> zip(Grid<I> target, Zipper<T, I, O> zipper) {
        assert view.getHeight() == target.height && view.getWidth() == target.width : "Invalid size!";
        return new SyncView<>(new Zipped<>(view, target, zipper));
    }

    public interface Mapper<T, O> {
        O apply(int x, int y, T value);
    }

    private interface View<T> {

        T get(int x, int y);

        int getHeight();

        int getWidth();

        Grid<T> grid();
    }

    public interface Zipper<T, I, O> {
        O apply(int x, int y, T source, I target);
    }

    private static class Source<T> implements View<T> {

        private final Grid<T> source;

        public Source(Grid<T> source) {
            this.source = source;
        }

        @Override
        public T get(int x, int y) {
            return source.get(x, y);
        }

        @Override
        public int getHeight() {
            return source.height;
        }

        @Override
        public int getWidth() {
            return source.width;
        }

        @Override
        public Grid<T> grid() {
            return source;
        }
    }

    private static class Zipped<T, I, O> implements View<O> {

        private final View<T> view;
        private final Grid<I> target;
        private final Zipper<T, I, O> zipper;

        public Zipped(View<T> view, Grid<I> target, Zipper<T, I, O> zipper) {
            this.view = view;
            this.target = target;
            this.zipper = zipper;
        }

        @Override
        public O get(int x, int y) {
            return zipper.apply(x, y, view.get(x, y), target.get(x, y));
        }

        @Override
        public int getHeight() {
            return view.getHeight();
        }

        @Override
        public int getWidth() {
            return view.getWidth();
        }

        @Override
        public Grid<O> grid() {
            var values = TreePVector.<O>empty();
            for (var x = 0; x < target.width; x++) {
                for (var y = 0; y < target.height; y++) {
                    values = values.plus(zipper.apply(x, y, view.get(x, y), target.get(x, y)));
                }
            }

            return Grid.xy(getWidth(), getHeight(), values);
        }
    }

    private static class Mapped<T, O> implements View<O> {

        private final View<T> view;
        private final Mapper<T, O> mapper;

        public Mapped(View<T> view, Mapper<T, O> mapper) {
            this.view = view;
            this.mapper = mapper;
        }

        @Override
        public O get(int x, int y) {
            return null;
        }

        @Override
        public int getHeight() {
            return view.getHeight();
        }

        @Override
        public int getWidth() {
            return view.getWidth();
        }

        @Override
        public Grid<O> grid() {
            var values = TreePVector.<O>empty();
            for (int x = 0, lx = view.getWidth(); x < lx; x++) {
                for (int y = 0, ly = view.getHeight(); y < ly; y++) {
                    values = values.plus(mapper.apply(x, y, view.get(x, y)));
                }
            }

            return Grid.xy(view.getWidth(), view.getHeight(), values);
        }
    }
}
