package com.github.rthoth.ginsu;

import com.github.rthoth.ginsu.detection.Detection;
import com.github.rthoth.ginsu.detection.DetectionShape;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Scan Line
 */
public class ScanLine {

    private final Dimension dimension;

    private final TreeMap<Double, E> lowerBorder;
    private final TreeMap<Double, E> upperBorder;

    private final HashMap<Event, E> eventToE = new HashMap<>();


    public ScanLine(Dimension dimension, final double offset) {

        Comparator<Double> comparator = (v1, v2) -> Ginsu.compare(v1, offset, v2);
        lowerBorder = new TreeMap<>(comparator);
        upperBorder = new TreeMap<>(comparator);

        this.dimension = dimension;
    }

    public void add(DetectionShape shape) {
        for (var detection : shape.detections) {
            for (var event : detection.events.getVector()) {
                var coordinate = event.getCoordinate();
                var border = event.getBorder(dimension);

                var e = new E(dimension.positionalOf(coordinate), border, event, detection, shape);
                if (border == Slice.LOWER_BORDER)
                    lowerBorder.put(e.ordinate, e);

                if (border == Slice.UPPER_BORDER)
                    upperBorder.put(e.ordinate, e);

                eventToE.put(event, e);
            }
        }
    }

    private E firstEntry(TreeMap<Double, E> border) {
        return !border.isEmpty() ? border.firstEntry().getValue() : null;
    }

    public E get(Event event, boolean remove) {
        var e = eventToE.get(event);
        if (e != null) {
            if (remove)
                getBorder(e).remove(e.ordinate, e);
            return e;
        } else
            throw new GinsuException.IllegalArgument("Invalid event: " + event);
    }

    private TreeMap<Double, E> getBorder(E e) {
        if (e.border == Slice.LOWER_BORDER)
            return lowerBorder;

        if (e.border == Slice.UPPER_BORDER)
            return upperBorder;

        throw new GinsuException.IllegalArgument("Invalid E:" + e);
    }

    public E higher(E e, boolean remove) {
        var border = getBorder(e);
        return returnAndRemove(border.higherEntry(e.ordinate), border, remove);
    }

    public T highest() {
        E l = lastOf(lowerBorder), u = lastOf(upperBorder);
        if (l != null && u != null) {
            if (l.ordinate > u.ordinate)
                return new T(l, u);

            if (u.ordinate > l.ordinate)
                return new T(null, l);
        }

        return new T(l, u);
    }

    private E lastOf(TreeMap<Double, E> border) {
        return !border.isEmpty() ? border.lastEntry().getValue() : null;
    }

    public E lower(E e, boolean remove) {
        var border = getBorder(e);
        return returnAndRemove(border.lowerEntry(e.ordinate), border, remove);
    }

    public T lowest() {
        E l = firstEntry(lowerBorder), u = firstEntry(upperBorder);
        if (l != null && u != null) {
            if (l.ordinate < u.ordinate)
                return new T(l, null);
            if (u.ordinate < l.ordinate)
                return new T(null, u);
        }

        return new T(l, u);
    }

    public boolean nonEmpty() {
        return !lowerBorder.isEmpty() || !upperBorder.isEmpty();
    }

    private E returnAndRemove(Map.Entry<Double, E> entry, TreeMap<Double, E> border, boolean remove) {
        if (entry != null) {
            if (remove)
                border.remove(entry.getKey());
            return entry.getValue();
        } else
            throw new GinsuException.IllegalState("No element!");
    }

    public static class E {

        final double ordinate;
        final int border;
        final Event event;
        final Detection detection;
        final DetectionShape shape;

        public E(double ordinate, int border, Event event, Detection detection, DetectionShape shape) {
            this.ordinate = ordinate;
            this.border = border;
            this.event = event;
            this.detection = detection;
            this.shape = shape;
        }

        @Override
        public String toString() {
            return event.toString();
        }
    }

    public static class T {

        public final E lower;
        public final E upper;

        public T(E lower, E upper) {
            this.lower = lower;
            this.upper = upper;
        }

        public E select() {
            if (lower != null && upper != null) {
                return Event.compare(lower.event, upper.event) <= 0 ? lower : upper;
            }

            return lower != null ? lower : upper;
        }
    }

}
