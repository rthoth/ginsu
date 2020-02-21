package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;

public class LineStringSlicer extends AbstractSlicer<MultiLineString> {

	public LineStringSlicer(LineString lineString, Order order, DetectionGrid detectionGrid) {
		super(MultiShape.of(lineString), order, detectionGrid, lineString.getFactory());
	}

	public LineStringSlicer(MultiLineString multiLineString, Order order, DetectionGrid detectionGrid) {
		super(MultiShape.of(multiLineString), order, detectionGrid, multiLineString.getFactory());
	}

	@Override
	protected Grid<MultiLineString> compute() {
		throw new UnsupportedOperationException();
	}
}
