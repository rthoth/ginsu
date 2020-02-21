package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.TopologyException;

import java.util.HashMap;
import java.util.TreeSet;

public class SliceBorder {

	private final TreeSet<Event> lowerBorder = new TreeSet<>(Event.COMPARATOR);
	private final TreeSet<Event> upperBorder = new TreeSet<>(Event.COMPARATOR);

	private final HashMap<Event, Detection> eventToDetection = new HashMap<>();
	private final HashMap<Event, Bond<Event>> eventToBond = new HashMap<>();
	private final HashMap<Detection, Bond<Event>> detectionToEnd = new HashMap<>();

	public SliceBorder add(Detection detection) {
		var it = detection.getEvents().iterator();

		Event previous = null, current = Ginsu.getNext(it).orElse(null), next;
		var first = current;

		while (current != null) {
			register(current);

			eventToDetection.put(current, detection);

			if (it.hasNext()) {
				next = it.next();
			} else {
				next = null;
			}

			eventToBond.put(current, new Bond<>(previous, current, next));
			previous = current;
			current = next;
		}

		if (first != null) {
			detectionToEnd.put(detection, new Bond<>(first, null, previous));
		}

		return this;
	}

	private void register(Event event) {
		var added = true;
		if (event.position < 0)
			added = lowerBorder.add(event);
		else if (event.position > 0)
			added = upperBorder.add(event);
		else
			throw new IllegalArgumentException("Invalid event " + event + "!");

		if (!added)
			throw new TopologyException("Invalid event position " + event + "!", event.getCoordinate());
	}

	public boolean hasMore() {
		return !(lowerBorder.isEmpty() && upperBorder.isEmpty());
	}

	public Event first() {
		return Ginsu.selectFirst(lowerBorder, upperBorder, Event.COMPARATOR);
	}

	public Event last() {
		return Ginsu.selectLast(lowerBorder, upperBorder, Event.COMPARATOR);
	}

	public Event nextEvent(Event event) {
		return nextEvent(event, true);
	}

	public Event nextEvent(Event event, boolean remove) {
		var bond = eventToBond.get(event);
		var next = bond.next != null ? bond.next : detectionToEnd.get(eventToDetection.get(event)).previous;
		if (remove && next != null)
			(next.position < 0 ? lowerBorder : upperBorder).remove(next);

		return next;
	}

	public Event previousEvent(Event event) {
		return previousEvent(event, true);
	}

	public Event previousEvent(Event event, boolean remove) {
		var bond = eventToBond.get(event);
		var previous = bond.previous != null ? bond.previous : detectionToEnd.get(eventToDetection.get(event)).next;
		if (remove && previous != null)
			(previous.position < 0 ? lowerBorder : upperBorder).remove(previous);

		return previous;
	}

	public Event higher(Event event) {
		return higher(event, true);
	}

	public Event higher(Event event, boolean remove) {
		var set = event.position < 0 ? lowerBorder : upperBorder;
		var higher = set.higher(event);
		if (remove && higher != null)
			set.remove(higher);

		return higher;
	}

	public Event lower(Event event) {
		return lower(event, true);
	}

	public Event lower(Event event, boolean remove) {
		var set = event.position < 0 ? lowerBorder : upperBorder;
		var lower = set.lower(event);
		if (remove && lower != null)
			set.remove(lower);

		return lower;
	}

	public void remove(Event event) {
		(event.position < 0 ? lowerBorder : upperBorder).remove(event);
	}
}
