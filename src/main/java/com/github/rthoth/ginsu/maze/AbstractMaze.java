package com.github.rthoth.ginsu.maze;

import com.github.rthoth.ginsu.*;
import com.github.rthoth.ginsu.Event.Side;
import com.github.rthoth.ginsu.detection.DetectionShape;
import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.geom.Coordinate;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.util.*;

public abstract class AbstractMaze<I> {

    private final double offset;
    private final PVector<Lane<I>> xLanes;
    private final PVector<Lane<I>> yLanes;
    private final TreeMap<Q, N<I>> index = new TreeMap<>(comparator());
    private final HashMap<SingleE<I>, Seq<I>> singleToSeq = new HashMap<>();


    public AbstractMaze(PVector<Knife.X> x, PVector<Knife.Y> y, double offset) {
        this.offset = offset;
        this.xLanes = Ginsu.map(x, Lane::x);
        this.yLanes = Ginsu.map(y, Lane::y);
    }

    public void add(DetectionShape shape) {
        for (var detection : shape.detections) {

            var singles = TreePVector.<SingleE<I>>empty();

            for (var event : detection.events.getVector()) {
                var n = searchOrCreateN(event.getCoordinate());

                final var singleE = new SingleE<>(event, shape, detection, this, n);
                register(singleE);
                singles = singles.plus(singleE);
            }

            if (!singles.isEmpty()) {
                var seq = new Seq<>(singles, detection.isRing);
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
                var n = searchOrCreateN(corner.getCoordinate());
                register(new SingleE<>(corner, shape, detection, this, n));
            }
        }
    }

    private void add(SingleE<I> e, N<I> n, PVector<Lane<I>> lanes) {
        for (var lane : lanes) {
            if (lane.positionOf(n.q) == 0) {
                lane.add(n);
                n.add(lane);
                return;
            }
        }

        throw new GinsuException.IllegalState("The event should be added: " + e.event);
    }

    private E<I> checkAndApply(E<I> old, SingleE<I> singleE, Dimension otherDimension) {
        if (old == null)
            return singleE;
        else if (old.isSingle())
            return newDoubleE((SingleE<I>) old, singleE, otherDimension);
        else
            throw new GinsuException.TopologyException("There are more than 2 events near to: " + singleE.event);
    }

    private Comparator<Q> comparator() {
        return (q1, q2) -> {
            var c = Ginsu.compare(q1.coordinate.getX(), offset, q2.coordinate.getX());
            return c != 0 ? c : Ginsu.compare(q1.coordinate.getY(), offset, q2.coordinate.getY());
        };
    }

    public void init(I initial, Lane.Visitor<I> visitor) {
        for (var lanes : Arrays.asList(xLanes, yLanes)) {
            for (var lane : lanes) {
                lane.init(initial, visitor);
            }
        }
    }

    public Iterable<N> iterable() {
        return Collections.unmodifiableCollection(index.values());
    }

    private DoubleE<I> newDoubleE(SingleE<I> _1, SingleE<I> _2, Dimension other) {
        var _1s = _1.event.getSide(other);
        var _2s = _2.event.getSide(other);

        if (_1s != _2s && _1s != Side.UNDEFINED && _2s != Side.UNDEFINED) {
            if (_1s == Side.LESS) {
                return new DoubleE<>(_1, _2);
            } else {
                return new DoubleE<>(_2, _1);
            }
        } else {
            var p2 = _1.detection.getNextInsideCoordinate(_1.event);
            var p1 = _1.event.getCoordinate();
            var q = _2.detection.getNextInsideCoordinate(_2.event);
            var dimension = other.complement();
            var orientation = Orientation.index(p1, p2, q) * dimension.value;

            if (orientation != Orientation.COLLINEAR) {

                _1s = _1.event.getSide(dimension);
                if (_1s == Side.LESS) {
                    return orientation == Orientation.CLOCKWISE ? new DoubleE<>(_1, _2) : new DoubleE<>(_2, _1);
                } else {
                    return orientation == Orientation.COUNTERCLOCKWISE ? new DoubleE<>(_1, _2) : new DoubleE<>(_2, _1);
                }
            } else {
                return null;
            }
        }
    }

    public SingleE<I> next(SingleE<I> e) {
        return singleToSeq.get(e).next(e).orElse(null);
    }

    public SingleE<I> previous(SingleE<I> e) {
        return singleToSeq.get(e).previous(e).orElse(null);
    }

    private void register(SingleE<I> e) {
        var event = e.event;
        var n = e.n;

        if (event.dimension == Dimension.X || event.dimension == Dimension.CORNER) {

            if (event.xSide == Side.LESS)
                n.setXL(checkAndApply(n.getXL(), e, Dimension.Y));
            else if (event.xSide == Side.GREAT)
                n.setXG(checkAndApply(n.getXG(), e, Dimension.Y));
            else
                throw new GinsuException.TopologyException("Invalid xSide: " + event);

            add(e, n, xLanes);
        }

        if (event.dimension == Dimension.Y || event.dimension == Dimension.CORNER) {

            if (event.ySide == Side.LESS)
                n.setYL(checkAndApply(n.getYL(), e, Dimension.X));
            else if (event.ySide == Side.GREAT)
                n.setYG(checkAndApply(n.getYG(), e, Dimension.X));
            else
                throw new GinsuException.TopologyException("Invalid ySide: " + event);

            add(e, n, yLanes);
        }
    }

    private N<I> searchOrCreateN(Coordinate coordinate) {
        var q = new Q(coordinate);
        var n = index.get(q);

        if (n == null) {
            n = new N<>(q);
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

    public Iterable<N<I>> unvisited() {

        return () -> new Iterator<>() {

            private final Iterator<N<I>> underlying = index.values().iterator();
            private N<I> next;
            private boolean _hasNext = false;

            @Override
            public boolean hasNext() {
                while (underlying.hasNext()) {
                    next = underlying.next();
                    if (next.nonVisited()) {
                        return _hasNext = true;
                    }
                }

                return _hasNext = false;
            }

            @Override
            public N<I> next() {
                if (_hasNext) {
                    _hasNext = false;
                    return next;
                } else {
                    throw new GinsuException.IllegalState("There is no element!");
                }
            }
        };
    }


    public static class Seq<I> extends AbstractSeq<SingleE<I>> {

        private Seq(PVector<SingleE<I>> vector, boolean isClosed) {
            super(vector, isClosed);
        }
    }


}
