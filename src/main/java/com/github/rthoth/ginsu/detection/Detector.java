package com.github.rthoth.ginsu.detection;

import com.github.rthoth.ginsu.Dimension;
import com.github.rthoth.ginsu.GinsuException;
import com.github.rthoth.ginsu.Knife;
import org.locationtech.jts.geom.Coordinate;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.util.Collections;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

import static com.github.rthoth.ginsu.detection.Event.IN;
import static com.github.rthoth.ginsu.detection.Event.OUT;
import static com.github.rthoth.ginsu.detection.Event.TOUCH;

public class Detector<K extends Knife> {

    private final Event.Factory factory;
    private final Slice<K> slice;
    private final boolean detectTouch;
    private Coordinate previousCoordinate;
    private int firstPosition = 0;
    private int previousIndex = 0;
    private int previousPosition;
    private Coordinate currentCoordinate;
    private int currentIndex;
    private int currentPosition;
    private int product;
    private Event candidate;
    private PVector<Event> events = TreePVector.empty();
    private TreeMap<Double, Event> lowerMap;
    private TreeMap<Double, Event> upperMap;

    public Detector(Slice<K> slice, Event.Factory factory, Coordinate coordinate, boolean detectTouch) {
        this.slice = slice;
        this.factory = factory;
        this.detectTouch = detectTouch;
        previousCoordinate = coordinate;
        previousPosition = slice.position(coordinate);

        if (Math.abs(previousPosition) != 2)
            firstPosition = previousPosition;
    }

    public static <K extends Knife> Detector<K> create(Slice<K> slice, Event.Factory factory, Coordinate coordinate, boolean detectTouch) {
        return new Detector<>(slice, factory, coordinate, detectTouch);
    }

    private void addCandidate(Event event) {
        if (candidate == null)
            candidate = event;
        else
            throw new GinsuException.InvalidState("Candidate is not null!");
    }

    private void addIn(Event event) {
        if (candidate == null) {
            events = events.plus(event);
        } else {
            if (candidate.index != event.index) {
                events = events.plus(candidate).plus(event);
            } else if (detectTouch) {
                events = events.plus(candidate.withType(TOUCH));
            }
            candidate = null;
        }
    }

    private void addOut(Event event) {
        if (candidate == null)
            events = events.plus(event);
        else
            throw new GinsuException.InvalidState("Candidate is not null!");
    }

    private void detect() {
        if (product == 3 || product == -3) {
            var knife = product == 3 ? slice.getUpper() : slice.getLower();
            if (currentPosition == 1) {
                addIn(newEvent(IN, currentIndex, knife, true));
            } else {
                addOut(newEvent(OUT, previousIndex, knife, true));
            }
        } else if (product == -9) {
            Optional<K> previous, current;
            if (currentPosition == 3) {
                previous = slice.getLower();
                current = slice.getUpper();
            } else {
                previous = slice.getUpper();
                current = slice.getLower();
            }

            addIn(newEvent(IN, -2, previous, true));
            addOut(newEvent(OUT, -1, current, true));

        } else if (product == 6) {
            if (previousPosition == 2 || previousPosition == -2) {
                pushCandidate();
            }
        } else if (product == -6) {
            Optional<K> previous, current;
            if (currentPosition == 2) {
                previous = slice.getLower();
                current = slice.getUpper();
            } else {
                previous = slice.getUpper();
                current = slice.getLower();
            }

            if (currentPosition == 2 || currentPosition == -2) {
                addIn(newEvent(IN, currentIndex, previous, true));
                addCandidate(newEvent(OUT, currentIndex, current, false));
            } else {
                addIn(newEvent(IN, previousIndex, previous, false));
                addOut(newEvent(OUT, previousIndex, current, true));
            }

        } else if (product == -4) {
            Optional<K> previous, current;
            if (currentPosition == 2) {
                previous = slice.getLower();
                current = slice.getUpper();
            } else {
                previous = slice.getUpper();
                current = slice.getLower();
            }

            addIn(newEvent(IN, previousIndex, previous, false));
            addCandidate(newEvent(OUT, currentIndex, current, false));
        } else if (product == 2 || product == -2) {
            var knife = product == 2 ? slice.getUpper() : slice.getLower();
            if (currentPosition == 1) {
                addIn(newEvent(IN, previousIndex, knife, false));
            } else {
                addCandidate(newEvent(OUT, currentIndex, knife, false));
            }
        }
    }

    public void detect(Coordinate coordinate, int index) {
        currentIndex = index;
        currentCoordinate = coordinate;
        currentPosition = slice.position(coordinate);
        product = currentPosition * previousPosition;

        if (firstPosition == 0 && Math.abs(currentPosition) != 2)
            firstPosition = currentPosition;

        if (product != 9 && product != 1 && product != 4)
            detect();

        previousIndex = currentIndex;
        previousCoordinate = currentCoordinate;
        previousPosition = currentPosition;
    }

    public void end(Coordinate coordinate, int index, boolean isRing) {
        detect(coordinate, index);

        if (candidate != null) {
            if (isRing) {
                if (candidate.index != index || events.get(0).index != 0) {
                    pushCandidate();
                } else {
                    events = events.minus(0);
                    candidate = null;
                }
            } else {
                pushCandidate();
            }
        }

        lowerMap = new TreeMap<>();
        upperMap = new TreeMap<>();
        var lowerKnife = slice.getLower().orElse(null);
        var upperKnife = slice.getUpper().orElse(null);

        for (var event : events) {
            Event old;
            if (event.knife.contains(lowerKnife)) {
                old = lowerMap.put(event.ordinate(), event);
            } else if (event.knife.contains(upperKnife)) {
                old = upperMap.put(event.ordinate(), event);
            } else {
                throw new GinsuException.InvalidState("Invalid knife!");
            }

            if (old != null)
                throw new GinsuException.InvalidState("There is a collision on " + old + "!");
        }
    }

    public Dimension getDimension() {
        return slice.getDimension();
    }

    public PVector<Event> getEvents() {
        return events;
    }

    public NavigableMap<Double, Event> getLowerEvents() {
        return Collections.unmodifiableNavigableMap(lowerMap);
    }

    public Optional<K> getLowerKnife() {
        return slice.getLower();
    }

    public NavigableMap<Double, Event> getUpperEvents() {
        return Collections.unmodifiableNavigableMap(upperMap);
    }

    public Optional<K> getUpperKnife() {
        return slice.getUpper();
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Event newEvent(Event.Type type, int index, Optional<K> knife, boolean computeIntersection) {
        if (knife.isPresent()) {
            var coordinate = computeIntersection ? knife.get().intersection(previousCoordinate, currentCoordinate) : null;
            return factory.create(type, index, previousIndex, knife.get(), coordinate);
        } else {
            throw new GinsuException.InvalidState("There is no knife!");
        }
    }

    public int position(Coordinate coordinate) {
        return slice.position(coordinate);
    }

    private void pushCandidate() {
        if (candidate != null) {
            events = events.plus(candidate);
            candidate = null;
        }
    }
}
