package com.github.rthoth.ginsu;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Polygon;

import static com.google.common.truth.Truth.assertThat;

public class PolygonSlicerTest extends GeometrySlicerTest {

	private Polygon polygon01 = wkt("POLYGON ((-5 7, -7 5, 3 -5, 1 -7, 7 -7, 7 -1, 5 -3, -5 7))");

	private Polygon polygon02 = wkt("POLYGON ((-5 7, -7 5, 3 -5, 1 -7, 7 -7, 7 -1, 5 -3, -5 7), (-5 6, -6 5, 4 -5, 5 -4, -5 6), (6 -3, 5 -5, 3 -6, 6 -6, 6 -3))");

	@Test
	void t01() {
		var grid = new Slicer(new double[]{-5, 1, 5}, new double[]{-7, -1, 1, 7})
			.apply(polygon01);

		assertThat(Ginsu.mapToString(grid))
			.containsExactly(
				"Entry(0, 0, MULTIPOLYGON EMPTY)",
				"Entry(0, 1, MULTIPOLYGON EMPTY)",
				"Entry(0, 2, MULTIPOLYGON EMPTY)",
				"Entry(0, 3, MULTIPOLYGON (((-5 7, -7 5, -5 3, -5 7))))",
				"Entry(0, 4, MULTIPOLYGON EMPTY)",
				"Entry(1, 0, MULTIPOLYGON EMPTY)",
				"Entry(1, 1, MULTIPOLYGON (((1 -1, -1 -1, 1 -3, 1 -1))))",
				"Entry(1, 2, MULTIPOLYGON (((1 1, -3 1, -1 -1, 1 -1, 1 1))))",
				"Entry(1, 3, MULTIPOLYGON (((-5 3, -3 1, 1 1, -5 7, -5 3))))",
				"Entry(1, 4, MULTIPOLYGON EMPTY)",
				"Entry(2, 0, MULTIPOLYGON EMPTY)",
				"Entry(2, 1, MULTIPOLYGON (((5 -3, 3 -1, 1 -1, 1 -3, 3 -5, 1 -7, 5 -7, 5 -3))))",
				"Entry(2, 2, MULTIPOLYGON (((1 -1, 3 -1, 1 1, 1 -1))))",
				"Entry(2, 3, MULTIPOLYGON EMPTY)",
				"Entry(2, 4, MULTIPOLYGON EMPTY)",
				"Entry(3, 0, MULTIPOLYGON EMPTY)",
				"Entry(3, 1, MULTIPOLYGON (((5 -7, 7 -7, 7 -1, 5 -3, 5 -7))))",
				"Entry(3, 2, MULTIPOLYGON EMPTY)",
				"Entry(3, 3, MULTIPOLYGON EMPTY)",
				"Entry(3, 4, MULTIPOLYGON EMPTY)"
			);
	}


	@Test
	void t02() {
		var slicer = new Slicer(new double[]{-5, 1, 5}, new double[]{-7, -1, 1, 7});
		var grid = slicer.apply(polygon02);

		assertThat(Ginsu.mapToString(grid))
			.containsExactly(
				"Entry(0, 0, MULTIPOLYGON EMPTY)",
				"Entry(0, 1, MULTIPOLYGON EMPTY)",
				"Entry(0, 2, MULTIPOLYGON EMPTY)",
				"Entry(0, 3, MULTIPOLYGON (((-5 7, -7 5, -5 3, -5 4, -6 5, -5 6, -5 7))))",
				"Entry(0, 4, MULTIPOLYGON EMPTY)",
				"Entry(1, 0, MULTIPOLYGON EMPTY)",
				"Entry(1, 1, MULTIPOLYGON (((1 -2, 0 -1, -1 -1, 1 -3, 1 -2))))",
				"Entry(1, 2, MULTIPOLYGON (((-3 1, -1 -1, 0 -1, -2 1, -3 1)), ((1 1, 0 1, 1 0, 1 1))))",
				"Entry(1, 3, MULTIPOLYGON (((-5 3, -3 1, -2 1, -5 4, -5 3)), ((-5 6, 0 1, 1 1, -5 7, -5 6))))",
				"Entry(1, 4, MULTIPOLYGON EMPTY)",
				"Entry(2, 0, MULTIPOLYGON EMPTY)",
				"Entry(2, 1, MULTIPOLYGON (((5 -3, 3 -1, 2 -1, 5 -4, 4 -5, 1 -2, 1 -3, 3 -5, 1 -7, 5 -7, 5 -6, 3 -6, 5 -5, 5 -3))))",
				"Entry(2, 2, MULTIPOLYGON (((1 0, 2 -1, 3 -1, 1 1, 1 0))))",
				"Entry(2, 3, MULTIPOLYGON EMPTY)",
				"Entry(2, 4, MULTIPOLYGON EMPTY)",
				"Entry(3, 0, MULTIPOLYGON EMPTY)",
				"Entry(3, 1, MULTIPOLYGON (((5 -7, 7 -7, 7 -1, 5 -3, 5 -5, 6 -3, 6 -6, 5 -6, 5 -7))))",
				"Entry(3, 2, MULTIPOLYGON EMPTY)",
				"Entry(3, 3, MULTIPOLYGON EMPTY)",
				"Entry(3, 4, MULTIPOLYGON EMPTY)"
			);
	}
}
