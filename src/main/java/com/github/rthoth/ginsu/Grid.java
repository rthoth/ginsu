package com.github.rthoth.ginsu;

import org.pcollections.Empty;
import org.pcollections.PMap;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public abstract class Grid<T> implements Iterable<Grid.Entry<T>> {

	protected final int width;
	protected final int height;

	protected Grid(int width, int height) {
		validate(width, height);
		this.width = width;
		this.height = height;
	}

	public abstract <R> Grid<R> map(Function<Entry<T>, R> mapper);

	public static <T> Grid<T> xy(List<T> data, int width, int height) {
		validate(width, height);
		validate(data, width, height);
		var it = data.iterator();
		var pMap = Empty.<Key, Entry<T>>map();
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height && it.hasNext(); y++) {
				pMap = pMap.plus(new Key(x, y), new Entry<>(x, y, it.next()));
			}
		}

		return new Dynamic<>(pMap, width, height, Optional.empty());
	}

	public static <T> Grid<T> yx(List<T> data, int width, int height) {
		validate(width, height);
		validate(data, width, height);
		var it = data.iterator();
		var pMap = Empty.<Key, Entry<T>>map();
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width && it.hasNext(); x++) {
				pMap = pMap.plus(new Key(x, y), new Entry<>(x, y, it.next()));
			}
		}

		return new Dynamic<>(pMap, width, height, Optional.empty());
	}

	private static void validate(int width, int height) {
		assert width >= 1 : "width = " + width + " is invalid!";
		assert height >= 1 : "height = " + height + " is invalid!";
	}

	private static void validate(List<?> data, int width, int height) {
		assert data.size() == width * height : "List must have " + (width * height) + " elements!";
	}

	public static class Fixed<T> extends Grid<T> {

		private final T value;

		public Fixed(int width, int height, T value) {
			super(width, height);
			this.value = value;
		}

		@Override
		public Iterator<Entry<T>> iterator() {

			var x = new AtomicInteger();
			var y = new AtomicInteger();

			return new Iterator<>() {
				@Override
				public boolean hasNext() {
					return y.get() < height;
				}

				@Override
				public Entry<T> next() {
					var entry = new Entry<>(x.getAndIncrement(), y.get(), value);
					if (x.get() == width) {
						x.set(0);
						y.incrementAndGet();
					}

					return entry;
				}
			};
		}

		@Override
		public <R> Grid<R> map(Function<Entry<T>, R> mapper) {
			throw new UnsupportedOperationException();
		}
	}

	private static class Key implements Comparable<Key> {

		private final int x;
		private final int y;

		public Key(int x, int y) {
			this.x = x;
			this.y = y;
		}

		@Override
		public int compareTo(Key o) {
			return (x != o.x) ? Integer.compare(x, o.x) : Integer.compare(y, o.y);
		}
	}

	public final static class Entry<T> implements Comparable<Entry<T>> {

		protected final int x;
		private final int y;
		private final T data;

		public Entry(int x, int y, T data) {
			assert x >= 0 : "X = " + x + " is invalid!";
			assert y >= 0 : "Y = " + y + " is invalid!";
			this.x = x;
			this.y = y;
			this.data = data;
		}

		@SuppressWarnings("unused")
		public int getX() {
			return x;
		}

		@SuppressWarnings("unused")
		public int getY() {
			return y;
		}

		@SuppressWarnings("unused")
		public T getData() {
			return data;
		}

		@Override
		public String toString() {
			return "Entry(" + x + ", " + y + ", " + data + ")";
		}

		@Override
		public int compareTo(Entry<T> other) {
			return x != other.x ? Integer.compare(x, other.x) : Integer.compare(y, other.y);
		}
	}

	public static class Dynamic<T> extends Grid<T> {

		private final PMap<Key, Entry<T>> pMap;
		private final Optional<T> defaultValue;

		private Dynamic(PMap<Key, Entry<T>> pMap, int width, int height, Optional<T> defaultValue) {
			super(width, height);

			Objects.requireNonNull(defaultValue, "Default value must be not null!");
			this.pMap = pMap;
			this.defaultValue = defaultValue;
		}

		@Override
		public Iterator<Entry<T>> iterator() {
			return new TreeSet<>(pMap.values()).iterator();
		}

		@Override
		public <R> Grid<R> map(Function<Entry<T>, R> mapper) {
			var ret = Empty.<Key, Entry<R>>map();
			for (var entry : pMap.entrySet()) {
				var newEntry = new Entry<>(entry.getKey().x, entry.getKey().y, mapper.apply(entry.getValue()));
				ret = ret.plus(new Key(newEntry.x, newEntry.y), newEntry);
			}

			return new Dynamic<>(ret, width, height, defaultValue.map(value -> mapper.apply(new Entry<>(0, 0, value))));
		}
	}
}
