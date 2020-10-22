package com.github.rthoth.ginsu;

import org.pcollections.PVector;

public class DetectionShape {

    final PVector<Detection> detections;
    final Shape source;

    public DetectionShape(PVector<Detection> detections, Shape source) {
        this.detections = detections;
        this.source = source;
    }

    public boolean nonEmpty() {
        for (var detection : detections) {
            if (detection.nonEmpty())
                return true;
        }

        return false;
    }
}
