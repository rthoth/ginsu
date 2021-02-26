package com.github.rthoth.ginsu;

import com.github.rthoth.ginsu.detection.MultiCell;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;

public class PolygonalSlicer extends GeometrySlicer<MultiPolygon> {

    public PolygonalSlicer(GeometryFactory factory) {
        super(factory);
    }

    @Override
    public MultiPolygon empty() {
        return factory.createMultiPolygon();
    }

    @Override
    public boolean hasCorner() {
        return true;
    }

    @Override
    public boolean hasTouch() {
        return true;
    }

    @Override
    public MultiPolygon slice(MultiCell multiCell) {
        return null;
    }
}
