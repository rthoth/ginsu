package com.github.rthoth.ginsu.io;

import com.github.rthoth.ginsu.GinsuException;
import com.github.rthoth.ginsu.Grid;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.InputStreamInStream;
import org.locationtech.jts.io.OutputStreamOutStream;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;
import org.pcollections.HashTreePMap;
import org.pcollections.PMap;

import java.io.*;
import java.util.concurrent.locks.ReentrantLock;

public class FileGrid<G extends Geometry> extends Grid<G> {

    //    private final ReentrantLock lock = new ReentrantLock();
    private final PMap<Key, ReentrantLock> locks;
    private final File directory;
    private final int outputDimension;
    private final boolean includeSRID;
    private final GeometryFactory factory;

    public FileGrid(int width, int height, File directory, GeometryFactory factory, int outputDimension, boolean includeSRID) {
        super(width, height);
        this.directory = directory;
        this.outputDimension = outputDimension;
        this.includeSRID = includeSRID;
        this.factory = factory;
        var locks = HashTreePMap.<Key, ReentrantLock>empty();
        for (var x = 0; x < width; x++) {
            for (var y = 0; y < height; y++) {
                locks = locks.plus(new Key(x, y), new ReentrantLock());
            }
        }
        this.locks = locks;
    }

    public FileGrid(int width, int height, File directory, GeometryFactory factory) {
        this(width, height, directory, factory, 2, true);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected G _get(int x, int y) {
        acquire(x, y);
        try {
            try (var input = new FileInputStream(new File(directory, x + File.separator + y))) {
                return (G) newReader().read(new InputStreamInStream(input));
            } catch (Exception e) {
                throw new GinsuException.IllegalState(String.format("It's impossible to read (%d, %d) geometry!", x, y), e);
            }
        } finally {
            release(x, y);
        }
    }

    private void acquire(int x, int y) {
        locks.get(new Key(x, y)).lock();
    }

    @Override
    public Grid<G> copy() {
        throw new GinsuException.Unsupported();
    }

    @SuppressWarnings("UnusedReturnValue")
    public FileGrid<G> generate(Generator<G> generator) {
        for (var x = 0; x < width; x++) {
            for (var y = 0; y < height; y++) {
                try {
                    write(generator.generate(x, y), x, y, true);
                } catch (Exception exception) {
                    throw new GinsuException.IllegalState(String.format("It's impossible to create (%d,%d) geometry!", x, y), exception);
                }
            }
        }

        return this;
    }

    @Override
    protected int mapToIndex(int x, int y) {
        throw new GinsuException.Unsupported();
    }

    private WKBReader newReader() {
        return new WKBReader(factory);
    }

    private WKBWriter newWriter() {
        return new WKBWriter(outputDimension, includeSRID);
    }

    private void release(int x, int y) {
        locks.get(new Key(x, y)).unlock();
    }

    public FileGrid<G> update(int x, int y, G geometry) {
        if (x >= 0 && y >= 0 && x < width && y < height) {
            write(geometry, x, y, true);
            return this;
        } else {
            throw new GinsuException.IllegalArgument("");
        }
    }

    public <T extends G> void updateWith(Grid<T> source) {
        if (source.getWidth() == width && source.getHeight() == height) {
            for (var e : source.iterable()) {
                write(e.value, e.x, e.y, true);
            }
        } else {
            throw new GinsuException.IllegalArgument("Different grids size!");
        }
    }

    public <T> void updateWith(Grid<T> source, Combiner<G, T> combiner) {
        if (source.getWidth() == width && source.getHeight() == height) {
            for (var e : source.iterable()) {
                write(combiner.combine(e.x, e.y, _get(e.x, e.y), e.value), e.x, e.y, false);
            }
        } else {
            throw new GinsuException.IllegalArgument("Different grids size!");
        }
    }

    private void write(Geometry geometry, int x, int y, boolean mkdir) {
        acquire(x, y);
        try {
            final var file = new File(directory, (x + File.separator + y));
            try (var output = new FileOutputStream(file)) {
                newWriter().write(geometry, new OutputStreamOutStream(output));
                output.flush();
            } catch (FileNotFoundException e) {
                if (mkdir && file.getParentFile().mkdirs()) {
                    write(geometry, x, y, false);
                } else {
                    throw new GinsuException.IllegalState("Impossible write!", e);
                }
            } catch (IOException e) {
                throw new GinsuException.IllegalState("Unexpected!", e);
            }
        } finally {
            release(x, y);
        }
    }

    public interface Combiner<G, T> {

        G combine(int x, int y, G my, T its);
    }

    public interface Generator<G> {

        G generate(int x, int y);
    }

    private static class Key {
        final int x;
        final int y;

        private Key(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Key) {
                var other = (Key) obj;
                return x == other.x && y == other.y;
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return x * x + y * y;
        }
    }
}
