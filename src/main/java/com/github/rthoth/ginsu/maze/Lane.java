package com.github.rthoth.ginsu.maze;

import com.github.rthoth.ginsu.Dimension;
import com.github.rthoth.ginsu.Ginsu;
import com.github.rthoth.ginsu.Knife;
import com.github.rthoth.ginsu.Knife.X;

import java.util.Comparator;
import java.util.TreeMap;

public class Lane<I> {

    private static final SideHandler X_GREATER_HANDLER = new SideHandler() {

        @Override
        public <I> E<I> getE(N<I> n) {
            return n.getXG();
        }

        @Override
        public <I> I getInfo(N<I> n) {
            return n.getXGI();
        }

        @Override
        public <I> void setInfo(N<I> n, I info) {
            n.setXGI(info);
        }
    };

    private static final SideHandler Y_LESS_HANDLER = new SideHandler() {
        @Override
        public <I> E<I> getE(N<I> n) {
            return n.getYL();
        }

        @Override
        public <I> I getInfo(N<I> n) {
            return n.getYLI();
        }

        @Override
        public <I> void setInfo(N<I> n, I info) {
            n.setYLI(info);
        }
    };

    private static final SideHandler X_LESS_HANDLER = new SideHandler() {
        @Override
        public <I> E<I> getE(N<I> n) {
            return n.getXL();
        }

        @Override
        public <I> I getInfo(N<I> n) {
            return n.getXLI();
        }

        @Override
        public <I> void setInfo(N<I> n, I info) {
            n.setXLI(info);
        }
    };

    private static final SideHandler Y_GREATER_HANDLER = new SideHandler() {
        @Override
        public <I> E<I> getE(N<I> n) {
            return n.getYG();
        }

        @Override
        public <I> I getInfo(N<I> n) {
            return n.getYGI();
        }

        @Override
        public <I> void setInfo(N<I> n, I info) {
            n.setYGI(info);
        }
    };

    private static final Controller X_CONTROLLER = new Controller() {

        @Override
        public SideHandler getGreaterHandler() {
            return X_GREATER_HANDLER;
        }

        @Override
        public SideHandler getLessHandler() {
            return X_LESS_HANDLER;
        }
    };

    private static final Controller Y_CONTROLLER = new Controller() {

        @Override
        public SideHandler getGreaterHandler() {
            return Y_GREATER_HANDLER;
        }

        @Override
        public SideHandler getLessHandler() {
            return Y_LESS_HANDLER;
        }
    };

    private final Knife<?> knife;
    private final Controller controller;
    private final TreeMap<Q, N<I>> index = new TreeMap<>(comparator());

    protected Lane(Knife<?> knife, Controller controller) {
        this.knife = knife;
        this.controller = controller;
    }

    public static <I> Lane<I> x(Knife<X> knife) {
        return new Lane<>(knife, X_CONTROLLER);
    }

    public static <I> Lane<I> y(Knife<Knife.Y> knife) {
        return new Lane<>(knife, Y_CONTROLLER);
    }

    public void add(N<I> n) {
        index.put(n.q, n);
    }

    Comparator<Q> comparator() {
        return (q1, q2) -> Ginsu.compare(knife.dimension.positionalOf(q1.coordinate), knife.offset, knife.dimension.positionalOf(q2.coordinate));
    }

    public Dimension getDimension() {
        return knife.dimension;
    }

    public void init(I initial, Visitor<I> visitor) {
        var lessHandler = controller.getLessHandler();
        var greaterHandler = controller.getGreaterHandler();
        init(initial, visitor, lessHandler);
        init(initial, visitor, greaterHandler);

        var lessI = initial;
        var greaterI = initial;

        for (var n : index.values()) {
            if (lessHandler.getE(n) == null)
                lessHandler.setInfo(n, visitor.apply(lessI, null, true));

            if (greaterHandler.getE(n) == null)
                greaterHandler.setInfo(n, visitor.apply(greaterI, null, true));

            lessI = lessHandler.getInfo(n);
            greaterI = greaterHandler.getInfo(n);
        }
    }

    private void init(I initial, Visitor<I> visitor, SideHandler handler) {
        var vector = Ginsu.filter(index.values(), n -> handler.getE(n) != null);

        if (!vector.isEmpty()) {
            var current = initial;
            var iterator = vector.iterator();

            while (iterator.hasNext()) {
                var n = iterator.next();
                current = visitor.apply(current, handler.getE(n), iterator.hasNext());
                handler.setInfo(n, current);
            }
        }
    }

    public <T> T map(N<I> n, Mapper<I, T> mapper) {
        var less = controller.getLessHandler().getInfo(n);
        var greater = controller.getGreaterHandler().getInfo(n);
        var le = controller.getLessHandler().getE(n);
        var he = controller.getGreaterHandler().getE(n);
        var ln = Ginsu.getValue(index.lowerEntry(n.q));
        var hn = Ginsu.getValue(index.higherEntry(n.q));

        return mapper.apply(less, greater, le, he, ln, hn);
    }

    public int positionOf(Q q) {
        return knife.positionOf(q.coordinate);
    }

    public <T> T visit(N<I> n, T value, N.Visitor<T, I> visitor) {
        var lessHandler = controller.getLessHandler();
        var greaterHandler = controller.getGreaterHandler();
        var lessI = lessHandler.getInfo(n);
        var greaterI = greaterHandler.getInfo(n);
        var lower = Ginsu.getValue(index.lowerEntry(n.q));
        var higher = Ginsu.getValue(index.higherEntry(n.q));

        return visitor.visit(value, lessI, greaterI, lower, higher);
    }

    public interface Mapper<I, T> {

        T apply(I li, I gi, E<I> le, E<I> he, N<I> ln, N<I> hn);
    }

    public interface Visitor<I> {

        I apply(I current, E<I> e, boolean hasMore);
    }

    private static abstract class Controller {

        public abstract SideHandler getGreaterHandler();

        public abstract SideHandler getLessHandler();
    }

    private static abstract class SideHandler {

        public abstract <I> E<I> getE(N<I> n);

        public abstract <I> I getInfo(N<I> n);

        public abstract <I> void setInfo(N<I> n, I info);
    }


}
