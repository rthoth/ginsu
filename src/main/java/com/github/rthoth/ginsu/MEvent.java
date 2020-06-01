package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;

public class MEvent {

    public static final int IN = 1;
    public static final int OUT = 2;

    protected final Factory factory;
    protected final int index;
    protected final Coordinate coordinate;
    protected final int type;

    public MEvent(Factory factory, int index, Coordinate coordinate, int type) {
        this.factory = factory;
        this.index = index;
        this.coordinate = coordinate;
        this.type = type;
    }

    @Override
    public String toString() {
        return (type == IN ? "In" : "Out") + "(" + index + ", " + coordinate + ")";
    }

    public Coordinate getCoordinate() {
        return coordinate != null ? coordinate : factory.sequence.getCoordinate(index);
    }

    public static class Factory {

        private final CoordinateSequence sequence;

        public Factory(CoordinateSequence sequence) {
            this.sequence = sequence;
        }

        public MEvent newIn(int index, Coordinate coordinate) {
            return new MEvent(this, index, coordinate, IN);
        }

        public MEvent newOut(int index, Coordinate coordinate) {
            return new MEvent(this, index, coordinate, OUT);
        }
    }
}
