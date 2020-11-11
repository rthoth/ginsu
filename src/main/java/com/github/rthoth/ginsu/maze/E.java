package com.github.rthoth.ginsu.maze;

import com.github.rthoth.ginsu.Event;
import org.pcollections.PSet;

import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class E<I> {

    protected abstract PSet<SingleE<I>> consume(Consumer<SingleE<I>> consumer, PSet<SingleE<I>> consumed);

    public abstract PSet<SingleE<I>> filterEvent(I info, BiPredicate<Event, I> predicate, PSet<SingleE<I>> set);

    public abstract boolean forall(I info, BiPredicate<Event, I> predicate);

    public abstract boolean isDouble();

    public abstract boolean isSingle();

    public abstract boolean xor(Predicate<Event> predicate);
}
