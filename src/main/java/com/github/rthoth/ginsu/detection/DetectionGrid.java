package com.github.rthoth.ginsu.detection;

import com.github.rthoth.ginsu.Ginsu;
import com.github.rthoth.ginsu.Grid;
import com.github.rthoth.ginsu.Knife;
import com.github.rthoth.ginsu.Knife.X;
import com.github.rthoth.ginsu.Knife.Y;
import com.github.rthoth.ginsu.Shape;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequences;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.util.Arrays;

public class DetectionGrid {

    public final int width;
    public final int height;

    private final PVector<Slice<X>> x;
    private final PVector<Slice<Y>> y;

    private DetectionGrid(PVector<Slice<X>> x, PVector<Slice<Y>> y) {
        this.width = x.size();
        this.height = y.size();
        this.x = x;
        this.y = y;
    }

    public static DetectionGrid create(PVector<X> x, PVector<Y> y, boolean extrude) {
        var xSlices = createSlices(x, extrude);
        var ySlices = createSlices(y, extrude);
        return new DetectionGrid(xSlices, ySlices);
    }

    public static <K extends Knife> PVector<Slice<K>> createSlices(PVector<K> knives, boolean extrude) {
        if (!knives.isEmpty()) {
            var result = TreePVector.<Slice<K>>empty();
            var iterator = knives.iterator();
            var previous = Ginsu.next(iterator);
            result = result.plus(Slice.lower(!extrude ? previous : previous.getUpper()));
            while (iterator.hasNext()) {
                var current = iterator.next();
                result = result.plus(!extrude ? Slice.middle(previous, current) : Slice.middle(previous.getLower(), current.getUpper()));
                previous = current;
            }

            return result.plus(Slice.upper(!extrude ? previous : previous.getLower()));
        } else {
            return TreePVector.singleton(Slice.inner());
        }
    }

    public Grid<Cell> detect(Shape shape, boolean hasCorner, boolean detectTouch) {
        if (shape.nonEmpty()) {
            final var cells = Ginsu.array(width * height, Cell[]::new, i -> Cell.create(shape, hasCorner));
            for (var sequence : shape.sequences()) {
                for (var entry : Ginsu.withIndex(detect(shape, sequence, hasCorner, detectTouch))) {
                    cells[entry.index] = cells[entry.index].plus(entry.value);
                }
            }
            return Grid.xy(width, height, TreePVector.from(Arrays.asList(cells)));
        } else {
            return Grid.of(width, height, Cell.empty());
        }
    }

    private PVector<Cell.Detection> detect(Shape shape, CoordinateSequence sequence, boolean hasCorner, boolean detectTouch) {
        var factory = Event.factory(shape, sequence);
        final var coordinate = new Coordinate[]{sequence.getCoordinate(0)};

        var xDetectors = Ginsu.map(x, slice -> Detector.create(slice, factory, coordinate[0], detectTouch));
        var yDetectors = Ginsu.map(y, slice -> Detector.create(slice, factory, coordinate[0], detectTouch));

        var lastIndex = sequence.size() - 1;
        for (var i = 0; i < lastIndex; i++) {
            coordinate[0] = sequence.getCoordinate(i);
            for (var x : xDetectors)
                x.detect(coordinate[0], i);
            for (var y : yDetectors)
                y.detect(coordinate[0], i);
        }

        coordinate[0] = sequence.getCoordinate(lastIndex);
        var isRing = CoordinateSequences.isRing(sequence);
        for (var x : xDetectors)
            x.end(coordinate[0], lastIndex, isRing);
        for (var y : yDetectors)
            y.end(coordinate[0], lastIndex, isRing);

        var detections = TreePVector.<Cell.Detection>empty();
        for (var x : xDetectors)
            for (var y : yDetectors)
                detections = detections.plus(Cell.detect(x, y, hasCorner));

        return detections;
    }

    public int size() {
        return width * height;
    }
}
