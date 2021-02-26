package com.github.rthoth.ginsu.detection;

import com.github.rthoth.ginsu.Dimension;
import com.github.rthoth.ginsu.GinsuException;
import com.github.rthoth.ginsu.Knife.X;
import com.github.rthoth.ginsu.Knife.Y;
import com.github.rthoth.ginsu.Shape;
import org.locationtech.jts.geom.Coordinate;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Stream;

public class Cell {

    public static final Cell EMPTY = new Cell(null, false, null);

    private final Shape shape;
    private final boolean hasCorner;
    private final PVector<Detection> detections;

    public Cell(Shape shape, boolean hasCorner, PVector<Detection> detections) {
        this.shape = shape;
        this.hasCorner = hasCorner;
        this.detections = detections;
    }

    public static Cell create(Shape shape, boolean hasCorner) {
        return new Cell(shape, hasCorner, TreePVector.empty());
    }

    public static Detection detect(Event.Factory factory, Detector<X> x, Detector<Y> y, boolean hasCorner) {
        var context = new Context(factory);
        context.select(x, y);
        context.select(y, x);
        context.mergeCorners();

        if (hasCorner)
            context.detectCorners(x, y);

        return new Detection(context);
    }

    public static Cell empty() {
        return EMPTY;
    }

    private static Event setCorner(Event oldEvent, Event newEvent) {
        if (oldEvent == null)
            return newEvent;
        else
            throw new GinsuException.InvalidState("Corner is already defined!");
    }

    public boolean nonEmpty() {
        return !detections.isEmpty();
    }

    public Cell plus(Detection detection) {
        return detection.nonEmpty() ? new Cell(shape, hasCorner, detections.plus(detection)) : this;
    }

    private static class Context {

        Event.Factory factory;
        TreeSet<Event> events = new TreeSet<>(Event.PREVIOUS_COMPARATOR);

        Event xL, yL, xU, yU, ll, lu, uu, ul;
        Coordinate cll, clu, cuu, cul;

        public Context(Event.Factory factory) {
            this.factory = factory;
        }

        public void detectCorners(Detector<X> x, Detector<Y> y) {
            cll = createCorner(ll, x.getLowerKnife(), y.getLowerKnife(), y.getLowerEvents());
            clu = createCorner(lu, x.getLowerKnife(), y.getUpperKnife(), y.getUpperEvents());
            cul = createCorner(ul, x.getUpperKnife(), y.getLowerKnife(), y.getLowerEvents());
            cuu = createCorner(uu, x.getUpperKnife(), y.getUpperKnife(), y.getUpperEvents());
        }

        public boolean nonEmpty() {
            return !events.isEmpty() || Stream.of(cll, clu, cul, cuu).allMatch(Objects::nonNull);
        }

        public void select(Detector<?> detector, Detector<?> filter) {
            for (var event : detector.getEvents()) {
                var position = filter.position(event.getCoordinate());
                if (position == 1)
                    events.add(event);
                else if (Math.abs(position) == 2)
                    addCorner(event, filter.getDimension(), position);
            }
        }

        void addCorner(Event event, Dimension dimension, int border) {
            if (dimension == Dimension.X) {
                if (border == 2) xU = setCorner(xU, event);
                else xL = setCorner(xL, event);
            } else {
                if (border == 2) yU = setCorner(yU, event);
                else yL = setCorner(yL, event);
            }
        }

        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        private Coordinate createCorner(Event corner, Optional<X> x, Optional<Y> y, NavigableMap<Double, Event> yEvents) {
            if (corner == null && x.isPresent() && y.isPresent()) {
                var lower = yEvents.headMap(x.get().value).size() % 2 == 1;
                var upper = yEvents.tailMap(x.get().value).size() % 2 == 1;
                if (lower && upper)
                    return new Coordinate(x.get().value, y.get().value);
            }

            return null;
        }

        Event mergeCorner(Event e1, Event e2) {
            if (e1 != null && e2 != null) {
                var newEvent = e1.merge(e2);
                events.add(newEvent);
                return newEvent;
            } else {
                return null;
            }
        }

        void mergeCorners() {
            ll = mergeCorner(xL, yL);
            lu = mergeCorner(xL, yU);
            uu = mergeCorner(xU, yU);
            ul = mergeCorner(xU, yL);
        }
    }

    public static class Detection {

        private final Context context;

        private Detection(Context context) {
            this.context = context;
        }

        public boolean nonEmpty() {
            return context.nonEmpty();
        }
    }
}
