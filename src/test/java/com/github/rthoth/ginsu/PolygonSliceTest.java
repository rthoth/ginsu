package com.github.rthoth.ginsu;

import org.junit.Test;
import org.pcollections.TreePVector;

import static com.google.common.truth.Truth.assertThat;

public class PolygonSliceTest extends AbstractTest implements DetectionUtil, Util {

    @Test
    public void i001() {
        var _slice = middle(y(-1), y(1));
        var shell = Detector.detect(_slice, parseSequence("(-5 7, -7 5, 3 -5, 1 -7, 7 -7, 7 -1, 5 -3, -5 7)"), true);
        var hole = Detector.detect(_slice, parseSequence("(-5 6, -6 5, 4 -5, 5 -4, -5 6)"), true);

        var slicer = new PolygonSlicer(GEOMETRY_FACTORY);
        var slice = slicer.toGeometry(slicer.apply(DetectionShape.of(TreePVector.singleton(shell).plus(hole)), Dimension.Y, Ginsu.DEFAULT_OFFSET));
        assertThat(slice.toText())
                .isEqualTo("MULTIPOLYGON (((-3 1, -1 -1, 0 -1, -2 1, -3 1)), ((3 -1, 1 1, 0 1, 2 -1, 3 -1)))");
    }

    @Test
    public void i002() {
        var kX = x(5);
        var kY = y(-1);
        var sequence = parseSequence("(-5 7, -7 5, 3 -5, 1 -7, 7 -7, 7 -1, 5 -3, -5 7)");
        var detection = Detector.detect(upper(kX), lower(kY), sequence, true);
//        println(toWKT(singletonList(kX), singletonList(kY), sequence, GEOMETRY_FACTORY));
        assertThat(Ginsu.map(detection.events.getVector(), Event::toString))
                .containsExactly(
                        "In.X(4, (5.0, -7.0, NaN))",
                        "Out.X(6, null)"
                );
    }

    @Test
    public void i003() {
        var polygon = parsePolygon("POLYGON((-5 7, -7 5, 3 -5, 1 -7, 7 -7, 7 -1, 5 -3, -5 7), (6 -3, 5 -5, 3 -6, 6 -6, 6 -3), (-5 6, -6 5, 4 -5, 5 -4, -5 6))");
        var slicer = new Slicer(new double[]{-5, 5}, new double[]{0});
        var gridXY = slicer.polygonal(polygon, Order.XY);
        var gridYX = slicer.polygonal(polygon, Order.YX);
//        println(gridXY.toWKT());
//        println(gridYX.toWKT());
        assertThat(Ginsu.map(gridXY.iterable(), e -> e.value))
                .comparingElementsUsing(compareTopology())
                .containsExactlyElementsIn(Ginsu.map(gridYX.iterable(), e -> e.value));
    }

    @Test
    public void i004() {
        final var polygon = parseMultiPolygon("MULTIPOLYGON(((-5 7, -7 5, 3 -5, 1 -7, 7 -7, 7 -1, 5 -3, -5 7), (6 -3, 5 -5, 3 -6, 6 -6, 6 -3), (-5 6, -6 5, 4 -5, 5 -4, -5 6)), ((4 8, 8 8, 8 11, 4 11, 4 8)))");
        final var slicer = new Slicer(new double[]{-3, -2, 1, 4, 8}, new double[]{-1, 1, 8, 10});
        var gXY = slicer.polygonal(polygon, Order.XY); // It's here the error.
//        println(gXY.toWKT());
        var gYX = slicer.polygonal(polygon, Order.YX);

        assertThat(Ginsu.map(gXY.iterable(), e -> e.value))
                .comparingElementsUsing(compareTopology())
                .containsExactlyElementsIn(Ginsu.map(gYX.iterable(), e -> e.value));
    }

