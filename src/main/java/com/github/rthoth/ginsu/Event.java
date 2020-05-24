package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.CoordinateSequence;

import javax.validation.constraints.NotNull;

public final class Event {

    private static final int IN = 1;
    private static final int OUT = 2;

    public final int index;
    public final CoordinateSequence sequence;
    public final int kind;
    public final Cell.Intersection intersection;

    private Event(CoordinateSequence sequence, int index, int kind, Cell.Intersection intersection) {
        this.sequence = sequence;
        this.index = index;
        this.kind = kind;
        this.intersection = intersection;
    }

    public static boolean isIn(Event event) {
        return event != null && event.kind == IN;
    }

    @SuppressWarnings("unused")
    public static boolean isOut(Event event) {
        return event != null && event.kind == OUT;
    }

    public int border() {
        return intersection.border;
    }

    public Double ordinate() {
        return intersection.ordinate;
    }

    @Override
    public String toString() {
        return String.format("%s(%d, %s, %s)", kind == IN ? "In" : "Out", index, intersection, intersection.coordinate != null ? "(" + intersection.coordinate.getX() + ", " + intersection.coordinate.getY() + ")" : "null");
    }

    public int sequenceSize() {
        return sequence.size();
    }

    public static class Factory {

        private final CoordinateSequence sequence;

        public Factory(CoordinateSequence sequence) {
            this.sequence = sequence;
        }

        public CoordinateSequence getCoordinateSequence() {
            return sequence;
        }


        public Event newIn(int index, @NotNull Cell.Intersection intersection) {
            return new Event(sequence, index, IN, intersection);
        }

        public Event newOut(int index, @NotNull Cell.Intersection intersection) {
            return new Event(sequence, index, OUT, intersection);
        }
    }
}
