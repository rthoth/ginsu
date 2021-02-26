package com.github.rthoth.ginsu;

import com.github.rthoth.ginsu.detection.DetectionGrid;
import com.github.rthoth.ginsu.detection.MultiCell;
import org.locationtech.jts.geom.Geometry;
import org.pcollections.PVector;

public class SlicerGrid<T extends Geometry> {

    private final DetectionGrid detectionGrid;
    private final GeometrySlicer<T> slicer;

    public SlicerGrid(PVector<Knife.X> x, PVector<Knife.Y> y, GeometrySlicer<T> slicer) {
        this.slicer = slicer;
        this.detectionGrid = DetectionGrid.create(x, y, true);
    }

    public Grid<T> apply(MultiShape multiShape) {
        if (multiShape.nonEmpty()) {
            var grid = Grid.xy(detectionGrid.width, detectionGrid.height, Ginsu.vector(detectionGrid.size(), i -> MultiCell.empty()));
            for (var shape : multiShape.shapes()) {
                grid = grid.sync()
                           .zip(detectionGrid.detect(shape, slicer.hasCorner(), slicer.hasTouch()), (x, y, multiCell, cell) -> multiCell.plus(cell))
                           .end();
            }

            return grid.sync()
                       .map((x, y, multiCell) -> slicer.slice(multiCell))
                       .end();
        } else {
            return Grid.of(detectionGrid.width, detectionGrid.height, slicer.empty());
        }
    }
}
