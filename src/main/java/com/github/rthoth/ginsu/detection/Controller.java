package com.github.rthoth.ginsu.detection;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;

public abstract class Controller {

    public abstract void apply(ProtoEvent protoEvent);

    public abstract void begin(Coordinate coordinate);

    public abstract void compute();

    public abstract Detection end(boolean isRing);

    public abstract CoordinateSequence getSequence();

    public abstract boolean isChanged();

    public abstract void next();

    public abstract boolean startsInside();

    public abstract void update(int index, Coordinate coordinate);
}
