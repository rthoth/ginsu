package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.pcollections.PVector;

public class PolygonMerger extends GeometryMerger<MultiPolygon> {

    private final GeometryFactory factory;
    private final double offset;

    public PolygonMerger(GeometryFactory factory, double offset) {
        this.factory = factory;
        this.offset = offset;
    }

    @Override
    public MultiPolygon apply(PVector<MShape> mshapes) {
        return new Merger(mshapes).result;
    }

    @Override
    public MShape.Result classify(MShape.Detection detection, Shape shape) {
        if (detection.events.nonEmpty()) {
            return new MShape.OngoingResult(detection);
        } else if (detection.location == MShape.Detection.INSIDE) {
            return new MShape.DoneResult(shape);
        } else {
            return new MShape.DoneResult(Shape.EMPTY);
        }
    }

    private class Merger {

        MultiPolygon result;
        MScanLine scanLine = new MScanLine(offset);

        public Merger(PVector<MShape> mshapes) {
            for (var mshape : mshapes) {
                if (!mshape.isDone()) {
                    scanLine.add(mshape);
                } else if (mshape.hasContent()) {

                }
            }

            var next = scanLine.next();
        }
    }

}
