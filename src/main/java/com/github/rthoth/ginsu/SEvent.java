package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.CoordinateSequence;

import javax.validation.constraints.NotNull;

/**
 * Slice Event
 */
public final class SEvent {

    private static final int IN = 1;
    private static final int OUT = 2;

    public final int index;
    public final CoordinateSequence sequence;
    public final int kind;
    public final Slice.Location location;

    private SEvent(CoordinateSequence sequence, int index, int kind, Slice.Location location) {
        this.sequence = sequence;
        this.index = index;
        this.kind = kind;
        this.location = location;
    }

    public static boolean isIn(SEvent event) {
        return event != null && event.kind == IN;
    }

    @SuppressWarnings("unused")
    public static boolean isOut(SEvent event) {
        return event != null && event.kind == OUT;
    }

    public int border() {
        return location.border;
    }

    public Double ordinate() {
        return location.ordinate;
    }

    public int sequenceSize() {
        return sequence.size();
    }

    @Override
    public String toString() {
        return String.format("%s(%d, %s, %s)", kind == IN ? "In" : "Out", index, location, location.coordinate != null ? "(" + location.coordinate.getX() + ", " + location.coordinate.getY() + ")" : "null");
    }

    public static class Factory {

        private final CoordinateSequence sequence;

        public Factory(CoordinateSequence sequence) {
            this.sequence = sequence;
        }

        public CoordinateSequence getCoordinateSequence() {
            return sequence;
        }


        public SEvent newIn(int index, @NotNull Slice.Location location) {
            return new SEvent(sequence, index, IN, location);
        }

        public SEvent newOut(int index, @NotNull Slice.Location location) {
            return new SEvent(sequence, index, OUT, location);
        }
    }
}
