package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.CoordinateSequence;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

public abstract class SShape {

    public static class Detection {

        public static final int BORDER = 8;
        public static final int INSIDE = 16;
        public static final int OUTSIDE = 32;
        public final int location;
        public final PVector<SEvent> events;
        public final CoordinateSequence sequence;
        public final boolean isRing;

        public Detection(PVector<SEvent> events, boolean isRing, int location, SEvent.Factory factory) {
            this.events = events;
            this.sequence = factory.getCoordinateSequence();
            this.location = location;
            this.isRing = isRing;
        }
    }

    public static class Done extends SShape {

        public final Shape shape;

        public Done(Shape shape) {
            this.shape = shape;
        }
    }

    public static class Ongoing extends SShape {

        private PVector<Detection> detections;

        public Ongoing(Detection detection) {
            detections = TreePVector.singleton(detection);
        }

        public void add(Detection detection) {
            detections = detections.plus(detection);
        }

        public PVector<Detection> getDetections() {
            return detections;
        }
    }
}
