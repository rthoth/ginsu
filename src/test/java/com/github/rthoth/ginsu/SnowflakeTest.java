package com.github.rthoth.ginsu;

import org.junit.Test;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.overlay.OverlayOp;
import org.locationtech.jts.shape.fractal.KochSnowflakeBuilder;

import java.util.concurrent.Executors;

public class SnowflakeTest extends AbstractTest implements Util {

    Lazy<Polygon> snowflake01 = lazy(() -> create(3500000, envelope(-1000, 1000, -1000, 1000)));
    Lazy<Polygon> snowflake02 = lazy(() -> create(7000000, envelope(-990, 1010, -990, 1010)));

    Lazy<Polygon> smallSnowflake01 = lazy(() -> create(4000, envelope(-1000, 1000, -1000, 1000)));
    Lazy<Polygon> smallSnowflake02 = lazy(() -> create(8000, envelope(-990, 1010, -990, 1010)));

    Polygon create(int points, Envelope envelope) {
        var builder = new KochSnowflakeBuilder(GEOMETRY_FACTORY);
        builder.setExtent(envelope);
        builder.setNumPoints(points);
        return (Polygon) builder.getGeometry();
    }

    private MultiPolygon difference(Polygon p1, Polygon p2) {
        return difference(toMultiPolygon(p1), toMultiPolygon(p2));
    }

    private MultiPolygon difference(MultiPolygon p1, MultiPolygon p2) {
        var result = OverlayOp.overlayOp(p1, p2, OverlayOp.DIFFERENCE);
        println(String.format("difference(a=%d, b=%d, a+b=%d, r=%d)", p1.getNumPoints(), p2.getNumPoints(), p1.getNumPoints() + p2.getNumPoints(), result.getNumPoints()));
        return toMultiPolygon(result);
    }

    @Test
    public void t00_with() {
        var result = Parallel.polygonal(Parallel.grid(2, 2), 100, smallSnowflake01.get(), smallSnowflake02.get(), this::difference, Executors.newFixedThreadPool(2))
                .toCompletableFuture().join();

        compareTopology().compare(result, difference(smallSnowflake01.get(), smallSnowflake02.get()));
        ///FileUtils.writeStringToFile(file("target/t00_with"), result.toText(), StandardCharsets.UTF_8);
    }

    @Test
    public void t00_without() {
        difference(smallSnowflake01.get(), smallSnowflake02.get());
    }

    @Test
    public void t01_with() {
        Parallel.polygonal(Parallel.grid(2, 2), 100000, snowflake01.get(), snowflake02.get(), this::difference, Executors.newFixedThreadPool(4))
                .toCompletableFuture().join();
    }

    @Test
    public void t01_without() {
        difference(snowflake01.get(), snowflake02.get());
    }
}
