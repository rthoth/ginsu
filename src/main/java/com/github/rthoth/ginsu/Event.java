package com.github.rthoth.ginsu;

import com.github.rthoth.ginsu.Dimension.Side;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;

import java.util.Objects;

public final class Event {

    public final int index;
    public final Factory factory;
    public final Type type;
    public final Coordinate coordinate;
    public final Dimension dimension;
    public final Side xSide;
    public final Side ySide;

    private Event(Type type, Factory factory, int index, Coordinate coordinate, Dimension dimension, Side xSide, Side ySide) {
        this.factory = factory;
        this.type = type;
        this.index = index;
        this.coordinate = coordinate;
        this.dimension = dimension;
        this.xSide = xSide;
        this.ySide = ySide;
    }

    public static int compare(Event e1, Event e2) {
        if (e1.type != e2.type) {
            if (e1.type == Type.IN || e2.type == Type.CORNER)
                return -1;

            if (e2.type == Type.IN || e1.type == Type.CORNER)
                return 1;

            return e1.type == Type.OUT ? -1 : 1;
        } else {
            if (e1.type == Type.IN || e1.type == Type.CORNER)
                return Integer.compare(e1.index, e2.index);

            return -Integer.compare(e1.index, e2.index);
        }
    }

    public static boolean isCorner(Event event) {
        return event != null && event.type == Type.CORNER;
    }

    public static boolean isIn(Event event) {
        return event != null && event.type == Type.IN;
    }

    public static boolean isNonCorner(Event event) {
        return event != null && event.type != Type.CORNER;
    }

    public Coordinate getCoordinate() {
        return coordinate != null ? coordinate : factory.getCoordinate(index);
    }

    public CoordinateSequence getSequence() {
        return factory.sequence;
    }

    @Override
    public String toString() {
        String prefix;

        if (type == Type.IN)
            prefix = "In." + dimension.toString();
        else if (type == Type.OUT)
            prefix = "Out." + dimension.toString();
        else
            prefix = "Corner";

        return prefix + "(" + index + ", " + coordinate + ")";
    }

    enum Type {
        IN, OUT, CORNER
    }

    public static class Factory {

        private final CoordinateSequence sequence;

        public Factory(CoordinateSequence sequence) {
            this.sequence = sequence;
        }

        public Event create(Type type, int index, Coordinate coordinate, Dimension dimension, Side xSide, Side ySide) {
            Objects.requireNonNull(type);
            Objects.requireNonNull(dimension);
            Objects.requireNonNull(xSide);
            Objects.requireNonNull(ySide);

            return new Event(type, this, index, coordinate, dimension, xSide, ySide);
        }

        public Coordinate getCoordinate(int index) {
            return sequence.getCoordinate(index);
        }

        public CoordinateSequence getSequence() {
            return sequence;
        }
    }
}
