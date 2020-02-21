package com.github.rthoth.ginsu;

import java.util.function.Function;

@FunctionalInterface
public interface Cropper extends Function<ShapeDetection, MultiShape> {

	MultiShape apply(ShapeDetection shapeDetection);
}
