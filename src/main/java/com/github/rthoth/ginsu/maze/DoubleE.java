package com.github.rthoth.ginsu.maze;

import com.github.rthoth.ginsu.Event;
import org.pcollections.PSet;

import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class DoubleE<I> extends E<I> {

    private final SingleE<I> less;
    private final SingleE<I> greater;

    protected DoubleE(SingleE<I> less, SingleE<I> greater) {
        this.less = less;
        this.greater = greater;
    }

    @Override
    protected PSet<SingleE<I>> consume(Consumer<SingleE<I>> consumer, PSet<SingleE<I>> consumed) {
        return greater.consume(consumer, less.consume(consumer, consumed));
    }

    @Override
    public PSet<SingleE<I>> filterEvent(I info, BiPredicate<Event, I> predicate, PSet<SingleE<I>> set) {
        return greater.filterEvent(info, predicate, less.filterEvent(info, predicate, set));
    }

    @Override
    public boolean forall(I info, BiPredicate<Event, I> predicate) {
        return less.forall(info, predicate) && greater.forall(info, predicate);
    }

    @Override
    public boolean isDouble() {
        return true;
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public String toString() {
        return "Double(less=" + less + ", greater=" + greater + ")";
    }

    @Override
    public boolean xor(Predicate<Event> predicate) {
        return Boolean.logicalXor(predicate.test(less.event), predicate.test(greater.event));
    }
}
