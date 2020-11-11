package com.github.rthoth.ginsu.maze;

import com.github.rthoth.ginsu.Event;
import com.github.rthoth.ginsu.GinsuException;
import org.locationtech.jts.geom.Coordinate;
import org.pcollections.HashTreePSet;
import org.pcollections.PSet;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.util.function.BiPredicate;
import java.util.function.Consumer;

public class N<I> {

    public final Q q;

    private PSet<Lane<I>> lanes = HashTreePSet.empty();
    private boolean nonVisited = true;

    private E<I> xL;
    private E<I> xG;
    private E<I> yL;
    private E<I> yG;

    private I xLI;
    private I xGI;
    private I yLI;
    private I yGI;

    public N(Q q) {
        this.q = q;
    }

    public void add(Lane<I> lane) {
        lanes = lanes.plus(lane);
    }

    private PSet<SingleE<I>> consume(E<I> e, Consumer<SingleE<I>> consumer, PSet<SingleE<I>> consumed) {
        return e != null ? e.consume(consumer, consumed) : consumed;
    }

    public boolean exist(BiPredicate<Event, I> predicate) {
        return t(xL, xLI, predicate, false) || t(xG, xGI, predicate, false) || t(yL, yLI, predicate, false) || t(yG, yGI, predicate, false);
    }

    public PSet<SingleE<I>> filterEvent(BiPredicate<Event, I> predicate) {
        PSet<SingleE<I>> set = HashTreePSet.empty();
        set = filterEvent(xL, xLI, predicate, set);
        set = filterEvent(xG, xGI, predicate, set);
        set = filterEvent(yL, yLI, predicate, set);
        set = filterEvent(yG, yGI, predicate, set);

        return set;
    }

    private PSet<SingleE<I>> filterEvent(E<I> e, I info, BiPredicate<Event, I> predicate, PSet<SingleE<I>> set) {
        return e != null ? e.filterEvent(info, predicate, set) : set;
    }

    public void forEachSingle(Consumer<SingleE<I>> consumer) {
        var consumed = consume(xL, consumer, HashTreePSet.empty());
        consumed = consume(yG, consumer, consumed);
        consumed = consume(yL, consumer, consumed);
        consume(yG, consumer, consumed);
    }

    public Coordinate getCoordinate() {
        return q.coordinate;
    }

    public E<I> getXG() {
        return xG;
    }

    public void setXG(E<I> xg) {
        this.xG = xg;
    }

    public I getXGI() {
        return xGI;
    }

    public void setXGI(I info) {
        xGI = info;
    }

    public E<I> getXL() {
        return xL;
    }

    public void setXL(E<I> e) {
        this.xL = e;
    }

    public I getXLI() {
        return xLI;
    }

    public void setXLI(I info) {
        xLI = info;
    }

    public E<I> getYG() {
        return yG;
    }

    public void setYG(E<I> e) {
        this.yG = e;
    }

    public I getYGI() {
        return yGI;
    }

    public void setYGI(I info) {
        this.yGI = info;
    }

    public E<I> getYL() {
        return yL;
    }

    public void setYL(E<I> e) {
        this.yL = e;
    }

    public I getYLI() {
        return yLI;
    }

    public void setYLI(I info) {
        yLI = info;
    }

    public <T> PVector<T> map(Lane.Mapper<I, T> mapper) {
        var vector = TreePVector.<T>empty();
        for (var lane : lanes) {
            var result = lane.map(this, mapper);
            if (result != null)
                vector = vector.plus(result);
        }

        return vector;
    }

    public boolean nonVisited() {
        return nonVisited;
    }

    public int size() {
        var size = 0;
        if (xL != null)
            size++;
        if (xG != null)
            size++;
        if (yL != null)
            size++;
        if (yG != null)
            size++;

        return size;
    }

    @SuppressWarnings("SameParameterValue")
    private boolean t(E<I> e, I info, BiPredicate<Event, I> predicate, boolean other) {
        return e != null ? e.forall(info, predicate) : other;
    }

    @Override
    public String toString() {
        return "N(" + q + ")";
    }

    public <T> T visit(T initial, Visitor<T, I> visitor) {
        var value = initial;
        for (var lane : lanes) {
            value = lane.visit(this, value, visitor);
        }

        return value;
    }

    public void visited() {
        if (!nonVisited)
            throw new GinsuException.TopologyException("N has already visited close to: " + getCoordinate());
        nonVisited = false;
    }

    public interface Visitor<T, I> {
        T visit(T value, I less, I greater, N<I> lower, N<I> higher);
    }
}
