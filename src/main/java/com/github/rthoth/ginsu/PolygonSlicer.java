package com.github.rthoth.ginsu;

import org.locationtech.jts.algorithm.RayCrossingCounter;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Location;
import org.locationtech.jts.geom.MultiPolygon;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.util.Collection;
import java.util.LinkedList;
import java.util.TreeSet;

public class PolygonSlicer extends GeometrySlicer<MultiPolygon> {

    private static final int UP = 1;
    private static final int DOWN = 2;

    public PolygonSlicer(GeometryFactory factory) {
        super(factory);
    }

    private static int rank(SEvent event) {
        var isIn = SEvent.isIn(event);
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
    public MultiShape apply(PVector<SShape.Detection> detections) {
        return new Slicer(detections).result;
    }

    @Override
    public SShape classify(SShape.Detection detection, Shape shape) {
        if (detection.events.isEmpty())
            return new SShape.Done(detection.location == SShape.Detection.INSIDE ? shape : Shape.EMPTY);

        return new SShape.Ongoing(detection);
    }

    @Override
    public MultiPolygon toGeometry(MultiShape multishape) {
        return multishape.toMultiPolygon(factory);
    }

    private static class ProtoPolygon implements Comparable<ProtoPolygon> {

        protected final CoordinateSequence shell;
        private PVector<CoordinateSequence> holes = TreePVector.empty();

        public ProtoPolygon(CoordinateSequence shell) {
            this.shell = shell;
        }

        public void add(Collection<CoordinateSequence> holes) {
            this.holes = this.holes.plusAll(holes);
        }

        public boolean addIfContains(CoordinateSequence hole) {
            for (int i = 0, l = hole.size(); i < l; i++) {
                final var coordinate = hole.getCoordinate(i);
                final var location = RayCrossingCounter.locatePointInRing(coordinate, shell);

                if (location == Location.INTERIOR) {
                    holes = holes.plus(hole);
                    return true;
                } else if (location == Location.EXTERIOR) {
                    return false;
                }
            }

            return false;
        }

        @Override
        public int compareTo(ProtoPolygon other) {
            return shell.size() <= other.shell.size() ? -1 : 1;
        }

        public Shape toShape() {
            return Shape.of(shell, holes);
        }
    }

    private static class Slicer {

        final SScanLine scanLine = new SScanLine();
        final MultiShape result;
        LinkedList<CoordinateSequence> inside = new LinkedList<>();
        TreeSet<ProtoPolygon> protoPolygons = new TreeSet<>();
        SEvent origin;
        int direction = 0;

        public Slicer(PVector<SShape.Detection> detections) {
            for (var detection : detections) {
                if (!detection.events.isEmpty()) {
                    scanLine.add(detection);
                } else if (detection.location == SShape.Detection.INSIDE) {
                    inside.add(detection.sequence);
                }
            }

            if (scanLine.nonEmpty()) {

                while (scanLine.nonEmpty()) {
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

                result = MultiShape.of(Ginsu.map(protoPolygons, ProtoPolygon::toShape));
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
                var forward = SEvent.isIn(start);
                var stop = forward ? scanLine.extractNext(start) : scanLine.extractPrevious(start);

                if (start.index >= 0 && stop.index >= 0) {
                    if (start.location.coordinate != null)
                        builder.addPoint(start.location.coordinate);

                    if (forward)
                        builder.addForward(start.index, stop.index, start.sequence);
                    else
                        builder.addBackward(start.index, stop.index, start.sequence);

                    if (stop.location.coordinate != null)
                        builder.addPoint(stop.location.coordinate);

                } else if (start.index < 0 && stop.index < 0) {
                    builder.addLine(start.location.coordinate, stop.location.coordinate);
                } else {
                    throw new GinsuException.TopologyException("Invalid event detection!");
                }

                if (stop.border() == origin.border()) {
                    start = direction == UP ? scanLine.extractLower(stop) : scanLine.extractHigher(stop);
                } else {
                    start = direction == UP ? scanLine.extractHigher(stop) : scanLine.extractLower(stop);
                }
            } while (start != origin);

            return builder.close();
        }

        void searchOrigin() {
            var lower = scanLine.lower();
            var upper = scanLine.upper();
            if (lower != upper) {
                origin = null;
                var ranking = updateOrigin(Integer.MIN_VALUE, UP, lower.getLower(), lower.getUpper());
                updateOrigin(ranking, DOWN, upper.getUpper(), upper.getLower());
            } else {
                throw new GinsuException.TopologyException("Invalid event detection!");
            }
        }

        int updateOrigin(int ranking, int newDirection, SEvent... events) {
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
