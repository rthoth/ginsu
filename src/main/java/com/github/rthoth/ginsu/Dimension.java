package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.Coordinate;

public enum Dimension {
    X(1), Y(-1), CORNER(0);

    public final int value;

    Dimension(int value) {
        this.value = value;
    }

    public Dimension complement() {
        switch (this) {
            case X:
                return Y;
            case Y:
                return X;
            default:
                throw new GinsuException.IllegalState();
        }
    }

    public double positionalOf(Coordinate coordinate) {
        switch (this) {
            case X:
                return coordinate.getY();
            case Y:
                return coordinate.getX();
            default:
                throw new GinsuException.IllegalState("Invalid dimension!");
        }
    }
}
