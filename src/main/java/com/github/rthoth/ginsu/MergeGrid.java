package com.github.rthoth.ginsu;

import com.github.rthoth.ginsu.Ginsu.IndexEntry;
import com.github.rthoth.ginsu.Knife.X;
import com.github.rthoth.ginsu.Knife.Y;
import com.github.rthoth.ginsu.detection.DetectionShape;
import com.github.rthoth.ginsu.detection.Detector;
import org.locationtech.jts.geom.Geometry;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.util.Optional;

public class MergeGrid<T extends Geometry> {

    private final Iterable<IndexEntry<Slice<X>>> xSlices;
    private final Iterable<IndexEntry<Slice<Y>>> ySlices;

    private final GeometryMerger<T> merger;
    private final int width;
    private final int height;
    private final PVector<X> x;
    private final PVector<Y> y;

    public MergeGrid(PVector<X> x, PVector<Y> y, GeometryMerger<T> merger) {
        this.merger = merger;
        this.width = x.size() + 1;
        this.height = y.size() + 1;
        this.x = x;
        this.y = y;

        var xSlices = x.size() > 0 ? slices(x) : TreePVector.singleton(Slice.<X>inner());
        var ySlices = y.size() > 0 ? slices(y) : TreePVector.singleton(Slice.<Y>inner());

        this.xSlices = Ginsu.zipWithIndex(xSlices);
        this.ySlices = Ginsu.zipWithIndex(ySlices);
    }

    private static <K extends Knife<K>> PVector<Slice<K>> slices(Iterable<K> iterable) {
        var iterator = iterable.iterator();
        var previous = Ginsu.next(iterator);
        var result = TreePVector.singleton(Slice.lower(previous));
        while (iterator.hasNext()) {
            var current = iterator.next();
            result = result.plus(Slice.middle(previous, current));
            previous = current;
        }

        return result.plus(Slice.upper(previous));
    }

    public T apply(Grid<MultiShape> grid) {
        if (grid.width == width && grid.height == height) {
            // TODO: Parallel?

            var shapes = TreePVector.<DetectionShape>empty();

            for (var xEntry : xSlices) {
                for (var yEntry : ySlices) {
                    shapes = shapes.plusAll(detect(xEntry.value, yEntry.value, grid.get(xEntry.index, yEntry.index)));
                }
            }

            return merger.apply(shapes, x, y);
        } else {
            throw new GinsuException.IllegalArgument("Invalid grid size!");
        }
    }

    private PVector<DetectionShape> detect(Slice<X> x, Slice<Y> y, Grid.Entry<Optional<MultiShape>> entry) {
        if (entry.value.isPresent()) {
            final var multishape = entry.value.get();
            if (multishape.nonEmpty()) {
                return detect(x, y, multishape);
            } else {
                return TreePVector.empty();
            }
        } else {
            return TreePVector.empty();
        }
    }

    private PVector<DetectionShape> detect(Slice<X> x, Slice<Y> y, MultiShape multishape) {
        var result = TreePVector.<DetectionShape>empty();
        for (final var shape : multishape) {
            result = result.plus(detect(x, y, shape));
        }

        return result;
    }

    private DetectionShape detect(Slice<X> x, Slice<Y> y, Shape shape) {
        final var iterator = shape.iterator();
        final var isPolygon = merger.isPolygon();
        var detections = TreePVector.singleton(Detector.detect(x, y, Ginsu.next(iterator), isPolygon));

        while (iterator.hasNext()) {
            detections = detections.plus(Detector.detect(x, y, iterator.next(), isPolygon));
        }

        return new DetectionShape(detections, shape);
    }
}
