package com.github.rthoth.ginsu;

public abstract class GinsuException extends RuntimeException {

    private GinsuException(String message) {
        super(message);
    }

    public static class InvalidSequence extends GinsuException {

        public InvalidSequence(String message) {
            super(message);
        }
    }

    public static class Unsupported extends GinsuException {
        public Unsupported(String message) {
            super(message);
        }
    }

    public static class IllegalArgument extends GinsuException {
        public IllegalArgument(String message) {
            super(message);
        }
    }

    public static class IllegalState extends GinsuException {

        public IllegalState(String message) {
            super(message);
        }
    }

    public static class TopologyException extends GinsuException {
        public TopologyException(String message) {
            super(message);
        }
    }
}
