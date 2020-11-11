package com.github.rthoth.ginsu;

import com.github.rthoth.ginsu.detection.DetectionShape;
import com.github.rthoth.ginsu.maze.AbstractMaze;
import com.github.rthoth.ginsu.maze.N;
import com.github.rthoth.ginsu.maze.SingleE;
import org.locationtech.jts.geom.*;
import org.pcollections.*;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class PolygonMerger extends GeometryMerger<MultiPolygon> {

    private static final int NO_DIRECTION = Integer.MAX_VALUE;
    private static final int DOWN = -1;
    private static final int UP = 1;
    private static final int BOTH = 0;

    private final GeometryFactory factory;
    private final double offset;

    public PolygonMerger(GeometryFactory factory, double offset) {
        this.factory = factory;
        this.offset = offset;
    }

    private static PSet<N<Info>> addCandidates(PSet<N<Info>> set, int direction, N<Info> lower, N<Info> higher, N<Info> previous) {
        if ((direction == UP || direction == BOTH) && higher != null && higher != previous)
            set = set.plus(higher);

        if ((direction == DOWN || direction == BOTH) && lower != null && lower != previous)
            set = set.plus(lower);

        return set;
    }

    private static int computeDirection(Info less, Info greater) {
        switch (less.value * greater.value) {
            case 2:
            case 15:
                return UP;
            case 5:
            case 6:
                return DOWN;
            case 1:
            case 4:
            case 9:
            case 10:
            case 25:
                return NO_DIRECTION;
            case 3:
                return BOTH;
            default:
                throw new GinsuException.Unsupported("Info product: " + (less.value * greater.value));
        }
    }

    @Override
    public MultiPolygon apply(PCollection<DetectionShape> shapes, PVector<Knife.X> x, PVector<Knife.Y> y) {
        return new Merger(shapes, x, y).result;
    }

    @Override
    public boolean isPolygon() {
        return true;
    }

    private enum Info {
        O(1),
        B(2),
        I(3),
        E(5);

        public final int value;

        Info(int value) {
            this.value = value;
        }

        public Info change() {
            if (this == B || this == I)
                return E;

            if (this == E || this == O)
                return B;

            throw new GinsuException.IllegalState();
        }

        public Info keep() {
            return this == B || this == I ? I : O;
        }
    }

    private static class Maze extends AbstractMaze<Info> {

        Maze(PVector<Knife.X> x, PVector<Knife.Y> y, double offset) {
            super(x, y, offset);
        }
    }

    private class Merger {

        MultiPolygon result;

        Maze maze;
        HashMap<DetectionShape, ProtoPolygon> shapeToProtoPolygon = new HashMap<>();

        PVector<ProtoPolygon> prototypes = TreePVector.empty();

        Merger(PCollection<DetectionShape> shapes, PVector<Knife.X> x, PVector<Knife.Y> y) {
            maze = new Maze(x, y, offset);

            var polygons = TreePVector.<Polygon>empty();

            for (var shape : shapes) {
                if (shape.nonEmpty())
                    maze.add(shape);
                else if (Ginsu.first(shape.detections).startsInside)
                    polygons = polygons.plus(shape.source.toPolygon(factory));
            }

            maze.init(Info.O, (current, e, hasMore) -> {
                if (e != null) {
                    if (hasMore) {
                        if (e.isSingle())
                            return current.change();
                        else if (e.xor(Event::isNonCorner))
                            return current.change();
                        else
                            return current.keep();
                    } else {
                        return Info.E;
                    }
                } else {
                    if (current == Info.O || current == Info.E)
                        return Info.O;
                    else if (current == Info.B || current == Info.I)
                        return Info.I;

                    throw new GinsuException.IllegalState();
                }
            });

            for (var n : maze.unvisited()) {
                var candidates = n.filterEvent(this::filterOrigin);
                if (candidates.size() == 1) {
                    searchProtoPolygon(Ginsu.first(candidates), n);
                } else if (candidates.size() == 2) {
                    searchProtoPolygon(choiceOneFlow(candidates), n);
                } else if (n.exist((event, info) -> info == Info.B)) {
                    searchProtoPolygon(null, n);
                }
            }

            var array = Ginsu.map(prototypes, ProtoPolygon::toPolygon)
                    .plusAll(polygons).toArray(Polygon[]::new);

            result = factory.createMultiPolygon(array);
        }

        SingleE<Info> choiceOneFlow(PSet<SingleE<Info>> set) {
            var iterator = set.iterator();
            var first = Ginsu.next(iterator);
            var second = Ginsu.next(iterator);

            return Event.compare(first.event, second.event) <= 0 ? first : second;
        }

        N<Info> choiceOneN(PSet<N<Info>> set) {
            var iterator = set.iterator();
            var first = Ginsu.next(iterator);
            var second = Ginsu.next(iterator);
            var firstFlow = searchFlow(first);
            var secondFlow = searchFlow(second);

            if (firstFlow != null && secondFlow != null) {
                return Event.compare(firstFlow.event, secondFlow.event) <= 0 ? first : second;
            } else if (firstFlow != null) {
                return first;
            } else if (secondFlow != null) {
                return second;
            } else {
                return first;
            }
        }

        void createProtoPolygon(Ring ring) {
            ProtoPolygon prototype = null;

            for (var shape : ring.shapes) {
                var candidate = shapeToProtoPolygon.get(shape);
                if (candidate != null) {
                    if (prototype == null)
                        prototype = candidate;
                    else if (prototype != candidate)
                        throw new GinsuException.IllegalState();
                }
            }

            if (prototype != null) {
                prototype.addHole(ring);
            } else {
                prototype = new ProtoPolygon(ring);
                prototypes = prototypes.plus(prototype);
            }

            for (var shape : ring.shapes) {
                if (!shapeToProtoPolygon.containsKey(shape))
                    shapeToProtoPolygon.put(shape, prototype);
            }
        }

        Ring extractRing(SingleE<Info> startE, final N<Info> origin) {
            final AtomicReference<PSet<DetectionShape>> used = new AtomicReference<>(HashTreePSet.empty());
            N<Info> start = origin, stop = null;
            var builder = new CSBuilder.Simplified(offset);

            Consumer<SingleE<Info>> addDetection = singleE -> {
                used.set(used.get().plus(singleE.shape));
            };

            do {
                start.visited();
                start.forEachSingle(addDetection);

                if (startE != null) {
                    var isIn = Event.isIn(startE.event);
                    var stopE = isIn ? startE.next() : startE.previous();

                    builder.add(startE.event.coordinate);
                    builder.add(isIn ?
                            Segment.forward(startE.event.index, stopE.event.index, startE.detection.sequence)
                            : Segment.backward(startE.event.index, stopE.event.index, startE.detection.sequence));
                    builder.add(stopE.event.coordinate);
                    stop = stopE.n;

                    final var copy = stopE.event;
                    var candidates = stop.filterEvent((event, info) -> event != copy && Event.isNonCorner(event));

                    if (candidates.size() == 1) {
                        start = stop;
                        startE = Ginsu.first(candidates);
                    } else if (candidates.isEmpty()) {
                        start = searchNextStart(stop, stopE);
                        startE = searchFlow(start);
                        stop.visited();
                    } else {
                        throw new GinsuException.Unsupported();
                    }

                } else {
                    builder.add(start.getCoordinate());

                    if (stop != null) {
                        var newStart = searchNextStart(start, stop);
                        stop = start;
                        start = newStart;
                    } else {
                        stop = start;
                        start = searchNextStart(start);
                    }

                    startE = searchFlow(start);
                }
            } while (start != origin);

            return new Ring(builder.close(), used.get());
        }

        boolean filterOrigin(Event event, Info info) {
            if (info == Info.B || info == Info.E)
                return Event.isNonCorner(event);
            else
                return false;
        }

        SingleE<Info> searchFlow(N<Info> n) {
            var candidates = n.filterEvent((event, info) -> Event.isNonCorner(event));
            return candidates.size() == 1 ? Ginsu.first(candidates) : null;
        }

        N<Info> searchNextStart(N<Info> reference, SingleE<Info> stop) {
            var candidates = reference.visit(Empty.<N<Info>>set(), (value, less, greater, lower, higher) ->
                    addCandidates(value, computeDirection(less, greater), lower, higher, null));

            if (candidates.size() == 1)
                return Ginsu.first(candidates);
            else
                throw new GinsuException.Unsupported();
        }

        N<Info> searchNextStart(N<Info> reference, N<Info> previous) {
            var candidates = reference.visit(Empty.<N<Info>>set(), (set, less, greater, lower, higher) ->
                    addCandidates(set, computeDirection(less, greater), lower, higher, previous));

            if (candidates.size() == 1)
                return Ginsu.first(candidates);
            else
                throw new GinsuException.IllegalState();
        }

        N<Info> searchNextStart(N<Info> origin) {
            var candidates = origin.visit(Empty.<N<Info>>set(), (set, less, greater, lower, higher) ->
                    addCandidates(set, computeDirection(less, greater), lower, higher, null));

            if (candidates.size() == 1) {
                return Ginsu.first(candidates);
            } else if (candidates.size() == 2) {
                return choiceOneN(candidates);
            }

            throw new GinsuException.Unsupported();
        }

        void searchProtoPolygon(SingleE<Info> start, final N<Info> origin) {
            createProtoPolygon(extractRing(start, origin));
        }
    }

    private class ProtoPolygon {

        private final Ring shell;
        private PVector<Ring> holes = TreePVector.empty();

        public ProtoPolygon(Ring ring) {
            shell = ring;
        }

        void addHole(Ring ring) {
            holes = holes.plus(ring);
        }

        public Polygon toPolygon() {
            var shell = this.shell.toLinearRing();
            var holes = Ginsu.map(this.holes, Ring::toLinearRing);
            return factory.createPolygon(shell, holes.toArray(LinearRing[]::new));
        }
    }

    private class Ring {

        final CoordinateSequence sequence;
        final PSet<DetectionShape> shapes;

        private Ring(CoordinateSequence sequence, PSet<DetectionShape> shapes) {
            this.sequence = sequence;
            this.shapes = shapes;
        }

        public LinearRing toLinearRing() {
            return factory.createLinearRing(sequence);
        }
    }

}
