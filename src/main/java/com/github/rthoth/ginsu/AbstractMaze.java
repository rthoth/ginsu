package com.github.rthoth.ginsu;

import com.github.rthoth.ginsu.Dimension.Side;
import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.geom.Coordinate;
import org.pcollections.HashTreePSet;
import org.pcollections.PSet;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class AbstractMaze<I> {

    private final double offset;
    private final PVector<Lane> xLanes;
    private final PVector<Lane> yLanes;
    private final TreeMap<Q, N> index = new TreeMap<>(comparator());
    private final HashMap<SingleE, Seq> singleToSeq = new HashMap<>();

    private final SideHandler xGreaterHandler = new SideHandler() {

        @Override
        public E getE(N n) {
            return n.xG;
        }

        @Override
        public I getInfo(N n) {
            return n.xGI;
        }

        @Override
        public void setInfo(N n, I info) {
            n.xGI = info;
        }
    };
    private final SideHandler xLessHandler = new SideHandler() {
        @Override
        public E getE(N n) {
            return n.xL;
        }

        @Override
        public I getInfo(N n) {
            return n.xLI;
        }

        @Override
        public void setInfo(N n, I info) {
            n.xLI = info;
        }
    };
    private final LaneController xController = new LaneController() {

        @Override
        public SideHandler getGreaterHandler() {
            return xGreaterHandler;
        }

        @Override
        public SideHandler getLessHandler() {
            return xLessHandler;
        }
    };
    private final SideHandler yLessHandler = new SideHandler() {
        @Override
        public E getE(N n) {
            return n.yL;
        }

        @Override
        public I getInfo(N n) {
            return n.yLI;
        }

        @Override
        public void setInfo(N n, I info) {
            n.yLI = info;
        }
    };

    private final SideHandler yGreaterHandler = new SideHandler() {
        @Override
        public E getE(N n) {
            return n.yG;
        }

        @Override
        public I getInfo(N n) {
            return n.yGI;
        }

        @Override
        public void setInfo(N n, I info) {
            n.yGI = info;
        }
    };

    private final LaneController yController = new LaneController() {

        @Override
        public SideHandler getGreaterHandler() {
            return yGreaterHandler;
        }

        @Override
        public SideHandler getLessHandler() {
            return yLessHandler;
        }
    };

    public AbstractMaze(PVector<Knife.X> x, PVector<Knife.Y> y, double offset) {
        this.offset = offset;
        this.xLanes = Ginsu.map(x, Lane::new);
        this.yLanes = Ginsu.map(y, Lane::new);
    }

    public void add(DetectionShape shape) {
        for (var detection : shape.detections) {

            var singles = TreePVector.<SingleE>empty();

            for (var event : detection.events.getVector()) {
                var n = searchN(event.getCoordinate());

                final var singleE = new SingleE(event, shape, detection, n);
                register(singleE, n);
                singles = singles.plus(singleE);
            }

            if (!singles.isEmpty()) {
                var seq = new Seq(singles, detection.isRing);
                for (var single : singles) {
                    singleToSeq.put(single, seq);
                }
            }
        }

        var iterator = shape.detections.iterator();
        var detection = Ginsu.next(iterator);
        var cornerSet = detection.cornerSet;

        while (iterator.hasNext()) {
            var set = iterator.next().cornerSet;
            if (set.ll != null) cornerSet = cornerSet.withLL(null);
            if (set.ul != null) cornerSet = cornerSet.withUL(null);
            if (set.uu != null) cornerSet = cornerSet.withUU(null);
            if (set.lu != null) cornerSet = cornerSet.withLU(null);
        }

        for (var corner : cornerSet.iterable()) {
            if (corner != null) {
                var n = searchN(corner.getCoordinate());
                register(new SingleE(corner, shape, detection, n), n);
            }
        }
    }

    private void add(SingleE e, N n, PVector<Lane> lanes) {
        for (var lane : lanes) {
            if (lane.knife.positionOf(n.q.coordinate) == 0) {
                lane.index.put(n.q, n);
                n.addLane(lane);
                return;
            }
        }

        throw new GinsuException.IllegalState("The event should be added: " + e.event);
    }

    private E checkAndApply(E old, SingleE singleE, Dimension otherDimension) {
        if (old == null)
            return singleE;
        else if (old.isSingle())
            return newDoubleE((SingleE) old, singleE, otherDimension);
        else
            throw new GinsuException.TopologyException("There are more than 2 events near to: " + singleE.event);
    }

    private Comparator<Q> comparator() {
        return (q1, q2) -> {
            var c = Ginsu.compare(q1.coordinate.getX(), offset, q2.coordinate.getX());
            return c != 0 ? c : Ginsu.compare(q1.coordinate.getY(), offset, q2.coordinate.getY());
        };
    }

    public void init(I initial, InitVisitor<I> visitor) {
        for (var lanes : Arrays.asList(xLanes, yLanes)) {
            for (var lane : lanes) {
                lane.init(initial, visitor);
            }
        }
    }

    public Iterable<N> iterable() {
        return Collections.unmodifiableCollection(index.values());
    }

    private DoubleE newDoubleE(SingleE _1, SingleE _2, Dimension other) {
        var _1s = other.sideOf(_1.event);
        var _2s = other.sideOf(_2.event);

        if (_1s != _2s && _1s != Side.UNDEFINED && _2s != Side.UNDEFINED) {
            if (_1s == Side.LESS) {
                return new DoubleE(_1, _2);
            } else {
                return new DoubleE(_2, _1);
            }
        } else {
            var p2 = _1.detection.getNextInsideCoordinate(_1.event);
            var p1 = _1.event.getCoordinate();
            var q = _2.detection.getNextInsideCoordinate(_2.event);
            var dimension = other.other();
            var orientation = Orientation.index(p1, p2, q) * dimension.value;

            if (orientation == Orientation.COLLINEAR)
                throw new GinsuException.TopologyException("It is impossible merge events close to: " + p1);

            _1s = dimension.sideOf(_1.event);
            if (_1s == Side.LESS) {
                return orientation == Orientation.CLOCKWISE ? new DoubleE(_1, _2) : new DoubleE(_2, _1);
            } else {
                return orientation == Orientation.COUNTERCLOCKWISE ? new DoubleE(_1, _2) : new DoubleE(_2, _1);
            }
        }
    }

    private void register(SingleE e, N n) {
        var event = e.event;

        if (event.dimension == Dimension.X || event.dimension == Dimension.CORNER) {

            if (event.xSide == Side.LESS)
                n.xL = checkAndApply(n.xL, e, Dimension.Y);
            else if (event.xSide == Side.GREATER)
                n.xG = checkAndApply(n.xG, e, Dimension.Y);
            else
                throw new GinsuException.TopologyException("Invalid xSide: " + event);

            add(e, n, xLanes);
        }

        if (event.dimension == Dimension.Y || event.dimension == Dimension.CORNER) {

            if (event.ySide == Side.LESS)
                n.yL = checkAndApply(n.yL, e, Dimension.X);
            else if (event.ySide == Side.GREATER)
                n.yG = checkAndApply(n.yG, e, Dimension.X);
            else
                throw new GinsuException.TopologyException("Invalid ySide: " + event);

            add(e, n, yLanes);
        }
    }

    private N searchN(Coordinate coordinate) {
        var q = new Q(coordinate);
        var n = index.get(q);

        if (n == null) {
            n = new N(q);
            index.put(q, n);
        }

        return n;
    }

    @SuppressWarnings("unused")
    public String toWKT() {
        var iterator = index.values().iterator();
        var builder = new StringBuilder();
        builder.append("MULTIPOINT M(");

        while (iterator.hasNext()) {
            var n = iterator.next();
            builder
                    .append("(")
                    .append(n.q.coordinate.getX())
                    .append(" ")
                    .append(n.q.coordinate.getY())
                    .append(" ")
                    .append(n.size())
                    .append(")");

            if (iterator.hasNext())
                builder.append(", ");
        }

        return builder.append(")").toString();
    }

    public Iterable<N> unvisited() {

        return () -> new Iterator<>() {

            private final Iterator<N> underlying = index.values().iterator();
            private N next;
            private boolean _hasNext = false;

            @Override
            public boolean hasNext() {
                while (underlying.hasNext()) {
                    next = underlying.next();
                    if (next.unvisited) {
                        return _hasNext = true;
                    }
                }

                return _hasNext = false;
            }

            @Override
            public N next() {
                if (_hasNext) {
                    _hasNext = false;
                    return next;
                } else {
                    throw new GinsuException.IllegalState("There is no element!");
                }
            }
        };
    }

    interface InitVisitor<I> {

        I apply(I current, AbstractMaze<I>.E e, boolean hasMore);
    }

    public interface LaneMapper<I, T> {

        T apply(I li, I gi, AbstractMaze<I>.E le, AbstractMaze<I>.E he, AbstractMaze<I>.N ln, AbstractMaze<I>.N hn);
    }

    public interface NVisitor<T, I> {
        T visit(T value, I less, I greater, AbstractMaze<I>.N lower, AbstractMaze<I>.N higher);
    }

    public static class Q {

        private final Coordinate coordinate;

        public Q(Coordinate coordinate) {
            this.coordinate = coordinate;
        }

        @Override
        public String toString() {
            return "Q(" + coordinate.toString() + ")";
        }
    }

    public class DoubleE extends E {

        private final SingleE less;
        private final SingleE greater;

        protected DoubleE(SingleE less, SingleE greater) {
            this.less = less;
            this.greater = greater;
        }

        @Override
        protected PSet<SingleE> consume(Consumer<SingleE> consumer, PSet<SingleE> consumed) {
            return greater.consume(consumer, less.consume(consumer, consumed));
        }

        @Override
        protected PSet<SingleE> filterEvent(I info, BiPredicate<Event, I> predicate, PSet<SingleE> set) {
            return greater.filterEvent(info, predicate, less.filterEvent(info, predicate, set));
        }

        @Override
        public boolean forall(I info, BiPredicate<Event, I> predicate) {
            return less.forall(info, predicate) && greater.forall(info, predicate);
        }

        @Override
        public boolean isDouble() {
            return true;
        }

        @Override
        public boolean isSingle() {
            return false;
        }

        @Override
        public String toString() {
            return "Double(less=" + less + ", greater=" + greater + ")";
        }

        @Override
        public boolean xor(Predicate<Event> predicate) {
            return Boolean.logicalXor(predicate.test(less.event), predicate.test(greater.event));
        }
    }

    public abstract class E {

        protected abstract PSet<SingleE> consume(Consumer<SingleE> consumer, PSet<SingleE> consumed);

        protected abstract PSet<SingleE> filterEvent(I info, BiPredicate<Event, I> predicate, PSet<SingleE> set);

        protected abstract boolean forall(I info, BiPredicate<Event, I> predicate);

        protected abstract boolean isDouble();

        protected abstract boolean isSingle();

        protected abstract boolean xor(Predicate<Event> predicate);
    }

    public class Lane {

        private final Knife<?> knife;
        private final TreeMap<Q, N> index = new TreeMap<>(comparator());
        private final LaneController controller;

        Lane(Knife<?> knife) {
            this.knife = knife;
            controller = knife.dimension == Dimension.X ? xController : yController;
        }

        Comparator<Q> comparator() {
            return (q1, q2) -> Ginsu.compare(knife.ordinateOf(q1.coordinate), offset, knife.ordinateOf(q2.coordinate));
        }

        public Dimension getDimension() {
            return knife.dimension;
        }

        public void init(I initial, InitVisitor<I> visitor) {
            var lessHandler = controller.getLessHandler();
            var greaterHandler = controller.getGreaterHandler();
            init(initial, visitor, lessHandler);
            init(initial, visitor, greaterHandler);

            var lessI = initial;
            var greaterI = initial;

            for (var n : index.values()) {
                if (lessHandler.getE(n) == null)
                    lessHandler.setInfo(n, visitor.apply(lessI, null, true));

                if (greaterHandler.getE(n) == null)
                    greaterHandler.setInfo(n, visitor.apply(greaterI, null, true));

                lessI = lessHandler.getInfo(n);
                greaterI = greaterHandler.getInfo(n);
            }
        }

        private void init(I initial, InitVisitor<I> visitor, SideHandler handler) {
            var vector = Ginsu.filter(index.values(), n -> handler.getE(n) != null);

            if (!vector.isEmpty()) {
                var current = initial;
                var iterator = vector.iterator();

                while (iterator.hasNext()) {
                    var n = iterator.next();
                    current = visitor.apply(current, handler.getE(n), iterator.hasNext());
                    handler.setInfo(n, current);
                }
            }
        }

        public <T> T map(N n, LaneMapper<I, T> mapper) {
            var less = controller.getLessHandler().getInfo(n);
            var greater = controller.getGreaterHandler().getInfo(n);
            var le = controller.getLessHandler().getE(n);
            var he = controller.getGreaterHandler().getE(n);
            var ln = Ginsu.getValue(index.lowerEntry(n.q));
            var hn = Ginsu.getValue(index.higherEntry(n.q));

            return mapper.apply(less, greater, le, he, ln, hn);
        }

        private <T> T visit(N n, T value, NVisitor<T, I> visitor) {
            var lessHandler = controller.getLessHandler();
            var greaterHandler = controller.getGreaterHandler();
            var lessI = lessHandler.getInfo(n);
            var greaterI = greaterHandler.getInfo(n);
            var lower = Ginsu.getValue(index.lowerEntry(n.q));
            var higher = Ginsu.getValue(index.higherEntry(n.q));

            return visitor.visit(value, lessI, greaterI, lower, higher);
        }
    }

    private abstract class LaneController {

        public abstract SideHandler getGreaterHandler();

        public abstract SideHandler getLessHandler();
    }

    public class N {

        private final Q q;
        private PSet<Lane> lanes = HashTreePSet.empty();

        private boolean unvisited = true;
        private E xL;
        private E xG;
        private E yL;
        private E yG;

        private I xLI;
        private I xGI;
        private I yLI;
        private I yGI;

        public N(Q q) {
            this.q = q;
        }

        public void addLane(Lane lane) {
            lanes = lanes.plus(lane);
        }

        private PSet<SingleE> consume(E e, Consumer<SingleE> consumer, PSet<SingleE> consumed) {
            return e != null ? e.consume(consumer, consumed) : consumed;
        }

        public boolean exist(BiPredicate<Event, I> predicate) {
            return t(xL, xLI, predicate, false) || t(xG, xGI, predicate, false) || t(yL, yLI, predicate, false) || t(yG, yGI, predicate, false);
        }

        public PSet<SingleE> filterEvent(BiPredicate<Event, I> predicate) {
            PSet<SingleE> set = HashTreePSet.empty();
            set = filterEvent(xL, xLI, predicate, set);
            set = filterEvent(xG, xGI, predicate, set);
            set = filterEvent(yL, yLI, predicate, set);
            set = filterEvent(yG, yGI, predicate, set);

            return set;
        }

        private PSet<SingleE> filterEvent(E e, I info, BiPredicate<Event, I> predicate, PSet<SingleE> set) {
            return e != null ? e.filterEvent(info, predicate, set) : set;
        }

        public void forEachSingle(Consumer<SingleE> consumer) {
            var consumed = consume(xL, consumer, HashTreePSet.empty());
            consumed = consume(yG, consumer, consumed);
            consumed = consume(yL, consumer, consumed);
            consume(yG, consumer, consumed);
        }

        public Coordinate getCoordinate() {
            return q.coordinate;
        }

        public <T> PVector<T> map(LaneMapper<I, T> mapper) {
            var vector = TreePVector.<T>empty();
            for (var lane : lanes) {
                var result = lane.map(this, mapper);
                if (result != null)
                    vector = vector.plus(result);
            }

            return vector;
        }

        public int size() {
            var size = 0;
            if (xL != null)
                size++;
            if (xG != null)
                size++;
            if (yL != null)
                size++;
            if (yG != null)
                size++;

            return size;
        }

        @SuppressWarnings("SameParameterValue")
        private boolean t(E e, I info, BiPredicate<Event, I> predicate, boolean other) {
            return e != null ? e.forall(info, predicate) : other;
        }

        @Override
        public String toString() {
            return "N(" + q + ")";
        }

        public <T> T visit(T initial, NVisitor<T, I> visitor) {
            var value = initial;
            for (var lane : lanes) {
                value = lane.visit(this, value, visitor);
            }

            return value;
        }

        public void visited() {
            if (!unvisited)
                throw new GinsuException.TopologyException("N has already visited close to: " + getCoordinate());
            unvisited = false;
        }
    }

    public class Seq extends AbstractSeq<SingleE> {

        private Seq(PVector<SingleE> vector, boolean isClosed) {
            super(vector, isClosed);
        }
    }

    private abstract class SideHandler {

        public abstract E getE(N n);

        public abstract I getInfo(N n);

        public abstract void setInfo(N n, I info);
    }

    public class SingleE extends E {

        final Event event;
        final DetectionShape shape;
        final Detection detection;
        private final N n;

        protected SingleE(Event event, DetectionShape shape, Detection detection, N n) {
            this.event = event;
            this.shape = shape;
            this.detection = detection;
            this.n = n;
        }

        @Override
        protected PSet<SingleE> consume(Consumer<SingleE> consumer, PSet<SingleE> consumed) {
            if (!consumed.contains(this)) {
                consumer.accept(this);
                return consumed.plus(this);
            } else {
                return consumed;
            }
        }

        @Override
        protected PSet<SingleE> filterEvent(I info, BiPredicate<Event, I> predicate, PSet<SingleE> set) {
            return !set.contains(this) && predicate.test(event, info) ? set.plus(this) : set;
        }

        @Override
        public boolean forall(I info, BiPredicate<Event, I> predicate) {
            return predicate.test(event, info);
        }

        public Dimension getDimension() {
            return event.dimension;
        }

        public N getN() {
            return n;
        }

        @Override
        public boolean isDouble() {
            return false;
        }

        @Override
        public boolean isSingle() {
            return true;
        }

        public SingleE next() {
            return singleToSeq.get(this).next(this).orElse(null);
        }

        public SingleE previous() {
            return singleToSeq.get(this).previous(this).orElse(null);
        }

        @Override
        public String toString() {
            return "Single(" + event + ")";
        }

        @Override
        public boolean xor(Predicate<Event> predicate) {
            throw new GinsuException.Unsupported();
        }
    }
}
