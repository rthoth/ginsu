package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.Coordinate;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import javax.validation.constraints.NotNull;

import static com.github.rthoth.ginsu.SDetection.*;

public class SDetector {

    private final SCell cell;
    private final SEvent.Factory factory;
    private PVector<SEvent> events = null;
    private Coordinate previousCoordinate;
    private int previousIndex;
    private int firstLocation;
    private int previousPosition;
    private int currentPosition;
    private int currentIndex;
    private Coordinate currentCoordinate;
    private SEvent candidate;
    private Coordinate firstCoordinate;


    public SDetector(SCell cell, SEvent.Factory factory) {
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
                        pushIn(currentIndex, cell.computeLocation(previousCoordinate, currentCoordinate, previousPosition));
                    } else if (previousLocation == BORDER) { // BORDER -> INSIDE
                        if (candidate != null) {
                            if (candidate.index != previousIndex) {
                                pushCandidate();
                                pushIn(previousIndex, cell.createLocation(previousCoordinate, previousPosition));
                            } else {
                                candidate = null;
                            }
                        } else {
                            pushIn(previousIndex, cell.createLocation(previousCoordinate, previousPosition));
                        }
                    }
                } else if (currentLocation == OUTSIDE) {
                    pushOut(previousIndex, cell.computeLocation(previousCoordinate, currentCoordinate, currentPosition));
                } else if (currentLocation == BORDER) {
                    candidate = factory.newOut(currentIndex, cell.createLocation(currentCoordinate, currentPosition));
                }
                break;
            case -4: // OUTSIDE -> INSIDE -> OUTSIDE
                events = events.plus(factory.newIn(-1, cell.computeLocation(previousCoordinate, currentCoordinate, previousPosition)));
                events = events.plus(factory.newOut(-1, cell.computeLocation(previousCoordinate, currentCoordinate, currentPosition)));
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
                        pushIn(previousIndex, cell.createLocation(previousCoordinate, previousPosition));
                    }

                    pushOut(previousIndex, cell.computeLocation(previousCoordinate, currentCoordinate, currentPosition));
                } else { // OUTSIDE -> INSIDE -> BORDER
                    pushIn(currentIndex, cell.computeLocation(previousCoordinate, currentCoordinate, previousPosition));
                    candidate = factory.newOut(currentIndex, cell.createLocation(currentCoordinate, currentPosition));
                }
                break;

            case -1:  // BORDER -> INSIDE -> BORDER
                if (candidate != null) {
                    if (candidate.index != previousIndex) {
                        pushCandidate();
                        pushIn(previousIndex, cell.createLocation(previousCoordinate, previousPosition));
                    }
                } else {
                    events = events.plus(factory.newIn(previousIndex, cell.createLocation(previousCoordinate, previousPosition)));
                }

                candidate = factory.newOut(currentIndex, cell.createLocation(currentCoordinate, currentPosition));
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

    public SDetection last(int index, Coordinate coordinate) {
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


        return new SDetection(events, firstCoordinate.equals2D(coordinate), firstLocation, factory);
    }

    private int location(int position) {
        switch (position) {
            case SCell.LOWER:
            case SCell.UPPER:
                return OUTSIDE;

            case SCell.MIDDLE:
                return INSIDE;

            default:
                return BORDER;
        }
    }

    private void pushCandidate() {
        events = events.plus(candidate);
        candidate = null;
    }

    private void pushIn(int index, @NotNull SCell.Location location) {
        events = events.plus(factory.newIn(index, location));
        candidate = null;
    }

    private void pushOut(int index, @NotNull SCell.Location location) {
        events = events.plus(factory.newOut(index, location));
        candidate = null;
    }
}
