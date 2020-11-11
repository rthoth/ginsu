package com.github.rthoth.ginsu.detection;

import com.github.rthoth.ginsu.*;
import com.github.rthoth.ginsu.detection.EventInfo.Type;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;

import java.util.TreeMap;

public final class XYController extends Controller {

    private static final int OUTSIDE = -1;
    private static final int INSIDE = 1;

    private final SingleController x;
    private final SingleController y;
    private final boolean hasCorner;
    private final Recorder recorder;

    private SingleController xL = null;
    private SingleController xU = null;
    private SingleController yL = null;
    private SingleController yU = null;

    XYController(Slice x, Slice y, Event.Factory factory, boolean hasCorner) {
        recorder = new Recorder(factory, hasCorner);

        this.hasCorner = hasCorner;
        this.x = new SingleController(x, null);
        this.y = new SingleController(y, null);
        if (hasCorner) {
            xL = createController(x.getLower(), factory);
            xU = createController(x.getUpper(), factory);
            yL = createController(y.getLower(), factory);
            yU = createController(y.getUpper(), factory);
        }
    }

    @Override
    public void apply(EventInfo eventInfo) {
        recorder.apply(eventInfo);
    }

    @Override
    public void begin(Coordinate coordinate) {
        x.begin(coordinate);
        y.begin(coordinate);

        if (hasCorner) {
            begin(coordinate, xL);
            begin(coordinate, xU);
            begin(coordinate, yL);
            begin(coordinate, yU);
        }
    }

    private void begin(Coordinate coordinate, SingleController controller) {
        if (controller != null) controller.begin(coordinate);
    }

    @Override
    public void compute() {
        var segment = x.isChanged() ? y.apply(x.apply(x.newSegment())) : x.apply(y.apply(y.newSegment()));

        if (segment.origin.type != Type.UNDEFINED)
            recorder.apply(segment.origin);
        if (segment.target.type != Type.UNDEFINED)
            recorder.apply(segment.target);
    }

    SingleController createController(Slice slice, Event.Factory factory) {
        return slice != null ? new SingleController(slice, new Recorder(factory, false)) : null;
    }

    private Event createCorner(SingleController x, SingleController y, CornerInfo xIn, CornerInfo yIn, Event.Factory factory, Dimension.Side xSide, Dimension.Side ySide) {
        if (xIn != null && yIn != null && xIn.position == INSIDE && yIn.position == INSIDE)
            return factory.create(Event.Type.CORNER, Event.CORNER_INDEX, new Coordinate(x.getKnifeValue(), y.getKnifeValue()), Dimension.CORNER, xSide, ySide);

        return null;
    }

    private Detection.CornerSet createCornerSet(boolean isRing) {
        var xLI = createEvents(xL, isRing);
        var xUI = createEvents(xU, isRing);
        var yLI = createEvents(yL, isRing);
        var yUI = createEvents(yU, isRing);

        var ll = createCorner(xL, yL, higher(yLI, xL), higher(xLI, yL), recorder.factory, Dimension.Side.GREATER, Dimension.Side.GREATER);
        var ul = createCorner(xU, yL, lower(yLI, xU), higher(xUI, yL), recorder.factory, Dimension.Side.LESS, Dimension.Side.GREATER);
        var uu = createCorner(xU, yU, lower(yUI, xU), lower(xUI, yU), recorder.factory, Dimension.Side.LESS, Dimension.Side.LESS);
        var lu = createCorner(xL, yU, higher(yUI, xL), lower(xLI, yU), recorder.factory, Dimension.Side.GREATER, Dimension.Side.LESS);

        return Detection.CornerSet.of(ll, ul, uu, lu);
    }

    private TreeMap<Double, Event> createEvents(SingleController controller, boolean isRing) {
        if (controller != null) {
            var treeMap = new TreeMap<Double, Event>();
            var dimension = controller.getDimension();
            for (var event : controller.getRecorder().end(x.getIndex(), isRing)) {
                treeMap.put(dimension.ordinateOf(event.getCoordinate()), event);
            }
            return treeMap;
        } else {
            return null;
        }
    }

    private void detect(int index, Coordinate coordinate, SingleController controller) {
        if (controller != null) {
            controller.update(index, coordinate);
            if (controller.isChanged()) {
                var segment = controller.apply(controller.newSegment());
                if (segment.origin.type != Type.UNDEFINED)
                    controller.apply(segment.origin);
                if (segment.target.type != Type.UNDEFINED)
                    controller.apply(segment.target);
            }

            controller.next();
        }
    }

    @Override
    public Detection end(boolean isRing) {
        var cornerSet = hasCorner ? createCornerSet(isRing) : Detection.EMPTY_CORNER_SET;
        return new Detection(getSequence(), recorder.end(x.getIndex(), isRing), isRing, startsInside(), cornerSet);
    }

    @Override
    protected double getKnifeValue() {
        throw new GinsuException.Unsupported();
    }

    @Override
    protected Recorder getRecorder() {
        return recorder;
    }

    @Override
    public CoordinateSequence getSequence() {
        return recorder.factory.getSequence();
    }

    private CornerInfo higher(TreeMap<Double, Event> treeMap, SingleController controller) {
        return higher(treeMap, controller, false);
    }

    private CornerInfo higher(TreeMap<Double, Event> treeMap, SingleController controller, boolean inclusive) {
        if (controller != null && treeMap != null) {
            var tailMap = treeMap.tailMap(controller.getKnifeValue(), inclusive);
            if (!tailMap.isEmpty())
                return new CornerInfo(tailMap.size(), tailMap.firstEntry().getValue());
        }

        return null;
    }

    @Override
    public boolean isChanged() {
        return x.product() != 9 && y.product() != 9 && (x.isChanged() || y.isChanged());
    }

    private CornerInfo lower(TreeMap<Double, Event> treeMap, SingleController controller) {
        return lower(treeMap, controller, false);
    }

    private CornerInfo lower(TreeMap<Double, Event> treeMap, SingleController controller, boolean inclusive) {
        if (controller != null && treeMap != null) {
            var headMap = treeMap.headMap(controller.getKnifeValue(), inclusive);
            if (!headMap.isEmpty())
                return new CornerInfo(headMap.size(), headMap.lastEntry().getValue());

        }

        return null;
    }

    @Override
    public void next() {
        x.next();
        y.next();
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
            detect(index, coordinate, xL);
            detect(index, coordinate, xU);
            detect(index, coordinate, yL);
            detect(index, coordinate, yU);
        }
    }

    static class CornerInfo {

        final int position;
        final Event event;

        public CornerInfo(int size, Event event) {
            position = size % 2 == 0 ? OUTSIDE : INSIDE;
            this.event = event;
        }
    }
}
