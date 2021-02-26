package com.github.rthoth.ginsu.detection;

import com.github.rthoth.ginsu.GinsuException;
import com.github.rthoth.ginsu.Knife;
import com.github.rthoth.ginsu.Shape;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;

import java.util.Comparator;

public class Event {

    public static final Type IN = Type.IN;
    public static final Type OUT = Type.OUT;
    public static final Type TOUCH = Type.TOUCH;

    public static final Comparator<Event> PREVIOUS_COMPARATOR = (e1, e2) -> e1.previous != e2.previous ?
            Integer.compare(e1.previous, e2.previous) : Integer.compare(e1.index, e2.index);

    public final Type type;
    public final int index;
    public final int previous;
    public final KnifeWrapper knife;
    public final Coordinate coordinate;
    public final Factory factory;

    public Event(Type type, int index, int previous, KnifeWrapper knife, Coordinate coordinate, Factory factory) {
        this.type = type;
        this.index = index;
        this.previous = previous;
        this.knife = knife;
        this.coordinate = coordinate;
        this.factory = factory;
    }

    public static Factory factory(Shape shape, CoordinateSequence sequence) {
        return new Factory(shape, sequence);
    }

    public Coordinate getCoordinate() {
        return coordinate != null ? coordinate : factory.getCoordinate(index);
    }

    public Event merge(Event other) {
        var wrapper = new XYWrapper(knife.getUnderlying(), other.knife.getUnderlying());
        var newCoordinate = coordinate != null ? coordinate : other.coordinate;

        if (type == other.type)
            return new Event(type, index, previous, wrapper, newCoordinate, factory);
        else if (type == Event.TOUCH || other.type == Event.TOUCH)
            return new Event(type != Event.TOUCH ? type : other.type, index, previous, wrapper, newCoordinate, factory);

        throw new GinsuException.InvalidState(String.format("These events are opposite close to [%s]!", getCoordinate()));
    }

    public double ordinate() {
        return knife.ordinate(getCoordinate());
    }

    @Override
    public String toString() {
        return String.format("Event(%s, %d, %d, %s)", type, index, previous, coordinate != null ? String.format("L[%s]", coordinate) : String.format("R[%s]", factory.getCoordinate(index)));
    }

    public Event withType(Type newType) {
        return new Event(newType, index, previous, knife, coordinate, factory);
    }

    public enum Type {IN, OUT, TOUCH}

    public static class Factory {

        public final Shape shape;
        public final CoordinateSequence sequence;

        private Factory(Shape shape, CoordinateSequence sequence) {
            this.shape = shape;
            this.sequence = sequence;
        }

        public Event create(Type type, int index, int previous, Knife knife, Coordinate coordinate) {
            return new Event(type, index, previous, new SWrapper(knife), coordinate, this);
        }

        public Coordinate getCoordinate(int index) {
            return sequence.getCoordinate(index);
        }
    }

    public static abstract class KnifeWrapper {

        public abstract boolean contains(Knife knife);

        public abstract Knife getUnderlying();

        public abstract double ordinate(Coordinate coordinate);
    }

    private static class SWrapper extends KnifeWrapper {

        private final Knife knife;

        public SWrapper(Knife knife) {
            this.knife = knife;
        }

        @Override
        public boolean contains(Knife knife) {
            return this.knife == knife;
        }

        @Override
        public Knife getUnderlying() {
            return knife;
        }

        @Override
        public double ordinate(Coordinate coordinate) {
            return knife.ordinate(coordinate);
        }
    }

    private static class XYWrapper extends KnifeWrapper {

        private final Knife x;
        private final Knife y;

        public XYWrapper(Knife x, Knife y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean contains(Knife knife) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Knife getUnderlying() {
            throw new UnsupportedOperationException();
        }

        @Override
        public double ordinate(Coordinate coordinate) {
            throw new UnsupportedOperationException();
        }
    }
}
