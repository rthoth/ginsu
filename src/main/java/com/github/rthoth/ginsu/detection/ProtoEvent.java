package com.github.rthoth.ginsu.detection;

import com.github.rthoth.ginsu.Dimension;
import com.github.rthoth.ginsu.Event;
import com.github.rthoth.ginsu.Slice;
import org.locationtech.jts.geom.Coordinate;

final class ProtoEvent {

    Type type = Type.UNDEFINED;

    int index;
    boolean isReference = true;
    Coordinate coordinate;

    Dimension dimension;
    Event.Side xSide = Event.Side.UNDEFINED;
    Event.Side ySide = Event.Side.UNDEFINED;

    int position;

    public Event createEvent(Event.Factory factory) {
        return factory.create(type.underlying, index, isReference ? null : coordinate, dimension, xSide, ySide);
    }

    void update(Slice slice) {
        position = slice.positionOf(coordinate);
    }

    void update(Type newType, int newIndex, Coordinate newCoordinate, Dimension newDimension, int border) {
        type = newType;
        index = newIndex;
        isReference = false;
        coordinate = newCoordinate;
        update(newDimension, border, true);
    }

    void update(Dimension newDimension, int border, boolean pointUpdated) {
        if (pointUpdated) {
            dimension = newDimension;
        } else {
            dimension = dimension == null ? newDimension : Dimension.CORNER;
        }
        updateSide(newDimension, border);
    }

    public void update(Type newType, Dimension newDimension, int border) {
        type = newType;
        update(newDimension, border, false);
    }

    public void update(Type newType) {
        type = newType;
    }

    void updateSide(Dimension newDimension, int border) {
        if (newDimension == Dimension.X)
            xSide = Slice.sideOf(border);
        else if (newDimension == Dimension.Y)
            ySide = Slice.sideOf(border);
    }

    enum Type {

        UNDEFINED(null), IN(Event.Type.IN), OUT(Event.Type.OUT), CANDIDATE(Event.Type.OUT), CORNER(Event.Type.CORNER);

        public final Event.Type underlying;

        Type(Event.Type underlying) {
            this.underlying = underlying;
        }
    }
}
