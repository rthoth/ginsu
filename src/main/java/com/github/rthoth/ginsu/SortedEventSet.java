package com.github.rthoth.ginsu;

import org.pcollections.HashTreePMap;
import org.pcollections.PMap;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class SortedEventSet {

    private final TreeMap<Double, ScanLineEntry> scanLine = new TreeMap<>();
    private final TreeMap<Double, Event> lowerBorder = new TreeMap<>();
    private final TreeMap<Double, Event> upperBorder = new TreeMap<>();
    private PMap<Event, EventInfo> eventToInfo = HashTreePMap.empty();

    public void add(Detection detection) {

        for (var index : Ginsu.zipWithIndex(detection.events)) {
            var event = index.value;
            eventToInfo = eventToInfo.plus(event, new EventInfo(index.index, detection));

            var entry = scanLine.get(event.ordinate());
            if (entry == null) {
                scanLine.put(event.ordinate(), new ScanLineEntry(event));
            } else {
                entry.put(event);
            }

            var border = getBorder(event);
            var old = border.put(event.ordinate(), event);
            if (old != null)
                throw new GinsuException.IllegalArgument("It already have a event on " + event);
        }
    }

    private ScanLineEntry getEntry(Map.Entry<Double, ScanLineEntry> mapEntry) {
        return mapEntry != null ? mapEntry.getValue() : null;
    }

    private TreeMap<Double, Event> getBorder(Event event) {
        switch (event.border()) {
            case Cell.LOWER_BORDER:
                return lowerBorder;
            case Cell.UPPER_BORDER:
                return upperBorder;
            default:
                throw new GinsuException.IllegalArgument(Objects.toString(event));
        }
    }

    public ScanLineEntry lower() {
        return getEntry(scanLine.firstEntry());
    }

    public boolean nonEmpty() {
        return !scanLine.isEmpty();
    }

    public ScanLineEntry upper() {
        return getEntry(scanLine.lastEntry());
    }

    public Event extractNext(Event origin) {
        var info = eventToInfo.get(origin);
        var nextIndex = info.index + 1;

        if (nextIndex < info.detection.events.size()) {
            return extract(info.detection.events.get(nextIndex));
        } else if (info.detection.isRing) {
            return extract(info.detection.events.get(0));
        } else
            throw new GinsuException.IllegalState("No next event!");
    }

    private Event extract(Event event) {
        getBorder(event).remove(event.ordinate());
        var entry = scanLine.get(event.ordinate());
        if (entry.remove(event)) {
            scanLine.remove(entry.ordinate);
        }

        return event;
    }

    public Event extractPrevious(Event origin) {
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

    public Event extractLower(Event origin) {
        var entry = getBorder(origin).lowerEntry(origin.ordinate());
        if (entry != null)
            return extract(entry.getValue());
        else
            throw new GinsuException.IllegalState("Invalid move!");
    }

    public Event extractHigher(Event origin) {
        var entry = getBorder(origin).higherEntry(origin.ordinate());
        if (entry != null)
            return extract(entry.getValue());
        else
            throw new GinsuException.IllegalState("Invalid move!");
    }

    private static class EventInfo {

        private final int index;
        private final Detection detection;

        public EventInfo(int index, Detection detection) {
            this.index = index;
            this.detection = detection;
        }
    }

    protected static class ScanLineEntry {

        private final Double ordinate;
        private Event lower = null;
        private Event upper = null;

        public ScanLineEntry(Event event) {
            ordinate = event.ordinate();
            switch (event.border()) {
                case Cell.LOWER_BORDER:
                    lower = event;
                    break;
                case Cell.UPPER_BORDER:
                    upper = event;
                    break;
                default:
                    throw new GinsuException.IllegalArgument(Objects.toString(event));
            }
        }

        public Event getLower() {
            return lower;
        }

        public Event getUpper() {
            return upper;
        }

        private void put(Event event) {
            switch (event.border()) {
                case Cell.LOWER_BORDER:
                    if (lower == null) {
                        lower = event;
                        return;
                    }

                case Cell.UPPER_BORDER:
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
        public boolean remove(Event event) {
            switch (event.border()) {
                case Cell.LOWER_BORDER:
                    if (lower == event)
                        lower = null;
                    break;

                case Cell.UPPER_BORDER:
                    if (upper == event)
                        upper = null;
                    break;
            }

            return lower == null && upper == null;
        }
    }
}
