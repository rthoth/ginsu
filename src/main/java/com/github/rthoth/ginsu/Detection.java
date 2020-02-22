package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.CoordinateSequence;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

public class Detection {

    public static final int BORDER = 8;
    public static final int INSIDE = 16;
    public static final int OUTSIDE = 32;
    public final int firstLocation;
    public final PVector<Event> events;
    public final CoordinateSequence sequence;
    public final boolean isRing;

    public Detection(PVector<Event> events, boolean isRing, int firstLocation, Event.Factory factory) {
        this.events = events;
        this.sequence = factory.getCoordinateSequence();
        this.firstLocation = firstLocation;
        this.isRing = isRing;
    }

    public static abstract class Status {

        public abstract void add(Detection detection);
    }

    public static class Ready extends Status {

        public final Shape shape;

        public Ready(Shape shape) {
            this.shape = shape;
        }

        @Override
        public void add(Detection detection) {
            throw new UnsupportedOperationException();
        }
    }

    public static class Unready extends Status {

        private PVector<Detection> detections;

        public Unready(Detection detection) {
            this.detections = TreePVector.singleton(detection);
        }

        @Override
        public void add(Detection detection) {
            detections = detections.plus(detection);
        }

        public PVector<Detection> getDetections() {
            return detections;
        }
    }
}
