package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.Geometry;
import org.pcollections.PVector;

public abstract class GeometryMerger<T extends Geometry> {

    public abstract MShape classify(MShape.Detection detection, Shape shape);

    public abstract T merge(PVector<MShape.Detection> detections, PVector<Shape> shapes);
}
