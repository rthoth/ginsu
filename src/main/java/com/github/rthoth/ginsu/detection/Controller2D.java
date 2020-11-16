package com.github.rthoth.ginsu.detection;

import com.github.rthoth.ginsu.*;
import com.github.rthoth.ginsu.Event.Side;
import com.github.rthoth.ginsu.Knife.X;
import com.github.rthoth.ginsu.Knife.Y;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateXY;
import org.pcollections.PVector;

import java.util.Comparator;
import java.util.TreeSet;

public final class Controller2D extends Controller {

    private static final int OUTSIDE = -1;
    private static final int INSIDE = 1;

    private final Controller1D<X> x;
    private final Controller1D<Y> y;
    private final boolean hasCorner;
    private final Recorder recorder;
    private final Event.Factory factory;

    Controller2D(Slice<X> x, Slice<Y> y, Event.Factory factory, boolean hasCorner) {
        recorder = new Recorder(factory, hasCorner);

        this.hasCorner = hasCorner;
        this.factory = factory;
        this.x = new Controller1D<>(x, hasCorner ? new Recorder(factory, true) : null);
        this.y = new Controller1D<>(y, hasCorner ? new Recorder(factory, true) : null);
    }

    @Override
    public void apply(ProtoEvent protoEvent) {
        recorder.apply(protoEvent);
    }

    @Override
    public void begin(Coordinate coordinate) {
        x.begin(coordinate);
        y.begin(coordinate);

        if (hasCorner) {
            x.begin(coordinate);
            y.begin(coordinate);
        }
    }

    @Override
    public void compute() {
        var segment = x.isChanged() ? y.apply(x.apply(x.newSegment())) : x.apply(y.apply(y.newSegment()));

        if (segment.origin.type != ProtoEvent.Type.UNDEFINED)
            recorder.apply(segment.origin);
        if (segment.target.type != ProtoEvent.Type.UNDEFINED)
            recorder.apply(segment.target);
    }

    private Detection.CornerSet createCornerSet(boolean isRing) {
        final var offset = (x.getOffset() + y.getOffset()) / 2D;
        Comparator<Double> comparator = (v1, v2) -> Ginsu.compare(v1, offset, v2);

        TreeSet<Double> xLower = new TreeSet<>(comparator), xUpper = new TreeSet<>(comparator);
        TreeSet<Double> yLower = new TreeSet<>(comparator), yUpper = new TreeSet<>(comparator);
        populate(xLower, xUpper, Dimension.X, x.endRecorder(isRing));
        populate(yLower, yUpper, Dimension.Y, y.endRecorder(isRing));

        var ll = detectCorner(xLower, yLower, x.getLower(), y.getLower(), Side.GREAT, Side.GREAT, offset);
        var ul = detectCorner(xUpper, yLower, x.getUpper(), y.getLower(), Side.LESS, Side.GREAT, offset);
        var uu = detectCorner(xUpper, yUpper, x.getUpper(), y.getUpper(), Side.LESS, Side.LESS, offset);
        var lu = detectCorner(xLower, yUpper, x.getLower(), y.getUpper(), Side.GREAT, Side.LESS, offset);

        return Detection.CornerSet.of(ll, ul, uu, lu);
    }

    private Event detectCorner(TreeSet<Double> xIndex, TreeSet<Double> yIndex, double x, double y, Side xSide, Side ySide, double offset) {
        if (Double.isFinite(x) && Double.isFinite(y)) {
            if (location(xIndex, y, ySide) == INSIDE && location(yIndex, x, xSide) == INSIDE)
                return factory.create(Event.Type.CORNER, Event.CORNER_INDEX, new CoordinateXY(x, y), Dimension.CORNER, xSide, ySide);
        }

        return null;
    }

    @Override
    public Detection end(boolean isRing) {
        var cornerSet = hasCorner ? createCornerSet(isRing) : Detection.EMPTY_CORNER_SET;
        return new Detection(getSequence(), recorder.end(x.getIndex(), isRing), isRing, startsInside(), cornerSet);
    }

    @Override
    public CoordinateSequence getSequence() {
        return recorder.factory.getSequence();
    }

    @Override
    public boolean isChanged() {
        return x.product() != 9 && y.product() != 9 && (x.isChanged() || y.isChanged());
    }

    private int locate(int size) {
        return size % 2 == 1 ? INSIDE : OUTSIDE;
    }

    private int location(TreeSet<Double> index, double reference, Side side) {
        if (side == Side.GREAT)
            return locate(index.tailSet(reference, false).size());
        else if (side == Side.LESS)
            return locate(index.headSet(reference, false).size());

        throw new GinsuException.IllegalArgument("Invalid side [" + side + "]!");
    }

    @Override
    public void next() {
        x.next();
        y.next();
    }

    private void populate(TreeSet<Double> lower, TreeSet<Double> upper, Dimension dimension, PVector<Event> events) {
        for (var event : events) {
            var border = event.getBorder(dimension);
            if (border == Slice.LOWER_BORDER)
                lower.add(event.positional(dimension));
            else if (border == Slice.UPPER_BORDER)
                upper.add(event.positional(dimension));
            else
                throw new GinsuException.IllegalState("Invalid border [" + border + "]!");
        }
    }

    @Override
    public boolean startsInside() {
        return x.startsInside() && y.startsInside();
    }

    @Override
    public void update(int index, Coordinate coordinate) {
        x.update(index, coordinate);
        y.update(index, coordinate);

        if (hasCorner) {
            if (x.isChanged())
                x.compute();
            if (y.isChanged())
                y.compute();
        }
    }
}
