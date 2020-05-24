package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequences;

public abstract class Segment {

    public static Segment backward(int start, int stop, CoordinateSequence sequence) {
        var isRing = CoordinateSequences.isRing(sequence);
        var limit = isRing ? sequence.size() - 1 : sequence.size();

        if (start >= stop) {
            if (start > stop) {
                return new Backward(start, limit, start - stop + 1, isRing, sequence);
            } else {
                return new PointView(start, sequence);
            }
        } else if (isRing) {
            var size = limit - stop + start + 1;
            return new Backward(start, limit, size, true, sequence);
        } else {
            throw new GinsuException.IllegalArgument("CoordinateSequence should be closed!");
        }
    }

    public static Segment forward(int start, int stop, CoordinateSequence sequence) {
        var isRing = CoordinateSequences.isRing(sequence);
        var limit = isRing ? sequence.size() - 1 : sequence.size();
        if (start <= stop) {
            if (start < stop) {
                return new Forward(start, limit, stop - start + 1, isRing, sequence);
            } else {
                return new PointView(start, sequence);
            }
        } else if (isRing) {
            return new Forward(start, limit, limit - start + stop + 1, true, sequence);
        } else {
            throw new GinsuException.IllegalArgument("CoordinateSequence should be closed!");
        }
    }

    public static Segment wrap(Segment segment, int size) {
        return new Wrapper(segment, size);
    }

    public abstract Coordinate getCoordinate(int index);

    public abstract void getCoordinate(int index, Coordinate coordinate);

    public abstract double getOrdinate(int index, int ordinateIndex);

    public abstract double getX(int index);

    public abstract double getY(int index);

    public abstract int size();

    public static class Backward extends View {
        public Backward(int start, int limit, int size, boolean isRing, CoordinateSequence sequence) {
            super(start, limit, size, isRing, sequence);
        }

        @Override
        protected int mapIndex(int index) {
            return index <= start ? start - index : limit - (index - start);
        }
    }

    public static class Forward extends View {
        public Forward(int start, int limit, int size, boolean isRing, CoordinateSequence sequence) {
            super(start, limit, size, isRing, sequence);
        }

        @Override
        protected int mapIndex(int index) {
            return (start + index) % limit;
        }
    }

    public static class Line extends Segment {
        private final Coordinate start;
        private final Coordinate stop;

        public Line(Coordinate start, Coordinate stop) {
            this.start = start;
            this.stop = stop;
        }

        @Override
        public Coordinate getCoordinate(int index) {
            switch (index) {
                case 0:
                    return start;
                case 1:
                    return stop;
                default:
                    throw new GinsuException.IllegalArgument(Integer.toString(index));
            }
        }

        @Override
        public void getCoordinate(int index, Coordinate coordinate) {
            coordinate.setCoordinate(getCoordinate(index));
        }

        @Override
        public double getOrdinate(int index, int ordinateIndex) {
            return getCoordinate(index).getOrdinate(ordinateIndex);
        }

        @Override
        public double getX(int index) {
            return getCoordinate(index).getX();
        }

        @Override
        public double getY(int index) {
            return getCoordinate(index).getY();
        }

        public Segment point(int index) {
            return new Point(getCoordinate(index));
        }

        @Override
        public int size() {
            return 2;
        }
    }

    public static class Point extends Segment {
        private final Coordinate point;

        public Point(Coordinate point) {
            this.point = point;
        }

        private void check(int index) {
            if (index != 0)
                throw new GinsuException.IllegalArgument(Integer.toString(index));
        }

        @Override
        public Coordinate getCoordinate(int index) {
            check(index);
            return point;
        }

        @Override
        public void getCoordinate(int index, Coordinate coordinate) {
            check(index);
            coordinate.setCoordinate(point);
        }

        @Override
        public double getOrdinate(int index, int ordinateIndex) {
            check(index);
            return point.getOrdinate(ordinateIndex);
        }

        @Override
        public double getX(int index) {
            check(index);
            return point.getX();
        }

        @Override
        public double getY(int index) {
            check(index);
            return point.getY();
        }

        @Override
        public int size() {
            return 1;
        }
    }

    public static class PointView extends Segment {
        private final int index;
        private final CoordinateSequence sequence;

        public PointView(int index, CoordinateSequence sequence) {
            this.index = index;
            this.sequence = sequence;
        }

        private void check(int index) {
            if (index != 0)
                throw new GinsuException.InvalidIndex(index);
        }

        @Override
        public Coordinate getCoordinate(int index) {
            if (index != 0)
                throw new GinsuException.IllegalArgument(Integer.toString(index));

            return sequence.getCoordinate(this.index);
        }

        @Override
        public void getCoordinate(int index, Coordinate coordinate) {
            coordinate.setCoordinate(getCoordinate(index));
        }

        @Override
        public double getOrdinate(int index, int ordinateIndex) {
            return getCoordinate(index).getOrdinate(ordinateIndex);
        }

        @Override
        public double getX(int index) {
            return getCoordinate(index).getX();
        }

        @Override
        public double getY(int index) {
            return getCoordinate(index).getY();
        }

        @Override
        public int size() {
            return 1;
        }
    }

    public static abstract class View extends Segment {

        protected final int start;
        protected final int limit;
        protected final int size;
        protected final boolean isRing;
        protected final CoordinateSequence sequence;

        public View(int start, int limit, int size, boolean isRing, CoordinateSequence sequence) {
            this.start = start;
            this.limit = limit;
            this.size = size;
            this.isRing = isRing;
            this.sequence = sequence;
        }

        @Override
        public Coordinate getCoordinate(int index) {
            return sequence.getCoordinate(mapIndex(index));
        }

        @Override
        public void getCoordinate(int index, Coordinate coordinate) {
            sequence.getCoordinate(mapIndex(index), coordinate);
        }

        @Override
        public double getOrdinate(int index, int ordinateIndex) {
            return sequence.getOrdinate(mapIndex(index), ordinateIndex);
        }

        @Override
        public double getX(int index) {
            return sequence.getX(mapIndex(index));
        }

        @Override
        public double getY(int index) {
            return sequence.getY(mapIndex(index));
        }

        protected abstract int mapIndex(int index);

        public Segment point(int index) {
            return new PointView(mapIndex(index), sequence);
        }

        @Override
        public int size() {
            return size;
        }
    }

    public static class Wrapper extends Segment {
        private final Segment segment;
        private final int bottom;

        public Wrapper(Segment segment, int bottom) {
            this.segment = segment;
            this.bottom = bottom;
        }

        @Override
        public Coordinate getCoordinate(int index) {
            return segment.getCoordinate(index - bottom);
        }

        @Override
        public void getCoordinate(int index, Coordinate coordinate) {
            segment.getCoordinate(index - bottom, coordinate);
        }

        @Override
        public double getOrdinate(int index, int ordinateIndex) {
            return segment.getOrdinate(index - bottom, ordinateIndex);
        }

        @Override
        public double getX(int index) {
            return segment.getX(index - bottom);
        }

        @Override
        public double getY(int index) {
            return segment.getY(index - bottom);
        }

        @Override
        public int size() {
            return segment.size();
        }
    }
}
