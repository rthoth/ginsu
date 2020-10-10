package com.github.rthoth.ginsu;

import com.github.rthoth.ginsu.Dimension.Side;
import org.locationtech.jts.geom.Coordinate;
import org.pcollections.HashTreePSet;
import org.pcollections.PSet;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.util.*;
import java.util.function.*;

public abstract class AbstractMaze<I> {

    private final NHandler XHandler = new NHandler() {
        @Override
        I greater(N n) {
            return n.xGI;
        }

        @Override
        E greaterGetter(N n) {
            return n.xG;
        }

        @Override
        void greaterSetter(N n, I info) {
            n.xGI = info;
        }

        @Override
        I less(N n) {
            return n.xLI;
        }

        @Override
        E lessGetter(N n) {
            return n.xL;
        }

        @Override
        void lessSetter(N n, I info) {
            n.xLI = info;
        }
    };

    private final NHandler YHandler = new NHandler() {
        @Override
        I greater(N n) {
            return n.yGI;
        }

        @Override
        E greaterGetter(N n) {
            return n.yG;
        }

        @Override
        void greaterSetter(N n, I info) {
            n.yGI = info;
        }

        @Override
        I less(N n) {
            return n.yLI;
        }

        @Override
        E lessGetter(N n) {
            return n.yL;
        }

        @Override
        void lessSetter(N n, I info) {
            n.yLI = info;
        }
    };

    private final double offset;
    private final PVector<Lane> xLanes;
    private final PVector<Lane> yLanes;
    private final TreeMap<Q, N> index = new TreeMap<>(comparator());
    private final HashMap<SingleE, Seq> singleToSeq = new HashMap<>();

    public AbstractMaze(PVector<Knife.X> x, PVector<Knife.Y> y, double offset) {
        this.offset = offset;
        this.xLanes = Ginsu.map(x, Lane::new);
        this.yLanes = Ginsu.map(y, Lane::new);
    }

