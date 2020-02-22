package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequences;
import org.locationtech.jts.geom.Envelope;
import org.pcollections.PVector;

import java.util.TreeMap;

public class SegmentCoordinateSequence implements CoordinateSequence {

    private final int size;
    private final TreeMap<IntRange, Segment> treeMap = new TreeMap<>();

    public SegmentCoordinateSequence(PVector<Segment> segments) {
        var size = 0;
        for (var segment : segments) {
            treeMap.put(IntRange.range(size, segment.size()), Segment.wrap(segment, size));
            size += segment.size();
        }

        this.size = size;
    }

    @Override
    public Object clone() {
        return copy();
    }

    @Override
    public CoordinateSequence copy() {
        throw new GinsuException.Unsupported("copy");
    }

    @Override
    public Envelope expandEnvelope(Envelope env) {
        return null;
    }

    @Override
    public Coordinate getCoordinate(int index) {
        return treeMap.get(IntRange.point(index)).getCoordinate(index);
    }

    @Override
    public void getCoordinate(int index, Coordinate coord) {
        treeMap.get(IntRange.point(index)).getCoordinate(index, coord);
    }

    @Override
    public Coordinate getCoordinateCopy(int index) {
        return treeMap.get(IntRange.point(index)).getCoordinate(index).copy();
    }

    @Override
    public int getDimension() {
        return 2;
    }

    @Override
    public double getOrdinate(int index, int ordinateIndex) {
        return treeMap.get(IntRange.point(index)).getOrdinate(index, ordinateIndex);
    }

    @Override
    public double getX(int index) {
        return treeMap.get(IntRange.point(index)).getX(index);
    }

    @Override
    public double getY(int index) {
        return treeMap.get(IntRange.point(index)).getY(index);
    }

    @Override
    public void setOrdinate(int index, int ordinateIndex, double value) {
        throw new GinsuException.Unsupported("setOrdinate");
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public Coordinate[] toCoordinateArray() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return CoordinateSequences.toString(this);
    }
}
