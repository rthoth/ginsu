package com.github.rthoth.ginsu;

import org.junit.jupiter.api.Test;

public class IntersectionTest implements GinsuTest {

    @Test
    public void t01() {

        var polygonA = parsePolygon("POLYGON EMPTY");
        var polygonB = parsePolygon("POLYGON EMPTY");

        var slicerA = new Slicer(array(-5, 5), array(-5, 5));
    }
}
