package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygonal;
import org.pcollections.PVector;

import java.util.Objects;

public class Slicer {

    private final PVector<Knife.X> x;
    private final PVector<Knife.Y> y;

    public Slicer(double[] x, double[] y) {
        this(x, y, Ginsu.DEFAULT_OFFSET, 0D);
    }

    @SuppressWarnings("unused")
    public Slicer(double[] x, double[] y, double offset, double extrusion) {
        var absOffset = Math.abs(offset);
        Ginsu.isAscendant(x, absOffset);
        Ginsu.isAscendant(y, absOffset);
        this.x = Ginsu.toVector(x, v -> new Knife.X(v, absOffset, extrusion));
        this.y = Ginsu.toVector(y, v -> new Knife.Y(v, absOffset, extrusion));
    }

    public Slicer(PVector<Knife.X> x, PVector<Knife.Y> y) {
        this.x = x;
        this.y = y;
    }

    public Slicer extrude(double extrusion) {
        return new Slicer(Ginsu.map(x, k -> k.extrude(extrusion)), Ginsu.map(y, k -> k.extrude(extrusion)));
    }

    public Merger merger() {
        return new Merger(x, y);
    }

    public Grid<MultiPolygon> polygonal(Polygonal polygonal) {
        if (polygonal instanceof Geometry) {
            return new SlicerGrid<>(x, y, new PolygonalSlicer(((Geometry) polygonal).getFactory()))
                    .apply(MultiShape.of(polygonal));
        } else {
            throw new GinsuException.InvalidArgument(Objects.toString(polygonal));
        }
    }
}
