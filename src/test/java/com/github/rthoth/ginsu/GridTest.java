package com.github.rthoth.ginsu;

import org.junit.Test;
import org.pcollections.TreePVector;

import java.util.Arrays;

import static com.google.common.truth.Truth.assertThat;

public class GridTest {

    @Test
    public void t01() {
        var xy = new Grid.YX<>(2, 2, TreePVector.from(Arrays.asList(1, 2, 3, 4)));
        var copy = xy.viewValue(integer -> integer).copy();

        assertThat(Ginsu.map(xy.iterable(), Grid.Entry::toString))
                .containsExactlyElementsIn(Ginsu.map(copy.iterable(), Grid.Entry::toString));
    }
}
