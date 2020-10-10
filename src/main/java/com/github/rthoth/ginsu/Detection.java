package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.CoordinateSequence;
import org.pcollections.PVector;

public final class Detection {

    public final Seq events;
    public final CoordinateSequence sequence;
    public final boolean startsInside;
    public final boolean isRing;

    public Detection(CoordinateSequence sequence, PVector<Event> events, boolean isRing, boolean startsInside) {
        this.events = new Seq(events, isRing);
        this.sequence = sequence;
        this.startsInside = startsInside;
        this.isRing = isRing;
    }

    public static final class Seq extends AbstractSeq<Event> {

        public Seq(PVector<Event> vector, boolean isClosed) {
            super(vector, isClosed);
        }
    }
}
