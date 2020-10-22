package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.shape.fractal.KochSnowflakeBuilder;

import java.util.concurrent.Executors;

public class SnowflakeTest extends AbstractTest implements Util {

    Lazy<Polygon> snowflake01 = lazy(() -> create(35000000, envelope(-1000, 1000, -1000, 1000)));
    Lazy<Polygon> snowflake02 = lazy(() -> create(70000000, envelope(-990, 1010, -990, 1010)));

    Polygon create(int points, Envelope envelope) {
        var builder = new KochSnowflakeBuilder(GEOMETRY_FACTORY);
        builder.setExtent(envelope);
        builder.setNumPoints(points);
        return (Polygon) builder.getGeometry();
    }

    //    @Test
    public void t01_with() {
        println(String.format("snowflake01: %d, snowflake02: %d", snowflake01.get().getNumPoints(), snowflake02.get().getNumPoints()));
        var result = Parallel.polygonal(Parallel.grid(1, 1), 100000, snowflake01.get(), snowflake02.get(), (p1, p2) -> {
            //println(String.format("Points: %d", p1.getNumPoints() + p2.getNumPoints()));
            return toMultiPolygon(p1.difference(p2));
        }, Executors.newFixedThreadPool(8)).toCompletableFuture().join();
        println(String.format("Result: %d", result.getNumPoints()));
    }

    //    @Test
    public void t01_without() {
        snowflake01.get().intersection(snowflake02.get());
    }
}
