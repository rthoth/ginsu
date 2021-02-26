package com.github.rthoth.ginsu;

import com.github.rthoth.ginsu.detection.Detector;
import com.github.rthoth.ginsu.detection.Event;
import com.github.rthoth.ginsu.detection.Slice;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.CoordinateSequences;

import static org.assertj.core.api.Assertions.assertThat;

public class DetectionTest implements GinsuTest {

    private <K extends Knife> Detector<K> detect(Slice<K> slice, Event.Factory factory, boolean detectTouch) {
        var detector = Detector.create(slice, factory, factory.getCoordinate(0), detectTouch);
        var lastIndex = factory.sequence.size() - 1;
        for (var i = 0; i < lastIndex; i++) {
            detector.detect(factory.getCoordinate(i), i);
        }

        detector.end(factory.getCoordinate(lastIndex), lastIndex, CoordinateSequences.isRing(factory.sequence));
        return detector;
    }

    @Test
    public void t001() {
        var obj = shape("6 6, 4 5, 4 0, 3 0, 3 5, 0 0, -3 5, -3 0, -4 0, -4 5, -6 6, -6 -6, -2 -6, -2 0, 0 -3, 2 0, 2 -6, 6 -6, 6 6");
        var seq = obj.sequences().get(0);
        var factory = Event.factory(obj, seq);
        var detector = detect(Slice.middle(y(-5), y(0)), factory, true);
        assertThat(Ginsu.map(detector.getEvents(), Event::toString))
                .containsExactly("Event(IN, -2, 10, L[(-6.0, 0.0, NaN)])",
                                 "Event(OUT, -1, 10, L[(-6.0, -5.0, NaN)])",
                                 "Event(IN, 13, 12, L[(-2.0, -5.0, NaN)])",
                                 "Event(TOUCH, 13, 12, R[(-2.0, 0.0, NaN)])",
                                 "Event(TOUCH, 15, 14, R[(2.0, 0.0, NaN)])",
                                 "Event(OUT, 15, 15, L[(2.0, -5.0, NaN)])",
                                 "Event(IN, -2, 17, L[(6.0, -5.0, NaN)])",
                                 "Event(OUT, -1, 17, L[(6.0, 0.0, NaN)])");
    }

    @Test
    public void t002() {
        var obj = shape("6 6, 4 5, 4 0, 3 0, 3 5, 0 0, -3 5, -3 0, -4 0, -4 5, -6 6, -6 -6, -2 -6, -2 0, 0 -3, 2 0, 2 -6, 6 -6, 6 6");
        var seq = obj.sequences().get(0);
        var factory = Event.factory(obj, seq);
        var detector = detect(Slice.middle(y(0), y(5)), factory, true);
        assertThat(Ginsu.map(detector.getEvents(), Event::toString))
                .containsExactly("Event(IN, 1, 1, R[(4.0, 5.0, NaN)])",
                                 "Event(OUT, 2, 1, R[(4.0, 0.0, NaN)])",
                                 "Event(IN, 3, 3, R[(3.0, 0.0, NaN)])",
                                 "Event(TOUCH, 4, 3, R[(3.0, 5.0, NaN)])",
                                 "Event(TOUCH, 5, 4, R[(0.0, 0.0, NaN)])",
                                 "Event(TOUCH, 6, 5, R[(-3.0, 5.0, NaN)])",
                                 "Event(OUT, 7, 6, R[(-3.0, 0.0, NaN)])",
                                 "Event(IN, 8, 8, R[(-4.0, 0.0, NaN)])",
                                 "Event(OUT, 9, 8, R[(-4.0, 5.0, NaN)])",
                                 "Event(IN, -2, 10, L[(-6.0, 5.0, NaN)])",
                                 "Event(OUT, -1, 10, L[(-6.0, 0.0, NaN)])",
                                 "Event(IN, -2, 17, L[(6.0, 0.0, NaN)])",
                                 "Event(OUT, -1, 17, L[(6.0, 5.0, NaN)])");
    }

    @Test
    public void t003() {
        var obj = shape("6 6, 4 5, 4 0, 3 0, 3 5, 0 0, -3 5, -3 0, -4 0, -4 5, -6 6, -6 -6, -2 -6, -2 0, 0 -3, 2 0, 2 -6, 6 -6, 6 6");
        var seq = obj.sequences().get(0);
        var factory = Event.factory(obj, seq);
        var detector = detect(Slice.middle(y(-4), y(3)), factory, true);
        assertThat(Ginsu.map(detector.getEvents(), Event::toString))
                .containsExactly("Event(IN, 2, 1, L[(4.0, 3.0, NaN)])",
                                 "Event(OUT, 3, 3, L[(3.0, 3.0, NaN)])",
                                 "Event(IN, 5, 4, L[(1.8, 3.0, NaN)])",
                                 "Event(OUT, 5, 5, L[(-1.8, 3.0, NaN)])",
                                 "Event(IN, 7, 6, L[(-3.0, 3.0, NaN)])",
                                 "Event(OUT, 8, 8, L[(-4.0, 3.0, NaN)])",
                                 "Event(IN, -2, 10, L[(-6.0, 3.0, NaN)])",
                                 "Event(OUT, -1, 10, L[(-6.0, -4.0, NaN)])",
                                 "Event(IN, 13, 12, L[(-2.0, -4.0, NaN)])",
                                 "Event(OUT, 15, 15, L[(2.0, -4.0, NaN)])",
                                 "Event(IN, -2, 17, L[(6.0, -4.0, NaN)])",
                                 "Event(OUT, -1, 17, L[(6.0, 3.0, NaN)])");
    }
}
