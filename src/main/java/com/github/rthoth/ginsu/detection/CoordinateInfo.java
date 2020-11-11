package com.github.rthoth.ginsu.detection;

import com.github.rthoth.ginsu.Slice;
import org.locationtech.jts.geom.Coordinate;

final class CoordinateInfo {

    int index;
    Coordinate coordinate;
    int position;

    public void copyFrom(CoordinateInfo next) {
        index = next.index;
        coordinate = next.coordinate;
        position = next.position;
    }

    public EventInfo newInfo() {
        var info = new EventInfo();
        info.index = index;
        info.coordinate = coordinate;
        return info;
    }

    public void update(int newIndex, Coordinate newCoordinate, Slice slice) {
        index = newIndex;
        coordinate = newCoordinate;
        position = slice.positionOf(newCoordinate);
    }
}
