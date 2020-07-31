package com.github.rthoth.ginsu;

import java.util.*;

public class MScanLine {

    private final double offset;
    private final TreeMap<Q, N> treeMap = new TreeMap<>();
    private final HashMap<E, N> eToN = new HashMap<>();
    private final HashMap<E, Q> eToQ = new HashMap<>();

    public MScanLine(double offset) {
        this.offset = offset;
    }

    public void add(MShape mshape) {

        for (var detection : mshape.getDetections()) {
            for (var event : detection.events.getPVector()) {
                final var q = new Q(event);
                final var n = treeMap.get(q);
                if (n == null) {
                    treeMap.put(q, new N(event, detection, mshape));
                } else {
                    n.add(new E(event, detection, mshape));
                }
            }
        }
    }

    public Optional<N> next() {
        return !treeMap.isEmpty() ? Optional.of(treeMap.firstEntry().getValue()) : Optional.empty();
    }

    @Override
    public String toString() {
        final var builder = new StringBuilder();
        builder.append("MULTIPOINT (");
        final var iterator = treeMap.keySet().iterator();

        while (iterator.hasNext()) {
            final var q = iterator.next();
            builder
                    .append("(")
                    .append(q.x)
                    .append(" ")
                    .append(q.y)
                    .append(")");

            if (iterator.hasNext())
                builder.append(", ");
        }

        return builder.append(")").toString();
    }

    public static class E {

        private final MEvent event;
        private final MShape.Detection detection;
        private final MShape mshape;

        public E(MEvent event, MShape.Detection detection, MShape mshape) {
            this.event = event;
            this.detection = detection;
            this.mshape = mshape;
        }
    }

    public static class N {

        private final List<E> elements = new ArrayList<>(2);

        public N(MEvent event, MShape.Detection detection, MShape mshape) {
            elements.add(new E(event, detection, mshape));
        }

        public void add(E element) {
            if (elements.size() == 1)
                elements.add(element);
            else
                throw new GinsuException.TopologyException("There are more than 2 points on " + element.event.getCoordinate() + "!");
        }

        public E first() {
            return elements.get(0);
        }

        public boolean isConnection() {
            return elements.size() == 2;
        }
    }

    public class Q implements Comparable<Q> {

        private final double x;
        private final double y;

        public Q(MEvent event) {
            final var coordinate = event.getCoordinate();
            x = coordinate.getX();
            y = coordinate.getY();
        }

        @Override
        public int compareTo(Q other) {
            final var comp = Ginsu.compare(x, offset, other.x);
            return comp != 0 ? comp : Ginsu.compare(y, offset, other.y);
        }
    }
}
