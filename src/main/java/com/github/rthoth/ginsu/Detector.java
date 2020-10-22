package com.github.rthoth.ginsu;

import com.github.rthoth.ginsu.Dimension.Side;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequences;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.util.TreeMap;

public class Detector {

    private static final int OUTSIDE = -1;
    private static final int INSIDE = 1;

    private final Controller controller;

    public Detector(Controller controller) {
        this.controller = controller;
    }

    public static Detector create(Slice slice, Event.Factory factory) {
        return new Detector(new SingleController(slice, new Recorder(factory, false)));
    }

    public static Detection detect(Slice slice, CoordinateSequence sequence) {
        return detect(new Detector(new SingleController(slice, new Recorder(new Event.Factory(sequence), false))), sequence);
    }

    private static Detection detect(Detector detector, CoordinateSequence sequence) {
        detector.begin(sequence.getCoordinate(0));

        final var lastIndex = sequence.size() - 1;
        for (var i = 1; i < lastIndex; i++)
            detector.check(i, sequence.getCoordinate(i));

        return detector.end(lastIndex, sequence.getCoordinate(lastIndex), CoordinateSequences.isRing(sequence));
    }

    public static Detection detect(Slice x, Slice y, CoordinateSequence sequence, boolean hasCorner) {
        return detect(new Detector(new XYController(x, y, new Event.Factory(sequence), hasCorner)), sequence);
    }

    public void begin(Coordinate coordinate) {
        controller.begin(coordinate);
    }

    public void check(int index, Coordinate coordinate) {
        controller.update(index, coordinate);

        if (controller.isChanged()) {
            final var segment = controller.compute();

            if (segment.origin.type != Type.UNDEFINED)
                controller.apply(segment.origin);

            if (segment.target.type != Type.UNDEFINED)
                controller.apply(segment.target);
        }

        controller.next();
    }

    public Detection end(int index, Coordinate coordinate, boolean isRing) {
        check(index, coordinate);
        return controller.end(isRing);
    }

    enum Type {

        UNDEFINED(null), IN(Event.Type.IN), OUT(Event.Type.OUT), CANDIDATE(Event.Type.OUT), CORNER(Event.Type.CORNER);

        public final Event.Type underlying;

        Type(Event.Type underlying) {
            this.underlying = underlying;
        }
    }

    public static abstract class Controller {

        public abstract void apply(EventInfo eventInfo);

        public abstract void begin(Coordinate coordinate);

        public abstract Segment compute();

        public abstract Detection end(boolean isRing);

        public abstract CoordinateSequence getSequence();

        protected abstract double getValue();

        public abstract boolean isChanged();

        public abstract void next();

        public abstract boolean startsInside();

        public abstract void update(int index, Coordinate coordinate);
    }

    static class CornerInfo {

        final int position;
        final Event event;

        public CornerInfo(int size, Event event) {
            position = size % 2 == 0 ? OUTSIDE : INSIDE;
            this.event = event;
        }
    }

    static final class EventInfo {
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

