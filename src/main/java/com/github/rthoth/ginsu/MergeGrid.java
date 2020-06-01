package com.github.rthoth.ginsu;

import com.github.rthoth.ginsu.Ginsu.IndexEntry;
import com.github.rthoth.ginsu.Knife.X;
import com.github.rthoth.ginsu.Knife.Y;
import org.locationtech.jts.geom.Geometry;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.util.Optional;

public class MergeGrid<T extends Geometry> {

    private final Iterable<IndexEntry<Slice>> xSlices;
    private final Iterable<IndexEntry<Slice>> ySlices;

    private final GeometryMerger<T> merger;
    private final int width;
    private final int height;

    public MergeGrid(PVector<X> x, PVector<Y> y, GeometryMerger<T> merger) {
        this.merger = merger;
        this.width = x.size() + 1;
        this.height = y.size() + 1;

        var xSlices = x.size() > 0 ? slices(x) : TreePVector.singleton(Slice.INNER);
        var ySlices = y.size() > 0 ? slices(y) : TreePVector.singleton(Slice.INNER);

        this.xSlices = Ginsu.zipWithIndex(xSlices);
        this.ySlices = Ginsu.zipWithIndex(ySlices);
    }

    private static <K extends Knife<K>> PVector<Slice> slices(Iterable<K> iterable) {
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

            var detections = TreePVector.<MShape.Detection>empty();
            var shapes = TreePVector.<Shape>empty();

            for (var xEntry : xSlices) {
                for (var yEntry : ySlices) {
                    for (var mshape : detect(xEntry.value, yEntry.value, grid.get(xEntry.index, yEntry.index))) {
                        if (mshape instanceof MShape.Ongoing) {
                            detections = detections.plusAll(((MShape.Ongoing) mshape).getDetections());
                        } else if (mshape instanceof MShape.Done) {
                            var shape = ((MShape.Done) mshape).shape;
                            if (shape.nonEmpty())
                                shapes = shapes.plus(shape);
                        } else {
                            throw new GinsuException.IllegalState("Invalid detection!");
                        }
                    }
                }
            }

            return merger.merge(detections, shapes);
        } else {
            throw new GinsuException.IllegalArgument("Invalid grid size!");
        }
    }

    private PVector<MShape> detect(Slice x, Slice y, Grid.Entry<Optional<MultiShape>> entry) {
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

    private PVector<MShape> detect(Slice x, Slice y, MultiShape multishape) {
        var result = TreePVector.<MShape>empty();
        for (final var shape : multishape) {
            result = result.plus(detect(x, y, shape));
        }

        return result;
    }

    private MShape detect(Slice x, Slice y, Shape shape) {
        final var iterator = shape.iterator();
        final var first = MDetector.detect(x, y, Ginsu.next(iterator));
        var mshape = merger.classify(first, shape);

        if (mshape instanceof MShape.Ongoing) {
            var ongoing = (MShape.Ongoing) mshape;
            while (iterator.hasNext()) {
                ongoing.add(MDetector.detect(x, y, iterator.next()));
            }

            return ongoing;
        } else if (mshape instanceof MShape.Done) {
            return mshape;
        } else {
            throw new GinsuException.IllegalState("Invalid status [" + mshape + "]!");
        }
    }
}