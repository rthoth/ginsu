package com.github.rthoth.ginsu;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public class SegmentTest extends AbstractTest {

    @Test
    public void t01() {
        final var sequence = parseSequence("(0 0, 1 1, 2 2, 3 3, 4 4, 5 5, 6 6, 7 7, 8 8, 9 9, 10 10, 11 11, 0 0)");
        var builder = new CSBuilder();
        builder.addForward(0, 2, sequence);
        builder.addBackward(5, 3, sequence);
        builder.addForward(10, 1, sequence);
        builder.addBackward(2, 7, sequence);
        assertThat(builder.build().toString())
                .isEqualTo("(0,0 1,1 2,2 5,5 4,4 3,3 10,10 11,11 0,0 1,1 2,2 1,1 0,0 11,11 10,10 9,9 8,8 7,7)");
    }

    @Test
    public void t02() {
        final var sequence = parseSequence("(0 0, 1 1, 2 2, 3 3, 4 4, 5 5, 6 6, 7 7, 8 8, 9 9, 10 10, 11 11, 0 0)");
        var builder = new CSBuilder();
        builder.addForward(0, 1, sequence);
        builder.addForward(2, 3, sequence);
        builder.addForward(4, 5, sequence);
        builder.addForward(6, 7, sequence);
        builder.addForward(8, 9, sequence);
        builder.addForward(10, 0, sequence);

        var intermediate = builder.build();
        assertThat(intermediate.toString())
                .isEqualTo("(0,0 1,1 2,2 3,3 4,4 5,5 6,6 7,7 8,8 9,9 10,10 11,11 0,0)");

        builder = new CSBuilder();
        builder.addBackward(10, 2, intermediate);
        assertThat(builder.build().toString())
                .isEqualTo("(10,10 9,9 8,8 7,7 6,6 5,5 4,4 3,3 2,2)");

        builder = new CSBuilder();
        builder.addBackward(3, 9, intermediate);
        assertThat(builder.build().toString())
                .isEqualTo("(3,3 2,2 1,1 0,0 11,11 10,10 9,9)");
    }
}
