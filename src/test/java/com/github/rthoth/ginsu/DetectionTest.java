package com.github.rthoth.ginsu;

import com.github.rthoth.ginsu.detection.Detector;
import org.junit.Test;

import java.util.Objects;

import static com.google.common.truth.Truth.assertThat;

public class DetectionTest extends AbstractTest implements DetectionUtil {

    @Test
    public void i001() {
        var detection = Detector.detect(middle(y(-7.0), y(-1)), parseSequence("(-1 3, 5 -3, 5 -5, 3 -6, 5 -6, 5 -7, 1 -7, 3 -5, -1 -1, -1 0, 4 -5, 5 -4, -1 2, -1 3)"));
        assertThat(Ginsu.map(detection.events.getVector(), Event::toString))
                .containsExactly(
                        "In.Y(1, (3.0, -1.0))",
                        "Out.Y(5, null)",
                        "In.Y(6, null)",
                        "Out.Y(8, null)",
                        "In.Y(10, (0.0, -1.0))",
                        "Out.Y(11, (2.0, -1.0))"
                );
    }

    @Test
    public void i002() {
        var hole = parseSequence("(-5 6, -6 5, 4 -5, 5 -4, -5 6)");
        var holeDetection = Detector.detect(middle(x(1), x(4)), hole);
        assertThat(Ginsu.map(holeDetection.events.getVector(), Event::toString))
                .containsExactly(
                        "In.X(2, (1.0, -2.0))",
                        "Out.X(2, null)",
                        "In.X(-1, (4.0, -3.0))",
                        "Out.X(-1, (1.0, 0.0))"
                );
    }

    @Test
    public void i003() {
        var sequence = parseSequence("(-643.9133393949296 -583.8945609485456, -644.320987654321 -583.8945609485456, -644.320987654321 -581.498398175382, -644.3209876543209 -581.498398175382, -644.3209876543209 -583.71647723693, -644.0161560737691 -583.71647723693, -643.9133393949296 -583.8945609485456)");
        var detection = Detector.detect(middle(y(-583.8945609485456), y(-581.498398175382)), sequence);
        assertThat(Ginsu.map(detection.events.getVector(), Event::toString))
                .containsExactly("A");
    }

    @Test
    public void t01() {
        var detection = Detector.detect(lower(x(-3)), upper(y(1)), parseSequence("(-3 5, -5 7, -7 5, -3 1, -3 2, -6 5, -5 6, -3 4, -3 5)"), true);
//        println(toWKT(singletonList(x(-3)), singletonList(y(1)), parseSequence("(-3 5, -5 7, -7 5, -3 1, -3 2, -6 5, -5 6, -3 4, -3 5)"), GEOMETRY_FACTORY));
        assertThat(Ginsu.map(detection.events.getVector(), Event::toString))
                .containsExactly(
                        "In.X(0, null)",
                        "Out.CORNER(3, null)",
                        "In.X(4, null)",
                        "Out.X(7, null)"
                );

    }

    @Test
    public void t02() {
        var detection = Detector.detect(middle(y(-7), y(-1)), parseSequence("(-5 7, -7 5, 3 -5, 1 -7, 7 -7, 7 -1, 5 -3, -5 7)"));
        assertThat(Ginsu.map(detection.events.getVector(), Event::toString))
                .containsExactly(
                        "In.Y(2, (-1.0, -1.0))",
                        "Out.Y(3, null)",
                        "In.Y(4, null)",
                        "Out.Y(6, (3.0, -1.0))"
                );
    }

    @Test
    public void t03() {
        var detection = Detector.detect(lower(x(-3)), upper(y(2)), parseSequence("(-3 5, -5 7, -7 5, -3 1, -3 2, -6 5, -5 6, -3 4, -3 5)"), true);
        assertThat(Ginsu.map(detection.events.getVector(), Event::toString))
                .containsExactly(
                        "In.X(0, null)",
                        "Out.Y(2, (-4.0, 2.0))",
                        "In.CORNER(4, null)",
                        "Out.X(7, null)"
                );
    }

