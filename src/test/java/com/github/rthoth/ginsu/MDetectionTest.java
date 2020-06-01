package com.github.rthoth.ginsu;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public class MDetectionTest extends AbstractTest implements MDetectionUtil {

    @Test
    public void t01() {
        var detection = MDetector.detect(lower(x(-3)), upper(y(1)), parseSequence("(-3 5, -5 7, -7 5, -3 1, -3 2, -6 5, -5 6, -3 4, -3 5)"));
        assertThat(Ginsu.map(detection.events, MEvent::toString))
                .containsExactly(
                        "In(0, null)",
                        "Out(3, null)",
                        "In(4, null)",
                        "Out(7, null)"
                );

    }

    @Test
    public void t02() {
        var detection = MDetector.detect(lower(x(-3)), upper(y(2)), parseSequence("(-3 5, -5 7, -7 5, -3 1, -3 2, -6 5, -5 6, -3 4, -3 5)"));
        assertThat(Ginsu.map(detection.events, MEvent::toString))
                .containsExactly(
                        "In(0, null)",
                        "Out(2, (-4.0, 2.0, NaN))",
                        "In(4, null)",
                        "Out(7, null)"
                );
    }

    @Test
    public void t03() {
        var detection = MDetector.detect(lower(x(-4)), upper(y(4)), parseSequence("(-3 5, -5 7, -7 5, -3 1, -3 2, -6 5, -5 6, -3 4, -3 5)"));
        assertThat(Ginsu.map(detection.events, MEvent::toString))
                .containsExactly("In(1, (-4.0, 6.0, NaN))", "Out(2, (-6.0, 4.0, NaN))", "In(5, (-5.0, 4.0, NaN))", "Out(6, (-4.0, 5.0, NaN))");
    }

    @Test
    public void t04() {
        var detection = MDetector.detect(lower(x(0)), upper(y(0)), parseSequence("(-5 1, -2 0, 0 9, -10 -1, -5 1)"));
        assertThat(Ginsu.map(detection.events, MEvent::toString))
                .containsExactly("Out(2, (-9.0, 0.0, NaN))", "In(4, (-7.5, 0.0, NaN))");
    }

    @Test
    public void t05() {
        var detection = MDetector.detect(middle(x(-3), x(-2)), middle(y(-1), y(1)), parseSequence("(-2 1, -2 0, -3 1, -2 1)"));
        assertThat(Ginsu.map(detection.events, MEvent::toString))
                .containsExactly("In(1, null)", "Out(2, null)");
    }
}