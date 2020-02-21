package com.github.rthoth.ginsu;

public interface Aggregation<E, O> {

	O aggregate();

	Aggregation<E, O> append(E element);

	interface Root<E, O> {

		Aggregation<E, O> begin();

		Aggregation<E, O> append(E element);
	}
}
