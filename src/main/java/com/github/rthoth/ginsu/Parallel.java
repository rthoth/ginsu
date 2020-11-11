package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygonal;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BiFunction;

public class Parallel {

    private static <A extends Polygonal, B extends Polygonal> CompletableFuture<MultiPolygon> _polygonal(GridPattern pattern, int limit, A a, B b, BiFunction<MultiPolygon, MultiPolygon, MultiPolygon> function, Executor executor) {
        var _a = Ginsu.toMulti(a);
        var _b = Ginsu.toMulti(b);

        if (_a.getNumPoints() + _b.getNumPoints() <= limit)
            return CompletableFuture.supplyAsync(() -> function.apply(_a, _b), executor);
        else {
            var slicer = pattern.slicer(executor, _a, _b);
            var aGrid = slicer.thenApplyAsync(s -> s.polygonal(_a), executor);
            var bGrid = slicer.thenApplyAsync(s -> s.extrude(0.01).polygonal(_b), executor);

            var factory = _a.getFactory();

            return aGrid.thenCombine(bGrid, PolygonCtx::new)
                    .thenCombine(slicer, PolygonCtx::slicer)
                    .thenComposeAsync(ctx -> {
                        var _1 = ctx._1;
                        var _2 = ctx._2;

                        var grid = _1.combine(_2, (m1, m2) -> _polygonal(pattern, limit, m1, m2, function, executor))
                                .copy();
                        return CompletableFuture
                                .allOf(Ginsu.map(grid.iterable(), entry -> entry.value).toArray(CompletableFuture[]::new))
                                .thenComposeAsync(v -> CompletableFuture.supplyAsync(() -> ctx.slicer.merger().polygonal(grid.view(CompletableFuture::join), factory)));
                    });
        }
    }

    public static GridPattern grid(int x, int y) {
        return new GridPattern(x, y);
    }

    public static <A extends Polygonal, B extends Polygonal> CompletionStage<MultiPolygon> polygonal(GridPattern pattern, int limit, A a, B b, BiFunction<MultiPolygon, MultiPolygon, MultiPolygon> function, Executor executor) {
        return _polygonal(pattern, limit, a, b, function, executor);
    }

    public static <A extends Polygonal, B extends Polygonal> CompletionStage<MultiPolygon> polygonal(GridPattern pattern, int limit, A a, B b, BiFunction<MultiPolygon, MultiPolygon, MultiPolygon> function) {
        return _polygonal(pattern, limit, a, b, function, ForkJoinPool.commonPool());
    }

    public static final class GridPattern {

        private final int x;
        private final int y;

        private GridPattern(int x, int y) {
            this.x = x;
            this.y = y;
        }

        private double[] createArray(double min, double max, int n) {
            if (n > 0) {
                var array = new double[n];
                var size = (max - min) / (array.length + 1);
                for (var i = 0; i < array.length; i++) {
                    min += size;
                    array[i] = min;
                }

                return array;
            } else {
                return new double[0];
            }
        }

        private CompletableFuture<Slicer> slicer(Executor executor, Geometry... geometries) {
            return CompletableFuture.supplyAsync(() -> {
                var env = new Envelope(geometries[0].getEnvelopeInternal());
                for (var i = 1; i < geometries.length; i++) {
                    env.expandToInclude(geometries[i].getEnvelopeInternal());
                }

                var x = createArray(env.getMinX(), env.getMaxX(), this.x);
                var y = createArray(env.getMinY(), env.getMaxY(), this.y);

                return new Slicer(x, y);
            }, executor);
        }
    }

    private static final class PolygonCtx {

        private final Grid<MultiPolygon> _1;
        private final Grid<MultiPolygon> _2;
        private Slicer slicer;

        private PolygonCtx(Grid<MultiPolygon> _1, Grid<MultiPolygon> _2) {
            this._1 = _1;
            this._2 = _2;
        }

        public PolygonCtx slicer(Slicer slicer) {
            this.slicer = slicer;
            return this;
        }
    }
}
