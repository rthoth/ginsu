package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.CoordinateSequence;

public interface SDetectionUtil extends Util {

    default SShape.Detection detect(Slice slice, CoordinateSequence cs) {
        return detect(detector(slice, new SEvent.Factory(cs)), cs);
    }

    default SShape.Detection detect(SDetector detector, CoordinateSequence sequence) {
        detector.first(sequence.getCoordinate(0));
        for (int i = 1, l = sequence.size() - 1; i < l; i++)
            detector.check(i, sequence.getCoordinate(i));

        return detector.last(sequence.size() - 1, sequence.getCoordinate(sequence.size() - 1));
    }

    default SDetector detector(Slice slice, SEvent.Factory factory) {
        return new SDetector(slice, factory);
    }
}
