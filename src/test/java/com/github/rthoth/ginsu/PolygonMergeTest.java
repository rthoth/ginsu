package com.github.rthoth.ginsu;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public class PolygonMergeTest extends AbstractTest implements Util {

    @Test
    public void dif01() {
        var p1 = parsePolygon("POLYGON ((-8 4, 2 -6, -1 -9, 8 -9, 8 0, 5 -3, -5 7, -8 4))");
        var p2 = parsePolygon("POLYGON ((-5 4, 2 -5, 4 -6, 2 -8, 7 -8, 7 -3, 5 -5, 4 -3, -5 4))");
        var slicer = new Slicer(new double[]{-6, -3, 3, 6}, new double[]{-8, -4, 0, 4, 8});
        println(toWKT(x(new double[]{-6, -3, 3, 6}), y(new double[]{-8, -4, 0, 4, 8}), p1));
        var g1 = slicer.polygonal(p1);
        var g2 = slicer.polygonal(p2);
        var gI = g1.combine(g2, (m1, m2) -> toMultiPolygon(m1.difference(m2)));
        println(gI.toWKT());
        var result = slicer.merger().polygonal(gI, GEOMETRY_FACTORY);
//        println(result.toText());
        assertThat(result.toText()).isEqualTo("MULTIPOLYGON (((-8 4, 2 -6, -1 -9, 8 -9, 8 0, 5 -3, -5 7, -8 4), (-3 1.4285714285714288, -1.8888888888888888 0, 1.2222222222222223 -4, 2 -5, 4 -6, 2 -8, 7 -8, 7 -3, 5 -5, 4 -3, 3 -2.2222222222222223, 0.1428571428571428 0, -3 2.4444444444444446, -5 4, -3 1.4285714285714288)))");
    }

    @Test
    public void i001() {
        final var polygon = parseMultiPolygon("MULTIPOLYGON (((0 4, -3 7, -3 -4, 0 -7, 0 4)), ((3 -11, 3 0, 0 3, 0 -8, 3 -11)))");
        final var slicer = new Slicer(new double[]{0}, new double[]{0});
        final var sliced = slicer.polygonal(polygon);
        final var restored = slicer.merger().polygonal(sliced, GEOMETRY_FACTORY);
        compareTopology().compare(restored, polygon);
    }

    @Test
    public void t01() {
        final var polygon = parseMultiPolygon("MULTIPOLYGON(((-5 7, -7 5, 3 -5, 1 -7, 7 -7, 7 -1, 5 -3, -5 7), (6 -3, 5 -5, 3 -6, 6 -6, 6 -3), (-5 6, -6 5, 4 -5, 5 -4, -5 6)), ((4 8, 8 8, 8 11, 4 11, 4 8)))");
        final var slicer = new Slicer(new double[]{-3, -2, 1, 4, 8}, new double[]{-1, 1, 8, 10});
//        println(toWKT(x(new double[]{-3, -2, 1, 4, 8}), y(new double[]{-1, 1, 8, 10}), polygon));
        final var grid = slicer.polygonal(polygon, Order.XY);
//        println(grid.toWKT());
        final var merger = slicer.merger();
        final var restored = merger.polygonal(grid, GEOMETRY_FACTORY);
//        println(restored.toText());
        compareTopology().compare(restored, polygon);
    }
}
