package com.github.rthoth.ginsu.maze;

import com.github.rthoth.ginsu.*;
import com.github.rthoth.ginsu.detection.Detection;
import com.github.rthoth.ginsu.detection.DetectionShape;
import org.pcollections.PSet;

import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class SingleE<I> extends E<I> {

    public final Event event;
    public final DetectionShape shape;
    public final Detection detection;
    public final N<I> n;

    private final AbstractMaze<I> maze;

    protected SingleE(Event event, DetectionShape shape, Detection detection, AbstractMaze<I> maze, N<I> n) {
        this.event = event;
        this.shape = shape;
        this.detection = detection;
        this.n = n;
        this.maze = maze;
    }

    @Override
    protected PSet<SingleE<I>> consume(Consumer<SingleE<I>> consumer, PSet<SingleE<I>> consumed) {
        if (!consumed.contains(this)) {
            consumer.accept(this);
            return consumed.plus(this);
        } else {
            return consumed;
        }
    }

    @Override
    public PSet<SingleE<I>> filterEvent(I info, BiPredicate<Event, I> predicate, PSet<SingleE<I>> set) {
        return !set.contains(this) && predicate.test(event, info) ? set.plus(this) : set;
    }

    @Override
    public boolean forall(I info, BiPredicate<Event, I> predicate) {
        return predicate.test(event, info);
    }

    public Dimension getDimension() {
        return event.dimension;
    }

    @Override
    public boolean isDouble() {
        return false;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    public SingleE<I> next() {
        return maze.next(this);
    }

    public SingleE<I> previous() {
        return maze.previous(this);
    }

    @Override
    public String toString() {
        return "Single(" + event + ")";
    }

    @Override
    public boolean xor(Predicate<Event> predicate) {
        throw new GinsuException.Unsupported();
    }
}
