package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.Coordinate;

public enum Dimension {

    X(Coordinate::getY),

    Y(Coordinate::getX);

    public final Info info;

    Dimension(Info info) {
        this.info = info;
    }

    public interface Info {

        Double ordinate(Coordinate coordinate);
    }
}
