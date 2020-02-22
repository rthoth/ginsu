package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.Coordinate;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import javax.validation.constraints.NotNull;

import static com.github.rthoth.ginsu.Detection.*;

public class Detector {

    private final Cell cell;
    private final Event.Factory factory;
    private PVector<Event> events = null;
    private Coordinate previousCoordinate;
    private int previousIndex;
    private int firstLocation;
    private int previousPosition;
    private int currentPosition;
    private int currentIndex;
    private Coordinate currentCoordinate;
    private Event candidate;
    private Coordinate firstCoordinate;


    public Detector(Cell cell, Event.Factory factory) {
        this.cell = cell;
        this.factory = factory;
    }

    public void check(int index, Coordinate coordinate) {
        currentPosition = cell.positionOf(coordinate);
        currentCoordinate = coordinate;
        currentIndex = index;

        if (currentPosition != previousPosition) {

            if (firstLocation == BORDER && location(currentPosition) != BORDER)
                firstLocation = location(currentPosition);

            if (currentPosition * previousPosition != 4)
                detect();

            previousPosition = currentPosition;
        }

        previousIndex = currentIndex;
        previousCoordinate = coordinate;
    }

    private void detect() {
        int currentLocation = location(currentPosition);
        int previousLocation = location(previousPosition);

        switch (previousPosition * currentPosition) {
            case 0:
                if (currentLocation == INSIDE) {
                    if (previousLocation == OUTSIDE) { // OUTSIDE -> INSIDE
                        pushIn(currentIndex, cell.computeIntersection(previousCoordinate, currentCoordinate, previousPosition));
                    } else if (previousLocation == BORDER) { // BORDER -> INSIDE
                        if (candidate != null) {
                            if (candidate.index != previousIndex) {
                                pushCandidate();
                                pushIn(previousIndex, cell.createIntersection(previousCoordinate, previousPosition));
                            } else {
                                candidate = null;
                            }
                        } else {
                            pushIn(previousIndex, cell.createIntersection(previousCoordinate, previousPosition));
                        }
                    }
                } else if (currentLocation == OUTSIDE) {
                    pushOut(previousIndex, cell.computeIntersection(previousCoordinate, currentCoordinate, currentPosition));
                } else if (currentLocation == BORDER) {
                    candidate = factory.newOut(currentIndex, cell.createIntersection(currentCoordinate, currentPosition));
                }
                break;
            case -4: // OUTSIDE -> INSIDE -> OUTSIDE
                events = events.plus(factory.newIn(-1, cell.computeIntersection(previousCoordinate, currentCoordinate, previousPosition)));
                events = events.plus(factory.newOut(-1, cell.computeIntersection(previousCoordinate, currentCoordinate, currentPosition)));
                candidate = null;
                break;

            case 2:
                if (currentLocation == OUTSIDE) { // BORDER -> OUTSIDE
                    if (candidate != null)
                        pushCandidate();
                }
                break;
            case -2:
                if (currentLocation == OUTSIDE) { // BORDER -> INSIDE -> OUTSIDE
                    if (candidate != null) {
                        if (candidate.index != previousIndex) {
                            pushCandidate();
                        }
                    } else {
                        pushIn(previousIndex, cell.createIntersection(previousCoordinate, previousPosition));
                    }

                    pushOut(previousIndex, cell.computeIntersection(previousCoordinate, currentCoordinate, currentPosition));
                } else { // OUTSIDE -> INSIDE -> BORDER
                    pushIn(currentIndex, cell.computeIntersection(previousCoordinate, currentCoordinate, previousPosition));
                    candidate = factory.newOut(currentIndex, cell.createIntersection(currentCoordinate, currentPosition));
                }
                break;

            case -1:  // BORDER -> INSIDE -> BORDER
                if (candidate != null) {
                    if (candidate.index != previousIndex) {
                        pushCandidate();
                        pushIn(previousIndex, cell.createIntersection(previousCoordinate, previousPosition));
                    }
                } else {
                    events = events.plus(factory.newIn(previousIndex, cell.createIntersection(previousCoordinate, previousPosition)));
                }

                candidate = factory.newOut(currentIndex, cell.createIntersection(currentCoordinate, currentPosition));
                break;
        }
    }

    public void first(Coordinate coordinate) {
        events = TreePVector.empty();
        previousCoordinate = coordinate;
        firstCoordinate = coordinate.copy();
        previousIndex = 0;
        previousPosition = cell.positionOf(coordinate);
        firstLocation = location(previousPosition);
        candidate = null;
    }

    public Detection last(int index, Coordinate coordinate) {
        check(index, coordinate);

        if (candidate != null) {
            if (candidate.index == index) {
                if (!events.isEmpty() && events.get(0).index == 0) {
                    events = events.minus(0);
                } else {
                    pushCandidate();
                }
            } else {
                pushCandidate();
            }
        }


        return new Detection(events, firstCoordinate.equals2D(coordinate), firstLocation, factory);
    }

    private int location(int position) {
        switch (position) {
            case Cell.LOWER:
            case Cell.UPPER:
                return OUTSIDE;

            case Cell.MIDDLE:
                return INSIDE;

            default:
                return BORDER;
        }
    }

    private void pushCandidate() {
        events = events.plus(candidate);
        candidate = null;
    }

    private void pushIn(int index, @NotNull Cell.Intersection intersection) {
        events = events.plus(factory.newIn(index, intersection));
        candidate = null;
    }

    private void pushOut(int index, @NotNull Cell.Intersection intersection) {
        events = events.plus(factory.newOut(index, intersection));
        candidate = null;
    }
}