    @Test
    public void t04() {
        var detection = Detector.detect(lower(y(7)), parseSequence("(-5 7, -7 5, 3 -5, 1 -7, 7 -7, 7 -1, 5 -3, -5 7)"));
        assertThat(Ginsu.map(detection.events.getVector(), Event::toString))
                .isEmpty();
    }

    @Test
    public void t05() {
        var sequence = parseSequence("(-3 5, -5 7, -7 5, -3 1, -3 2, -6 5, -5 6, -3 4, -3 5)");
        var slice1 = lower(x(-4));
        var slice2 = upper(y(4));

        var detection = Detector.detect(slice1, slice2, sequence, true);
//        System.out.println(toWKT(singletonList(x(-4)), singletonList(y(4)), sequence, GEOMETRY_FACTORY));
        assertThat(Ginsu.map(detection.events.getVector(), Event::toString))
                .containsExactly(
                        "In.X(1, (-4.0, 6.0))",
                        "Out.Y(2, (-6.0, 4.0))",
                        "In.Y(5, (-5.0, 4.0))",
                        "Out.X(6, (-4.0, 5.0))"
                );

        var d2 = Detector.detect(slice2, slice1, sequence, true);
        assertThat(Ginsu.map(d2.events.getVector(), Event::toString))
                .containsExactlyElementsIn(Ginsu.map(detection.events.getVector(), Event::toString));
    }

    @Test
    public void t06() {
        var detection = Detector.detect(upper(y(7)), parseSequence("(-5 7, -7 5, 3 -5, 1 -7, 7 -7, 7 -1, 5 -3, -5 7)"));
        assertThat(Ginsu.map(detection.events.getVector(), Event::toString))
                .isEmpty();
    }

    @Test
    public void t07() {
        var detection = Detector.detect(lower(x(0)), upper(y(0)), parseSequence("(-5 1, -2 0, 0 9, -10 -1, -5 1)"), true);
        assertThat(Ginsu.map(detection.events.getVector(), Event::toString))
                .containsExactly(
                        "Out.Y(2, (-9.0, 0.0))",
                        "In.Y(4, (-7.5, 0.0))"
                );
    }

    @Test
    public void t08() {
        var detection = Detector.detect(middle(x(-1), x(5)), parseSequence("(-5 7, -7 5, 3 -5, 1 -7, 7 -7, 7 -1, 5 -3, -5 7)"));
        assertThat(Ginsu.map(detection.events.getVector(), Event::toString))
                .containsExactly(
                        "In.X(2, (-1.0, -1.0))",
                        "Out.X(3, (5.0, -7.0))",
                        "In.X(6, null)",
                        "Out.X(6, (-1.0, 3.0))"
                );
    }

    @Test
    public void t09() {
        var detection = Detector.detect(middle(x(-3), x(-2)), middle(y(-1), y(1)), parseSequence("(-2 1, -2 0, -3 1, -2 1)"), true);
        assertThat(Ginsu.map(detection.events.getVector(), Event::toString))
                .containsExactly(
                        "In.X(1, null)",
                        "Out.CORNER(2, null)"
                );
//                .containsExactly(
//                        "Corner(0, null)",
//                        "In.X(1, null)",
//                        "Out.CORNER(2, null)"
//                );
    }

    @Test
    public void t10() {
        var detection = Detector.detect(middle(y(1), y(7)), parseSequence("(-5 7, -7 5, 3 -5, 1 -7, 7 -7, 7 -1, 5 -3, -5 7)"));
        assertThat(Ginsu.map(detection.events.getVector(), Event::toString))
                .containsExactly(
                        "Out.Y(1, (-3.0, 1.0))",
                        "In.Y(7, (1.0, 1.0))"
                );
    }

