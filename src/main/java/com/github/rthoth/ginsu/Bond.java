package com.github.rthoth.ginsu;

public class Bond<T> {

	public final T previous;
	public final T current;
	public final T next;

	public Bond(T previous, T current, T next) {
		this.previous = previous;
		this.current = current;
		this.next = next;
	}
}
