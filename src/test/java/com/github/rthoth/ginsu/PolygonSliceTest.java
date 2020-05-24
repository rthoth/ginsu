package com.github.rthoth.ginsu;

import org.junit.Test;
import org.pcollections.TreePVector;

import java.util.Arrays;

import static com.google.common.truth.Truth.assertThat;

public class PolygonSliceTest extends AbstractTest implements SDetectionUtil, Util {

    @Test
    public void t01() {
        var polygon = parsePolygon("POLYGON((-5 7, -7 5, 3 -5, 1 -7, 7 -7, 7 -1, 5 -3, -5 7), (6 -3, 5 -5, 3 -6, 6 -6, 6 -3), (-5 6, -6 5, 4 -5, 5 -4, -5 6))");
        var grid = new Slicer(new double[]{-6, -1, 5}, new double[]{-7, -1, 1, 7})
                .polygonal(polygon, Order.XY);

        assertThat(Ginsu.map(grid.iterable(), Grid.Entry::toString))
                .containsExactly(
                        "Entry(0, 0, MULTIPOLYGON EMPTY)",
                        "Entry(1, 0, MULTIPOLYGON EMPTY)",
                        "Entry(2, 0, MULTIPOLYGON EMPTY)",
                        "Entry(3, 0, MULTIPOLYGON EMPTY)",
                        "Entry(0, 1, MULTIPOLYGON EMPTY)",
                        "Entry(1, 1, MULTIPOLYGON EMPTY)",
                        "Entry(2, 1, MULTIPOLYGON (((-1 -1, 3 -5, 1 -7, 5 -7, 5 -6, 3 -6, 5 -5, 5 -3, 3 -1, 2 -1, 5 -4, 4 -5, 0 -1, -1 -1))))",
                        "Entry(3, 1, MULTIPOLYGON (((7 -7, 7 -1, 5 -3, 5 -5, 6 -3, 6 -6, 5 -6, 5 -7, 7 -7))))",
                        "Entry(0, 2, MULTIPOLYGON EMPTY)",
                        "Entry(1, 2, MULTIPOLYGON (((-2 1, -1 0, -1 -1, -3 1, -2 1))))",
                        "Entry(2, 2, MULTIPOLYGON (((-1 -1, -1 0, 0 -1, -1 -1)), ((0 1, 2 -1, 3 -1, 1 1, 0 1))))",
                        "Entry(3, 2, MULTIPOLYGON EMPTY)",
                        "Entry(0, 3, MULTIPOLYGON (((-6 6, -7 5, -6 4, -6 6))))",
                        "Entry(1, 3, MULTIPOLYGON (((-3 1, -6 4, -6 6, -5 7, -1 3, -1 2, -5 6, -6 5, -2 1, -3 1))))",
                        "Entry(2, 3, MULTIPOLYGON (((0 1, -1 2, -1 3, 1 1, 0 1))))",
                        "Entry(3, 3, MULTIPOLYGON EMPTY)",
                        "Entry(0, 4, MULTIPOLYGON EMPTY)",
                        "Entry(1, 4, MULTIPOLYGON EMPTY)",
                        "Entry(2, 4, MULTIPOLYGON EMPTY)",
                        "Entry(3, 4, MULTIPOLYGON EMPTY)"
                );
    }

