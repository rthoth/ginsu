package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.Coordinate;
import org.pcollections.TreePVector;

import javax.validation.constraints.NotNull;
import java.util.LinkedList;

import static com.github.rthoth.ginsu.Location.*;

public class Detector<K extends Knife<K>> {

	private final Cell<K> cell;
	private final Event.Factory factory;

	private final LinkedList<Event> events = new LinkedList<>();
	private Event candidate = null;
	private Coordinate previousCoordinate;
	private int previousPosition;
	private Location previousLocation;
	private Location firstLocation;
	private Location currentLocation;
	private int currentPosition;
	private Coordinate currentCoordinate;
	private int previousIndex;
	private int currentIndex;

	public Detector(Cell<K> cell, Event.Factory factory) {
		this.cell = cell;
		this.factory = factory;
	}

	public void first(Coordinate coordinate) {
		previousCoordinate = coordinate;
		previousIndex = 0;
		previousPosition = cell.positionOf(coordinate);
		previousLocation = Location.of(previousPosition);
		firstLocation = previousLocation;
	}

	public void check(Coordinate coordinate, int index) {
		currentPosition = cell.positionOf(coordinate);

		if (currentPosition != previousPosition) {
			currentCoordinate = coordinate;
			currentLocation = Location.of(currentPosition);
			currentIndex = index;

			if (firstLocation == BORDER && currentLocation != BORDER)
				firstLocation = currentLocation;

			detect();
			previousPosition = currentPosition;
			previousLocation = currentLocation;
		}

		previousIndex = index;
		previousCoordinate = coordinate;
	}

	public Detection last(Coordinate coordinate, int index, boolean closed) {
		check(coordinate, index);

		if (closed) {
			if (candidate instanceof Event.Out) {
				if (!events.isEmpty() && events.peekFirst().index == 0) {
					events.removeFirst();
				} else {
					pushCandidate();
				}
			}
		} else if (candidate != null) {
			pushCandidate();
		}

		return Detection.of(TreePVector.from(events), factory.getIndexedCoordinateSequence(), cell, firstLocation);
	}

	private void addEvent(Event event) {
		if (event != null) {
			events.addLast(event);
			candidate = null;
		} else {
			throw new IllegalArgumentException();
		}
	}

	private void detect() {
		switch (currentPosition * previousPosition) {
			case -4: // OUTSIDE -> INSIDE -> OUTSIDE
				newIn(-1, previousPosition, cell.intersection(previousCoordinate, currentCoordinate, previousPosition));
				newOut(-1, currentPosition, cell.intersection(previousCoordinate, currentCoordinate, currentPosition));
				break;

			case 0: // OUTSIDE -> INSIDE || BORDER -> INSIDE || INSIDE -> BORDER || INSIDE -> OUTSIDE
				if (previousLocation == OUTSIDE) {
					newIn(currentIndex, previousPosition, cell.intersection(previousCoordinate, currentCoordinate, previousPosition));
				} else if (currentLocation == OUTSIDE) {
					newOut(previousIndex, currentPosition, cell.intersection(previousCoordinate, currentCoordinate, currentPosition));
				} else if (currentLocation == BORDER) {
					addCandidate(factory.newOut(currentIndex, currentPosition, cell.ordinateOf(currentCoordinate), null));
				} else {
					if (candidate instanceof Event.Out) {
						if (candidate.getIndex() != previousIndex) {
							pushCandidate();
						} else {
							candidate = null;
						}
					} else {
						newIn(previousIndex, previousPosition);
					}
				}

				break;

			case -2: // OUTSIDE -> INSIDE -> BORDER || BORDER -> INSIDE -> OUTSIDE
				if (currentLocation == BORDER) {
					newIn(currentIndex, previousPosition, cell.intersection(previousCoordinate, currentCoordinate, previousPosition));
					addCandidate(factory.newOut(currentIndex, currentPosition, cell.ordinateOf(currentCoordinate), null));
				} else {
					if (candidate != null) {
						if (candidate.getIndex() != previousIndex) {
							pushCandidate();
							newIn(previousIndex, previousPosition);
						} else {
							candidate = null;
						}
					} else {
						newIn(previousIndex, previousPosition);
					}

					newOut(previousIndex, currentPosition, cell.intersection(previousCoordinate, currentCoordinate, currentPosition));
				}
				break;

			case 2: // OUTSIDE -> BORDER || BORDER -> OUTSIDE
				if (currentLocation == OUTSIDE) {
					if (candidate instanceof Event.Out) {
						pushCandidate();
					}
				}
				break;

			case -1: // BORDER -> INSIDE -> BORDER
				if (candidate != null) {
					if (candidate.getIndex() != previousIndex) {
						pushCandidate();
						newIn(previousIndex, previousPosition);
					} else {
						candidate = null;
					}
				} else {
					newIn(previousIndex, previousPosition);
				}

				addCandidate(factory.newOut(currentIndex, currentPosition, cell.ordinateOf(currentCoordinate), null));
				break;

			default:
				throw new IllegalStateException(String.valueOf(currentPosition * previousPosition));
		}
	}

	private void addCandidate(Event event) {
		if (candidate == null) {
			candidate = event;
		} else {
			throw new IllegalStateException("There already is a candidate " + candidate + "!");
		}
	}

	private void pushCandidate() {
		if (candidate != null) {
			addEvent(candidate);
		}
	}

	private void newIn(int index, int position) {
		addEvent(factory.newIn(index, position, cell.ordinateOf(factory.getIndexedCoordinateSequence().get(index)), null));
	}

	private void newIn(int index, int position, @NotNull Coordinate coordinate) {
		addEvent(factory.newIn(index, position, cell.ordinateOf(coordinate), coordinate));
	}

	private void newOut(int index, int position, @NotNull Coordinate coordinate) {
		addEvent(factory.newOut(index, position, cell.ordinateOf(coordinate), coordinate));
	}
}
