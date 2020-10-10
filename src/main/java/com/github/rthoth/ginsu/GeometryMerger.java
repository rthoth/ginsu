package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.Geometry;
import org.pcollections.PCollection;
import org.pcollections.PVector;

import java.util.Optional;

public abstract class GeometryMerger<T extends Geometry> {

    public abstract T apply(PCollection<DetectionShape> shapes, PVector<Knife.X> x, PVector<Knife.Y> y);

    public abstract boolean isPolygon();

    public abstract Optional<Shape> preApply(Detection detection, Shape shape);
}
