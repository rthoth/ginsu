package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.Coordinate;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import javax.validation.constraints.NotNull;

import static com.github.rthoth.ginsu.SShape.Detection.*;

public class SDetector {

    private final Slice slice;
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


    public SDetector(Slice slice, SEvent.Factory factory) {
        this.slice = slice;
        this.factory = factory;
    }

    public void check(int index, Coordinate coordinate) {
        currentPosition = slice.positionOf(coordinate);
        currentCoordinate = coordinate;
        currentIndex = index;

        if (currentPosition != previousPosition) {

            if (firstLocation == BORDER && location(currentPosition) != BORDER)
                firstLocation = location(currentPosition);

            detect();
            previousPosition = currentPosition;
        }

        previousIndex = currentIndex;
        previousCoordinate = coordinate;
    }

    private void detect() {
        int currentLocation = location(currentPosition);

        switch (previousPosition * currentPosition) {
            case -3:
            case 3:
                if (currentLocation == INSIDE) {
                    pushIn(currentIndex, slice.computeLocation(previousCoordinate, currentCoordinate, previousPosition));
                } else {
                    pushOut(previousIndex, slice.computeLocation(previousCoordinate, currentCoordinate, currentPosition));
                }
                break;

            case -9:
                pushIn(-1, slice.computeLocation(previousCoordinate, currentCoordinate, previousPosition));
                pushOut(-1, slice.computeLocation(previousCoordinate, currentCoordinate, currentPosition));
                break;

            case -2:
            case 2:
                if (currentLocation == INSIDE) {
                    if (candidate != null) {
                        if (candidate.index != previousIndex) {
                            pushCandidate();
                            pushIn(previousIndex, slice.createLocation(previousCoordinate, previousPosition));
                        } else {
                            candidate = null;
                        }
                    } else {
                        pushIn(previousIndex, slice.createLocation(previousCoordinate, previousPosition));
                    }
                } else {
                    candidate = factory.newOut(currentIndex, slice.createLocation(currentCoordinate, currentPosition));
                }
                break;

            case 6:
                if (currentLocation == OUTSIDE) {
                    if (candidate != null)
                        pushCandidate();
                }
                break;

            case -6:
                if (currentLocation == BORDER) {
                    pushIn(currentIndex, slice.computeLocation(previousCoordinate, currentCoordinate, previousPosition));
                    candidate = factory.newOut(currentIndex, slice.createLocation(currentCoordinate, currentPosition));
                } else {
                    if (candidate != null) {
                        if (candidate.index != previousIndex) {
                            pushCandidate();
                            pushIn(previousIndex, slice.createLocation(previousCoordinate, previousPosition));
                        } else {
                            candidate = null;
                        }
                    } else {
                        pushIn(previousIndex, slice.createLocation(previousCoordinate, previousPosition));
                    }
                    pushOut(previousIndex, slice.computeLocation(previousCoordinate, currentCoordinate, currentPosition));
                }
                break;

            case -4:
                if (candidate != null) {
                    if (candidate.index != previousIndex) {
                        pushCandidate();
                        pushIn(previousIndex, slice.createLocation(previousCoordinate, previousPosition));
                    } else {
                        candidate = null;
                    }
                } else {
                    pushIn(previousIndex, slice.createLocation(previousCoordinate, previousPosition));
                }

                candidate = factory.newOut(currentIndex, slice.createLocation(currentCoordinate, currentPosition));
                break;

        }
    }

    public void first(Coordinate coordinate) {
        events = TreePVector.empty();
        previousCoordinate = coordinate;
        firstCoordinate = coordinate.copy();
        previousIndex = 0;
        previousPosition = slice.positionOf(coordinate);
        firstLocation = location(previousPosition);
        candidate = null;
    }

    public SShape.Detection last(int index, Coordinate coordinate) {
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


        return new SShape.Detection(events, firstCoordinate.equals2D(coordinate), firstLocation, factory);
    }

    private int location(int position) {
        switch (position) {
            case Slice.LOWER:
            case Slice.UPPER:
                return OUTSIDE;

            case Slice.MIDDLE:
                return INSIDE;

            default:
                return BORDER;
        }
    }

    private void pushCandidate() {
        events = events.plus(candidate);
        candidate = null;
    }

    private void pushIn(int index, @NotNull Slice.Location location) {
        events = events.plus(factory.newIn(index, location));
        candidate = null;
    }

    private void pushOut(int index, @NotNull Slice.Location location) {
        events = events.plus(factory.newOut(index, location));
        candidate = null;
    }
}
