package com.github.rthoth.ginsu;

public interface Util {

    default <K extends Knife<K>> SCell.Lower<K> lower(K upper) {
        return new SCell.Lower<>(upper);
    }

    default <K extends Knife<K>> SCell.Middle<K> middle(K lower, K upper) {
        return new SCell.Middle<>(lower, upper);
    }

    default <K extends Knife<K>> SCell.Upper<K> upper(K lower) {
        return new SCell.Upper<>(lower);
    }

    default Knife.X x(double value) {
        return new Knife.X(value, Ginsu.DEFAULT_OFFSET, 0D);
    }

    default Knife.Y y(double value) {
        return new Knife.Y(value, Ginsu.DEFAULT_OFFSET, 0D);
    }
}
