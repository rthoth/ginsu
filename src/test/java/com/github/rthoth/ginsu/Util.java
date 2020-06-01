package com.github.rthoth.ginsu;

public interface Util {

    default <K extends Knife<K>> Slice.Lower<K> lower(K upper) {
        return new Slice.Lower<>(upper);
    }

    default <K extends Knife<K>> Slice.Middle<K> middle(K lower, K upper) {
        return new Slice.Middle<>(lower, upper);
    }

    default <K extends Knife<K>> Slice.Upper<K> upper(K lower) {
        return new Slice.Upper<>(lower);
    }

    default Knife.X x(double value) {
        return new Knife.X(value, Ginsu.DEFAULT_OFFSET, 0D);
    }

    default Knife.Y y(double value) {
        return new Knife.Y(value, Ginsu.DEFAULT_OFFSET, 0D);
    }
}
