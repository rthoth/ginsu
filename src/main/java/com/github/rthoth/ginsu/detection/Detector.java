package com.github.rthoth.ginsu.detection;

import com.github.rthoth.ginsu.Event;
import com.github.rthoth.ginsu.Knife;
import com.github.rthoth.ginsu.Knife.X;
import com.github.rthoth.ginsu.Knife.Y;
import com.github.rthoth.ginsu.Slice;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequences;

public class Detector {

    private final Controller controller;

    public Detector(Controller controller) {
        this.controller = controller;
    }

    public static <K extends Knife<K>> Detector create(Slice<K> slice, Event.Factory factory) {
        return new Detector(new Controller1D<>(slice, new Recorder(factory, false)));
    }

    public static <K extends Knife<K>> Detection detect(Slice<K> slice, CoordinateSequence sequence) {
        return detect(new Detector(new Controller1D<>(slice, new Recorder(new Event.Factory(sequence), false))), sequence);
    }

    public static Detection detect(Detector detector, CoordinateSequence sequence) {
        detector.begin(sequence.getCoordinate(0));

        final var lastIndex = sequence.size() - 1;
        for (var i = 1; i < lastIndex; i++)
            detector.check(i, sequence.getCoordinate(i));

        return detector.end(lastIndex, sequence.getCoordinate(lastIndex), CoordinateSequences.isRing(sequence));
    }

    public static Detection detect(Slice<X> x, Slice<Y> y, CoordinateSequence sequence, boolean hasCorner) {
        return detect(new Detector(new Controller2D(x, y, new Event.Factory(sequence), hasCorner)), sequence);
    }

    public void begin(Coordinate coordinate) {
        controller.begin(coordinate);
    }

    public void check(int index, Coordinate coordinate) {
        controller.update(index, coordinate);
        if (controller.isChanged()) {
            controller.compute();
        }
        controller.next();
    }

    public Detection end(int index, Coordinate coordinate, boolean isRing) {
        check(index, coordinate);
        return controller.end(isRing);
    }

}
