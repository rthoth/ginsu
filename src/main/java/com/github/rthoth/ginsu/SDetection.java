package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.CoordinateSequence;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

public class SDetection {

    public static final int BORDER = 8;
    public static final int INSIDE = 16;
    public static final int OUTSIDE = 32;
    public final int location;
    public final PVector<SEvent> events;
    public final CoordinateSequence sequence;
    public final boolean isRing;

    public SDetection(PVector<SEvent> events, boolean isRing, int location, SEvent.Factory factory) {
        this.events = events;
        this.sequence = factory.getCoordinateSequence();
        this.location = location;
        this.isRing = isRing;
    }

    public static abstract class Status {

        public abstract void add(SDetection detection);
    }

    public static class Ready extends Status {

        public final Shape shape;

        public Ready(Shape shape) {
            this.shape = shape;
        }

        @Override
        public void add(SDetection detection) {
            throw new UnsupportedOperationException();
        }
    }

    public static class Unready extends Status {

        private PVector<SDetection> detections;

        public Unready(SDetection detection) {
            this.detections = TreePVector.singleton(detection);
        }

        @Override
        public void add(SDetection detection) {
            detections = detections.plus(detection);
        }

        public PVector<SDetection> getDetections() {
            return detections;
        }
    }
}
