package com.github.rthoth.ginsu;

import org.junit.jupiter.api.Test;

import static com.github.rthoth.ginsu.IndexedCoordinateSequenceTest.indexedSequence;
import static com.google.common.truth.Truth.*;

public class DetectorTest {

	private final IndexedCoordinateSequence list01 = indexedSequence("-5 7, -7 5, 3 -5, 1 -7, 7 -7, 7 -1, 5 -3, -5 7");

	@Test
	void t01() {
		var detection = detect(lower(x(-5)), list01);
		assertThat(Ginsu.mapToString(detection.getEvents()))
			.containsExactly("In(0, 1, 7.0, null)", "Out(1, 2, 3.0, (-5.0, 3.0))");
	}

	@Test
	void t02() {
		var detection = detect(middle(x(-5), x(1)), list01);
		assertThat(Ginsu.mapToString(detection.getEvents()))
			.containsExactly(
				"In(-1, -2, 3.0, (-5.0, 3.0))",
				"Out(-1, 2, -3.0, (1.0, -3.0))",
				"In(7, 2, 1.0, (1.0, 1.0))",
				"Out(7, -1, 7.0, null)"
			);
	}

	@Test
	void t03() {
		var detection = detect(middle(x(1), x(5)), list01);
		assertThat(Ginsu.mapToString(detection.getEvents()))
			.containsExactly(
				"In(2, -2, -3.0, (1.0, -3.0))",
				"Out(3, 2, -7.0, (5.0, -7.0))",
				"In(6, 1, -3.0, null)",
				"Out(6, -2, 1.0, (1.0, 1.0))"
			);
	}

	@Test
	void t04() {
		var detection = detect(upper(x(5)), list01);
		assertThat(Ginsu.mapToString(detection.getEvents()))
			.containsExactly(
				"In(4, -2, -7.0, (5.0, -7.0))",
				"Out(6, -1, -3.0, null)"
			);
	}

	@Test
	void t05() {
		var detection = detect(upper(y(7)), list01);
		assertThat(Ginsu.mapToString(detection.getEvents()))
			.isEmpty();
	}

	@Test
	void t06() {
		var detection = detect(middle(y(1), y(7)), list01);
		assertThat(Ginsu.mapToString(detection.getEvents()))
			.containsExactly(
				"Out(1, -2, -3.0, (-3.0, 1.0))",
				"In(7, -2, 1.0, (1.0, 1.0))"
			);
	}

	@Test
	void t07() {
		var detection = detect(middle(y(-1), y(1)), list01);
		assertThat(Ginsu.mapToString(detection.getEvents()))
			.containsExactly(
				"In(-1, 2, -3.0, (-3.0, 1.0))",
				"Out(-1, -2, -1.0, (-1.0, -1.0))",
				"In(-1, -2, 3.0, (3.0, -1.0))",
				"Out(-1, 2, 1.0, (1.0, 1.0))"
			);
	}

	@Test
	void t08() {
		var detection = detect(middle(y(-7), y(-1)), list01);
		assertThat(Ginsu.mapToString(detection.getEvents()))
			.containsExactly(
				"In(2, 2, -1.0, (-1.0, -1.0))",
				"Out(3, -1, 1.0, null)",
				"In(4, -1, 7.0, null)",
				"Out(6, 2, 3.0, (3.0, -1.0))"
			);
	}

	@Test
	void t09() {
		var detection = detect(lower(y(-7)), list01);
		assertThat(Ginsu.mapToString(detection.getEvents()))
			.isEmpty();
	}

	public static Detection detect(Cell<?> cell, IndexedCoordinateSequence list) {
		return detect(detector(cell, new Event.Factory(list)), list);
	}

	public static Detection detect(Detector<?> detector, IndexedCoordinateSequence list) {
		detector.first(list.get(0));

		final var lastIndex = list.size() - 1;
		for (var i = 0; i < lastIndex; i++)
			detector.check(list.get(i), i);

		return detector.last(list.get(lastIndex), lastIndex, list.isClosed());
	}

	public static <K extends Knife<K>> Detector<K> detector(Cell<K> cell, Event.Factory factory) {
		return new Detector<>(cell, factory);
	}

	public static <K extends Knife<K>> Cell.Lower<K> lower(K knife) {
		return new Cell.Lower<>(knife);
	}

	public static <K extends Knife<K>> Cell.Middle<K> middle(K lower, K upper) {
		return new Cell.Middle<>(lower, upper);
	}

	public static <K extends Knife<K>> Cell.Upper<K> upper(K knife) {
		return new Cell.Upper<>(knife);
	}

	public static Knife.Y y(double position) {
		return y(position, Slicer.DEFAULT_OFFSET);
	}

	public static Knife.Y y(double position, double offset) {
		return new Knife.Y(position, offset);
	}

	public static Knife.X x(double position) {
		return x(position, Slicer.DEFAULT_OFFSET);
	}

	public static Knife.X x(double value, double offset) {
		return new Knife.X(value, offset);
	}
}
