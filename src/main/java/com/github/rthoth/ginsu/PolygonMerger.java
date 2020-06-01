package com.github.rthoth.ginsu;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.pcollections.PVector;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class PolygonMerger extends GeometryMerger<MultiPolygon> {

    private final GeometryFactory factory;
    private final double offset;

    public PolygonMerger(GeometryFactory factory, double offset) {
        this.factory = factory;
        this.offset = offset;
    }

    @Override
    public MShape classify(MShape.Detection detection, Shape shape) {
        if (!detection.events.isEmpty()) {
            return new MShape.Ongoing(detection);
        } else if (detection.location == MShape.Detection.INSIDE) {
            return new MShape.Done(shape);
        } else {
            return new MShape.Done(Shape.EMPTY);
        }
    }

    @Override
    public MultiPolygon merge(PVector<MShape.Detection> detections, PVector<Shape> shapes) {
        return new Merger(detections, shapes).result;
    }

    private class E {

        final MEvent event;
        final MShape.Detection detection;

        public E(MEvent event, MShape.Detection detection) {
            this.event = event;
            this.detection = detection;
        }
    }

    private class Node {

        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        final List<E> elements = new ArrayList<>(2);

        public Node(MEvent event, MShape.Detection detection) {
            elements.add(new E(event, detection));
        }

    }

    private class Merger {

        MultiPolygon result;
        TreeMap<Q, Node> nodes;

        public Merger(PVector<MShape.Detection> detections, PVector<Shape> shapes) {
            if (!detections.isEmpty()) {
                nodes = new TreeMap<>();

                for (var detection : detections) {
                    for (var event : detection.events) {
                        final var q = new Q(event.getCoordinate());
                        final var node = nodes.get(q);
                        if (node == null) {
                            nodes.put(q, new Node(event, detection));
                        } else {
                            node.elements.add(new E(event, detection));
                        }
                    }
                }

                for (var entry : nodes.entrySet()) {
                    final var node = entry.getValue();

                    if (node.elements.size() == 2) {
                        System.out.println("Conectado!");
                    } else if (node.elements.size() == 1) {
                        System.out.println("Não conectado!");
                    } else {
                        System.out.println("ESTRANHO!");
                    }
                }

            } else {
                result = MultiShape.of(shapes).toMultiPolygon(factory);
            }
        }
    }

    private class Q implements Comparable<Q> {

        final double x;
        final double y;


        public Q(Coordinate coordinate) {
            x = coordinate.getX();
            y = coordinate.getY();
        }

        @Override
        public int compareTo(Q q) {
            final var c = Ginsu.compare(x, offset, q.x);
            return c != 0 ? c : Ginsu.compare(y, offset, q.y);
        }
    }
}