    @Test
    public void t02() {
        var polygon = parsePolygon("POLYGON((-5 7, -7 5, 3 -5, 1 -7, 7 -7, 7 -1, 5 -3, -5 7), (6 -3, 5 -5, 3 -6, 6 -6, 6 -3), (-5 6, -6 5, 4 -5, 5 -4, -5 6))");
        var grid = new Slicer(new double[]{-6, -1, 5}, new double[]{-7, -1, 1, 7})
                .polygonal(polygon, Order.YX);

        assertThat(Ginsu.map(grid.iterable(), Grid.Entry::toString))
                .containsExactly("Entry(0, 0, MULTIPOLYGON EMPTY)",
                        "Entry(1, 0, MULTIPOLYGON EMPTY)",
                        "Entry(2, 0, MULTIPOLYGON EMPTY)",
                        "Entry(3, 0, MULTIPOLYGON EMPTY)",
                        "Entry(0, 1, MULTIPOLYGON EMPTY)",
                        "Entry(1, 1, MULTIPOLYGON EMPTY)",
                        "Entry(2, 1, MULTIPOLYGON (((5 -3, 3 -1, 2 -1, 5 -4, 4 -5, 0 -1, -1 -1, 3 -5, 1 -7, 5 -7, 5 -6, 3 -6, 5 -5, 5 -3))))",
                        "Entry(3, 1, MULTIPOLYGON (((5 -7, 7 -7, 7 -1, 5 -3, 5 -5, 6 -3, 6 -6, 5 -6, 5 -7))))",
                        "Entry(0, 2, MULTIPOLYGON EMPTY)",
                        "Entry(1, 2, MULTIPOLYGON (((-1 0, -2 1, -3 1, -1 -1, -1 0))))",
                        "Entry(2, 2, MULTIPOLYGON (((3 -1, 1 1, 0 1, 2 -1, 3 -1)), ((-1 -1, 0 -1, -1 0, -1 -1))))",
                        "Entry(3, 2, MULTIPOLYGON EMPTY)",
                        "Entry(0, 3, MULTIPOLYGON (((-6 6, -7 5, -6 4, -6 6))))",
                        "Entry(1, 3, MULTIPOLYGON (((-1 2, -5 6, -6 5, -2 1, -3 1, -6 4, -6 6, -5 7, -1 3, -1 2))))",
                        "Entry(2, 3, MULTIPOLYGON (((-1 2, 0 1, 1 1, -1 3, -1 2))))",
                        "Entry(3, 3, MULTIPOLYGON EMPTY)",
                        "Entry(0, 4, MULTIPOLYGON EMPTY)",
                        "Entry(1, 4, MULTIPOLYGON EMPTY)",
                        "Entry(2, 4, MULTIPOLYGON EMPTY)",
                        "Entry(3, 4, MULTIPOLYGON EMPTY)");
    }

    @Test
    public void test03() {
        var polygon = parsePolygon("POLYGON((5 5, 8 0, 5 -5, 2 -3, -2 -3, -5 -5, -8 0, -5 5, -2 3, 2 3, 5 5), (-5 0, -2 3, -2 -3, -5 0), (2 -3, 5 0, 2 3, 2 -3))");
        var grid = new Slicer(new double[]{-2, 2}, new double[]{})
                .extrude(-0.5)
                .polygonal(polygon, Order.XY);
        assertThat(Ginsu.map(grid.iterable(), Grid.Entry::toString))
                .containsExactly(
                        "Entry(0, 0, MULTIPOLYGON (((-2.5 -3.3333333333333335, -5 -5, -8 0, -5 5, -2.5 3.333333333333333, -2.5 2.5, -5 0, -2.5 -2.5, -2.5 -3.3333333333333335))))",
                        "Entry(1, 0, MULTIPOLYGON (((1.5 -3, -1.5 -3, -1.5 3, 1.5 3, 1.5 -3))))",
                        "Entry(2, 0, MULTIPOLYGON (((2.5 3.3333333333333335, 5 5, 8 0, 5 -5, 2.5 -3.333333333333333, 2.5 -2.5, 5 0, 2.5 2.5, 2.5 3.3333333333333335))))"
                );
    }

    @Test
    public void issue001() {
        var shell = detect(middle(y(-1), y(1)), parseSequence("(-5 7, -7 5, 3 -5, 1 -7, 7 -7, 7 -1, 5 -3, -5 7)"));
        var hole = detect(middle(y(-1), y(1)), parseSequence("(-5 6, -6 5, 4 -5, 5 -4, -5 6)"));
        var slicer = new PolygonSlicer(GEOMETRY_FACTORY);
        var slice = slicer.apply(slicer.slice(TreePVector.from(Arrays.asList(shell, hole))));
        assertThat(slice.toText())
                .isEqualTo("MULTIPOLYGON (((3 -1, 1 1, 0 1, 2 -1, 3 -1)), ((-3 1, -1 -1, 0 -1, -2 1, -3 1)))");
    }
}
