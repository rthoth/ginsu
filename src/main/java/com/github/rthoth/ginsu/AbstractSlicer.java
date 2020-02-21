package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

import java.util.Objects;

public abstract class AbstractSlicer<G extends Geometry> {

	protected static final Object UNDEFINED = new Object();

	protected final Order order;
	protected final DetectionGrid detectionGrid;
	protected Object grid = UNDEFINED;
	protected final MultiShape multiShape;
	protected final GeometryFactory factory;

	public AbstractSlicer(MultiShape multiShape, Order order, DetectionGrid detectionGrid, GeometryFactory factory) {
		Objects.requireNonNull(factory, "GeometryFactory is null!");
		this.order = order;
		this.detectionGrid = detectionGrid;
		this.multiShape = multiShape;
		this.factory = factory;
	}

	protected abstract Grid<G> compute();

	@SuppressWarnings("unchecked")
	public final Grid<G> getGrid() {
		if (grid == UNDEFINED) {
			synchronized (this) {
				if (grid == UNDEFINED)
					grid = compute();
			}
		}

		return (Grid<G>) grid;
	}
}