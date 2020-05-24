package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.pcollections.PVector;

public abstract class GeometrySlicer<G extends Geometry> {

    protected final GeometryFactory factory;

    public GeometrySlicer(GeometryFactory factory) {
        this.factory = factory;
    }

    public abstract G apply(MultiShape multishape);

    public abstract SDetection.Status classify(SDetection detection, Shape shape);

    public abstract MultiShape slice(PVector<SDetection> detections);
}
