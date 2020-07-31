package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.Geometry;
import org.pcollections.PVector;

public abstract class GeometryMerger<T extends Geometry> {

    public abstract T apply(PVector<MShape> mshapes);

    public abstract MShape.Result classify(MShape.Detection detection, Shape shape);
}
