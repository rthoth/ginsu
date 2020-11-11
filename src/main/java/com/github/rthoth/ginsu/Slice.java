package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.Coordinate;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

public abstract class Slice {

    public static final int LOWER = -3;
    public static final int LOWER_BORDER = -2;
    public static final int MIDDLE = 1;
    public static final int UPPER_BORDER = 2;
    public static final int UPPER = 3;

    public static final Slice INNER = new Inner();

    private final Dimension dimension;

    protected Slice(Dimension dimension) {
        this.dimension = dimension;
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

    public static Dimension.Side sideOf(int border) {
        switch (border) {
            case LOWER:
            case LOWER_BORDER:
                return Dimension.Side.GREATER;

            case UPPER:
            case UPPER_BORDER:
                return Dimension.Side.LESS;

            default:
                throw new GinsuException.IllegalArgument("Invalid border:" + border);
        }
    }

    public static <K extends Knife<K>> Slice upper(K lower) {
        return new Upper<>(lower);
    }

    public Dimension getDimension() {

        return dimension;
    }

    public abstract double getKnifeValue();

    public abstract Slice getLower();

    public abstract Slice getUpper();

    public abstract Coordinate intersection(Coordinate origin, Coordinate target, int border);

    public abstract double ordinateOf(Coordinate coordinate);

    public abstract int positionOf(Coordinate coordinate);

    private static class Inner extends Slice {

        private Inner() {
            super(null);
        }

        @Override
        public double getKnifeValue() {
            throw new GinsuException.Unsupported();
        }

        @Override
        public Slice getLower() {
            return null;
        }

        @Override
        public Slice getUpper() {
            return null;
        }

        @Override
        public Coordinate intersection(Coordinate origin, Coordinate target, int border) {
            throw new GinsuException.Unsupported();
        }

        @Override
        public double ordinateOf(Coordinate coordinate) {
            throw new GinsuException.Unsupported();
        }

        @Override
        public int positionOf(Coordinate coordinate) {
            return MIDDLE;
        }
    }

    public static class Lower<K extends Knife<K>> extends Slice {

        private final K upper;

        public Lower(K upper) {
            super(upper.dimension);
            this.upper = upper;
        }

        @Override
        public double getKnifeValue() {
            return upper.value;
        }

        @Override
        public Slice getLower() {
            return null;
        }

        @Override
        public Slice getUpper() {
            return this;
        }

        @Override
        public Coordinate intersection(Coordinate origin, Coordinate target, int border) {
            if (border > 1)
                return upper.intersection(origin, target);
            else
                throw new GinsuException.IllegalArgument(String.format("Border [%d]", border));
        }

        @Override
        public double ordinateOf(Coordinate coordinate) {
            return upper.ordinateOf(coordinate);
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
            super(lower.dimension);
            this.lower = lower;
            this.upper = upper;
        }

        @Override
        public double getKnifeValue() {
            throw new GinsuException.Unsupported();
        }

        @Override
        public Slice getLower() {
            return new Upper<>(lower);
        }

        @Override
        public Slice getUpper() {
            return new Lower<>(upper);
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
        public double ordinateOf(Coordinate coordinate) {
            return lower.ordinateOf(coordinate);
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
            super(lower.dimension);
            this.lower = lower;
        }

        @Override
        public double getKnifeValue() {
            return lower.value;
        }

        @Override
        public Slice getLower() {
            return this;
        }

        @Override
        public Slice getUpper() {
            return null;
        }

        @Override
        public Coordinate intersection(Coordinate origin, Coordinate target, int border) {
            if (border < 1)
                return lower.intersection(origin, target);
            else
                throw new GinsuException.IllegalArgument(String.format("Border [%d]!", border));
        }

        @Override
        public double ordinateOf(Coordinate coordinate) {
            return lower.ordinateOf(coordinate);
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