        public void update(Type newType) {
            type = newType;
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

        public EventInfo newInfo() {
            var info = new EventInfo();
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

    private static class Recorder {

        final Event.Factory factory;
        final boolean hasCorner;

        PVector<Event> events = TreePVector.empty();
        Event candidate = null;
        Event last = null;

        Recorder(Event.Factory factory, boolean hasCorner) {
            this.hasCorner = hasCorner;
            this.factory = factory;
        }

        void add(Event event) {
            last = event;
            events = events.plus(event);
        }

        void addCandidate(Event event) {
            if (candidate == null) {
                candidate = event;
            } else {
                throw new GinsuException.TopologyException("Already exists a candidate!");
            }
        }

        void addCorner(Event event) {
            if (hasCorner) {
                if (candidate != null) {
                    add(candidate);
                    candidate = null;
                } else if (last == null || last.index != event.index) {
                    add(event);
                }
            }
        }

        void addIn(Event event) {
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

        void addOut(Event event) {
            if (candidate == null) {
                add(event);
            } else if (candidate.index == event.index) {
                add(candidate);
                candidate = null;
            } else {
                throw new GinsuException.IllegalState("Double output!");
            }
        }

        void apply(EventInfo eventInfo) {
            final var event = eventInfo.createEvent(factory);
            if (eventInfo.type == Type.IN) {
                addIn(event);
            } else if (eventInfo.type == Type.OUT) {
                addOut(event);
            } else if (eventInfo.type == Type.CANDIDATE) {
                addCandidate(event);
            } else if (eventInfo.type == Type.CORNER) {
                addCorner(event);
            }
        }

        PVector<Event> end(int index, boolean isRing) {
            if (candidate != null) {
                if (isRing) {
                    if (candidate.index != index || events.get(0).index != 0)
                        pushCandidate();
                    else
                        remove(0);
                } else {
                    pushCandidate();
                }
            } else if (isRing) {
                if (!events.isEmpty()) {
                    var last = events.get(events.size() - 1);
                    if (last.type == Event.Type.CORNER && last.index == index)
                        remove(events.size() - 1);
                }
            }

            return events;
        }

        void pushCandidate() {
            add(candidate);
        }

        public void remove(int index) {
            events = events.minus(index);
        }
    }

    static final class Segment {
        final EventInfo origin;
        final EventInfo target;

        Segment(EventInfo origin, EventInfo target) {
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
        private final Recorder recorder;
        private int firstPosition;

        SingleController(Slice slice, Recorder recorder) {
            this.slice = slice;
            this.recorder = recorder;
        }

        @Override
        public void apply(EventInfo eventInfo) {
            recorder.apply(eventInfo);
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

        private void apply(int product, EventInfo origin, EventInfo target) {
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
                origin.update(Type.IN, Event.NO_INDEX, slice.intersection(pCoordinate, cCoordinate, previous), slice.getDimension(), previous);
                target.update(Type.OUT, Event.NO_INDEX, slice.intersection(pCoordinate, cCoordinate, current), slice.getDimension(), current);
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
//                if (origin.type != Type.UNDEFINED)
//                    origin.update(Type.CORNER, slice.getDimension(), previous);
//                if (target.type != Type.UNDEFINED)
//                    target.update(Type.CORNER, slice.getDimension(), current);

                origin.update(Type.UNDEFINED);
                target.update(Type.UNDEFINED);
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
        public Detection end(boolean isRing) {
            return new Detection(getSequence(), recorder.end(current.index, isRing), isRing, startsInside(), Detection.EMPTY_CORNER_SET);
        }

        @Override
        public CoordinateSequence getSequence() {
            return recorder.factory.getSequence();
        }

        @Override
        protected double getValue() {
            return slice.getValue();
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
        private final boolean hasCorner;
        private final Recorder recorder;

        private SingleController xL = null;
        private SingleController xU = null;
        private SingleController yL = null;
        private SingleController yU = null;

        XYController(Slice x, Slice y, Event.Factory factory, boolean hasCorner) {
            recorder = new Recorder(factory, hasCorner);

            this.hasCorner = hasCorner;
            this.x = new SingleController(x, null);
            this.y = new SingleController(y, null);
            if (hasCorner) {
                xL = createController(x.getLower(), factory);
                xU = createController(x.getUpper(), factory);
                yL = createController(y.getLower(), factory);
                yU = createController(y.getUpper(), factory);
            }
        }

        @Override
        public void apply(EventInfo eventInfo) {
            recorder.apply(eventInfo);
        }

        @Override
        public void begin(Coordinate coordinate) {
            x.begin(coordinate);
            y.begin(coordinate);

            if (hasCorner) {
                begin(coordinate, xL);
                begin(coordinate, xU);
                begin(coordinate, yL);
                begin(coordinate, yU);
            }
        }

        private void begin(Coordinate coordinate, SingleController controller) {
            if (controller != null) controller.begin(coordinate);
        }

        @Override
        public Segment compute() {
            return x.isChanged() ? y.apply(x.compute()) : x.apply(y.compute());
        }

        SingleController createController(Slice slice, Event.Factory factory) {
            return slice != null ? new SingleController(slice, new Recorder(factory, false)) : null;
        }

        private Event createCorner(SingleController x, SingleController y, CornerInfo xIn, CornerInfo yIn, Event.Factory factory, Side xSide, Side ySide) {
            if (xIn != null && yIn != null && xIn.position == INSIDE && yIn.position == INSIDE)
                return factory.create(Event.Type.CORNER, Event.CORNER_INDEX, new Coordinate(x.getValue(), y.getValue()), Dimension.CORNER, xSide, ySide);

            return null;
        }

        private Detection.CornerSet createCornerSet(boolean isRing) {
            var xLI = populate(xL, isRing);
            var xUI = populate(xU, isRing);
            var yLI = populate(yL, isRing);
            var yUI = populate(yU, isRing);

            var ll = createCorner(xL, yL, higher(yLI, xL), higher(xLI, yL), recorder.factory, Side.GREATER, Side.GREATER);
            var ul = createCorner(xU, yL, lower(yLI, xU), higher(xUI, yL), recorder.factory, Side.LESS, Side.GREATER);
            var uu = createCorner(xU, yU, lower(yUI, xU), lower(xUI, yU), recorder.factory, Side.LESS, Side.LESS);
            var lu = createCorner(xL, yU, higher(yUI, xL), lower(xLI, yU), recorder.factory, Side.GREATER, Side.LESS);

            return Detection.CornerSet.of(ll, ul, uu, lu);
        }

        private void detect(int index, Coordinate coordinate, SingleController controller) {
            if (controller != null) {
                controller.update(index, coordinate);
                if (controller.isChanged()) {
                    var segment = controller.compute();
                    if (segment.origin.type != Type.UNDEFINED)
                        controller.apply(segment.origin);
                    if (segment.target.type != Type.UNDEFINED)
                        controller.apply(segment.target);
                }

                controller.next();
            }
        }

        @Override
        public Detection end(boolean isRing) {
            var cornerSet = hasCorner ? createCornerSet(isRing) : Detection.EMPTY_CORNER_SET;
            return new Detection(getSequence(), recorder.end(x.current.index, isRing), isRing, startsInside(), cornerSet);
        }

        @Override
        public CoordinateSequence getSequence() {
            return recorder.factory.getSequence();
        }

        @Override
        protected double getValue() {
            throw new GinsuException.Unsupported();
        }

        private CornerInfo higher(TreeMap<Double, Event> treeMap, SingleController controller) {
            return higher(treeMap, controller, false);
        }

        private CornerInfo higher(TreeMap<Double, Event> treeMap, SingleController controller, boolean inclusive) {
            if (controller != null && treeMap != null) {
                var tailMap = treeMap.tailMap(controller.slice.getValue(), inclusive);
                if (!tailMap.isEmpty())
                    return new CornerInfo(tailMap.size(), tailMap.firstEntry().getValue());
            }

            return null;
        }

        @Override
        public boolean isChanged() {
            return x.product() != 9 && y.product() != 9 && (x.isChanged() || y.isChanged());
        }

        private CornerInfo lower(TreeMap<Double, Event> treeMap, SingleController controller) {
            return lower(treeMap, controller, false);
        }

        private CornerInfo lower(TreeMap<Double, Event> treeMap, SingleController controller, boolean inclusive) {
            if (controller != null && treeMap != null) {
                var headMap = treeMap.headMap(controller.slice.getValue(), inclusive);
                if (!headMap.isEmpty())
                    return new CornerInfo(headMap.size(), headMap.lastEntry().getValue());

            }

            return null;
        }

        @Override
        public void next() {
            x.next();
            y.next();
        }

        private TreeMap<Double, Event> populate(SingleController controller, boolean isRing) {
            if (controller != null) {
                var treeMap = new TreeMap<Double, Event>();
                var dimension = controller.slice.getDimension();
                for (var event : controller.recorder.end(x.current.index, isRing)) {
                    treeMap.put(dimension.ordinateOf(event.getCoordinate()), event);
                }
                return treeMap;
            } else {
                return null;
            }
        }

        @Override
        public boolean startsInside() {
            return x.startsInside() && y.startsInside();
        }

        @Override
        public void update(int index, Coordinate coordinate) {
            x.update(index, coordinate);
            y.update(index, coordinate);

            if (hasCorner) {
                detect(index, coordinate, xL);
                detect(index, coordinate, xU);
                detect(index, coordinate, yL);
                detect(index, coordinate, yU);
            }
        }
    }
}
