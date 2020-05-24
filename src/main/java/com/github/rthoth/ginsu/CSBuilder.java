package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

public class CSBuilder {

    private PVector<Segment> segments = TreePVector.empty();

    public void addBackward(int start, int stop, CoordinateSequence sequence) {
        segments = segments.plus(Segment.backward(start, stop, sequence));
    }

    public void addForward(int start, int stop, CoordinateSequence sequence) {
        segments = segments.plus(Segment.forward(start, stop, sequence));
    }

    public void addLine(Coordinate start, Coordinate stop) {
        segments = segments.plus(new Segment.Line(start, stop));
    }

    public void addPoint(Coordinate point) {
        segments = segments.plus(new Segment.Point(point));
    }

    public SegmentCoordinateSequence close() {
        if (!segments.isEmpty()) {
            var first = segments.get(0);
            var last = segments.get(segments.size() - 1);

            if (!first.getCoordinate(0).equals2D(last.getCoordinate(last.size() - 1))) {
                if (first instanceof Segment.View) {
                    return new SegmentCoordinateSequence(segments.plus(((Segment.View) first).point(0)));
                } else if (first instanceof Segment.PointView || first instanceof Segment.Point) {
                    return new SegmentCoordinateSequence(segments.plus(first));
                } else if (first instanceof Segment.Line) {
                    return new SegmentCoordinateSequence(segments.plus(((Segment.Line) first).point(0)));
                } else {
                    throw new GinsuException.IllegalState("Invalid segments!");
                }
            } else {
                return new SegmentCoordinateSequence(segments);
            }
        } else {
            throw new GinsuException.IllegalState("It is empty!");
        }
    }

    public CoordinateSequence build() {
        return new SegmentCoordinateSequence(segments);
    }
}
