package com.github.rthoth.ginsu;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

public class IntersectionTest implements GinsuTest {

    @Test
    public void t01() {

        var polygonA = parseMultiPolygon("MULTIPOLYGON (((6 6, 4 5, 4 0, 3 0, 3 5, 0 0, -3 5, -3 0, -4 0, -4 5, -6 6, -6 -6, -2 -6, -2 0, 0 -3, 2 0, 2 -6, 6 -6, 6 6)))");
        var slicerA = new Slicer(array(-5, 0, 5), array(-5, 0, 5));

        var gridA = slicerA.polygonal(polygonA);
        assertThat(gridA).isNotNull();
        assertThat(gridA.entries()).isEmpty();
    }
}
