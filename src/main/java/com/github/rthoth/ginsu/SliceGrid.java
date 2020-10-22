package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequences;
import org.locationtech.jts.geom.Geometry;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.util.ArrayList;

public class SliceGrid<T extends Geometry> {

    protected final PVector<Slice> xSlices;
    protected final PVector<Slice> ySlices;
    protected final GeometrySlicer<T> slicer;
    protected final double offset;

    public SliceGrid(PVector<Knife.X> x, PVector<Knife.Y> y, double offset, GeometrySlicer<T> slicer) {
        xSlices = Slice.from(x);
        ySlices = Slice.from(y);
        this.slicer = slicer;
        this.offset = offset;
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

    private PVector<Detection> detect(PVector<Slice> slices, CoordinateSequence sequence) {
        final var factory = new Event.Factory(sequence);
        final var detectors = Ginsu.map(slices, slice -> Detector.create(slice, factory));

        final var firstCoordinate = sequence.getCoordinate(0);
        for (var detector : detectors)
            detector.begin(firstCoordinate);

        final var lastIndex = sequence.size() - 1;
        for (var i = 1; i < lastIndex; i++) {
            final var coordinate = sequence.getCoordinate(i);
            for (var detector : detectors)
                detector.check(i, coordinate);
        }

        final var lastCoordinate = sequence.getCoordinate(lastIndex);
        final var isRing = CoordinateSequences.isRing(sequence);
        return Ginsu.map(detectors, detector -> detector.end(lastIndex, lastCoordinate, isRing));
    }

    private PVector<MultiShape> slice(PVector<Slice> slices, Shape shape) {
        final var iterator = shape.iterator();
        var ongoings = TreePVector.<Ongoing>empty();
        var result = new ArrayList<MultiShape>(slices.size());

        for (var entry : Ginsu.zipWithIndex(detect(slices, Ginsu.next(iterator)))) {
            var detection = entry.value;
            var optional = slicer.preApply(detection, shape);
            if (optional.isEmpty()) {
                ongoings = ongoings.plus(new Ongoing(entry.index, detection, slices.get(entry.index), shape));
                result.add(null);
            } else {
                result.add(MultiShape.of(optional.get()));
            }
        }

        if (!ongoings.isEmpty()) {
            var ongoingSlices = Ginsu.map(ongoings, o -> o.slice);

            while (iterator.hasNext()) {
                for (var entry : Ginsu.zipWithIndex(detect(ongoingSlices, iterator.next()))) {
                    ongoings.get(entry.index).add(entry.value);
                }
            }

            for (var ongoing : ongoings) {
                result.set(ongoing.index, ongoing.apply());
            }
        }

        return TreePVector.from(result);
    }

    private PVector<T> slice(PVector<Slice> _1, PVector<Slice> _2, MultiShape multiShape) {
        var data = TreePVector.<T>empty();
        if (!_1.isEmpty() && !_2.isEmpty()) {
            for (var _1Cell : slice(_1, multiShape)) {
                for (var _2Cell : slice(_2, _1Cell)) {
                    data = data.plus(slicer.toGeometry(_2Cell));
                }
            }
        } else if (!_1.isEmpty()) {
            for (var _1Cell : slice(_1, multiShape)) {
                data = data.plus(slicer.toGeometry(_1Cell));
            }
        } else if (!_2.isEmpty()) {
            for (var _2Cell : slice(_2, multiShape)) {
                data = data.plus(slicer.toGeometry(_2Cell));
            }
        } else {
            data = data.plus(slicer.toGeometry(multiShape));
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

    private class Ongoing {

        final int index;
        final Detection detection;
        final Slice slice;
        final Shape shape;

        private PVector<Detection> detections;

        public Ongoing(int index, Detection detection, Slice slice, Shape shape) {
            this.index = index;
            this.detection = detection;
            this.slice = slice;
            this.shape = shape;
            detections = TreePVector.singleton(detection);
        }

        public void add(Detection detection) {
            detections = detections.plus(detection);
        }

        public MultiShape apply() {
            return slicer.apply(new DetectionShape(detections, shape), slice.getDimension(), offset);
        }
    }
}