    @Test
    public void t11() {
        var detection = Detector.detect(middle(x(-4), x(-3)), middle(y(-1), y(6)), parseSequence("(-3 5, -5 7, -7 5, -3 1, -3 2, -6 5, -5 6, -3 4, -3 5)"), true);
//        println(toWKT(list(x(-4), x(-3)), list(y(-1), y(6)), parseSequence("(-3 5, -5 7, -7 5, -3 1, -3 2, -6 5, -5 6, -3 4, -3 5)"), GEOMETRY_FACTORY));
        assertThat(Ginsu.map(detection.events.getVector(), Event::toString))
                .containsExactly(
                        "In.X(0, null)",
                        "Out.CORNER(0, (-4.0, 6.0))",
                        "In.X(3, (-4.0, 2.0))",
                        "Out.X(3, null)",
                        "In.X(4, null)",
                        "Out.X(4, (-4.0, 3.0))",
                        "In.X(7, (-4.0, 5.0))",
                        "Out.X(7, null)"
                );
    }

    @Test
    public void t12() {
        var detection = Detector.detect(middle(x(-6), x(-1)), parseSequence("(-5 6, -6 5, 4 -5, 5 -4, -5 6)"));
        assertThat(Ginsu.map(detection.events.getVector(), Event::toString))
                .containsExactly(
                        "Out.X(1, (-1.0, 0.0))",
                        "In.X(4, (-1.0, 2.0))"
                );
    }

    @Test
    public void t13() {
        var detection = Detector.detect(middle(x(-2), x(2)), middle(y(-2), y(2)), parseSequence("(2 2, 2 -2, 0 -2, 0 2, 2 2)"), true);
        assertThat(Ginsu.map(detection.events.getVector(), Event::toString))
                .containsExactly(
                        "In.Y(2, null)",
                        "Out.Y(3, null)"
                );
        assertThat(Ginsu.map(detection.cornerSet.iterable(), Objects::toString))
                .containsExactly(
                        "null",
                        "Corner(-2147483648, (2.0, -2.0))",
                        "Corner(-2147483648, (2.0, 2.0))",
                        "null"
                );
    }

    @Test
    public void t14() {
        var detection = Detector.detect(middle(x(-2), x(2)), middle(y(-2), y(2)), parseSequence("(2 2, 2 -2, -2 -2, -2 2, 2 2)"), true);
        assertThat(Ginsu.map(detection.events.getVector(), Event::toString))
                .isEmpty();
        assertThat(Ginsu.map(detection.cornerSet.iterable(), Objects::toString))
                .containsExactly(
                        "Corner(-2147483648, (-2.0, -2.0))",
                        "Corner(-2147483648, (2.0, -2.0))",
                        "Corner(-2147483648, (2.0, 2.0))",
                        "Corner(-2147483648, (-2.0, 2.0))"
                );
    }

    @Test
    public void t15() {
        var detection = Detector.detect(middle(x(4), x(8)), upper(y(10)), parseSequence("(8 10, 8 11, 4 11, 4 10, 8 10)"), true);
        assertThat(Ginsu.map(detection.events.getVector(), Event::toString))
                .containsExactly(
                        "In.X(1, null)",
                        "Out.X(2, null)"
                );
        assertThat(Ginsu.map(detection.cornerSet.iterable(), Objects::toString))
                .containsExactly(
                        "Corner(-2147483648, (4.0, 10.0))",
                        "Corner(-2147483648, (8.0, 10.0))",
                        "null",
                        "null"
                );
    }

    @Test
    public void t16() {
        var detection = Detector.detect(middle(x(4), x(8)), middle(y(8), y(10)), parseSequence("(8 8, 8 10, 4 10, 4 8, 8 8)"), true);
        assertThat(detection.events.getVector())
                .isEmpty();
        assertThat(Ginsu.map(detection.cornerSet.iterable(), Objects::toString))
                .containsExactly(
                        "Corner(-2147483648, (4.0, 8.0))",
                        "Corner(-2147483648, (8.0, 8.0))",
                        "Corner(-2147483648, (8.0, 10.0))",
                        "Corner(-2147483648, (4.0, 10.0))"
                );
    }
}
