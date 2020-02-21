package com.github.rthoth.ginsu;

import org.locationtech.jts.algorithm.RayCrossingCounter;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.TopologyException;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import javax.validation.constraints.NotNull;

import java.util.LinkedList;

import static org.locationtech.jts.geom.Location.BOUNDARY;
import static org.locationtech.jts.geom.Location.INTERIOR;

public class PolygonSlicer extends AbstractSlicer<MultiPolygon> {

	private static final int FORWARD = 1;
	private static final int BACKWARD = 2;

	private final Cropper cropper = shapeDetection -> {
		if (shapeDetection.nonEmpty()) {
			return new Algorithm(shapeDetection).result;
		} else {
			return MultiShape.EMPTY;
		}
	};

	public PolygonSlicer(@NotNull Polygon polygon, @NotNull Order order, @NotNull DetectionGrid detectionGrid) {
		super(MultiShape.of(polygon), order, detectionGrid, polygon.getFactory());
	}

	public PolygonSlicer(@NotNull MultiPolygon multiPolygon, @NotNull Order order, @NotNull DetectionGrid detectionGrid) {
		super(MultiShape.of(multiPolygon), order, detectionGrid, multiPolygon.getFactory());
	}

	@Override
	protected Grid<MultiPolygon> compute() {
		if (multiShape.nonEmpty()) {
			return detectionGrid.crop(multiShape, order, cropper)
				.map(entry -> MultiShape.toMultiPolygon(entry.getData(), factory));
		} else {
			return detectionGrid.empty(factory.createMultiPolygon());
		}
	}

	private static class Algorithm {

		MultiShape result;
		SliceBorder border;
		LinkedList<CoordinateSequence> inside;

		Event origin;

		int direction = 0;


		public Algorithm(ShapeDetection shapeDetection) {
			var detections = shapeDetection.iterator();
			var firstDetection = Ginsu.next(detections);
			if (firstDetection.nonEmpty()) {
				inside = new LinkedList<>();
				border = new SliceBorder();
				border.add(firstDetection);

				while (detections.hasNext()) {
					var detection = detections.next();
					if (detection.nonEmpty()) {
						border.add(detection);
					} else if (detection.getLocation() == Location.INSIDE) {
						inside.add(detection.getSequence().getCoordinateSequence());
					}
				}

				var shells = TreePVector.<CoordinateSequence>empty();

				while (border.hasMore()) {
					searchOrigin();

					if (origin != null) {
						shells = shells.plus(createRing());
					} else {
						throw new IllegalStateException();
					}
				}

				result = MultiShape.of(createShapes(shells));
			} else {
				if (firstDetection.getLocation() == Location.INSIDE)
					result = shapeDetection.getSourceAsMultiShape();
				else
					result = MultiShape.EMPTY;
			}
		}

		void searchOrigin() {
			var first = border.first();
			var last = border.last();

			if (first instanceof Event.In && last instanceof Event.In) {
				if (first.index <= last.index)
					defineOrigin(first, FORWARD);
				else
					defineOrigin(last, BACKWARD);
			} else if (first instanceof Event.Out && last instanceof Event.Out) {
				if (first.index >= last.index)
					defineOrigin(first, FORWARD);
				else
					defineOrigin(last, BACKWARD);
			} else {
				if (first instanceof Event.In)
					defineOrigin(first, FORWARD);
				else
					defineOrigin(last, BACKWARD);
			}
		}

		void defineOrigin(Event origin, int direction) {
			this.origin = origin;
			this.direction = direction;
		}

		CoordinateSequence createRing() {
			var start = origin;
			var builder = new SegmentedCoordinateSequence.Builder(2);
			Event stop;

			do {
				if (start instanceof Event.In) {
					stop = border.nextEvent(start);
					builder.forward(start.getCoordinateSequence(), start, stop);
				} else {
					stop = border.previousEvent(start);
					builder.backward(start.getCoordinateSequence(), start, stop);
				}

				if (onSameBorder(stop, origin)) {
					if (direction == FORWARD) {
						start = border.lower(stop);
					} else {
						start = border.higher(stop);
					}
				} else {
					if (direction == FORWARD) {
						start = border.higher(stop);
					} else {
						start = border.lower(stop);
					}
				}

			} while (start != origin && start != null);

			if (start == origin) {
				var opt = builder.close();

				if (opt.isPresent())
					return opt.get();
				else
					throw new TopologyException("No Coordinate Sequence!");
			} else {
				throw new TopologyException("Start is null!");
			}
		}

		PVector<Shape> createShapes(PVector<CoordinateSequence> shells) {
			var ret = TreePVector.<Shape>empty();

			for (var shell : shells) {
				if (!inside.isEmpty()) {
					LinkedList<CoordinateSequence> proto = new LinkedList<>();
					proto.add(shell);
					var it = inside.iterator();
					while (it.hasNext()) {
						var hole = it.next();
						if (holeInside(hole, shell)) {
							it.remove();
							proto.add(hole);
						}
					}

					ret = ret.plus(Shape.of(proto));
				} else {
					ret = ret.plus(Shape.of(shell));
				}
			}

			return ret;
		}

		boolean onSameBorder(Event e1, Event e2) {
			return e1.position * e2.position > 0;
		}

		boolean holeInside(CoordinateSequence hole, CoordinateSequence shell) {
			var location = BOUNDARY;
			for (var i = 0; location == BOUNDARY && i < hole.size(); i++) {
				location = RayCrossingCounter.locatePointInRing(hole.getCoordinate(i), shell);
			}

			return location == INTERIOR;
		}
	}
}
