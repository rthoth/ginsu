package com.github.rthoth.ginsu.detection;

import com.github.rthoth.ginsu.AbstractSeq;
import com.github.rthoth.ginsu.Event;
import com.github.rthoth.ginsu.Ginsu;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.pcollections.PVector;

import java.util.Arrays;

public final class Detection {

    public static final CornerSet EMPTY_CORNER_SET = new CornerSet(null, null, null, null);

    public final Seq events;
    public final CoordinateSequence sequence;
    public final boolean startsInside;
    public final boolean isRing;
    public final CornerSet cornerSet;

    public Detection(CoordinateSequence sequence, PVector<Event> events, boolean isRing, boolean startsInside, CornerSet cornerSet) {
        this.events = new Seq(events, isRing);
        this.sequence = sequence;
        this.startsInside = startsInside;
        this.isRing = isRing;
        this.cornerSet = cornerSet;
    }

    public Coordinate getNextInsideCoordinate(Event event) {
        if (event.coordinate != null) {
            if (Event.isIn(event)) {
                return event.sequence.getCoordinate(event.index);
            } else {
                return Ginsu.previous(event.index, sequence, isRing);
            }
        } else {
            return Event.isIn(event) ? Ginsu.next(event.index, sequence, isRing) : Ginsu.previous(event.index, sequence, isRing);
        }
    }

    public boolean nonEmpty() {
        return events.nonEmpty() || cornerSet.nonEmpty();
    }

    public static final class CornerSet {

        public final Event ll;
        public final Event ul;
        public final Event uu;
        public final Event lu;

        public CornerSet(Event ll, Event ul, Event uu, Event lu) {
            this.ll = ll;
            this.ul = ul;
            this.uu = uu;
            this.lu = lu;
        }

        public static CornerSet of(Event ll, Event ul, Event uu, Event lu) {
            return ll != null || ul != null || uu != null || lu != null ? new CornerSet(ll, ul, uu, lu) : EMPTY_CORNER_SET;
        }

        public Iterable<Event> iterable() {
            return Arrays.asList(ll, ul, uu, lu);
        }

        public boolean nonEmpty() {
            return ll != null || ul != null || uu != null || lu != null;
        }

        public CornerSet withLL(Event event) {
            return new CornerSet(event, ul, uu, lu);
        }

        public CornerSet withLU(Event event) {
            return new CornerSet(ll, ul, uu, event);
        }

        public CornerSet withUL(Event event) {
            return new CornerSet(ll, event, uu, lu);
        }

        public CornerSet withUU(Event event) {
            return new CornerSet(ll, ul, event, lu);
        }
    }

    public static final class Seq extends AbstractSeq<Event> {

        public Seq(PVector<Event> vector, boolean isClosed) {
            super(vector, isClosed);
        }
    }
}
