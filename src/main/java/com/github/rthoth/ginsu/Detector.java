package com.github.rthoth.ginsu;

import com.github.rthoth.ginsu.Dimension.Side;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequences;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

public class Detector {

    private final Controller controller;
    private final Event.Factory factory;
    private final boolean fill;
    private Event candidate;
    private PVector<Event> events = TreePVector.empty();
    private Event last;

    public Detector(Controller controller, Event.Factory factory, boolean fill) {
        this.controller = controller;
        this.factory = factory;
        this.fill = fill;
    }

    public static Detector create(Slice slice, Event.Factory factory, boolean fill) {
        return new Detector(new SingleController(slice), factory, fill);
    }

    public static Detection detect(Slice slice, CoordinateSequence sequence, boolean fill) {
        return detect(new Detector(new SingleController(slice), new Event.Factory(sequence), fill), sequence);
    }

    private static Detection detect(Detector detector, CoordinateSequence sequence) {
        detector.begin(sequence.getCoordinate(0));

        final var lastIndex = sequence.size() - 1;
        for (var i = 1; i < lastIndex; i++)
            detector.check(i, sequence.getCoordinate(i));

        return detector.end(lastIndex, sequence.getCoordinate(lastIndex), CoordinateSequences.isRing(sequence));
    }

    public static Detection detect(Slice x, Slice y, CoordinateSequence sequence, boolean fill) {
        return detect(new Detector(new XYController(x, y), new Event.Factory(sequence), fill), sequence);
    }

    private void add(Event event) {
        last = event;
        events = events.plus(event);
    }

    private void addCandidate(Event event) {
        if (candidate == null) {
            candidate = event;
        } else {
            throw new GinsuException.TopologyException("Already exists a candidate!");
        }
    }

    private void addCorner(Event event) {
        if (fill) {
            if (candidate != null) {
                add(candidate);
                candidate = null;
            } else if (last == null || last.index != event.index) {
                add(event);
            }
        }
    }

    private void addIn(Event event) {
        if (candidate == null) {
            add(event);
        } else {
            if (event.index >= 0) {
                if (candidate.index < event.index) {
                    add(candidate);
                    add(event);
                }
            } else {
                add(candidate);
                add(event);
            }

            candidate = null;
        }
    }

    private void addOut(Event event) {
        if (candidate == null) {
            add(event);
        } else if (candidate.index == event.index) {
            add(candidate);
            candidate = null;
        } else {
            throw new GinsuException.IllegalState("Double output!");
        }
    }

    private void apply(Info info) {
        final var event = info.createEvent(factory);
        if (info.type == Type.IN) {
            addIn(event);
        } else if (info.type == Type.OUT) {
            addOut(event);
        } else if (info.type == Type.CANDIDATE) {
            addCandidate(event);
        } else if (info.type == Type.CORNER) {
            addCorner(event);
        }

    }

    public void begin(Coordinate coordinate) {
        controller.begin(coordinate);
    }

    public void check(int index, Coordinate coordinate) {
        controller.update(index, coordinate);

        if (controller.isChanged()) {
            final var segment = controller.compute();

            if (segment.origin.type != Type.UNDEFINED)
                apply(segment.origin);

            if (segment.target.type != Type.UNDEFINED)
                apply(segment.target);
        }

        controller.next();
    }

    public Detection end(int index, Coordinate coordinate, boolean isRing) {
        check(index, coordinate);

        if (candidate != null) {
            if (isRing) {
                if (candidate.index != index || events.get(0).index != 0)
                    add(candidate);
                else
                    events = events.minus(0);
            } else {
                add(candidate);
            }
        } else if (isRing) {
            if (!events.isEmpty()) {
                var last = events.get(events.size() - 1);
                if (last.type == Event.Type.CORNER && last.index == index)
                    events = events.minus(events.size() - 1);
            }
        }


        return new Detection(factory.getSequence(), events, isRing, controller.startsInside());
    }

    enum Type {

        UNDEFINED(null), IN(Event.Type.IN), OUT(Event.Type.OUT), CANDIDATE(Event.Type.OUT), CORNER(Event.Type.CORNER);

        public final Event.Type underlying;

        Type(Event.Type underlying) {
            this.underlying = underlying;
        }
    }

    public static abstract class Controller {

        public abstract void begin(Coordinate coordinate);

        public abstract Segment compute();

        public abstract boolean isChanged();

        public abstract void next();

        public abstract boolean startsInside();

        public abstract void update(int index, Coordinate coordinate);
    }

    static final class Info {
        Type type = Type.UNDEFINED;

        int index;
        boolean isReference = true;
        Coordinate coordinate;

        Dimension dimension;
        Side xSide = Side.UNDEFINED;
        Side ySide = Side.UNDEFINED;

        int position;

        public Event createEvent(Event.Factory factory) {
            return factory.create(type.underlying, index, isReference ? null : coordinate, dimension, xSide, ySide);
        }

        void update(Slice slice) {
            position = slice.positionOf(coordinate);
        }

        void update(Type newType, int newIndex, Coordinate newCoordinate, Dimension newDimension, int border) {
            type = newType;
            index = newIndex;
            isReference = false;
            coordinate = newCoordinate;
            update(newDimension, border, true);
        }

        void update(Dimension newDimension, int border, boolean pointUpdated) {
            if (pointUpdated) {
                dimension = newDimension;
            } else {
                dimension = dimension == null ? newDimension : Dimension.CORNER;
            }
            updateSide(newDimension, border);
        }

        public void update(Type newType, Dimension newDimension, int border) {
            type = newType;
            update(newDimension, border, false);
        }

