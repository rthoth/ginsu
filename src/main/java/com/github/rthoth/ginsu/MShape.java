package com.github.rthoth.ginsu;

import org.pcollections.PVector;
import org.pcollections.TreePVector;

public abstract class MShape {

    public abstract Iterable<Detection> getDetections();

    public abstract boolean hasContent();

    public abstract boolean isDone();

    interface Result {

        MShape apply();
    }

    public static class Detection {

        public static final int INSIDE = 1;
        public static final int OUTSIDE = 0;
        public static final int BORDER = -1;
        public final Seq events;
        public final boolean isRing;
        public final int location;
        public final MEvent.Factory factory;

        public Detection(PVector<MEvent> events, boolean isRing, int location, MEvent.Factory factory) {
            this.events = new Seq(events, isRing);
            this.isRing = isRing;
            this.location = location;
            this.factory = factory;
        }
    }

    private static class Done extends MShape {

        private final Shape shape;

        public Done(Shape shape) {
            this.shape = shape;
        }

        @Override
        public Iterable<Detection> getDetections() {
            throw new GinsuException.Unsupported();
        }

        @Override
        public boolean hasContent() {
            return shape.nonEmpty();
        }

        @Override
        public boolean isDone() {
            return true;
        }
    }

    public static class DoneResult implements Result {

        public final Shape shape;

        public DoneResult(Shape shape) {
            this.shape = shape;
        }

        @Override
        public MShape apply() {
            return new Done(shape);
        }
    }

    public static class OngoingResult implements Result {

        private PVector<Detection> detections;

        public OngoingResult(Detection detection) {
            detections = TreePVector.singleton(detection);
        }

        public void add(Detection detection) {
            detections = detections.plus(detection);
        }

        @Override
        public MShape apply() {
            return new Some(detections);
        }

        public PVector<Detection> getDetections() {
            return detections;
        }

    }

    public static class Seq extends AbstractSeq<MEvent> {

        public Seq(PVector<MEvent> events, boolean closed) {
            super(events, closed);
        }
    }

    private static class Some extends MShape {

        private final PVector<Detection> detections;

        public Some(PVector<Detection> detections) {
            this.detections = detections;
        }

        @Override
        public Iterable<Detection> getDetections() {
            return detections;
        }

        @Override
        public boolean hasContent() {
            return false;
        }

        @Override
        public boolean isDone() {
            return false;
        }
    }
}
