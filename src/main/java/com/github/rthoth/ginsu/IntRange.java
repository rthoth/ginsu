package com.github.rthoth.ginsu;

import java.util.Objects;

public abstract class IntRange implements Comparable<IntRange> {

    public static IntRange point(int index) {
        return new Point(index);
    }

    public static IntRange range(int start, int size) {
        if (size > 1)
            return new Range(start, size);
        else
            return new Point(start);
    }

    private static class Point extends IntRange {
        private final int start;

        public Point(int start) {
            this.start = start;
        }

        @Override
        public int compareTo(IntRange other) {
            if (this != other) {
                if (other instanceof Point)
                    return Integer.compare(start, ((Point) other).start);
                else if (other instanceof Range) {
                    final var range = (Range) other;
                    if (start < range.start)
                        return -1;
                    else if (start > range.stop)
                        return 1;
                    else
                        return 0;
                } else {
                    throw new GinsuException.IllegalArgument(Objects.toString(other));
                }
            } else {
                return 0;
            }
        }

        @Override
        public String toString() {
            return "Point[" + start + "]";
        }
    }

    private static class Range extends IntRange {

        private final int start;
        private final int stop;

        public Range(int start, int size) {
            this.start = start;
            this.stop = start + size - 1;
        }

        @Override
        public int compareTo(IntRange other) {
            if (other instanceof Range)
                return compareTo((Range) other);

            if (other instanceof Point)
                return compareTo((Point) other);

            throw new GinsuException.IllegalArgument(Objects.toString(other));
        }

        private int compareTo(Point other) {
            if (stop < other.start)
                return -1;

            if (start > other.start)
                return 1;

            return 0;
        }

        private int compareTo(Range other) {
            if (this != other) {
                if (stop < other.start)
                    return -1;

                if (start > other.stop)
                    return 1;

                throw new GinsuException.IllegalState(this + " ∩ " + other);
            } else {
                return 0;
            }
        }

        @Override
        public String toString() {
            return "Range[" + start + ", " + stop + "]";
        }
    }
}
