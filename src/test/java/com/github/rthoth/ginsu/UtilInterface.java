package com.github.rthoth.ginsu;

public interface UtilInterface {

    default <K extends Knife<K>> Cell.Lower<K> lower(K upper) {
        return new Cell.Lower<>(upper);
    }

    default <K extends Knife<K>> Cell.Middle<K> middle(K lower, K upper) {
        return new Cell.Middle<>(lower, upper);
    }

    default <K extends Knife<K>> Cell.Upper<K> upper(K lower) {
        return new Cell.Upper<>(lower);
    }

    default Knife.X x(double value) {
        return new Knife.X(value, Ginsu.DEFAULT_OFFSET, 0D);
    }

    default Knife.Y y(double value) {
        return new Knife.Y(value, Ginsu.DEFAULT_OFFSET, 0D);
    }
}
