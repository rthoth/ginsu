package com.github.rthoth.ginsu;

import com.github.rthoth.ginsu.Knife.X;
import com.github.rthoth.ginsu.Knife.Y;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

public class DetectionGrid {

	private final PVector<Cell<X>> x;
	private final PVector<Cell<Y>> y;

	public DetectionGrid(PVector<X> x, PVector<Y> y) {
		this.x = Cell.create(x);
		this.y = Cell.create(y);
	}

	protected int getWidth() {
		return !x.isEmpty() ? x.size() : 1;
	}

	protected int getHeight() {
		return !y.isEmpty() ? y.size() : 1;
	}

	public Grid<MultiShape> crop(MultiShape multiShape, Order order, Cropper cropper) {
		if (order == Order.X_Y || (order == Order.AUTOMATIC && x.size() >= y.size()))
			return Grid.xy(flatCrop(crop(multiShape, x, cropper), y, cropper), getWidth(), getHeight());
		else if (order == Order.Y_X || order == Order.AUTOMATIC)
			return Grid.yx(flatCrop(crop(multiShape, y, cropper), x, cropper), getWidth(), getHeight());
		else
			throw new IllegalArgumentException();
	}

	protected <K extends Knife<K>> PVector<MultiShape> crop(MultiShape multiShape, PVector<Cell<K>> cells, Cropper cropper) {
		if (!cells.isEmpty()) {
			if (multiShape.nonEmpty()) {
				return Ginsu.aggregate(MultiShape.MULTI_ROOT, Ginsu.mapToVector(multiShape, shape -> crop(shape, cells, cropper)));
			} else {
				return Ginsu.mapToVector(cells, cell -> MultiShape.EMPTY);
			}
		} else {
			return TreePVector.singleton(multiShape);
		}
	}

	protected <K extends Knife<K>> PVector<MultiShape> flatCrop(PVector<MultiShape> multiShapes, PVector<Cell<K>> cells, Cropper cropper) {
		return Ginsu.flatMapToVector(multiShapes, multiShape -> crop(multiShape, cells, cropper));
	}

	protected <K extends Knife<K>> PVector<MultiShape> crop(Shape shape, PVector<Cell<K>> cells, Cropper cropper) {
		if (!cells.isEmpty()) {
			if (shape.nonEmpty()) {
				var detections = Ginsu.aggregate(new ShapeDetection.Root(shape), Ginsu.mapToVector(shape, list -> detect(list, cells)));
				return Ginsu.mapToVector(detections, cropper);
			} else {
				return Ginsu.mapToVector(cells, cell -> MultiShape.EMPTY);
			}
		} else {
			return TreePVector.singleton(MultiShape.of(shape));
		}
	}

	protected <K extends Knife<K>> PVector<Detection> detect(IndexedCoordinateSequence sequence, PVector<Cell<K>> cells) {
		var factory = new Event.Factory(sequence);
		var detectors = Ginsu.mapToVector(cells, cell -> new Detector<>(cell, factory));

		final var first = sequence.get(0);
		for (var cell : detectors)
			cell.first(first);

		final var lastIndex = sequence.size() - 1;
		for (int i = 1; i < lastIndex; i++) {
			final var coordinate = sequence.get(i);
			for (var detector : detectors)
				detector.check(coordinate, i);
		}

		final var last = sequence.get(lastIndex);
		return Ginsu.mapToVector(detectors, detector -> detector.last(last, lastIndex, sequence.isClosed()));
	}

	public <T> Grid<T> empty(T emptyValue) {
		return new Grid.Fixed<>(x.size() + 1, y.size() + 1, emptyValue);
	}
}