        void updateSide(Dimension newDimension, int border) {
            if (newDimension == Dimension.X)
                xSide = Slice.sideOf(border);
            else if (newDimension == Dimension.Y)
                ySide = Slice.sideOf(border);
        }
    }

    static final class Point {
        int index;
        Coordinate coordinate;
        int position;

        public void copyFrom(Point next) {
            index = next.index;
            coordinate = next.coordinate;
            position = next.position;
        }

        public Info newInfo() {
            var info = new Info();
            info.index = index;
            info.coordinate = coordinate;
            return info;
        }

        public void update(int newIndex, Coordinate newCoordinate, Slice slice) {
            index = newIndex;
            coordinate = newCoordinate;
            position = slice.positionOf(newCoordinate);
        }
    }

    static final class Segment {
        final Info origin;
        final Info target;

        Segment(Info origin, Info target) {
            this.origin = origin;
            this.target = target;
        }

        public int product() {
            return origin.position * target.position;
        }

        public void update(Slice slice) {
            origin.update(slice);
            target.update(slice);
        }

        public void update(Type type) {
            origin.type = type;
            target.type = type;
        }
    }

    public static final class SingleController extends Controller {

        private final Slice slice;

        private final Point previous = new Point();
        private final Point current = new Point();
        private int firstPosition;

        SingleController(Slice slice) {
            this.slice = slice;
        }

        public Segment apply(Segment segment) {
            segment.update(slice);
            final var product = segment.product();

            if (product != 9 && product != 1 && product != 6) {
                apply(product, segment.origin, segment.target);
            } else if (product == 9 || product == 6) {
                segment.update(Type.UNDEFINED);
            }

            return segment;
        }

        private void apply(int product, Info origin, Info target) {
            final int current = target.position, previous = origin.position;
            final int cIndex = target.index, pIndex = origin.index;
            final Coordinate cCoordinate = target.coordinate, pCoordinate = origin.coordinate;

            if (product == 3 || product == -3) {
                if (current == Slice.MIDDLE) {
                    origin.update(Type.IN, cIndex, slice.intersection(pCoordinate, cCoordinate, previous), slice.getDimension(), previous);
                } else {
                    target.update(Type.OUT, pIndex, slice.intersection(pCoordinate, cCoordinate, current), slice.getDimension(), current);
                }
            } else if (product == -9) {
                origin.update(Type.IN, -1, slice.intersection(pCoordinate, cCoordinate, previous), slice.getDimension(), previous);
                target.update(Type.OUT, -1, slice.intersection(pCoordinate, cCoordinate, current), slice.getDimension(), current);
            } else if (product == 2 || product == -2) {
                if (current == Slice.MIDDLE) {
                    origin.update(Type.IN, slice.getDimension(), previous);
                } else {
                    target.update(Type.CANDIDATE, slice.getDimension(), current);
                }
            } else if (product == -4) {
                origin.update(Type.IN, slice.getDimension(), previous);
                target.update(Type.CANDIDATE, slice.getDimension(), current);
            } else if (product == -6) {
                if (Math.abs(current) == Slice.UPPER_BORDER) {
                    origin.update(Type.IN, cIndex, slice.intersection(pCoordinate, cCoordinate, previous), slice.getDimension(), previous);
                    target.update(Type.CANDIDATE, slice.getDimension(), current);
                } else {
                    origin.update(Type.IN, slice.getDimension(), previous);
                    target.update(Type.OUT, pIndex, slice.intersection(pCoordinate, cCoordinate, current), slice.getDimension(), current);
                }
            } else if (product == 4) {
                if (origin.type != Type.UNDEFINED)
                    origin.update(Type.CORNER, slice.getDimension(), previous);
                if (target.type != Type.UNDEFINED)
                    target.update(Type.CORNER, slice.getDimension(), current);
            }
        }

        @Override
        public void begin(Coordinate coordinate) {
            previous.update(0, coordinate, slice);
            firstPosition = Math.abs(previous.position);
        }

        @Override
        public Segment compute() {
            return apply(newSegment());
        }

        @Override
        public boolean isChanged() {
            return previous.position != current.position;
        }

        public Segment newSegment() {
            return new Segment(previous.newInfo(), current.newInfo());
        }

        @Override
        public void next() {
            previous.copyFrom(current);
        }

        public int product() {
            return previous.position * current.position;
        }

        @Override
        public boolean startsInside() {
            return firstPosition == Slice.MIDDLE;
        }

        @Override
        public void update(int index, Coordinate coordinate) {
            current.update(index, coordinate, slice);
            if (firstPosition == Slice.UPPER_BORDER)
                firstPosition = Math.abs(current.position);
        }
    }

    public static final class XYController extends Controller {

        private final SingleController x;
        private final SingleController y;

        XYController(Slice x, Slice y) {
            this.x = new SingleController(x);
            this.y = new SingleController(y);
        }

        @Override
        public void begin(Coordinate coordinate) {
            x.begin(coordinate);
            y.begin(coordinate);
        }

        @Override
        public Segment compute() {
            return x.isChanged() ? y.apply(x.compute()) : x.apply(y.compute());
        }

        @Override
        public boolean isChanged() {
            return x.product() != 9 && y.product() != 9 && (x.isChanged() || y.isChanged());
        }

        @Override
        public void next() {
            x.next();
            y.next();
        }

        @Override
        public boolean startsInside() {
            return x.startsInside() && y.startsInside();
        }

        @Override
        public void update(int index, Coordinate coordinate) {
            x.update(index, coordinate);
            y.update(index, coordinate);
        }
    }
}
