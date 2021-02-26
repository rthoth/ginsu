package com.github.rthoth.ginsu;

import com.github.rthoth.ginsu.detection.MultiCell;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

public abstract class GeometrySlicer<T extends Geometry> {

    protected final GeometryFactory factory;

    public GeometrySlicer(GeometryFactory factory) {
        this.factory = factory;
    }

    public abstract T empty();

    public abstract boolean hasCorner();

    public abstract boolean hasTouch();

    public abstract T slice(MultiCell multiCell);
}
