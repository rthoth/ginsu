package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.Coordinate;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

public abstract class Slice {

    public static final int MIDDLE = 1;
    public static final int LOWER = -3;
    public static final int LOWER_BORDER = -2;
    public static final int UPPER_BORDER = 2;
    public static final int UPPER = 3;
    public static final Slice INNER = new Inner();

    protected static Location computeLocation(Coordinate origin, Coordinate target, int border, Knife<?> knife) {
        final var intersection = knife.intersection(origin, target);
        return new Location(border, knife.ordinateOf(intersection), intersection);
    }

    public static <K extends Knife<K>> PVector<Slice> from(Iterable<K> knives) {
        var result = TreePVector.<Slice>empty();
        final var iterator = knives.iterator();
        if (iterator.hasNext()) {
            var previous = Ginsu.next(iterator);
            result = result.plus(Slice.lower(previous.getUpper()));

            while (iterator.hasNext()) {
                var current = iterator.next();
                result = result.plus(Slice.middle(previous.getLower(), current.getUpper()));
                previous = current;
            }

            result = result.plus(Slice.upper(previous.getLower()));
        }

        return result;
    }

    public static <K extends Knife<K>> Slice lower(K upper) {
        return new Lower<>(upper);
    }

    public static <K extends Knife<K>> Slice middle(K lower, K upper) {
        return new Middle<>(lower, upper);
    }

    public static <K extends Knife<K>> Slice upper(K lower) {
        return new Upper<>(lower);
    }

    public abstract Location computeLocation(Coordinate origin, Coordinate target, int border);

    public abstract Location createLocation(Coordinate coordinate, int border);

    public abstract Coordinate intersection(Coordinate origin, Coordinate target, int border);

    public abstract int positionOf(Coordinate coordinate);

    private static class Inner extends Slice {

        @Override
        public Location computeLocation(Coordinate origin, Coordinate target, int border) {
            throw new GinsuException.Unsupported();
        }

        @Override
        public Location createLocation(Coordinate coordinate, int border) {
            throw new GinsuException.Unsupported();
        }

        @Override
        public Coordinate intersection(Coordinate origin, Coordinate target, int border) {
            throw new GinsuException.Unsupported();
        }

        @Override
        public int positionOf(Coordinate coordinate) {
            return MIDDLE;
        }
    }

    public static class Location {

        public final int border;

        public final double ordinate;

        public final Coordinate coordinate;

        public Location(int border, double ordinate, Coordinate coordinate) {
            this.border = border > 1 ? UPPER_BORDER : LOWER_BORDER;
            this.ordinate = ordinate;
            this.coordinate = coordinate;
        }

        @Override
        public String toString() {
            return (border > 1 ? "U" : "L") + "[" + ordinate + "]";
        }
    }

    public static class Lower<K extends Knife<K>> extends Slice {

        private final K upper;

        public Lower(K upper) {
            this.upper = upper;
        }

        @Override
        public Location computeLocation(Coordinate origin, Coordinate target, int border) {
            if (border > 1)
                return computeLocation(origin, target, border, upper);
            else
                throw new GinsuException.Unsupported();
        }

        @Override
        public Location createLocation(Coordinate coordinate, int border) {
            if (border > 1)
                return new Location(border, upper.ordinateOf(coordinate), null);
            else
                throw new GinsuException.Unsupported();
        }

        @Override
        public Coordinate intersection(Coordinate origin, Coordinate target, int border) {
            if (border > 1)
                return upper.intersection(origin, target);
            else
                throw new GinsuException.IllegalArgument(String.format("Border [%d]", border));
        }

        @Override
        public int positionOf(Coordinate coordinate) {
            switch (upper.positionOf(coordinate)) {
                case -1:
                    return MIDDLE;

                case 1:
                    return UPPER;

                default:
                    return UPPER_BORDER;
            }
        }
    }

    public static class Middle<K extends Knife<K>> extends Slice {

        private final K lower;
        private final K upper;

        public Middle(K lower, K upper) {
            this.lower = lower;
            this.upper = upper;
        }

        @Override
        public Location computeLocation(Coordinate origin, Coordinate target, int border) {
            if (border > 1)
                return computeLocation(origin, target, border, upper);
            else if (border < 1)
                return computeLocation(origin, target, border, lower);
            else
                throw new GinsuException.Unsupported();
        }

        @Override
        public Location createLocation(Coordinate coordinate, int border) {
            if (border > 1)
                return new Location(border, upper.ordinateOf(coordinate), null);
            else if (border < 1)
                return new Location(border, lower.ordinateOf(coordinate), null);
            else
                throw new GinsuException.Unsupported();
        }

        @Override
        public Coordinate intersection(Coordinate origin, Coordinate target, int border) {
            if (border > 1)
                return upper.intersection(origin, target);
            else if (border < 1)
                return lower.intersection(origin, target);
            else
                throw new GinsuException.IllegalArgument(String.format("Border [%d]!", border));
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
                        default:
                            return UPPER_BORDER;
                    }

                default:
                    return LOWER_BORDER;
            }
        }
    }

    public static class Upper<K extends Knife<K>> extends Slice {

        private final K lower;

        public Upper(K lower) {
            this.lower = lower;
        }

        @Override
        public Location computeLocation(Coordinate origin, Coordinate target, int border) {
            if (border < 1)
                return computeLocation(origin, target, border, lower);
            else
                throw new GinsuException.Unsupported();
        }

        @Override
        public Location createLocation(Coordinate coordinate, int border) {
            if (border < 1)
                return new Location(border, lower.ordinateOf(coordinate), null);
            else
                throw new GinsuException.Unsupported();
        }

        @Override
        public Coordinate intersection(Coordinate origin, Coordinate target, int border) {
            if (border < 1)
                return lower.intersection(origin, target);
            else
                throw new GinsuException.IllegalArgument(String.format("Border [%d]!", border));
        }

        @Override
        public int positionOf(Coordinate coordinate) {
            switch (lower.positionOf(coordinate)) {
                case -1:
                    return LOWER;

                case 1:
                    return MIDDLE;

                default:
                    return LOWER_BORDER;
            }
        }
    }
}
