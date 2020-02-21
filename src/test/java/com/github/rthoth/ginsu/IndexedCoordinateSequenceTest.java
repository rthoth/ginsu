package com.github.rthoth.ginsu;

import com.github.rthoth.ginsu.SegmentedCoordinateSequence.Builder;
import com.google.common.truth.Truth;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.LineString;

import static com.github.rthoth.ginsu.CoordinateSequenceSubject.*;
import static com.github.rthoth.ginsu.EventTest.in;
import static com.github.rthoth.ginsu.EventTest.out;
import static com.github.rthoth.ginsu.GeometryTest.wkt;

public class IndexedCoordinateSequenceTest {

	public CoordinateSequence sequence01 = sequence("-5 -2, -5 2, -9 9, -2 5, 2 5, 9 9, 5 2, 5 -2, 9 -9, 2 -5, -2 -5, -9 -9, -5 -2");

	public static IndexedCoordinateSequence indexedSequence(String partialWKT) {
		return new IndexedCoordinateSequence(sequence(partialWKT), 0);
	}

	@SuppressWarnings("unused")
	public static IndexedCoordinateSequence indexedCoordinateSequence(String partialWKT, int index) {
		return new IndexedCoordinateSequence(sequence(partialWKT), index);
	}

	public static CoordinateSequence sequence(String partialWKT) {
		return ((LineString) wkt("LINESTRING(" + partialWKT + ")")).getCoordinateSequence();
	}

	@Test
	void t01() {
		var sequence = indexedSequence("6 -3, 5 -5, 3 -6, 6 -6, 6 -3");
		var outEvt = out(sequence, 1, -1, -5, null);
		var inEvt = in(sequence, 3, -1, -6, null);
		assertThat(backwardClosed(sequence, outEvt, inEvt))
			.isEqual(sequence("5 -5, 6 -3, 6 -6, 5 -5"));
	}

	@Test
	void t02() {
		var sequence = indexedSequence("6 -3, 5 -5, 3 -6, 6 -6, 6 -3");
		var outEvt = out(sequence, 4, -1, 0, null);
		var inEvt = in(sequence, 1, -1, 0, null);
		assertThat(backwardClosed(sequence, outEvt, inEvt))
			.isEqual(sequence("6 -3, 6 -6, 3 -6, 5 -5, 6 -3"));
	}

	@Test
	void t03() {
		var sequence = new IndexedCoordinateSequence(sequence01, 0);
		var inEvt = in(sequence, 0, -1, 0, null);
		var outEvt = out(sequence, 4, -1, 0, null);
		assertThat(forwardClosed(sequence, inEvt, outEvt))
			.isEqual(sequence("-5 -2, -5 2, -9 9, -2 5, 2 5, -5 -2"));
	}

	public static Builder builder(int dimension) {
		return new Builder(dimension);
	}

	@SuppressWarnings("OptionalGetWithoutIsPresent")
	public static SegmentedCoordinateSequence backwardClosed(IndexedCoordinateSequence sequence, Event start, Event stop) {
		return (SegmentedCoordinateSequence) builder(2)
			.backward(sequence.getCoordinateSequence(), start, stop).close().get();
	}

	@SuppressWarnings("OptionalGetWithoutIsPresent")
	public static SegmentedCoordinateSequence forwardClosed(IndexedCoordinateSequence sequence, Event start, Event stop) {
		return (SegmentedCoordinateSequence) builder(2)
			.forward(sequence.getCoordinateSequence(), start, stop).close().get();
	}
}
