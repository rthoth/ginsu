package com.github.rthoth.ginsu;

import com.github.rthoth.ginsu.io.FileGrid;
import org.junit.Test;
import org.locationtech.jts.geom.Polygon;

import static com.google.common.truth.Truth.assertThat;

public class FileGridTest extends AbstractTest implements Util {

    @Test
    public void t01() {
        var grid = new FileGrid<Polygon>(3, 3, file("target/grid-test-01"), GEOMETRY_FACTORY);
        grid.generate((x, y) -> parsePolygon(String.format("POLYGON ((0.5 -1, %d %d, 1.5 -1, 0.5 -1))", x, y)));
        assertThat(grid.toWKT())
                .isEqualTo("GEOMETRYCOLLECTION (POLYGON ((0.5 -1, 0 0, 1.5 -1, 0.5 -1)), POLYGON ((0.5 -1, 1 0, 1.5 -1, 0.5 -1)), POLYGON ((0.5 -1, 2 0, 1.5 -1, 0.5 -1)), POLYGON ((0.5 -1, 0 1, 1.5 -1, 0.5 -1)), POLYGON ((0.5 -1, 1 1, 1.5 -1, 0.5 -1)), POLYGON ((0.5 -1, 2 1, 1.5 -1, 0.5 -1)), POLYGON ((0.5 -1, 0 2, 1.5 -1, 0.5 -1)), POLYGON ((0.5 -1, 1 2, 1.5 -1, 0.5 -1)), POLYGON ((0.5 -1, 2 2, 1.5 -1, 0.5 -1)))");

    }

    @Test
    public void t02() {
        var grid1 = new FileGrid<Polygon>(3, 3, file("target/grid-test-02-01"), GEOMETRY_FACTORY);
        var grid2 = new FileGrid<Polygon>(3, 3, file("target/grid-test-02-02"), GEOMETRY_FACTORY);
        grid1.generate((x, y) -> parsePolygon("POLYGON ((" + x + " " + y + ", " + (x + 1) + " " + (y + 1) + ", " + (x + 1) + " " + y + ", " + x + " " + y + "))"));
        grid2.generate((x, y) -> parsePolygon("POLYGON ((" + x + " " + y + ", " + x + " " + (y + 1) + ", " + (x + 1) + " " + (y + 1) + ", " + x + " " + y + "))"));
        grid1.updateWith(grid2, (x, y, my, its) -> (Polygon) my.union(its));
        assertThat(grid1.toWKT()).isEqualTo("GEOMETRYCOLLECTION (POLYGON ((1 1, 1 0, 0 0, 0 1, 1 1)), POLYGON ((2 1, 2 0, 1 0, 1 1, 2 1)), POLYGON ((3 1, 3 0, 2 0, 2 1, 3 1)), POLYGON ((1 2, 1 1, 0 1, 0 2, 1 2)), POLYGON ((2 2, 2 1, 1 1, 1 2, 2 2)), POLYGON ((3 2, 3 1, 2 1, 2 2, 3 2)), POLYGON ((1 3, 1 2, 0 2, 0 3, 1 3)), POLYGON ((2 3, 2 2, 1 2, 1 3, 2 3)), POLYGON ((3 3, 3 2, 2 2, 2 3, 3 3)))");
    }
}
