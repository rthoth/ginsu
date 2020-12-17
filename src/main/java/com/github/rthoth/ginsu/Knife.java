package com.github.rthoth.ginsu;

public abstract class Knife {

    public final double value;
    public final double offset;
    public final double extrusion;
    public final Dimension dimension;

    public Knife(double value, double offset, double extrusion, Dimension dimension) {
        this.value = value;
        this.offset = offset;
        this.extrusion = extrusion;
        this.dimension = dimension;
    }

    public static class X extends Knife {

        public X(double value, double offset, double extrusion) {
            super(value, offset, extrusion, Dimension.X);
        }
    }

    public static class Y extends Knife {

        public Y(double value, double offset, double extrusion) {
            super(value, offset, extrusion, Dimension.Y);
        }
    }
}
