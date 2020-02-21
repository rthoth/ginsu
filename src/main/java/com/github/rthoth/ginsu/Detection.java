package com.github.rthoth.ginsu;

import org.pcollections.PVector;
import org.pcollections.TreePVector;

public abstract class Detection {

	protected final IndexedCoordinateSequence sequence;
	protected final Cell<?> cell;
	protected final Location location;

	protected Detection(IndexedCoordinateSequence sequence, Cell<?> cell, Location location) {
		this.sequence = sequence;
		this.cell = cell;
		this.location = location;
	}

	public abstract boolean nonEmpty();

	public abstract PVector<Event> getEvents();

	public Location getLocation() {
		return location;
	}

	public IndexedCoordinateSequence getSequence() {
		return sequence;
	}

	public static Detection of(PVector<Event> events, IndexedCoordinateSequence sequence, Cell<?> cell, Location location) {
		return !events.isEmpty() ? new NotEmpty(events, sequence, cell, location) : new Empty(sequence, cell, location);
	}

	private static class NotEmpty extends Detection {

		private final PVector<Event> events;

		public NotEmpty(PVector<Event> events, IndexedCoordinateSequence sequence, Cell<?> cell, Location location) {
			super(sequence, cell, location);
			assert !events.isEmpty() : "Events is empty!";
			this.events = events;
		}

		@Override
		public PVector<Event> getEvents() {
			return events;
		}

		@Override
		public boolean nonEmpty() {
			return true;
		}

	}

	public static class Empty extends Detection {

		public Empty(IndexedCoordinateSequence sequence, Cell<?> cell, Location location) {
			super(sequence, cell, location);
		}

		@Override
		public PVector<Event> getEvents() {
			return TreePVector.empty();
		}

		@Override
		public boolean nonEmpty() {
			return false;
		}
	}
}
