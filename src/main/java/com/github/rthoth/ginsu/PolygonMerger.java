package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.*;
import org.pcollections.*;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class PolygonMerger extends GeometryMerger<MultiPolygon> {

    private final GeometryFactory factory;
    private final double offset;

    public PolygonMerger(GeometryFactory factory, double offset) {
        this.factory = factory;
        this.offset = offset;
    }

    @Override
    public MultiPolygon apply(PCollection<DetectionShape> shapes, PVector<Knife.X> x, PVector<Knife.Y> y) {
        return new Merger(shapes, x, y).result;
    }

    @Override
    public boolean isPolygon() {
        return true;
    }

    @Override
    public Optional<Shape> preApply(Detection detection, Shape shape) {
        if (!detection.events.isEmpty()) {
            return Optional.empty();
        } else {
            return detection.startsInside ? Optional.of(shape) : Optional.of(Shape.EMPTY);
        }
    }

    private enum IO {

        I, O;

        public IO invert() {
            return this == I ? O : I;
        }
    }

    private static class Maze extends AbstractMaze<IO> {

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
                if (shape.isOngoing()) {
                    maze.add(shape);
                } else {
                    polygons = polygons.plus(shape.getShape().toPolygon(factory));
                }
            }

            maze.initialize(IO.O).forEach((current, e, hasNext) -> {
                if (hasNext) {
                    if (e.isSingle())
                        return current.invert();
                    else
                        return e.forall(Event::isCorner) ? IO.I : current.invert();
                } else {
                    return IO.O;
                }
            });

            for (var n : maze.unvisited()) {
                n.visited();
                var nonCorners = n.filter(Event::isNonCorner);
                if (nonCorners.size() == 1) {
                    searchProtoPolygon(Ginsu.first(nonCorners), n);
                } else if (nonCorners.size() == 2) {
                    searchProtoPolygon(choiceOne(nonCorners), n);
                } else {
                    searchProtoPolygon(null, n);
                }
            }

            result = factory.createMultiPolygon(Ginsu.map(prototypes, ProtoPolygon::toPolygon).toArray(Polygon[]::new));
        }

        Maze.SingleE choiceOne(Iterable<Maze.SingleE> iterable) {
            var iterator = iterable.iterator();
            var first = Ginsu.next(iterator);
            var second = Ginsu.next(iterator);

            return Event.compare(first.event, second.event) <= 0 ? first : second;
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

        Ring extractRing(Maze.SingleE startE, final Maze.N origin) {
            Maze.N start = origin, stop = null;
            Maze.SingleE stopE;
            var builder = new CSBuilder.Simplified(offset);
            final AtomicReference<PSet<DetectionShape>> used = new AtomicReference<>(HashTreePSet.empty());

            do {
                start.forEachSingleE(singleE -> used.set(used.get().plus(singleE.shape)));
                start.visited();

                if (startE != null) {
                    var isIn = Event.isIn(startE.event);
                    stopE = isIn ? startE.next() : startE.previous();

                    builder.add(startE.event.coordinate);

                    if (isIn)
                        builder.addForward(startE.event.index, stopE.event.index, startE.detection.sequence);
                    else
                        builder.addBackward(startE.event.index, stopE.event.index, startE.detection.sequence);

                    builder.add(stopE.event.coordinate);

                    stop = stopE.getN();
                    stop.visited();

                    final var copy = stopE.event;
                    var candidates = stop.filter(event -> event != copy && Event.isNonCorner(event));

                    if (candidates.size() == 1) {
                        start = stop;
                        startE = Ginsu.first(candidates);
                    } else if (candidates.isEmpty()) {
                        start = searchNextStart(stop, stopE);
                        candidates = start.filter(Event::isNonCorner);

                        if (candidates.size() == 1) {
                            startE = Ginsu.first(candidates);
                        } else if (candidates.isEmpty()) {
                            startE = null;
                            stopE = null;
                        } else {
                            throw new GinsuException.TopologyException("");
                        }
                    } else {
                        throw new GinsuException.TopologyException("Close to:" + stop.getCoordinate());
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

                    var candidates = start.filter(Event::isNonCorner);
                    if (candidates.size() == 1)
                        startE = Ginsu.first(candidates);
                    else if (candidates.size() == 2) {
                        startE = choiceOne(candidates);
                    }
                }

            } while (start != origin);

            return new Ring(builder.close(), used.get());
        }

        Maze.N searchNextStart(Maze.N origin) {
            var candidate = origin.filter(Event::isCorner);
            if (candidate.size() == 1) {
                return Ginsu.first(candidate).next().getN();
            } else if (candidate.size() > 1) {
                throw new GinsuException.TopologyException("There is a connection close to: " + origin.getCoordinate());
            } else {
                throw new GinsuException.IllegalState();
            }
        }

        Maze.N searchNextStart(Maze.N reference, final Maze.N previous) {
            var neighbours = reference.filterNeighbour((n, e) -> {
                if (n == previous)
                    return false;
                else {
                    return e != null && (e.isSingle() || !e.forall(Event::isCorner));
                }
            });
            if (neighbours.size() == 1) {
                return Ginsu.first(neighbours);
            } else {
                throw new GinsuException.Unsupported();
            }
        }

        Maze.N searchNextStart(Maze.N reference, Maze.SingleE stop) {
            var iSet = reference.getISet();
            PSet<Maze.N> candidates = HashTreePSet.empty();
            var previous = stop.getN();
            if (iSet.size() == 1) {
                if (Ginsu.first(iSet) == IO.O) {
                    candidates = reference.filterNeighbour((n, e) -> e == null && n != previous);
                } else {

                }
            } else {
                candidates = reference.filterNeighbour((n, e) -> e == null && n != previous);
            }

            if (candidates.size() == 1) {
                return Ginsu.first(candidates);
            }

            throw new GinsuException.Unsupported();
        }

        void searchProtoPolygon(Maze.SingleE start, final Maze.N origin) {
            createProtoPolygon(extractRing(start, origin));
        }
    }

    private class ProtoPolygon {

        private final Ring shell;
        private PVector<Ring> holes = TreePVector.empty();

        public ProtoPolygon(Ring ring) {
            shell = ring;
        }

        public Polygon toPolygon() {
            var shell = this.shell.toLinearRing();
            var holes = Ginsu.map(this.holes, Ring::toLinearRing);
            return factory.createPolygon(shell, holes.toArray(LinearRing[]::new));
        }

        void addHole(Ring ring) {
            holes = holes.plus(ring);
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
