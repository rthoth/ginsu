package com.github.rthoth.ginsu;

import com.github.rthoth.ginsu.io.FileGrid;
import org.junit.Test;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;

import java.util.concurrent.ForkJoinPool;

import static com.google.common.truth.Truth.assertThat;

public class FileGridTest extends AbstractTest implements Util {

    @Test
    public void t01() {
        var builder = new FileGrid.Builder<Polygon>(3, 3, file("target/grid-test-01"), GEOMETRY_FACTORY);
        builder.add((x, y) -> parsePolygon(String.format("POLYGON ((0.5 -1, %d %d, 1.5 -1, 0.5 -1))", x, y)));
        var grid = builder.build("test-01", (x, y, values) -> Ginsu.first(values));
        assertThat(grid.toWKT())
                .isEqualTo("GEOMETRYCOLLECTION (POLYGON ((0.5 -1, 0 0, 1.5 -1, 0.5 -1)), POLYGON ((0.5 -1, 1 0, 1.5 -1, 0.5 -1)), POLYGON ((0.5 -1, 2 0, 1.5 -1, 0.5 -1)), POLYGON ((0.5 -1, 0 1, 1.5 -1, 0.5 -1)), POLYGON ((0.5 -1, 1 1, 1.5 -1, 0.5 -1)), POLYGON ((0.5 -1, 2 1, 1.5 -1, 0.5 -1)), POLYGON ((0.5 -1, 0 2, 1.5 -1, 0.5 -1)), POLYGON ((0.5 -1, 1 2, 1.5 -1, 0.5 -1)), POLYGON ((0.5 -1, 2 2, 1.5 -1, 0.5 -1)))");

    }

    @Test
    public void t02() {
        var builder1 = new FileGrid.Builder<Polygon>(3, 3, file("target/grid-test-02-01"), GEOMETRY_FACTORY);
        var builder2 = new FileGrid.Builder<Polygon>(3, 3, file("target/grid-test-02-02"), GEOMETRY_FACTORY);

        builder1.add((x, y) -> parsePolygon("POLYGON ((" + x + " " + y + ", " + (x + 1) + " " + (y + 1) + ", " + (x + 1) + " " + y + ", " + x + " " + y + "))"));
        builder2.add((x, y) -> parsePolygon("POLYGON ((" + x + " " + y + ", " + x + " " + (y + 1) + ", " + (x + 1) + " " + (y + 1) + ", " + x + " " + y + "))"));

        var grid1 = builder1.build("first-element", (x, y, values) -> Ginsu.first(values));
        var grid2 = builder2.build("first-element", (x, y, values) -> Ginsu.first(values));

        var unionBuilder = new FileGrid.Builder<Polygon>(3, 3, file("target/grid-test-02-union"), GEOMETRY_FACTORY);
        unionBuilder
                .add(grid1)
                .add(grid2);

        var union = unionBuilder.build("result", (x, y, values) -> (Polygon) CascadedPolygonUnion.union(values), ForkJoinPool.commonPool());
        assertThat(union.toWKT()).isEqualTo("GEOMETRYCOLLECTION (POLYGON ((1 1, 1 0, 0 0, 0 1, 1 1)), POLYGON ((2 1, 2 0, 1 0, 1 1, 2 1)), POLYGON ((3 1, 3 0, 2 0, 2 1, 3 1)), POLYGON ((1 2, 1 1, 0 1, 0 2, 1 2)), POLYGON ((2 2, 2 1, 1 1, 1 2, 2 2)), POLYGON ((3 2, 3 1, 2 1, 2 2, 3 2)), POLYGON ((1 3, 1 2, 0 2, 0 3, 1 3)), POLYGON ((2 3, 2 2, 1 2, 1 3, 2 3)), POLYGON ((3 3, 3 2, 2 2, 2 3, 3 3)))");
    }
}
