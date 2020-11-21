package com.github.rthoth.ginsu.io;

import com.github.rthoth.ginsu.GinsuException;
import com.github.rthoth.ginsu.Grid;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.InputStreamInStream;
import org.locationtech.jts.io.OutputStreamOutStream;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;

import java.io.*;
import java.util.concurrent.locks.ReentrantLock;

public class FileGrid<G extends Geometry> extends Grid<G> {

    private final WKBWriter writer;
    private final ReentrantLock lock = new ReentrantLock();
    private final File directory;
    private final WKBReader reader;

    public FileGrid(int width, int height, File directory, GeometryFactory factory, int outputDimension, boolean includeSRID) {
        super(width, height);
        this.directory = directory;
        writer = new WKBWriter(outputDimension, includeSRID);
        reader = new WKBReader(factory);
    }

    public FileGrid(int width, int height, File directory, GeometryFactory factory) {
        this(width, height, directory, factory, 2, true);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected G _get(int x, int y) {
        lock.lock();
        try {
            try (var input = new FileInputStream(new File(directory, x + File.separator + y))) {
                return (G) reader.read(new InputStreamInStream(input));
            } catch (Exception e) {
                throw new GinsuException.IllegalState(String.format("It's impossible to read (%d, %d) geometry!", x, y), e);
            }
        } finally {
            lock.unlock();
        }
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

    public <T> void updateWith(Grid<T> source, Combiner<G, T> combiner) {
        if (source.getWidth() == width && source.getHeight() == height) {
            lock.lock();
            try {
                for (var e : source.iterable()) {
                    write(combiner.combine(e.x, e.y, _get(e.x, e.y), e.value), e.x, e.y, false);
                }
            } finally {
                lock.unlock();
            }
        } else {
            throw new GinsuException.IllegalArgument("Different grids size!");
        }

    }

    private void write(Geometry geometry, int x, int y, boolean mkdir) {
        lock.lock();
        try {
            final var file = new File(directory, (x + File.separator + y));
            try (var output = new FileOutputStream(file)) {
                writer.write(geometry, new OutputStreamOutStream(output));
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
            lock.unlock();
        }
    }

    public interface Combiner<G, T> {

        G combine(int x, int y, G my, T its);
    }

    public interface Generator<G> {

        G generate(int x, int y);
    }
}
