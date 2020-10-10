package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.util.Optional;

public class PolygonSlicer extends GeometrySlicer<MultiPolygon> {

    public PolygonSlicer(GeometryFactory factory) {
        super(factory);
    }

    @Override
    public MultiShape apply(DetectionShape shape, Dimension dimension, double offset) {
        return new Slicer(shape, dimension, offset).result;
    }

    @Override
    public boolean isPolygon() {
        return true;
    }

    @Override
    public Optional<Shape> preApply(Detection detection, Shape shape) {
        if (detection.events.isEmpty()) {
            return detection.startsInside ? Optional.of(shape) : Optional.of(Shape.EMPTY);
        } else
            return Optional.empty();
    }

    @Override
    public MultiPolygon toGeometry(MultiShape multishape) {
        return multishape.toMultiPolygon(factory);
    }

    private static class ProtoPolygon {

        final CoordinateSequence shell;
        PVector<CoordinateSequence> holes = TreePVector.empty();

        public ProtoPolygon(CoordinateSequence shell) {
            this.shell = shell;
        }

        public void addHole(CoordinateSequence hole) {
            holes = holes.plus(hole);
        }

        public Shape toShape() {
            return Shape.of(shell, holes);
        }
    }

    private static class Slicer {

        static final int UP = 1;
        static final int DOWN = -1;

        MultiShape result;
        SScanLine scanLine;
        int direction;
        SScanLine.E origin;

        public Slicer(DetectionShape shape, Dimension dimension, double offset) {
            scanLine = new SScanLine(dimension, offset);
            scanLine.add(shape);

            var protos = TreePVector.<ProtoPolygon>empty();

            while (scanLine.nonEmpty()) {
                searchOrigin();
                protos = protos.plus(new ProtoPolygon(createRing()));
            }

            for (var detection : shape.getDetections()) {
                if (detection.events.isEmpty() && detection.startsInside) {
                    for (var proto : protos) {
                        if (Ginsu.inside(detection.sequence, proto.shell)) {
                            proto.addHole(detection.sequence);
                        }
                    }
                }
            }

            result = MultiShape.of(Ginsu.map(protos, ProtoPolygon::toShape));
        }

        @SuppressWarnings("OptionalGetWithoutIsPresent")
        private CoordinateSequence createRing() {
            var start = origin;
            var builder = new CSBuilder();

            SScanLine.E stop;

            do {
                var stopEvent = Event.isIn(start.event)
                        ? start.detection.events.next(start.event).get() :
                        start.detection.events.previous(start.event).get();

                stop = scanLine.get(stopEvent, true);

                builder.add(start.event.coordinate);

                if (start.event.index >= 0 && stopEvent.index >= 0) {
                    if (start.event.type == Event.Type.IN)
                        builder.addForward(start.event.index, stopEvent.index, start.detection.sequence);
                    else if (start.event.type == Event.Type.OUT)
                        builder.addBackward(start.event.index, stopEvent.index, start.detection.sequence);
                    else
                        throw new GinsuException.IllegalState("Invalid event type!");
                }

                builder.add(stopEvent.coordinate);
                if (stop.border == origin.border) {
                    start = searchStart(stop, true);
                } else {
                    start = searchStart(stop, false);
                }
            } while (start != origin);

            return builder.close();
        }

        void searchOrigin() {
            var lowest = scanLine.lowest().select();
            var highest = scanLine.highest().select();

            if (Event.compare(lowest.event, highest.event) <= 0) {
                direction = UP;
                origin = lowest;
            } else {
                direction = DOWN;
                origin = highest;
            }
        }

        private SScanLine.E searchStart(SScanLine.E stop, boolean close) {
            if (direction == UP)
                return close ? scanLine.lower(stop, true) : scanLine.higher(stop, true);
            else
                return close ? scanLine.higher(stop, true) : scanLine.lower(stop, true);
        }

    }
}
