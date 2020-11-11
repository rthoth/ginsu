package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.Coordinate;

public enum Dimension {
    X(1), Y(-1), CORNER(0);

    public final int value;

    Dimension(int value) {
        this.value = value;
    }

    public int border(Event event) {
        switch (this) {
            case X:
                return Side.border(event.xSide);

            case Y:
                return Side.border(event.ySide);

            default:
                throw new GinsuException.IllegalArgument("Invalid dimension!");
        }
    }

    public double ordinateOf(Coordinate coordinate) {
        switch (this) {
            case X:
                return coordinate.getY();
            case Y:
                return coordinate.getX();
            default:
                throw new GinsuException.IllegalState("Invalid dimension!");
        }
    }

    public Dimension other() {
        switch (this) {
            case X:
                return Y;
            case Y:
                return X;
            default:
                throw new GinsuException.IllegalState();
        }
    }

    public Side sideOf(Event event) {
        switch (this) {
            case X:
                return event.xSide;

            case Y:
                return event.ySide;

            default:
                throw new GinsuException.IllegalState("Invalid dimension!");
        }
    }

    public enum Side {

        LESS, GREATER, UNDEFINED;

        public static int border(Side side) {
            if (side == LESS)
                return Slice.UPPER_BORDER;

            if (side == GREATER)
                return Slice.LOWER_BORDER;

            throw new GinsuException.IllegalArgument("Invalid side: " + side);
        }
    }
}
