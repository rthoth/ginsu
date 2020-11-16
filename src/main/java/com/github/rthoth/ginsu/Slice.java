package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.Coordinate;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

@SuppressWarnings("unused")
public abstract class Slice<K> {

    public static final Slice<Knife<?>> INNER = new Inner();

    public static final int LOWER = -3;
    public static final int LOWER_BORDER = -2;
    public static final int MIDDLE = 1;
    public static final int UPPER_BORDER = 2;
    public static final int UPPER = 3;


    protected final Dimension dimension;

    protected Slice(Dimension dimension) {
        this.dimension = dimension;
    }

    public static <K extends Knife<K>> PVector<Slice<K>> from(Iterable<K> knives) {
        var result = TreePVector.<Slice<K>>empty();
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

    @SuppressWarnings("unchecked")
    public static <K extends Knife<?>> Slice<K> inner() {
        return (Slice<K>) INNER;
    }

    public static <K extends Knife<K>> Slice<K> lower(K upper) {
        return new Lower<>(upper);
    }

    public static <K extends Knife<K>> Slice<K> middle(K lower, K upper) {
        return new Middle<>(lower, upper);
    }

    public static Event.Side sideOf(int border) {
        switch (border) {
            case LOWER:
            case LOWER_BORDER:
                return Event.Side.GREAT;

            case UPPER:
            case UPPER_BORDER:
                return Event.Side.LESS;

            default:
                throw new GinsuException.IllegalArgument("Invalid border:" + border);
        }
    }

    public static <K extends Knife<K>> Slice<K> upper(K lower) {
        return new Upper<>(lower);
    }

    public Dimension getDimension() {

        return dimension;
    }

    public abstract double getKnifeValue();

    public abstract double getLower();

    public abstract double getOffset();

    public abstract double getUpper();

    public abstract Coordinate intersection(Coordinate origin, Coordinate target, int border);

//    public abstract double ordinateOf(Coordinate coordinate);

    public abstract int positionOf(Coordinate coordinate);

    private static class Inner extends Slice<Knife<?>> {

        private Inner() {
            super(null);
        }

        @Override
        public double getKnifeValue() {
            throw new GinsuException.Unsupported();
        }

        @Override
        public double getLower() {
            return Double.NEGATIVE_INFINITY;
        }

        @Override
        public double getOffset() {
            return 0;
        }

        @Override
        public double getUpper() {
            return Double.POSITIVE_INFINITY;
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

    public static class Lower<K extends Knife<K>> extends Slice<K> {

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
        public double getLower() {
            return Double.NEGATIVE_INFINITY;
        }

        @Override
        public double getOffset() {
            return upper.offset;
        }

        @Override
        public double getUpper() {
            return upper.value;
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

        @Override
        public String toString() {
            return "Lower[" + upper + "]";
        }
    }

    public static class Middle<K extends Knife<K>> extends Slice<K> {

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
        public double getLower() {
            return lower.value;
        }

        @Override
        public double getOffset() {
            return lower.offset;
        }

        @Override
        public double getUpper() {
            return upper.value;
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

        @Override
        public String toString() {
            return "Middle[" + lower + ", " + upper + "]";
        }
    }

    public static class Upper<K extends Knife<K>> extends Slice<K> {

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
        public double getLower() {
            return lower.value;
        }

        @Override
        public double getOffset() {
            return lower.offset;
        }

        @Override
        public double getUpper() {
            return Double.POSITIVE_INFINITY;
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

        @Override
        public String toString() {
            return "Upper[" + lower + "]";
        }
    }
}
