package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;

import java.util.Objects;

public final class Event {

    public static final int NO_INDEX = -1;
    public static final int CORNER_INDEX = -2;

    public final int index;
    public final CoordinateSequence sequence;
    public final Type type;
    public final Coordinate coordinate;
    public final Dimension dimension;
    public final Side xSide;
    public final Side ySide;

    private Event(Type type, CoordinateSequence sequence, int index, Coordinate coordinate, Dimension dimension, Side xSide, Side ySide) {
        this.sequence = sequence;
        this.type = type;
        this.index = index;
        this.coordinate = coordinate;
        this.dimension = dimension;
        this.xSide = xSide;
        this.ySide = ySide;
    }

    public static int compare(Event e1, Event e2) {
        if (e1.type != e2.type) {
            return e1.type == Type.IN ? -1 : 1;
        } else {
            return e1.type == Type.IN ? Integer.compare(e1.index, e2.index) : -Integer.compare(e1.index, e2.index);
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

    public static String toString(Coordinate coordinate) {
        return coordinate != null ? "(" + coordinate.getX() + ", " + coordinate.getY() + ")" : "null";
    }

    public int getBorder(Dimension dimension) {
        return getSide(dimension).border;
    }

    public Coordinate getCoordinate() {
        return coordinate != null ? coordinate : sequence.getCoordinate(index);
    }

    public CoordinateSequence getSequence() {
        return sequence;
    }

    public Side getSide(Dimension dimension) {
        if (dimension == Dimension.X)
            return xSide;
        if (dimension == Dimension.Y)
            return ySide;

        throw new GinsuException.IllegalArgument("Invalid dimension!");
    }

    public double positional(Dimension dimension) {
        if (dimension == Dimension.X)
            return getCoordinate().getY();
        if (dimension == Dimension.Y)
            return getCoordinate().getX();

        throw new GinsuException.IllegalArgument("Invalid dimension [" + dimension + "]!");
    }

    @Override
    public String toString() {
        if (type != Type.CORNER) {
            return type.name + "." + dimension + "(" + index + ", " + toString(coordinate) + ")";
        } else {
            return "Corner(" + index + ", " + toString(coordinate) + ")";
        }
    }

    public enum Side {
        LESS(Slice.UPPER_BORDER), GREAT(Slice.LOWER_BORDER), UNDEFINED(0);

        public final int border;

        Side(int border) {
            this.border = border;
        }
    }

    public enum Type {
        IN("In"), OUT("Out"), CORNER("Corner");

        public final String name;

        Type(String name) {
            this.name = name;
        }
    }

    public static class Factory {

        private final CoordinateSequence sequence;

        public Factory(CoordinateSequence sequence) {
            this.sequence = sequence;
        }

        public Event create(Type type, int index, Coordinate coordinate, Dimension dimension, Side xSide, Side ySide) {

            if (index < 0 && index != NO_INDEX && index != CORNER_INDEX)
                throw new GinsuException.InvalidIndex(index);

            Objects.requireNonNull(type, "A type is required!");
            Objects.requireNonNull(dimension, "A dimension is required!");
            Objects.requireNonNull(xSide, "xSide doesn't be null!");
            Objects.requireNonNull(ySide, "ySide doesn't be null!");

            return new Event(type, sequence, index, coordinate, dimension, xSide, ySide);
        }

        public Coordinate getCoordinate(int index) {
            return sequence.getCoordinate(index);
        }

        public CoordinateSequence getSequence() {
            return sequence;
        }
    }
}
