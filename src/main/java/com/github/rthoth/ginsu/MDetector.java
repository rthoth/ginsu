package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import static com.github.rthoth.ginsu.MShape.Detection.*;
import static com.github.rthoth.ginsu.Slice.*;

public class MDetector {

    private final MEvent.Factory factory;
    private final Dimension x;
    private final Dimension y;
    private Coordinate firstCoordinate;
    private int firstLocation;
    private MEvent candidate;
    private PVector<MEvent> events;

    public MDetector(Slice x, Slice y, MEvent.Factory factory) {
        this.x = new Dimension(x);
        this.y = new Dimension(y);
        this.factory = factory;
    }

    public static MShape.Detection detect(Slice x, Slice y, CoordinateSequence sequence) {
        var detector = new MDetector(x, y, new MEvent.Factory(sequence));
        detector.first(sequence.getCoordinate(0));

        final var lastIndex = sequence.size() - 1;
        for (var index = 1; index < lastIndex; index++)
            detector.check(index, sequence.getCoordinate(index));

        return detector.last(lastIndex, sequence.getCoordinate(lastIndex));
    }

    public void check(int index, Coordinate coordinate) {
        x.check(index, coordinate);
        y.check(index, coordinate);

        if (x.product != 9 && y.product != 9 && (x.changed || y.changed)) {

            if (firstLocation == BORDER)
                firstLocation = locate(coordinate);

            if (x.changed) {
                check(x, y);
            } else {
                check(y, x);
            }
        }

        x.update();
        y.update();
    }

    private void check(Dimension _1, Dimension _2) {
        var segment = _1.createSegment();
        if (segment != null) {
            segment = _2.filter(segment);
            if (segment != null) {
                segment.origin.apply(this);
                segment.target.apply(this);
            }
        }
    }

    public void first(Coordinate coordinate) {
        firstCoordinate = coordinate.copy();
        firstLocation = locate(coordinate);

        x.first(coordinate);
        y.first(coordinate);
        events = TreePVector.empty();
    }

    public MShape.Detection last(int index, Coordinate coordinate) {
        check(index, coordinate);

        if (candidate != null) {
            if (candidate.index == index) {
                if (!events.isEmpty() && events.get(0).index == 0) {
                    events = events.minus(0);
                } else {
                    push(candidate);
                }
            } else {
                push(candidate);
            }
        }

        return new MShape.Detection(events, firstCoordinate.equals2D(coordinate), firstLocation, factory);
    }

    private int locate(Coordinate coordinate) {
        var xPosition = x.positionOf(coordinate);
        var yPosition = y.positionOf(coordinate);

        if (xPosition == LOWER || xPosition == UPPER || yPosition == LOWER || yPosition == UPPER)
            return OUTSIDE;
        else
            return (xPosition == MIDDLE && yPosition == MIDDLE) ? INSIDE : BORDER;
    }

    private void push(MEvent event) {
        events = events.plus(event);
        candidate = null;
    }

    private void pushCandidate(int index, Coordinate coordinate) {
        if (candidate == null) {
            candidate = factory.newOut(index, coordinate);
        } else {
            throw new GinsuException.IllegalState("There is a candidate!");
        }
    }

    private void pushIn(int index, Coordinate coordinate) {
        if (candidate == null) {
            events = events.plus(factory.newIn(index, coordinate));
        } else if (candidate.index < index) {
            events = events.plus(candidate);
            events = events.plus(factory.newIn(index, coordinate));
            candidate = null;
        } else {
            candidate = null;
        }
    }

    private void pushOut(int index, Coordinate coordinate) {
        if (candidate == null) {
            events = events.plus(factory.newOut(index, coordinate));
        } else {
            throw new GinsuException.IllegalState("There is a candidate!");
        }
    }

    private static class Action {


        public static final int OUT = 1;
        public static final int UNDEFINED = 0;
        public static final int IN = 2;
        public static final int CANDIDATE = 3;

        private final int index;
        private final Coordinate coordinate;
        private final boolean referenced;
        private final int kind;

        public Action(int index, Coordinate coordinate, boolean referenced, int kind) {
            this.index = index;
            this.coordinate = coordinate;
            this.referenced = referenced;
            this.kind = kind;
        }

