package com.github.rthoth.ginsu.detection;

import com.github.rthoth.ginsu.Dimension;
import com.github.rthoth.ginsu.Knife;
import org.locationtech.jts.geom.Coordinate;

import java.util.Optional;
import java.util.TreeMap;

@SuppressWarnings("unused")
public abstract class Slice<K extends Knife> {

    @SuppressWarnings("rawtypes")
    private static final Slice INNER = new Inner<>();

    @SuppressWarnings("unchecked")
    public static <K extends Knife> Slice<K> inner() {
        return (Slice<K>) INNER;
    }

    public static <K extends Knife> Slice<K> lower(K knife) {
        return new Lower<>(knife);
    }

    public static <K extends Knife> Slice<K> middle(K lower, K upper) {
        return new Middle<>(lower, upper);
    }

    public static <K extends Knife> Slice<K> upper(K knife) {
        return new Upper<>(knife);
    }

    public abstract Dimension getDimension();

    public abstract Optional<K> getLower();

    public abstract Optional<K> getUpper();

    public abstract int position(Coordinate coordinate);

    private abstract static class Abstract<K extends Knife> extends Slice<K> {

        private final Dimension dimension;

        public Abstract(Dimension dimension) {
            this.dimension = dimension;
        }

        @Override
        public Dimension getDimension() {
            return dimension;
        }
    }

    private static class Inner<K extends Knife> extends Slice<K> {

        @Override
        public Dimension getDimension() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<K> getLower() {
            return Optional.empty();
        }

        @Override
        public Optional<K> getUpper() {
            return Optional.empty();
        }

        @Override
        public int position(Coordinate coordinate) {
            return 1;
        }
    }

    private static class Lower<K extends Knife> extends Abstract<K> {

        private final K knife;
        private TreeMap<Double, Event> events;

        public Lower(K knife) {
            super(knife.dimension);
            this.knife = knife;
        }

        @Override
        public Optional<K> getLower() {
            return Optional.empty();
        }

        @Override
        public Optional<K> getUpper() {
            return Optional.of(knife);
        }

        @Override
        public int position(Coordinate coordinate) {
            switch (knife.position(coordinate)) {
                case -1:
                    return 1;
                case 1:
                    return 3;
                default:
                    return 2;
            }
        }
    }

    private static class Middle<K extends Knife> extends Abstract<K> {

        private final K lower;
        private final K upper;
        private TreeMap<Double, Event> lowerEvents;
        private TreeMap<Double, Event> upperEvents;

        public Middle(K lower, K upper) {
            super(lower.dimension);
            this.lower = lower;
            this.upper = upper;
        }

        @Override
        public Optional<K> getLower() {
            return Optional.of(lower);
        }

        @Override
        public Optional<K> getUpper() {
            return Optional.of(upper);
        }

        @Override
        public int position(Coordinate coordinate) {
            switch (lower.position(coordinate)) {
                case -1:
                    return -3;
                case 0:
                    return -2;
            }

            switch (upper.position(coordinate)) {
                case 1:
                    return 3;
                case 0:
                    return 2;
            }

            return 1;
        }
    }

    private static class Upper<K extends Knife> extends Abstract<K> {

        private final K knife;
        private TreeMap<Double, Event> events;

        public Upper(K knife) {
            super(knife.dimension);
            this.knife = knife;
        }

        @Override
        public Optional<K> getLower() {
            return Optional.of(knife);
        }

        @Override
        public Optional<K> getUpper() {
            return Optional.empty();
        }

        @Override
        public int position(Coordinate coordinate) {
            switch (knife.position(coordinate)) {
                case 1:
                    return 1;
                case -1:
                    return -3;
                default:
                    return -2;
            }
        }
    }
}
