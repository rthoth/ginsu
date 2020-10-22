package com.github.rthoth.ginsu;

import com.github.rthoth.ginsu.Knife.X;
import com.github.rthoth.ginsu.Knife.Y;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygonal;
import org.pcollections.PVector;

public class Merger {

    private final PVector<X> x;
    private final PVector<Y> y;
    private final double offset;

    public Merger(double[] x, double[] y) {
        this(x, y, Ginsu.DEFAULT_OFFSET);
    }

    public Merger(double[] x, double[] y, double offset) {
        this(Ginsu.map(x, v -> new X(v, offset, 0D)), Ginsu.map(y, v -> new Y(v, offset, 0D)), offset);
    }

    protected Merger(PVector<X> x, PVector<Y> y, double offset) {
        this.x = x;
        this.y = y;
        this.offset = offset;
    }

    @SuppressWarnings("unused")
    public <T extends Polygonal> MultiPolygon polygonal(Grid<T> grid, GeometryFactory factory) {
        return new MergeGrid<>(x, y, new PolygonMerger(factory, offset)).apply(grid.view(MultiShape::of));
    }
}