        public void apply(MDetector detector) {
            switch (kind) {
                case UNDEFINED:
                    break;

                case IN:
                    detector.pushIn(index, referenced ? null : coordinate);
                    break;

                case OUT:
                    detector.pushOut(index, referenced ? null : coordinate);
                    break;

                case CANDIDATE:
                    detector.pushCandidate(index, referenced ? null : coordinate);
                    break;
            }

        }

        public Action upgrade(int nIndex, Coordinate nCoordinate, boolean referenced, int newKind) {
            return new Action(nIndex, nCoordinate, referenced && this.referenced, newKind);
        }

        public Action withKind(int newKind) {
            return new Action(index, coordinate, referenced, newKind);
        }
    }

    private static class Dimension {

        private final Slice slice;
        private int previous;
        private int product;
        private boolean changed;
        private int current;
        private int pIndex;
        private Coordinate pCoordinate;
        private int cIndex;
        private Coordinate cCoordinate;

        public Dimension(Slice slice) {
            this.slice = slice;
        }

        public void check(int index, Coordinate coordinate) {
            current = slice.positionOf(coordinate);
            product = previous * current;
            changed = previous != current;
            cIndex = index;
            cCoordinate = coordinate;
        }

        private Coordinate computeIntersection(int position) {
            return slice.intersection(pCoordinate, cCoordinate, position);
        }

        public Segment createSegment() {
            final var segment = new Segment(newAction(pIndex, pCoordinate, true, Action.UNDEFINED), newAction(cIndex, cCoordinate, true, Action.UNDEFINED));
            return filter(segment);
        }

        private Segment filter(int previous, int current, int product, Segment segment) {
            final var origin = segment.origin;
            final var target = segment.target;
            final var pIndex = origin.index;
            final var cIndex = target.index;

            switch (product) {
                case 1:
                    return segment;
                case 3:
                case -3:
                    if (previous == MIDDLE) {
                        return segment.withTarget(target.upgrade(pIndex, computeIntersection(current), false, Action.OUT));
                    } else {
                        return segment.withOrigin(origin.upgrade(cIndex, computeIntersection(previous), false, Action.IN));
                    }

                case -9:
                    return new Segment(newAction(-1, computeIntersection(previous), false, Action.IN), newAction(-1, computeIntersection(current), false, Action.OUT));

                case 2:
                case -2:
                    if (previous == MIDDLE) {
                        return segment.withTarget(target.upgrade(cIndex, cCoordinate, true, Action.CANDIDATE));
                    } else {
                        return segment.withOrigin(origin.upgrade(pIndex, pCoordinate, true, Action.IN));
                    }

                case -4:
                    return new Segment(origin.upgrade(pIndex, pCoordinate, origin.referenced, Action.IN), target.upgrade(cIndex, cCoordinate, target.referenced, Action.CANDIDATE));

                case 4:
                    if (origin.kind != Action.UNDEFINED)
                        segment = segment.withOrigin(origin.withKind(Action.UNDEFINED));
                    if (target.kind != Action.UNDEFINED)
                        segment = segment.withTarget(target.withKind(Action.UNDEFINED));

                    return segment;

                default:
                    throw new GinsuException.IllegalState(String.format("Product %d is invalid!", product));
            }
        }

        public Segment filter(Segment segment) {
            final var previous = slice.positionOf(segment.origin.coordinate);
            final var current = slice.positionOf(segment.target.coordinate);
            final var product = previous * current;

            if (product != 9 && product != 6) {
                return filter(previous, current, product, segment);
            } else {
                return null;
            }
        }

        public void first(Coordinate coordinate) {
            previous = slice.positionOf(coordinate);
            pIndex = 0;
            pCoordinate = coordinate;
        }

        private Action newAction(int index, Coordinate coordinate, boolean referenced, int kind) {
            return new Action(index, coordinate, referenced, kind);
        }

        public int positionOf(Coordinate coordinate) {
            return slice.positionOf(coordinate);
        }

        public void update() {
            previous = current;
            pIndex = cIndex;
            pCoordinate = cCoordinate;
        }
    }

    private static class Segment {
        private final Action origin;
        private final Action target;

        public Segment(Action origin, Action target) {
            this.origin = origin;
            this.target = target;
        }

        public Segment withOrigin(Action action) {
            return new Segment(action, target);
        }

        public Segment withTarget(Action action) {
            return new Segment(origin, action);
        }
    }
}