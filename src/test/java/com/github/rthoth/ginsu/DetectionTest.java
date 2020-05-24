package com.github.rthoth.ginsu;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public class DetectionTest extends AbstractTest implements DetectionInterface {

    @Test
    public void t01() {
        var detection = detect(middle(y(-7), y(-1)), parseSequence("(-5 7, -7 5, 3 -5, 1 -7, 7 -7, 7 -1, 5 -3, -5 7)"));
        assertThat(Ginsu.map(detection.events, Event::toString))
                .containsExactly(
                        "In(2, U[-1.0], (-1.0, -1.0))",
                        "Out(3, L[1.0], null)",
                        "In(4, L[7.0], null)",
                        "Out(6, U[3.0], (3.0, -1.0))"
                );
    }

    @Test
    public void t02() {
        var detection = detect(lower(y(7)), parseSequence("(-5 7, -7 5, 3 -5, 1 -7, 7 -7, 7 -1, 5 -3, -5 7)"));
        assertThat(Ginsu.map(detection.events, Event::toString))
                .isEmpty();
    }

    @Test
    public void t03() {
        var detection = detect(upper(y(7)), parseSequence("(-5 7, -7 5, 3 -5, 1 -7, 7 -7, 7 -1, 5 -3, -5 7)"));
        assertThat(Ginsu.map(detection.events, Event::toString))
                .isEmpty();
    }

    @Test
    public void t04() {
        var detection = detect(middle(x(-1), x(5)), parseSequence("(-5 7, -7 5, 3 -5, 1 -7, 7 -7, 7 -1, 5 -3, -5 7)"));
        assertThat(Ginsu.map(detection.events, Event::toString))
                .containsExactly(
                        "In(2, L[-1.0], (-1.0, -1.0))",
                        "Out(3, U[-7.0], (5.0, -7.0))",
                        "In(6, U[-3.0], null)",
                        "Out(6, L[3.0], (-1.0, 3.0))"
                );
    }

    @Test
    public void t05() {
        var detection = detect(middle(y(1), y(7)), parseSequence("(-5 7, -7 5, 3 -5, 1 -7, 7 -7, 7 -1, 5 -3, -5 7)"));
        assertThat(Ginsu.map(detection.events, Event::toString))
                .containsExactly(
                        "Out(1, L[-3.0], (-3.0, 1.0))",
                        "In(7, L[1.0], (1.0, 1.0))"
                );
    }

    @Test
    public void issue001() {
        var detection = detect(middle(y(-7.0), y(-1)), parseSequence("(-1 3, 5 -3, 5 -5, 3 -6, 5 -6, 5 -7, 1 -7, 3 -5, -1 -1, -1 0, 4 -5, 5 -4, -1 2, -1 3)"));
        assertThat(Ginsu.map(detection.events, Event::toString))
                .containsExactly(
                        "In(1, U[3.0], (3.0, -1.0))",
                        "Out(5, L[5.0], null)",
                        "In(6, L[1.0], null)",
                        "Out(8, U[-1.0], null)",
                        "In(10, U[0.0], (0.0, -1.0))",
                        "Out(11, U[2.0], (2.0, -1.0))"
                );
    }

    @Test
    public void issue002() {

    }
}
