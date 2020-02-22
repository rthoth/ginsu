package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.CoordinateSequence;

public interface DetectionInterface extends UtilInterface {

    default Detection detect(Cell cell, CoordinateSequence cs) {
        return detect(detector(cell, new Event.Factory(cs)), cs);
    }

    default Detection detect(Detector detector, CoordinateSequence sequence) {
        detector.first(sequence.getCoordinate(0));
        for (int i = 1, l = sequence.size() - 1; i < l; i++)
            detector.check(i, sequence.getCoordinate(i));

        return detector.last(sequence.size() - 1, sequence.getCoordinate(sequence.size() - 1));
    }

    default Detector detector(Cell cell, Event.Factory factory) {
        return new Detector(cell, factory);
    }
}
