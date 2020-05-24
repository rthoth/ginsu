package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.util.Optional;

public class SliceGrid<T extends Geometry> {

    protected final PVector<SCell> xCells;
    protected final PVector<SCell> yCells;
    protected final GeometrySlicer<T> slicer;

    public SliceGrid(PVector<Knife.X> x, PVector<Knife.Y> y, GeometrySlicer<T> slicer) {
        xCells = SCell.from(x);
        yCells = SCell.from(y);
        this.slicer = slicer;
    }

    @SuppressWarnings("unused")
    public Grid<T> apply(MultiShape multishape) {
        return apply(multishape, Order.AUTOMATIC);
    }

    public Grid<T> apply(MultiShape multishape, Order order) {
        if (order == Order.XY || (order == Order.AUTOMATIC && xCells.size() >= yCells.size())) {
            return xy(multishape);
        } else {
            return yx(multishape);
        }
    }

    private PVector<SDetection> detect(PVector<SCell> cells, CoordinateSequence sequence) {
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
        var detections = TreePVector.<SDetection>empty();
        for (var detector : detectors) {
            detections = detections.plus(detector.last(lastIndex, lastCoordinate));
        }

        return detections;
    }

    private PVector<MultiShape> slice(PVector<SCell> cells, Shape shape) {
        final var iterator = shape.iterator();
        var current = Ginsu.next(iterator);

        var classified = Ginsu.map(detect(cells, current), detection -> slicer.classify(detection, shape));
        var unreadyVector = Ginsu.collect(Ginsu.zipWithIndex(classified), entry ->
                entry.value instanceof SDetection.Unready ?
                        Optional.of(entry.copy((SDetection.Unready) entry.value)) : Optional.empty());

        if (!unreadyVector.isEmpty()) {
            final var unreadyCells = Ginsu.map(unreadyVector, entry -> cells.get(entry.index));
            while (iterator.hasNext()) {
                var sequence = iterator.next();
                var detections = detect(unreadyCells, sequence);
                for (var index = 0; index < detections.size(); index++) {
                    unreadyVector.get(index).value.add(detections.get(index));
                }
            }
        }

        // TODO: Parallel?
        var slices = TreePVector.<MultiShape>empty();
        for (var status : classified) {
            if (status instanceof SDetection.Unready) {
                slices = slices.plus(slicer.slice(((SDetection.Unready) status).getDetections()));
            } else if (status instanceof SDetection.Ready)
                slices = slices.plus(MultiShape.of(((SDetection.Ready) status).shape));
        }

        return slices;
    }

    private PVector<T> slice(PVector<SCell> _1, PVector<SCell> _2, MultiShape multiShape) {
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

    private PVector<MultiShape> slice(PVector<SCell> cells, MultiShape multishape) {
        if (!cells.isEmpty()) {
            if (multishape.nonEmpty()) {
                var slices = TreePVector.<PVector<MultiShape>>empty();
                for (var shape : multishape)
                    slices = slices.plus(slice(cells, shape));

                return Ginsu.flatten(slices);
            } else {
                return Ginsu.map(cells, cell -> MultiShape.EMPTY);
            }
        } else {
            // TODO: Check!
            return TreePVector.empty();
        }
    }

    private Grid<T> xy(MultiShape multishape) {
        return new Grid.XY<>(xCells.size(), yCells.size(), slice(xCells, yCells, multishape));
    }

    private Grid<T> yx(MultiShape multishape) {
        return new Grid.YX<>(xCells.size(), yCells.size(), slice(yCells, xCells, multishape));
    }
}
