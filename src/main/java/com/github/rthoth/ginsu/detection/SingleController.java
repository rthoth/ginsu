package com.github.rthoth.ginsu.detection;

import com.github.rthoth.ginsu.Dimension;
import com.github.rthoth.ginsu.Event;
import com.github.rthoth.ginsu.Slice;
import com.github.rthoth.ginsu.detection.EventInfo.Type;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;

public final class SingleController extends Controller {

    private final Slice slice;
    private final CoordinateInfo previous = new CoordinateInfo();
    private final CoordinateInfo current = new CoordinateInfo();
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
    public void compute() {
        var segment = apply(newSegment());
        if (segment.origin.type != Type.UNDEFINED)
            recorder.apply(segment.origin);

        if (segment.target.type != Type.UNDEFINED)
            recorder.apply(segment.target);

    }

    @Override
    public Detection end(boolean isRing) {
        return new Detection(getSequence(), recorder.end(current.index, isRing), isRing, startsInside(), Detection.EMPTY_CORNER_SET);
    }

    public Dimension getDimension() {
        return slice.getDimension();
    }

    public int getIndex() {
        return current.index;
    }

    @Override
    protected double getKnifeValue() {
        return slice.getKnifeValue();
    }

    @Override
    protected Recorder getRecorder() {
        return recorder;
    }

    @Override
    public CoordinateSequence getSequence() {
        return recorder.factory.getSequence();
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
