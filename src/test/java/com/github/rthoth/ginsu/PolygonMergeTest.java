package com.github.rthoth.ginsu;

import com.google.common.truth.Truth;
import org.junit.Test;

public class PolygonMergeTest extends AbstractTest {

    @Test
    public void test01() {
        final var polygon = parsePolygon("POLYGON((-5 7, -7 5, 3 -5, 1 -7, 7 -7, 7 -1, 5 -3, -5 7), (6 -3, 5 -5, 3 -6, 6 -6, 6 -3), (-5 6, -6 5, 4 -5, 5 -4, -5 6))");
        final var slicer = new Slicer(new double[]{-3, -2, 1, 4}, new double[]{-1, 1});
        final var grid = slicer.polygonal(polygon);
        final var merger = slicer.merger();
        final var restored = merger.polygonal(grid, GEOMETRY_FACTORY);
        Truth.assertThat(restored.toText()).isEqualTo(polygon.toText());
    }
}
