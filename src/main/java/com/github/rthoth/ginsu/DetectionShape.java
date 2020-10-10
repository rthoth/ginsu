package com.github.rthoth.ginsu;

import org.pcollections.PVector;

public abstract class DetectionShape {

    public static DetectionShape of(PVector<Detection> detections) {
        return new Ongoing(detections);
    }

    public static DetectionShape of(Shape shape) {
        return new Done(shape);
    }

    public abstract PVector<Detection> getDetections();

    public abstract Shape getShape();

    public abstract boolean isOngoing();

    private static class Done extends DetectionShape {

        private final Shape shape;

        public Done(Shape shape) {
            this.shape = shape;
        }

        @Override
        public PVector<Detection> getDetections() {
            throw new GinsuException.Unsupported();
        }

        @Override
        public Shape getShape() {
            return shape;
        }

        @Override
        public boolean isOngoing() {
            return false;
        }
    }

    private static class Ongoing extends DetectionShape {

        private final PVector<Detection> detections;

        public Ongoing(PVector<Detection> detections) {
            this.detections = detections;
        }

        @Override
        public PVector<Detection> getDetections() {
            return detections;
        }

        @Override
        public Shape getShape() {
            throw new GinsuException.Unsupported();
        }

        @Override
        public boolean isOngoing() {
            return true;
        }
    }
}
