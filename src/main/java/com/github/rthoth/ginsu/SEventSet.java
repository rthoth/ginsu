package com.github.rthoth.ginsu;

import org.pcollections.HashTreePMap;
import org.pcollections.PMap;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Slice Event Set
 */
public class SEventSet {

    private final TreeMap<Double, Entry> scanLine = new TreeMap<>();
    private final TreeMap<Double, SEvent> lowerBorder = new TreeMap<>();
    private final TreeMap<Double, SEvent> upperBorder = new TreeMap<>();
    private PMap<SEvent, EventInfo> eventToInfo = HashTreePMap.empty();

    public void add(SShape.Detection detection) {

        for (var index : Ginsu.zipWithIndex(detection.events)) {
            var event = index.value;
            eventToInfo = eventToInfo.plus(event, new EventInfo(index.index, detection));

            var entry = scanLine.get(event.ordinate());
            if (entry == null) {
                scanLine.put(event.ordinate(), new Entry(event));
            } else {
                entry.put(event);
            }

            var border = getBorder(event);
            var old = border.put(event.ordinate(), event);
            if (old != null)
                throw new GinsuException.IllegalArgument("It already have a event on " + event);
        }
    }

    private SEvent extract(SEvent event) {
        getBorder(event).remove(event.ordinate());
        var entry = scanLine.get(event.ordinate());
        if (entry.remove(event)) {
            scanLine.remove(entry.ordinate);
        }

        return event;
    }

    public SEvent extractHigher(SEvent origin) {
        var entry = getBorder(origin).higherEntry(origin.ordinate());
        if (entry != null)
            return extract(entry.getValue());
        else
            throw new GinsuException.IllegalState("Invalid move!");
    }

    public SEvent extractLower(SEvent origin) {
        var entry = getBorder(origin).lowerEntry(origin.ordinate());
        if (entry != null)
            return extract(entry.getValue());
        else
            throw new GinsuException.IllegalState("Invalid move!");
    }

    public SEvent extractNext(SEvent origin) {
        var info = eventToInfo.get(origin);
        var nextIndex = info.index + 1;

        if (nextIndex < info.detection.events.size()) {
            return extract(info.detection.events.get(nextIndex));
        } else if (info.detection.isRing) {
            return extract(info.detection.events.get(0));
        } else
            throw new GinsuException.IllegalState("No next event!");
    }

    public SEvent extractPrevious(SEvent origin) {
        var info = eventToInfo.get(origin);
        var previousIndex = info.index - 1;

        if (previousIndex >= 0) {
            return extract(info.detection.events.get(previousIndex));
        } else if (info.detection.isRing) {
            return extract(info.detection.events.get(info.detection.events.size() + previousIndex));
        } else {
            throw new GinsuException.IllegalState("No previous event!");
        }
    }

    private TreeMap<Double, SEvent> getBorder(SEvent event) {
        switch (event.border()) {
            case Slice.LOWER_BORDER:
                return lowerBorder;
            case Slice.UPPER_BORDER:
                return upperBorder;
            default:
                throw new GinsuException.IllegalArgument(Objects.toString(event));
        }
    }

    private Entry getEntry(Map.Entry<Double, Entry> mapEntry) {
        return mapEntry != null ? mapEntry.getValue() : null;
    }

    public Entry lower() {
        return getEntry(scanLine.firstEntry());
    }

    public boolean nonEmpty() {
        return !scanLine.isEmpty();
    }

    public Entry upper() {
        return getEntry(scanLine.lastEntry());
    }

    protected static class Entry {

        private final Double ordinate;
        private SEvent lower = null;
        private SEvent upper = null;

        public Entry(SEvent event) {
            ordinate = event.ordinate();
            switch (event.border()) {
                case Slice.LOWER_BORDER:
                    lower = event;
                    break;
                case Slice.UPPER_BORDER:
                    upper = event;
                    break;
                default:
                    throw new GinsuException.IllegalArgument(Objects.toString(event));
            }
        }

        public SEvent getLower() {
            return lower;
        }

        public SEvent getUpper() {
            return upper;
        }

        private void put(SEvent event) {
            switch (event.border()) {
                case Slice.LOWER_BORDER:
                    if (lower == null) {
                        lower = event;
                        return;
                    }

                case Slice.UPPER_BORDER:
                    if (upper == null) {
                        upper = event;
                        return;
                    }
            }

            throw new GinsuException.IllegalState(Objects.toString(event));
        }

        /**
         * @param event
         * @return true if it's empty!
         */
        public boolean remove(SEvent event) {
            switch (event.border()) {
                case Slice.LOWER_BORDER:
                    if (lower == event)
                        lower = null;
                    break;

                case Slice.UPPER_BORDER:
                    if (upper == event)
                        upper = null;
                    break;
            }

            return lower == null && upper == null;
        }
    }

    private static class EventInfo {

        private final int index;
        private final SShape.Detection detection;

        public EventInfo(int index, SShape.Detection detection) {
            this.index = index;
            this.detection = detection;
        }
    }
}
