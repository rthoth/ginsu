package com.github.rthoth.ginsu;

import org.pcollections.PVector;
import org.pcollections.TreePVector;

public abstract class MShape {

    public static class Detection {

        public static final int INSIDE = 1;
        public static final int OUTSIDE = 0;
        public static final int BORDER = -1;
        public final PVector<MEvent> events;
        public final boolean isRing;
        public final int location;
        public final MEvent.Factory factory;

        public Detection(PVector<MEvent> events, boolean isRing, int location, MEvent.Factory factory) {
            this.events = events;
            this.isRing = isRing;
            this.location = location;
            this.factory = factory;
        }
    }

    public static class Done extends MShape {

        public final Shape shape;

        public Done(Shape shape) {
            this.shape = shape;
        }
    }

    public static class Ongoing extends MShape {

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
