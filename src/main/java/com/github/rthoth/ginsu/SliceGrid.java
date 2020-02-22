package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequences;
import org.locationtech.jts.geom.Geometry;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.util.Optional;

public class SliceGrid<T extends Geometry> {

    private final Grid<T> result;

    public SliceGrid(PVector<Knife.X> x, PVector<Knife.Y> y, MultiShape multishape, GeometrySlicer<T> slicer, Order order) {
        var xCells = Cell.from(x);
        var yCells = Cell.from(y);
        result = order == Order.XY || (order == Order.AUTOMATIC && (xCells.size() >= yCells.size())) ?
                xy(xCells, yCells, multishape, slicer) : yx(yCells, xCells, multishape, slicer);
    }

    private PVector<Detection> detect(PVector<Cell> cells, CoordinateSequence sequence) {
        final var factory = new Event.Factory(sequence);
        final var detectors = Ginsu.map(cells, cell -> new Detector(cell, factory));

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
        final var closed = CoordinateSequences.isRing(sequence);
        var detections = TreePVector.<Detection>empty();
        for (var detector : detectors) {
            detections = detections.plus(detector.last(lastIndex, lastCoordinate));
        }

        return detections;
    }

    public Grid<T> getResult() {
        return result;
    }

    private PVector<MultiShape> slice(PVector<Cell> cells, Shape shape, GeometrySlicer<T> slicer) {
        final var iterator = shape.iterator();
        var current = Ginsu.next(iterator);
        var classified = Ginsu.map(detect(cells, current), detection -> slicer.classify(detection, shape));
        var unreadyVector = Ginsu.collect(Ginsu.zipWithIndex(classified), entry ->
                entry.value instanceof Detection.Unready ?
                        Optional.of(entry.copy((Detection.Unready) entry.value)) : Optional.empty());

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
            if (status instanceof Detection.Unready) {
                slices = slices.plus(slicer.slice(((Detection.Unready) status).getDetections()));
            } else if (status instanceof Detection.Ready)
                slices = slices.plus(MultiShape.of(((Detection.Ready) status).shape));
        }

        return slices;
    }

    private PVector<T> slice(PVector<Cell> _1, PVector<Cell> _2, MultiShape multiShape, GeometrySlicer<T> slicer) {
        var data = TreePVector.<T>empty();
        if (!_1.isEmpty() && !_2.isEmpty()) {
            for (var _1Cell : slice(_1, multiShape, slicer)) {
                for (var _2Cell : slice(_2, _1Cell, slicer)) {
                    data = data.plus(slicer.apply(_2Cell));
                }
            }
        } else if (!_1.isEmpty()) {
            for (var _1Cell : slice(_1, multiShape, slicer)) {
                data = data.plus(slicer.apply(_1Cell));
            }
        } else if (!_2.isEmpty()) {
            for (var _2Cell : slice(_2, multiShape, slicer)) {
                data = data.plus(slicer.apply(_2Cell));
            }
        } else {
            data = data.plus(slicer.apply(multiShape));
        }

        return data;
    }

    private PVector<MultiShape> slice(PVector<Cell> cells, MultiShape multishape, GeometrySlicer<T> slicer) {
        if (!cells.isEmpty()) {
            if (multishape.nonEmpty()) {
                var slices = TreePVector.<PVector<MultiShape>>empty();
                for (var shape : multishape)
                    slices = slices.plus(slice(cells, shape, slicer));

                return Ginsu.flatten(slices);
            } else {
                return Ginsu.map(cells, cell -> MultiShape.EMPTY);
            }
        } else {
            return TreePVector.empty();
        }
    }

    private Grid<T> xy(PVector<Cell> xCells, PVector<Cell> yCells, MultiShape multishape, GeometrySlicer<T> slicer) {
        return new Grid.XY<>(xCells.size(), yCells.size(), slice(xCells, yCells, multishape, slicer));
    }

    private Grid<T> yx(PVector<Cell> yCells, PVector<Cell> xCells, MultiShape multishape, GeometrySlicer<T> slicer) {
        return new Grid.YX<>(xCells.size(), yCells.size(), slice(yCells, xCells, multishape, slicer));
    }
}
