package com.github.rthoth.ginsu;

import com.github.rthoth.ginsu.detection.Detection;
import com.github.rthoth.ginsu.detection.DetectionShape;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

import java.util.Optional;

public abstract class GeometrySlicer<G extends Geometry> {

    protected final GeometryFactory factory;

    public GeometrySlicer(GeometryFactory factory) {
        this.factory = factory;
    }

    public abstract MultiShape apply(DetectionShape shape, Dimension dimension, double offset);

    public abstract boolean isPolygon();

    public abstract Optional<Shape> preApply(Detection detection, Shape shape);

    public abstract G toGeometry(MultiShape multishape);
}
