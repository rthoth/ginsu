package com.github.rthoth.ginsu;

public abstract class GinsuException extends RuntimeException {

    private GinsuException(String message) {
        super(message);
    }

    private GinsuException() {

    }

    public GinsuException(String message, Throwable cause) {
        super(message, cause);
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

        public IllegalState() {
        }

        public IllegalState(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class InvalidIndex extends GinsuException {

        public InvalidIndex(int index) {
            super(Integer.toString(index));
        }
    }

    public static class InvalidSequence extends GinsuException {

        public InvalidSequence(String message) {
            super(message);
        }
    }

    public static class TopologyException extends GinsuException {
        public TopologyException(String message) {
            super(message);
        }
    }

    public static class Unsupported extends GinsuException {
        public Unsupported(String message) {
            super(message);
        }

        public Unsupported() {
            super();
        }
    }
}
