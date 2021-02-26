package com.github.rthoth.ginsu;

public abstract class DoubleRange {


    public static class Range {

        private final double min;
        private final double max;

        public Range(double min, double max) {
            this.min = min;
            this.max = max;
        }
    }
}