    @Test
    public void t01() {
        var polygon = parsePolygon("POLYGON((-5 7, -7 5, 3 -5, 1 -7, 7 -7, 7 -1, 5 -3, -5 7), (6 -3, 5 -5, 3 -6, 6 -6, 6 -3), (-5 6, -6 5, 4 -5, 5 -4, -5 6))");
        var grid = new Slicer(new double[]{-6, -1, 5}, new double[]{-7, -1, 1, 7})
                .polygonal(polygon, Order.XY);

//        println(toWKT(x(new double[]{-6, -1, 5}), y(new double[]{-7, -1, 1, 7}), polygon));
//        println(grid.toWKT());

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
                        "Entry(1, 2, MULTIPOLYGON (((-3 1, -1 -1, -1 0, -2 1, -3 1))))",
                        "Entry(2, 2, MULTIPOLYGON (((3 -1, 1 1, 0 1, 2 -1, 3 -1)), ((0 -1, -1 0, -1 -1, 0 -1))))",
                        "Entry(3, 2, MULTIPOLYGON EMPTY)",
                        "Entry(0, 3, MULTIPOLYGON (((-6 6, -7 5, -6 4, -6 6))))",
                        "Entry(1, 3, MULTIPOLYGON (((-2 1, -6 5, -5 6, -1 2, -1 3, -5 7, -6 6, -6 4, -3 1, -2 1))))",
                        "Entry(2, 3, MULTIPOLYGON (((1 1, -1 3, -1 2, 0 1, 1 1))))",
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
        var slicer = new Slicer(new double[]{-6, -1, 5}, new double[]{-7, -1, 1, 7});
        var grid = slicer.polygonal(polygon, Order.YX);


        //println(grid.toWKT());

        assertThat(Ginsu.map(grid.iterable(), Grid.Entry::toString))
                .containsExactly(
                        "Entry(0, 0, MULTIPOLYGON EMPTY)",
                        "Entry(1, 0, MULTIPOLYGON EMPTY)",
                        "Entry(2, 0, MULTIPOLYGON EMPTY)",
                        "Entry(3, 0, MULTIPOLYGON EMPTY)",
                        "Entry(0, 1, MULTIPOLYGON EMPTY)",
                        "Entry(1, 1, MULTIPOLYGON EMPTY)",
                        "Entry(2, 1, MULTIPOLYGON (((5 -3, 3 -1, 2 -1, 5 -4, 4 -5, 0 -1, -1 -1, 3 -5, 1 -7, 5 -7, 5 -6, 3 -6, 5 -5, 5 -3))))",
                        "Entry(3, 1, MULTIPOLYGON (((5 -7, 7 -7, 7 -1, 5 -3, 5 -5, 6 -3, 6 -6, 5 -6, 5 -7))))",
                        "Entry(0, 2, MULTIPOLYGON EMPTY)",
                        "Entry(1, 2, MULTIPOLYGON (((-1 0, -2 1, -3 1, -1 -1, -1 0))))",
                        "Entry(2, 2, MULTIPOLYGON (((-1 -1, 0 -1, -1 0, -1 -1)), ((3 -1, 1 1, 0 1, 2 -1, 3 -1))))",
                        "Entry(3, 2, MULTIPOLYGON EMPTY)",
                        "Entry(0, 3, MULTIPOLYGON (((-6 6, -7 5, -6 4, -6 6))))",
                        "Entry(1, 3, MULTIPOLYGON (((-1 3, -5 7, -6 6, -6 4, -3 1, -2 1, -6 5, -5 6, -1 2, -1 3))))",
                        "Entry(2, 3, MULTIPOLYGON (((-1 2, 0 1, 1 1, -1 3, -1 2))))",
                        "Entry(3, 3, MULTIPOLYGON EMPTY)",
                        "Entry(0, 4, MULTIPOLYGON EMPTY)",
                        "Entry(1, 4, MULTIPOLYGON EMPTY)",
                        "Entry(2, 4, MULTIPOLYGON EMPTY)",
                        "Entry(3, 4, MULTIPOLYGON EMPTY)"
                );
    }

    @Test
    public void t03() {
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
    public void t04() {
        final var polygon = parsePolygon("POLYGON((-5 7, -7 5, 3 -5, 1 -7, 7 -7, 7 -1, 5 -3, -5 7), (-5 6, -6 5, 4 -5, 5 -4, -5 6))");
        final var slicer = new Slicer(new double[]{1, 4}, new double[]{});
        var gXY = slicer.polygonal(polygon, Order.XY); // It's here the error.
        assertThat(Ginsu.map(gXY.iterable(), Grid.Entry::toString))
                .containsExactly(
                        "Entry(0, 0, MULTIPOLYGON (((1 1, -5 7, -7 5, 1 -3, 1 -2, -6 5, -5 6, 1 0, 1 1))))",
                        "Entry(1, 0, MULTIPOLYGON (((4 -2, 1 1, 1 0, 4 -3, 4 -2)), ((1 -2, 4 -5, 4 -7, 1 -7, 3 -5, 1 -3, 1 -2))))",
                        "Entry(2, 0, MULTIPOLYGON (((4 -7, 7 -7, 7 -1, 5 -3, 4 -2, 4 -3, 5 -4, 4 -5, 4 -7))))"
                );
    }
}
