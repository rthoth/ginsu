package com.github.rthoth.ginsu.maze;

import org.locationtech.jts.geom.Coordinate;

public class Q {

    final Coordinate coordinate;

    public Q(Coordinate coordinate) {
        this.coordinate = coordinate;
    }

    @Override
    public String toString() {
        return "Q(" + coordinate.toString() + ")";
    }
}
