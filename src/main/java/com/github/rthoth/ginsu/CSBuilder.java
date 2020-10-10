package com.github.rthoth.ginsu;

import org.locationtech.jts.algorithm.Distance;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequences;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

public class CSBuilder {

    private PVector<Segment> segments = TreePVector.empty();

    public void add(Coordinate coordinate) {
        if (coordinate != null)
            addPoint(coordinate);
    }

    public void add(Segment segment) {
        if (segment != null)
            segments = segments.plus(segment);
    }

    public void addBackward(int start, int stop, CoordinateSequence sequence) {
        segments = segments.plus(Segment.backward(start, stop, sequence));
    }

    public void addForward(int start, int stop, CoordinateSequence sequence) {
        segments = segments.plus(Segment.forward(start, stop, sequence));
    }

    public void addLine(Coordinate start, Coordinate stop) {
        segments = segments.plus(new Segment.Line(start, stop));
    }

    public void addPoint(Coordinate point) {
        segments = segments.plus(new Segment.Point(point));
    }

    public CoordinateSequence build() {
        return new SegmentedCoordinateSequence(segments);
    }

    public SegmentedCoordinateSequence close() {
        if (!segments.isEmpty()) {
            var first = segments.get(0);
            var last = segments.get(segments.size() - 1);

            if (!first.getCoordinate(0).equals2D(last.getCoordinate(last.size() - 1))) {
                if (first instanceof Segment.View) {
                    return new SegmentedCoordinateSequence(segments.plus(((Segment.View) first).point(0)));
                } else if (first instanceof Segment.PointView || first instanceof Segment.Point) {
                    return new SegmentedCoordinateSequence(segments.plus(first));
                } else if (first instanceof Segment.Line) {
                    return new SegmentedCoordinateSequence(segments.plus(((Segment.Line) first).point(0)));
                } else {
                    throw new GinsuException.IllegalState("Invalid segments!");
                }
            } else {
                return new SegmentedCoordinateSequence(segments);
            }
        } else {
            throw new GinsuException.IllegalState("It is empty!");
        }
    }

    public Coordinate getLastCoordinate() {
        if (!segments.isEmpty()) {
            var last = segments.get(segments.size() - 1);
            return last.getCoordinate(last.size() - 1);
        } else {
            return null;
        }
    }

    /**
     * previous -> pivot -> new Coordinate
     */
    public static class Simplified {

        private static final int IGNORE = 0;
        private static final int PUSH = 1;
        private static final int COLLINEAR = 2;

        private final CSBuilder builder;
        private final double offset;
        private Coordinate previous;
        private Coordinate pivot;
        private boolean previousShouldAdd = true;
        private boolean pivotShouldAdd = true;

        public Simplified(double offset) {
            this(offset, new CSBuilder());
        }

        protected Simplified(double offset, CSBuilder builder) {
            this.offset = offset;
            this.builder = builder;
        }

        public void add(Coordinate coordinate) {
            if (coordinate != null) {
                switch (whatToDo(coordinate)) {
                    case PUSH:
                        pushPrevious();

                        previous = pivot;
                        previousShouldAdd = pivotShouldAdd;

                        pivot = coordinate;
                        pivotShouldAdd = true;

                        break;

                    case COLLINEAR:
                        pivotShouldAdd = true;
                        pivot = coordinate;
                        break;
                    case IGNORE:
                        break;
                }
            }
        }

        public void addBackward(int start, int stop, CoordinateSequence sequence) {
            switch (whatToDo(sequence.getCoordinate(start))) {
                case IGNORE:
                case COLLINEAR:
                    start = start > 1 ? start - 1 : (CoordinateSequences.isRing(sequence) ? sequence.size() - 2 : sequence.size() - 1);
                    break;
            }
            push(Segment.backward(start, stop, sequence));
        }

        public void addForward(int start, int stop, CoordinateSequence sequence) {
            switch (whatToDo(sequence.getCoordinate(start))) {
                case IGNORE:
                case COLLINEAR:
                    start = (start + 1) % (sequence.size() - (CoordinateSequences.isRing(sequence) ? 1 : 0));
                    break;
            }
            push(Segment.forward(start, stop, sequence));
        }

        public CoordinateSequence close() {
            pushPrevious();
            pushPivot();

            return builder.close();
        }

        private boolean nonLinear(Coordinate coordinate) {
            return Distance.pointToSegment(coordinate, previous, pivot) > offset;
        }

        private void push(Segment segment) {
            if (segment.size() > 0) {
                pushPrevious();
                pushPivot();

                builder.add(segment);

                previousShouldAdd = false;
                pivotShouldAdd = false;

                if (segment.size() > 1) {
                    previous = segment.getCoordinate(segment.size() - 2);
                } else {
                    previous = null;
                }

                pivot = segment.getCoordinate(segment.size() - 1);
            }
        }

        void pushPivot() {
            if (pivotShouldAdd && pivot != null) {
                builder.add(pivot);
            }
        }

        private void pushPrevious() {
            if (previousShouldAdd && previous != null) {
                builder.add(previous);
                previous = null;
            }
        }

        private int whatToDo(Coordinate coordinate) {
            if (pivot != null) {
                if (!pivot.equals2D(coordinate, offset)) {
                    if (previous != null) {
                        return nonLinear(coordinate) ? PUSH : COLLINEAR;
                    } else {
                        return PUSH;
                    }
                } else {
                    return IGNORE;
                }
            } else {
                return PUSH;
            }
        }
    }
}
