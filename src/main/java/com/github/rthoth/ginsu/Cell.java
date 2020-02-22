package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.Coordinate;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.util.Comparator;
import java.util.Objects;

public abstract class Cell {

    public static final int LOWER = -2;
    public static final int LOWER_BORDER = -1;
    public static final int MIDDLE = 0;
    public static final int UPPER_BORDER = 1;
    public static final int UPPER = 2;

    @SuppressWarnings("ComparatorCombinators")
    public static final Comparator<Intersection> INTERSECTION_COMPARATOR = (i1, i2) -> Double.compare(i1.ordinate, i2.ordinate);

    public static <K extends Knife<K>> PVector<Cell> from(PVector<K> knives) {
        var cells = TreePVector.<Cell>empty();

        if (!knives.isEmpty()) {
            var previous = knives.get(0);
            var index = 1;

            cells = cells.plus(new Lower<>(knives.get(0).getUpper()));
            for (; index < knives.size(); index++) {
                var current = knives.get(index);
                cells = cells.plus(new Middle<>(previous.getLower(), current.getUpper()));
                previous = current;
            }

            cells = cells.plus(new Upper<>(previous.getLower()));
        }

        return cells;
    }

    public abstract Intersection computeIntersection(Coordinate origin, Coordinate target, int position);

    public abstract Intersection createIntersection(Coordinate coordinate, int position);

    public abstract int positionOf(Coordinate coordinate);

    public static class Intersection {
        public final double ordinate;
        public final int border;
        public final Coordinate coordinate;

        public Intersection(double ordinate, int border, Coordinate coordinate) {
            this.ordinate = ordinate;
            this.coordinate = coordinate;
            if (border < 0)
                this.border = LOWER_BORDER;
            else if (border > 0)
                this.border = UPPER_BORDER;
            else
                throw new GinsuException.IllegalArgument(Integer.toString(border));
        }

        @Override
        public String toString() {
            return (border == LOWER_BORDER ? "L[" : "U[") + ordinate + "]";
        }
    }

    public static class Lower<K extends Knife<K>> extends Cell {

        private final K upper;

        public Lower(K upper) {
            this.upper = upper;
        }

        private K getKnife(int position) {
            switch (position) {
                case UPPER:
                case UPPER_BORDER:
                    return upper;
                default:
                    throw new GinsuException.IllegalArgument(Integer.toString(position));
            }
        }

        @Override
        public Intersection computeIntersection(Coordinate origin, Coordinate target, int position) {
            var knife = getKnife(position);
            var coordinate = knife.intersection(origin, target);
            return new Intersection(knife.ordinateOf(coordinate), position, coordinate);
        }

        @Override
        public Intersection createIntersection(Coordinate coordinate, int position) {
            return new Intersection(getKnife(position).ordinateOf(coordinate), position, null);
        }

        @Override
        public int positionOf(Coordinate coordinate) {
            switch (upper.positionOf(coordinate)) {
                case -1:
                    return MIDDLE;
                case 1:
                    return UPPER;
                case 0:
                    return UPPER_BORDER;
                default:
                    throw new GinsuException.IllegalArgument(Objects.toString(coordinate));
            }
        }
    }

    public static class Middle<K extends Knife<K>> extends Cell {

        private final K lower;
        private final K upper;

        public Middle(K lower, K upper) {
            this.lower = lower;
            this.upper = upper;
        }

        private K getKnife(int position) {
            switch (position) {
                case LOWER:
                case LOWER_BORDER:
                    return lower;

                case UPPER:
                case UPPER_BORDER:
                    return upper;

                default:
                    throw new GinsuException.IllegalArgument(Integer.toString(position));
            }
        }

        @Override
        public Intersection computeIntersection(Coordinate origin, Coordinate target, int position) {
            var knife = getKnife(position);
            var coordinate = knife.intersection(origin, target);
            return new Intersection(knife.ordinateOf(coordinate), position, coordinate);
        }

        @Override
        public Intersection createIntersection(Coordinate coordinate, int position) {
            return new Intersection(getKnife(position).ordinateOf(coordinate), position, null);
        }

        @Override
        public int positionOf(Coordinate coordinate) {
            switch (lower.positionOf(coordinate)) {
                case -1:
                    return LOWER;
                case 1:
                    switch (upper.positionOf(coordinate)) {
                        case 1:
                            return UPPER;
                        case -1:
                            return MIDDLE;
                        case 0:
                            return UPPER_BORDER;
                        default:
                            throw new GinsuException.IllegalArgument(Objects.toString(coordinate));
                    }
                case 0:
                    return LOWER_BORDER;
                default:
                    throw new GinsuException.IllegalArgument(Objects.toString(coordinate));
            }
        }
    }

    public static class Upper<K extends Knife<K>> extends Cell {

        private final K lower;

        public Upper(K lower) {
            this.lower = lower;
        }

        private K getKnife(int position) {
            switch (position) {
                case LOWER:
                case LOWER_BORDER:
                    return lower;
                default:
                    throw new GinsuException.IllegalArgument(Integer.toString(position));
            }
        }

        @Override
        public Intersection computeIntersection(Coordinate origin, Coordinate target, int position) {
            var knife = getKnife(position);
            var coordinate = knife.intersection(origin, target);
            return new Intersection(knife.ordinateOf(coordinate), position, coordinate);
        }

        @Override
        public Intersection createIntersection(Coordinate coordinate, int position) {
            return new Intersection(getKnife(position).ordinateOf(coordinate), position, null);
        }

        @Override
        public int positionOf(Coordinate coordinate) {
            switch (lower.positionOf(coordinate)) {
                case -1:
                    return LOWER;
                case 1:
                    return MIDDLE;
                case 0:
                    return LOWER_BORDER;
                default:
                    throw new GinsuException.IllegalArgument(Objects.toString(coordinate));
            }
        }
    }
}
