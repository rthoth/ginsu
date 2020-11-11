package com.github.rthoth.ginsu.detection;

import com.github.rthoth.ginsu.Event;
import com.github.rthoth.ginsu.GinsuException;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

class Recorder {

    final Event.Factory factory;
    final boolean hasCorner;

    private PVector<Event> events = TreePVector.empty();
    private Event candidate = null;
    private Event last = null;

    Recorder(Event.Factory factory, boolean hasCorner) {
        this.hasCorner = hasCorner;
        this.factory = factory;
    }

    private void add(Event event) {
        last = event;
        events = events.plus(event);
    }

    void addCandidate(Event event) {
        if (candidate == null) {
            candidate = event;
        } else {
            throw new GinsuException.TopologyException("Already exists a candidate!");
        }
    }

    void addCorner(Event event) {
        if (hasCorner) {
            if (candidate != null) {
                add(candidate);
                candidate = null;
            } else if (last == null || last.index != event.index) {
                add(event);
            }
        }
    }

    void addIn(Event event) {
        if (candidate == null) {
            add(event);
        } else {
            if (event.index >= 0) {
                if (candidate.index < event.index) {
                    add(candidate);
                    add(event);
                }
            } else {
                add(candidate);
                add(event);
            }

            candidate = null;
        }
    }

    void addOut(Event event) {
        if (candidate == null) {
            add(event);
        } else if (candidate.index == event.index) {
            add(candidate);
            candidate = null;
        } else {
            throw new GinsuException.IllegalState("Double output!");
        }
    }

    void apply(EventInfo eventInfo) {
        final var event = eventInfo.createEvent(factory);
        if (eventInfo.type == EventInfo.Type.IN) {
            addIn(event);
        } else if (eventInfo.type == EventInfo.Type.OUT) {
            addOut(event);
        } else if (eventInfo.type == EventInfo.Type.CANDIDATE) {
            addCandidate(event);
        } else if (eventInfo.type == EventInfo.Type.CORNER) {
            addCorner(event);
        }
    }

    PVector<Event> end(int index, boolean isRing) {
        if (candidate != null) {
            if (isRing) {
                if (candidate.index != index || events.get(0).index != 0)
                    pushCandidate();
                else
                    remove(0);
            } else {
                pushCandidate();
            }
        } else if (isRing) {
            if (!events.isEmpty()) {
                var last = events.get(events.size() - 1);
                if (last.type == Event.Type.CORNER && last.index == index)
                    remove(events.size() - 1);
            }
        }

        return events;
    }

    void pushCandidate() {
        add(candidate);
    }

    public void remove(int index) {
        events = events.minus(index);
    }
}