    public void add(DetectionShape shape) {
        for (var detection : shape.getDetections()) {

            var singles = TreePVector.<SingleE>empty();

            for (var event : detection.events.getVector()) {
                var q = new Q(event.getCoordinate());
                var n = index.get(q);

                if (n == null) {
                    n = new N(q);
                    index.put(q, n);
                }

                final var e = new SingleE(event, shape, detection, n);
                singles = singles.plus(e);

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

            if (!singles.isEmpty()) {
                var seq = new Seq(singles, detection.isRing);
                for (var single : singles) {
                    singleToSeq.put(single, seq);
                }
            }
        }
    }

    public Initializer initialize(I initial) {
        return new Initializer(initial);
    }

    public Iterable<N> iterable() {
        return Collections.unmodifiableCollection(index.values());
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
            return new DoubleE((SingleE) old, singleE, otherDimension);
        else
            throw new GinsuException.TopologyException("There are more than 2 events near to: " + singleE.event);
    }

    private Comparator<Q> comparator() {
        return (q1, q2) -> {
            var c = Ginsu.compare(q1.coordinate.getX(), offset, q2.coordinate.getX());
            return c != 0 ? c : Ginsu.compare(q1.coordinate.getY(), offset, q2.coordinate.getY());
        };
    }

    public interface InitialFunc<A> {

        A apply(A current, AbstractMaze<A>.E e, boolean hasNext);
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

        protected DoubleE(SingleE _1, SingleE _2, Dimension otherDimension) {
            var _1s = otherDimension.sideOf(_1.event);
            var _2s = otherDimension.sideOf(_2.event);

            if (_1s != _2s && _1s != Side.UNDEFINED && _2s != Side.UNDEFINED) {
                if (_1s == Side.LESS) {
                    less = _1;
                    greater = _2;
                } else {
                    less = _2;
                    greater = _1;
                }
            } else {
                throw new GinsuException.TopologyException("Invalid joining point near to: " + _1.event.getCoordinate());
            }
        }

        @Override
        public void consume(Consumer<SingleE> consumer) {
            consumer.accept(less);
            consumer.accept(greater);
        }

        @Override
        public PSet<SingleE> filter(PSet<SingleE> set, Predicate<Event> predicate) {
            return greater.filter(less.filter(set, predicate), predicate);
        }

        @Override
        public boolean forall(Predicate<Event> predicate) {
            return less.forall(predicate) && greater.forall(predicate);
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
    }

    public abstract class E {

        public abstract void consume(Consumer<SingleE> consumer);

        public abstract PSet<SingleE> filter(PSet<SingleE> set, Predicate<Event> predicate);

        public abstract boolean forall(Predicate<Event> predicate);

        public abstract boolean isDouble();

        public abstract boolean isSingle();
    }

    public class Initializer {

        private final I initial;

        public Initializer(I initial) {
            this.initial = initial;
        }

        public void forEach(InitialFunc<I> func) {
            for (var lanes : Arrays.asList(xLanes, yLanes)) {
                for (var lane : lanes)
                    lane.initialize(initial, func);
            }
        }
    }

    public class Lane {

        private final Knife<?> knife;
        private final TreeMap<Q, N> index = new TreeMap<>(comparator());

        Lane(Knife<?> knife) {
            this.knife = knife;
        }

        public Dimension getDimension() {
            return knife.dimension;
        }

        Comparator<Q> comparator() {
            return (q1, q2) -> Ginsu.compare(knife.ordinateOf(q1.coordinate), offset, knife.ordinateOf(q2.coordinate));
        }

        PSet<N> filterNeighbour(Q q, PSet<N> set, E lower, E greater, BiPredicate<N, E> predicate) {
            Map.Entry<Q, N> entry;

            entry = index.lowerEntry(q);
            if (entry != null && predicate.test(entry.getValue(), lower))
                set = set.plus(entry.getValue());

            entry = index.higherEntry(q);
            if (entry != null && predicate.test(entry.getValue(), greater))
                set = set.plus(entry.getValue());

            return set;
        }

        private void initialize(I initial, InitialFunc<I> func, NHandler handler) {
            initialize(initial, func, handler::lessGetter, handler::lessSetter);
            initialize(initial, func, handler::greaterGetter, handler::greaterSetter);

            var less = initial;
            var greater = initial;

            for (var n : index.values()) {
                if (handler.greaterGetter(n) == null)
                    handler.greaterSetter(n, greater);

                if (handler.lessGetter(n) == null)
                    handler.lessSetter(n, less);

                less = handler.less(n);
                greater = handler.greater(n);
            }
        }

        private void initialize(I initial, InitialFunc<I> func, Function<N, E> getter, BiConsumer<N, I> setter) {
            var vector = Ginsu.filter(index.values(), n -> getter.apply(n) != null);
            if (!vector.isEmpty()) {
                var current = initial;
                var iterator = vector.iterator();
                while (iterator.hasNext()) {
                    var n = iterator.next();
                    current = func.apply(current, getter.apply(n), iterator.hasNext());
                    setter.accept(n, current);
                }
            }
        }

        void initialize(I initial, InitialFunc<I> func) {
            if (knife.dimension == Dimension.X) {
                initialize(initial, func, XHandler);
            } else {
                initialize(initial, func, YHandler);
            }
        }
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

        public PSet<SingleE> filter(Predicate<Event> predicate) {
            PSet<SingleE> set = HashTreePSet.empty();

            set = filter(xL, set, predicate);
            set = filter(xG, set, predicate);
            set = filter(yL, set, predicate);
            set = filter(yG, set, predicate);

            return set;
        }

        public PSet<N> filterNeighbour(BiPredicate<N, E> predicate) {
            PSet<N> set = HashTreePSet.empty();
            for (var lane : lanes) {
                if (lane.knife.dimension == Dimension.X)
                    set = lane.filterNeighbour(q, set, yL, yG, predicate);
                else if (lane.knife.dimension == Dimension.Y)
                    set = lane.filterNeighbour(q, set, xL, xG, predicate);
            }

            return set;
        }

        public void forEachSingleE(Consumer<SingleE> consumer) {
            consume(xL, consumer);
            consume(xG, consumer);
            consume(yL, consumer);
            consume(yG, consumer);
        }

        public Coordinate getCoordinate() {
            return q.coordinate;
        }

        public PSet<I> getISet() {
            return Ginsu.plus(HashTreePSet.empty(), xLI, xGI, yLI, yGI);
        }

        public Optional<Lane> getLane(Dimension dimension) {
            return Ginsu.first(lanes, lane -> lane.getDimension() == dimension);
        }

        public boolean isUnvisited() {
            return unvisited;
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

        @Override
        public String toString() {
            return "N(" + q + ")";
        }

        public void visited() {
            unvisited = false;
        }

        private void consume(E e, Consumer<SingleE> consumer) {
            if (e != null)
                e.consume(consumer);
        }

        private PSet<SingleE> filter(E e, PSet<SingleE> set, Predicate<Event> predicate) {
            return e == null ? set : e.filter(set, predicate);
        }
    }

    private abstract class NHandler {

        abstract I greater(N n);

        abstract E greaterGetter(N n);

        abstract void greaterSetter(N n, I info);

        abstract I less(N n);

        abstract E lessGetter(N n);

        abstract void lessSetter(N n, I info);
    }

    public class Seq extends AbstractSeq<SingleE> {

        private Seq(PVector<SingleE> vector, boolean isClosed) {
            super(vector, isClosed);
        }
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
        public void consume(Consumer<SingleE> consumer) {
            consumer.accept(this);
        }

        @Override
        public PSet<SingleE> filter(PSet<SingleE> set, Predicate<Event> predicate) {
            return predicate.test(event) ? set.plus(this) : set;
        }

        @Override
        public boolean forall(Predicate<Event> predicate) {
            return predicate.test(event);
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
    }
}
