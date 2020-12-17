package com.github.rthoth.ginsu;

import org.pcollections.PVector;

public class Merger {

    private final PVector<Knife.X> x;
    private final PVector<Knife.Y> y;

    @SuppressWarnings("unused")
    public Merger(double[] x, double[] y, double offset, double extrusion) {
        var finalOffset = Math.abs(offset);
        Ginsu.isAscendant(x, finalOffset);
        Ginsu.isAscendant(y, finalOffset);
        this.x = Ginsu.toVector(x, v -> new Knife.X(v, finalOffset, extrusion));
        this.y = Ginsu.toVector(y, v -> new Knife.Y(v, finalOffset, extrusion));
    }

    public Merger(PVector<Knife.X> x, PVector<Knife.Y> y) {
        this.x = x;
        this.y = y;
    }
}
