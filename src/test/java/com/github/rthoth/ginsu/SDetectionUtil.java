package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.CoordinateSequence;

public interface SDetectionUtil extends Util {

    default SDetection detect(SCell cell, CoordinateSequence cs) {
        return detect(detector(cell, new SEvent.Factory(cs)), cs);
    }

    default SDetection detect(SDetector detector, CoordinateSequence sequence) {
        detector.first(sequence.getCoordinate(0));
        for (int i = 1, l = sequence.size() - 1; i < l; i++)
            detector.check(i, sequence.getCoordinate(i));

        return detector.last(sequence.size() - 1, sequence.getCoordinate(sequence.size() - 1));
    }

    default SDetector detector(SCell cell, SEvent.Factory factory) {
        return new SDetector(cell, factory);
    }
}
