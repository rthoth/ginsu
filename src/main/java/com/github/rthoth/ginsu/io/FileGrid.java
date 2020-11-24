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
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class FileGrid<G extends Geometry> extends Grid<G> {

    public static final Executor NOOB_EXECUTOR = Runnable::run;
    private final File directory;
    private final Supplier<WKBReader> wkbReader;

    public FileGrid(int width, int height, File directory, Supplier<WKBReader> wkbReader) {
        super(width, height);
        this.directory = directory;
        this.wkbReader = wkbReader;
    }

    private static String cellName(int x, int y) {
        return x + "-" + y;
    }

    private static FileInputStream openToRead(File directory, int x, int y) {
        try {
            return new FileInputStream(new File(directory, cellName(x, y)));
        } catch (Exception e) {
            throw new GinsuException.IllegalState("It's impossible to open [" + x + "," + y + "]!", e);
        }
    }

    private static FileOutputStream openToWrite(File directory, int x, int y, boolean append) {
        try {
            return new FileOutputStream(new File(directory, cellName(x, y)), append);
        } catch (Exception e) {
            throw new GinsuException.IllegalState("It's impossible to open [" + x + ", " + y + "]!", e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected G _get(int x, int y) {
        try (var input = openToRead(directory, x, y)) {
            return (G) wkbReader.get().read(new InputStreamInStream(input));
        } catch (Exception e) {
            throw new GinsuException.IllegalState("It's impossible to read cell [" + x + ", " + y + "]!", e);
        }
    }

    @Override
    public Grid<G> copy() {
        return this;
    }

    @Override
    protected int mapToIndex(int x, int y) {
        throw new GinsuException.Unsupported();
    }

    public interface Calculator<I, G> {

        G calculate(int x, int y, Collection<I> values);
    }

    public interface Generator<G> {
        G generate(int x, int y);
    }

    public static class Builder<I extends Geometry> {

        private final int width;
        private final int height;
        private final File directory;
        private final Supplier<WKBReader> wkbReader;
        private final Supplier<WKBWriter> wkbWriter;

        private final PMap<Key, ReentrantLock> locks;

        public Builder(int width, int height, File directory, GeometryFactory factory) {
            this(width, height, directory, factory, 2, true);
        }

        public Builder(int width, int height, File directory, GeometryFactory factory, int outputDimension, boolean includeSRID) {
            assert width > 0 : "Invalid width!";
            assert height > 0 : "Invalid height!";
            //noinspection ResultOfMethodCallIgnored
            directory.mkdirs();

            this.width = width;
            this.height = height;
            this.directory = directory;
            var locks = HashTreePMap.<Key, ReentrantLock>empty();
            for (var x = 0; x < width; x++) {
                for (var y = 0; y < height; y++) {
                    locks = locks.plus(new Key(x, y), new ReentrantLock());
                }
            }

            wkbReader = () -> new WKBReader(factory);
            wkbWriter = () -> new WKBWriter(outputDimension, includeSRID);

            this.locks = locks;
        }

        public Builder<I> add(Generator<I> generator) {
            for (var x = 0; x < width; x++) {
                for (var y = 0; y < height; y++) {
                    add(x, y, generator.generate(x, y));
                }
            }
            return this;
        }

        public Builder<I> add(Grid<I> grid) {
            if (grid.getWidth() == width && grid.getHeight() == height) {
                for (var entry : grid.iterable()) {
                    add(entry.x, entry.y, entry.value);
                }
            } else {
                throw new GinsuException.IllegalArgument("Invalid grid size!");
            }

            return this;
        }

        private void add(int x, int y, I value) {
            lock(x, y);
            try {
                try (var output = openToWrite(directory, x, y, true)) {
                    wkbWriter.get().write(value, new OutputStreamOutStream(output));
                    output.flush();
                } catch (IOException e) {
                    throw new GinsuException.IllegalState("It's impossible to close [" + x + ", " + y + "]!");
                }
            } finally {
                unlock(x, y);
            }
        }

        public <G extends Geometry> FileGrid<G> build(String id, Calculator<I, G> calculator) {
            return build(id, calculator, NOOB_EXECUTOR);
        }

        public <G extends Geometry> FileGrid<G> build(String id, Calculator<I, G> calculator, Executor executor) {
            final var outputDirectory = new File(directory, id);
            //noinspection ResultOfMethodCallIgnored
            outputDirectory.mkdirs();

            final var completeables = new ArrayList<>(width * height);

            for (var x = 0; x < width; x++) {
                for (var y = 0; y < height; y++) {
                    final var completeable = new CompletableFuture<Void>();
                    completeable.completeAsync(supplier(outputDirectory, x, y, calculator), executor);
                    completeables.add(completeable);
                }
            }

            try {
                //noinspection SuspiciousToArrayCall
                CompletableFuture.allOf(completeables.toArray(CompletableFuture[]::new))
                        .join();

                return new FileGrid<>(width, height, outputDirectory, wkbReader);
            } catch (Exception e) {
                throw new GinsuException.IllegalState("It's impossible to build grid [" + id + "]!", e);
            }
        }

        private void lock(int x, int y) {
            locks.get(new Key(x, y)).lock();
        }


        private <G extends Geometry> Supplier<Void> supplier(File outputDirectory, int x, int y, Calculator<I, G> calculator) {
            return () -> {
                PVector<I> values = TreePVector.empty();

                try (var input = openToRead(directory, x, y)) {
                    final var wkbReader = this.wkbReader.get();

                    while (input.available() > 0) {
                        //noinspection unchecked
                        values = values.plus((I) wkbReader.read(new InputStreamInStream(input)));
                    }
                } catch (Exception e) {
                    throw new GinsuException.IllegalState("It's impossible to read grid-cell input [" + x + ", " + y + "]!", e);
                }

                try (var output = openToWrite(outputDirectory, x, y, false)) {
                    wkbWriter.get().write(calculator.calculate(x, y, values), new OutputStreamOutStream(output));
                } catch (Exception e) {
                    throw new GinsuException.IllegalState("It's impossible create grid-cell [" + x + "," + y + "]!", e);
                }

                return null;
            };
        }

        private void unlock(int x, int y) {
            locks.get(new Key(x, y)).unlock();
        }
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
                final var other = (Key) obj;
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
