package com.github.rthoth.ginsu;

import org.pcollections.PVector;

public class Slicer {

    private final PVector<Knife.X> x;
    private final PVector<Knife.Y> y;

    public Slicer(double[] x, double[] y) {
        this(x, y, Ginsu.DEFAULT_OFFSET, 0D);
    }

    @SuppressWarnings("unused")
    public Slicer(double[] x, double[] y, double offset, double extrusion) {
        var finalOffset = Math.abs(offset);
        Ginsu.isAscendant(x, finalOffset);
        Ginsu.isAscendant(y, finalOffset);
        this.x = Ginsu.toVector(x, v -> new Knife.X(v, finalOffset, extrusion));
        this.y = Ginsu.toVector(y, v -> new Knife.Y(v, finalOffset, extrusion));
    }

    private Slicer(PVector<Knife.X> x, PVector<Knife.Y> y) {
        this.x = x;
        this.y = y;
    }

    public Merger merger() {
        return new Merger(x, y);
    }
}
