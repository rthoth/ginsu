package com.github.rthoth.ginsu.detection;

import com.github.rthoth.ginsu.Shape;
import org.pcollections.PVector;

public class DetectionShape {

    public final PVector<Detection> detections;
    public final Shape source;

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
