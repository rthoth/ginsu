package com.github.rthoth.ginsu;

import org.locationtech.jts.algorithm.RayCrossingCounter;
import org.locationtech.jts.geom.*;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.util.*;

public class PolygonSlicer extends GeometrySlicer<MultiPolygon> {

    private static final int UP = 1;
    private static final int DOWN = 2;

    public PolygonSlicer(GeometryFactory factory) {
        super(factory);
    }

    private static int rank(Event event) {
        var isIn = Event.isIn(event);
        var in = isIn ? 4096 : 0;
        if (event.index >= 0) {
            if (isIn)
                return in | (int) (1000 - (1000D * event.index) / event.sequenceSize());
            else
                return in | (int) ((1000D * event.index) / event.sequenceSize());
        } else {
            return in;
        }
    }

    @Override
    public MultiPolygon apply(MultiShape multishape) {
        if (!(multishape.getSource() instanceof MultiPolygon)) {
            if (multishape.nonEmpty()) {
                return factory.createMultiPolygon(Ginsu.map(multishape, this::apply).toArray(Polygon[]::new));
            } else {
                return factory.createMultiPolygon();
            }
        } else {
            return (MultiPolygon) multishape.getSource();
        }
    }

    public Polygon apply(Shape shape) {
        if (!(shape.getSource() instanceof Polygon)) {
            if (shape.nonEmpty()) {
                var it = shape.iterator();
                var shell = factory.createLinearRing(Ginsu.next(it));
                var holes = Ginsu.map(it, factory::createLinearRing);
                return factory.createPolygon(shell, holes.toArray(LinearRing[]::new));

            } else {
                throw new GinsuException.IllegalArgument(Objects.toString(shape));
            }
        } else {
            return (Polygon) shape.getSource();
        }
    }

    @Override
    public Detection.Status classify(Detection detection, Shape shape) {
        if (detection.events.isEmpty())
            return new Detection.Ready(detection.firstLocation == Detection.INSIDE ? shape : Shape.EMPTY);

        return new Detection.Unready(detection);
    }

    @Override
    public MultiShape slice(PVector<Detection> detections) {
        return new Slicer(detections).multishape;
    }

    private class ProtoPolygon implements Comparable<ProtoPolygon> {

        protected final CoordinateSequence shell;
        private PVector<CoordinateSequence> holes = TreePVector.empty();

        public ProtoPolygon(CoordinateSequence shell) {
            this.shell = shell;
        }

        @Override
        public int compareTo(ProtoPolygon other) {
            var mySize = shell.size();
            var otherSize = other.shell.size();

            if (mySize != otherSize)
                return Integer.compare(mySize, otherSize);
            else
                return -1;
        }

        public Shape toShape() {
            return Shape.of(shell, holes);
        }

        public void add(Collection<CoordinateSequence> holes) {
            this.holes = this.holes.plusAll(holes);
        }

        public boolean addIfContains(CoordinateSequence hole) {
            for (int i = 0, l = hole.size(); i < l; i++) {
                final var coordinate = hole.getCoordinate(i);
                final var location = RayCrossingCounter.locatePointInRing(coordinate, shell);

                if (location == Location.INTERIOR || location == Location.EXTERIOR) {
                    holes = holes.plus(hole);
                    return true;
                }
            }

            return false;
        }
    }

    private class Slicer {

        final SortedEventSet eventSet = new SortedEventSet();
        final MultiShape multishape;
        LinkedList<CoordinateSequence> inside = new LinkedList<>();
        TreeSet<ProtoPolygon> protoPolygons = new TreeSet<>();
        Event origin;
        int direction = 0;

        public Slicer(PVector<Detection> detections) {
            for (var detection : detections) {
                if (!detection.events.isEmpty()) {
                    eventSet.add(detection);
                } else if (detection.firstLocation == Detection.INSIDE) {
                    inside.add(detection.sequence);
                }
            }

            if (eventSet.nonEmpty()) {

                while (eventSet.nonEmpty()) {
                    protoPolygons.add(new ProtoPolygon(createShell()));
                }

                if (!inside.isEmpty()) {

                    if (protoPolygons.size() > 1)
                        for (final var hole : inside) {
                            var found = false;

                            for (final var protoPolygon : protoPolygons) {
                                if (protoPolygon.addIfContains(hole)) {
                                    found = true;
                                    break;
                                }
                            }

                            if (found)
                                throw new GinsuException.TopologyException("There is a hole on outside!");
                        }
                    else
                        protoPolygons.first().add(inside);
                }

                multishape = MultiShape.of(Ginsu.map(protoPolygons, ProtoPolygon::toShape));
            } else {
                throw new GinsuException.IllegalState("No events!");
            }
        }

        CoordinateSequence createShell() {
            searchOrigin();
            if (origin == null)
                throw new GinsuException.TopologyException("There is no origin!");

            var builder = new CSBuilder();
            var start = origin;

            do {
                var forward = Event.isIn(start);
                var stop = forward ? eventSet.extractNext(start) : eventSet.extractPrevious(start);

                if (start.index >= 0 && stop.index >= 0) {
                    if (start.intersection.coordinate != null)
                        builder.addPoint(start.intersection.coordinate);

                    if (forward)
                        builder.addForward(start.index, stop.index, start.sequence);
                    else
                        builder.addBackward(start.index, stop.index, start.sequence);

                    if (stop.intersection.coordinate != null)
                        builder.addPoint(stop.intersection.coordinate);

                } else if (start.index < 0 && stop.index < 0) {
                    builder.addLine(start.intersection.coordinate, stop.intersection.coordinate);
                } else {
                    throw new GinsuException.TopologyException("Invalid event detection!");
                }

                if (stop.border() == origin.border()) {
                    start = direction == UP ? eventSet.extractLower(stop) : eventSet.extractHigher(stop);
                } else {
                    start = direction == UP ? eventSet.extractHigher(stop) : eventSet.extractLower(stop);
                }
            } while (start != origin);

            return builder.close();
        }

        void searchOrigin() {
            var lower = eventSet.lower();
            var upper = eventSet.upper();
            if (lower != upper) {
                origin = null;
                var ranking = updateOrigin(Integer.MIN_VALUE, UP, lower.getLower(), lower.getUpper());
                updateOrigin(ranking, DOWN, upper.getUpper(), upper.getLower());
            } else {
                throw new GinsuException.TopologyException("Invalid event detection!");
            }
        }

        int updateOrigin(int ranking, int newDirection, Event... events) {
            for (var event : events) {
                if (event != null) {
                    var newRanking = rank(event);
                    if (newRanking > ranking) {
                        ranking = newRanking;
                        origin = event;
                        direction = newDirection;
                    }
                }
            }

            return ranking;
        }
    }
}
