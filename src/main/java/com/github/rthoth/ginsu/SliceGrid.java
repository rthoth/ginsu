package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.util.Optional;

public class SliceGrid<T extends Geometry> {

    protected final PVector<Slice> xSlices;
    protected final PVector<Slice> ySlices;
    protected final GeometrySlicer<T> slicer;

    public SliceGrid(PVector<Knife.X> x, PVector<Knife.Y> y, GeometrySlicer<T> slicer) {
        xSlices = Slice.from(x);
        ySlices = Slice.from(y);
        this.slicer = slicer;
    }

    @SuppressWarnings("unused")
    public Grid<T> apply(MultiShape multishape) {
        return apply(multishape, Order.AUTOMATIC);
    }

    public Grid<T> apply(MultiShape multishape, Order order) {
        if (order == Order.XY || (order == Order.AUTOMATIC && xSlices.size() >= ySlices.size())) {
            return xy(multishape);
        } else {
            return yx(multishape);
        }
    }

    private PVector<SShape.Detection> detect(PVector<Slice> cells, CoordinateSequence sequence) {
        final var factory = new SEvent.Factory(sequence);
        final var detectors = Ginsu.map(cells, cell -> new SDetector(cell, factory));

        final var lastIndex = sequence.size() - 1;
        final var firstCoordinate = sequence.getCoordinate(0);

        for (var detector : detectors) {
            detector.first(firstCoordinate);
        }

        for (var index = 1; index < lastIndex; index++) {
            final var coordinate = sequence.getCoordinate(index);
            for (var detector : detectors) {
                detector.check(index, coordinate);
            }
        }

        final var lastCoordinate = sequence.getCoordinate(lastIndex);
        var detections = TreePVector.<SShape.Detection>empty();
        for (var detector : detectors) {
            detections = detections.plus(detector.last(lastIndex, lastCoordinate));
        }

        return detections;
    }

    private PVector<MultiShape> slice(PVector<Slice> slices, Shape shape) {
        final var iterator = shape.iterator();
        var current = Ginsu.next(iterator);

        var classified = Ginsu.map(detect(slices, current), detection -> slicer.classify(detection, shape));
        var ongoing = Ginsu.collect(Ginsu.zipWithIndex(classified), entry ->
                entry.value instanceof SShape.Ongoing ?
                        Optional.of(entry.copy((SShape.Ongoing) entry.value)) : Optional.empty());

        if (!ongoing.isEmpty()) {
            final var ongoingSlices = Ginsu.map(ongoing, entry -> slices.get(entry.index));
            while (iterator.hasNext()) {
                var detections = detect(ongoingSlices, iterator.next());

                for (var index = 0; index < detections.size(); index++) {
                    ongoing.get(index).value.add(detections.get(index));
                }
            }
        }

        // TODO: Parallel?
        var multishapes = TreePVector.<MultiShape>empty();
        for (var sshape : classified) {
            if (sshape instanceof SShape.Ongoing) {
                multishapes = multishapes.plus(slicer.slice(((SShape.Ongoing) sshape).getDetections()));
            } else if (sshape instanceof SShape.Done)
                multishapes = multishapes.plus(MultiShape.of(((SShape.Done) sshape).shape));
        }

        return multishapes;
    }

    private PVector<T> slice(PVector<Slice> _1, PVector<Slice> _2, MultiShape multiShape) {
        var data = TreePVector.<T>empty();
        if (!_1.isEmpty() && !_2.isEmpty()) {
            for (var _1Cell : slice(_1, multiShape)) {
                for (var _2Cell : slice(_2, _1Cell)) {
                    data = data.plus(slicer.apply(_2Cell));
                }
            }
        } else if (!_1.isEmpty()) {
            for (var _1Cell : slice(_1, multiShape)) {
                data = data.plus(slicer.apply(_1Cell));
            }
        } else if (!_2.isEmpty()) {
            for (var _2Cell : slice(_2, multiShape)) {
                data = data.plus(slicer.apply(_2Cell));
            }
        } else {
            data = data.plus(slicer.apply(multiShape));
        }

        return data;
    }

    private PVector<MultiShape> slice(PVector<Slice> slices, MultiShape multishape) {
        if (!slices.isEmpty()) {
            if (multishape.nonEmpty()) {
                var multishapes = TreePVector.<PVector<MultiShape>>empty();
                for (var shape : multishape)
                    multishapes = multishapes.plus(slice(slices, shape));

                return Ginsu.flatten(multishapes);
            } else {
                return Ginsu.map(slices, cell -> MultiShape.EMPTY);
            }
        } else {
            // TODO: Check!
            return TreePVector.empty();
        }
    }

    private Grid<T> xy(MultiShape multishape) {
        return new Grid.XY<>(xSlices.size(), ySlices.size(), slice(xSlices, ySlices, multishape));
    }

    private Grid<T> yx(MultiShape multishape) {
        return new Grid.YX<>(xSlices.size(), ySlices.size(), slice(ySlices, xSlices, multishape));
    }
}
