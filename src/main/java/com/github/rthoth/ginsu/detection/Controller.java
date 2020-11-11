package com.github.rthoth.ginsu.detection;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;

public abstract class Controller {

    public abstract void apply(EventInfo eventInfo);

    public abstract void begin(Coordinate coordinate);

    public abstract void compute();

    public abstract Detection end(boolean isRing);

    protected abstract Recorder getRecorder();

    public abstract CoordinateSequence getSequence();

    protected abstract double getKnifeValue();

    public abstract boolean isChanged();

    public abstract void next();

    public abstract boolean startsInside();

    public abstract void update(int index, Coordinate coordinate);
}
