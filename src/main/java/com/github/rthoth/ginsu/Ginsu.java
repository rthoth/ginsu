package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.Geometry;
import org.pcollections.Empty;
import org.pcollections.PVector;

import java.util.*;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.IntFunction;

/**
 * Utility class.
 */
public class Ginsu {

	public static <E, I extends Iterable<E>, O> PVector<O> aggregate(Aggregation.Root<E, O> root, Iterable<I> iterable) {
		var externalIt = iterable.iterator();
		if (externalIt.hasNext()) {
			var aggregations = Ginsu.mapToVector(externalIt.next(), root::append);
			final var expectedSize = aggregations.size();

			while (externalIt.hasNext()) {
				var count = 0;

				for (var elem : externalIt.next()) {
					aggregations = aggregations.with(count, aggregations.get(count).append(elem));
					count++;
				}

				if (count != expectedSize)
					throw new IllegalStateException();
			}

			return Ginsu.mapToVector(aggregations, Aggregation::aggregate);
		} else {
			return Empty.vector();
		}
	}

	public static <T, R> PVector<R> flatMapToVector(Iterable<T> iterable, Function<T, Iterable<R>> mapper) {
		var ret = Empty.<R>vector();
		for (var element : iterable) {
			for (var value : mapper.apply(element)) {
				ret = ret.plus(value);
			}
		}

		return ret;
	}

	public static <T> PVector<String> mapToString(Iterable<T> iterable) {
		return mapToVector(iterable, T::toString);
	}

	public static <O> PVector<O> mapToVector(double[] values, DoubleFunction<O> mapper) {
		PVector<O> ret = Empty.vector();

		for (double value : values)
			ret = ret.plus(mapper.apply(value));

		return ret;
	}

	public static <I, O> PVector<O> mapToVector(Iterable<I> iterable, Function<I, O> mapper) {
		return mapToVector(iterable.iterator(), mapper);
	}

	public static <I, O> PVector<O> mapToVector(Iterator<I> iterator, Function<I, O> mapper) {
		var ret = Empty.<O>vector();
		while (iterator.hasNext())
			ret = ret.plus(mapper.apply(iterator.next()));

		return ret;
	}

	public static <I, O> O[] mapToArray(Iterable<I> iterable, Function<I, O> mapper, IntFunction<O[]> generator) {
		return mapToArray(iterable.iterator(), mapper, generator);
	}

	public static <I, O> O[] mapToArray(Iterator<I> iterator, Function<I, O> mapper, IntFunction<O[]> generator) {
		var newList = new LinkedList<O>();
		while (iterator.hasNext())
			newList.add(mapper.apply(iterator.next()));

		return newList.toArray(generator);
	}

	public static <T> T next(Iterator<T> it) {
		if (it.hasNext())
			return it.next();
		else
			throw new IllegalStateException();
	}

	public static <T> Optional<T> getNext(Iterator<T> it) {
		return it.hasNext() ? Optional.of(it.next()) : Optional.empty();
	}

	public static <T> T selectFirst(SortedSet<T> s1, SortedSet<T> s2, Comparator<T> comparator) {
		var v1 = !s1.isEmpty() ? s1.first() : null;
		var v2 = !s2.isEmpty() ? s2.first() : null;

		if (v1 != null && v2 != null) {
			var comp = comparator.compare(v1, v2);
			return comp < 0 ? v1 : (comp > 0 ? v2 : null);
		} else
			return v1 != null ? v1 : v2;
	}

	public static <T> T selectLast(SortedSet<T> s1, SortedSet<T> s2, Comparator<T> comparator) {
		var v1 = !s1.isEmpty() ? s1.last() : null;
		var v2 = !s2.isEmpty() ? s2.last() : null;

		if (v1 != null && v2 != null) {
			var comp = comparator.compare(v1, v2);
			return comp < 0 ? v1 : (comp > 0 ? v2 : null);
		} else
			return v1 != null ? v1 : v2;
	}

	public static Iterable<Geometry> components(Geometry geometry) {
		return () -> new Iterator<>() {

			private int index = 0;

			@Override
			public boolean hasNext() {
				return index < geometry.getNumGeometries();
			}

			@Override
			public Geometry next() {
				return geometry.getGeometryN(index++);
			}
		};
	}
}
