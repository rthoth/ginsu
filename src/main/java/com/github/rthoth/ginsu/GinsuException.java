package com.github.rthoth.ginsu;

public class GinsuException {

    public static class InvalidArgument extends RuntimeException {

        public InvalidArgument(String message) {
            super(message);
        }
    }

    public static class InvalidPosition extends RuntimeException {

        public InvalidPosition(String message) {
            super(message);
        }

        @SuppressWarnings("unused")
        public InvalidPosition(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class InvalidState extends RuntimeException {

        public InvalidState(String message) {
            super(message);
        }
    }

    public static class ParseException extends RuntimeException {

        public ParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
