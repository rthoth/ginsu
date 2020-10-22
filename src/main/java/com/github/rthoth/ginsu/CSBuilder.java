package com.github.rthoth.ginsu;

import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.util.LinkedList;

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

        private final double offset;
        private final CSBuilder builder;
        private final LinkedList<Segment> segments = new LinkedList<>();

        public Simplified(double offset) {
            this(offset, new CSBuilder());
        }

        public Simplified(double offset, CSBuilder builder) {
            this.offset = offset;
            this.builder = builder;
        }

        public void add(Coordinate coordinate) {
            if (coordinate != null) {
                add(new Segment.Point(coordinate));
            }
        }

        public void add(Segment segment) {
            var lastSegment = segments.peekLast();
            if (lastSegment != null) {
                var lastCoordinate = lastSegment.getLast();
                var firstCoordinate = segment.getFirst();

                if (firstCoordinate.equals2D(lastCoordinate, offset)) {
                    segment = segment.dropFirst();
                    if (segment != null)
                        firstCoordinate = segment.getFirst();
                    else
                        return;
                }

                if (isCollinear(getPenultimate(), lastCoordinate, firstCoordinate)) {
                    replaceLast(lastSegment.dropLast());
                }
            }
            segments.addLast(segment);
        }

        @SuppressWarnings("ConstantConditions")
        public CoordinateSequence close() {
            if (!segments.isEmpty()) {
                var lastSegment = segments.peekLast();

                if (isCollinear(getPenultimate(), lastSegment.getLast(), segments.peekFirst().getFirst())) {
                    replaceLast(lastSegment.dropLast());
                }

                if (segments.isEmpty())
                    throw new GinsuException.IllegalState("It's empty!");

                var firstSegment = segments.peekFirst();

                if (isCollinear(getSecond(), firstSegment.getFirst(), segments.peekLast().getLast())) {
                    replaceFirst(firstSegment.dropFirst());
                }

                for (var segment : segments)
                    builder.add(segment);

                return builder.close();
            } else {
                throw new GinsuException.IllegalState("It's empty!");
            }
        }

        private Coordinate getPenultimate() {
            var segment = segments.getLast();
            if (segment.size() > 1)
                return segment.getCoordinate(segment.size() - 2);
            else if (segments.size() > 1) {
                return segments.get(segments.size() - 2).getLast();
            } else {
                return null;
            }
        }

        private Coordinate getSecond() {
            var first = segments.getFirst();
            if (first.size() > 1)
                return first.getCoordinate(1);
            else if (segments.size() > 1)
                return segments.get(1).getFirst();
            else
                return null;
        }

        private boolean isCollinear(Coordinate penultimate, Coordinate lastCoordinate, Coordinate firstCoordinate) {
            if (penultimate != null)
                return Orientation.index(penultimate, lastCoordinate, firstCoordinate) == Orientation.COLLINEAR;
            else
                return false;
        }

        private void replaceFirst(Segment newFirst) {
            segments.removeFirst();
            if (newFirst != null) segments.addFirst(newFirst);
        }

        private void replaceLast(Segment newLast) {
            segments.removeLast();
            if (newLast != null) segments.addLast(newLast);
        }
    }
}
