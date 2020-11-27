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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class FileGrid<G extends Geometry> extends Grid<G> {

    public static final int DEFAULT_MAX_BUFFER = 1024 * 1024 * 8;

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
        private final PMap<Key, ByteArrayOutputStream> buffers;
        private final int maxBuffer;
        private final Semaphore semaphore;
        private final AtomicLong bufferSize;


        public Builder(int width, int height, File directory, GeometryFactory factory) {
            this(width, height, DEFAULT_MAX_BUFFER, directory, factory);
        }

        public Builder(int width, int height, int maxBuffer, File directory, GeometryFactory factory) {
            this(width, height, maxBuffer, directory, factory, 2, true);
        }

        public Builder(int width, int height, int maxBuffer, File directory, GeometryFactory factory, int outputDimension, boolean includeSRID) {
            assert width > 0 : "Invalid width!";
            assert height > 0 : "Invalid height!";
            //noinspection ResultOfMethodCallIgnored
            directory.mkdirs();

            this.width = width;
            this.height = height;
            this.directory = directory;
            this.maxBuffer = maxBuffer;
            this.semaphore = new Semaphore(width * height);

            var locks = HashTreePMap.<Key, ReentrantLock>empty();
            var buffers = HashTreePMap.<Key, ByteArrayOutputStream>empty();

            for (var x = 0; x < width; x++) {
                for (var y = 0; y < height; y++) {
                    final var key = new Key(x, y);
                    buffers = buffers.plus(key, new ByteArrayOutputStream());
                    locks = locks.plus(key, new ReentrantLock());
                }
            }

            wkbReader = () -> new WKBReader(factory);
            wkbWriter = () -> new WKBWriter(outputDimension, includeSRID);

            this.locks = locks;
            this.buffers = buffers;
            bufferSize = new AtomicLong();
        }

        private <G extends Geometry> CompletableFuture<FileGrid<G>> _build(String id, Calculator<I, CompletionStage<G>> calculator, Executor executor) {
            try {
                semaphore.acquire(width * height);
            } catch (InterruptedException e) {
                throw new GinsuException.IllegalState("Impossible build!", e);
            }

            try {
                if (bufferSize.get() > 0) {
                    doDump();
                }

                final var outputDirectory = new File(directory, id);
                //noinspection ResultOfMethodCallIgnored
                outputDirectory.mkdirs();

                final var futures = new ArrayList<CompletableFuture<?>>(width * height);

                for (var x = 0; x < width; x++) {
                    for (var y = 0; y < height; y++) {
                        futures.add(calculate(outputDirectory, x, y, calculator, executor));
                    }
                }

                return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(any -> {
                    //noinspection CodeBlock2Expr
                    return new FileGrid<>(width, height, outputDirectory, wkbReader);
                });
            } catch (Throwable throwable) {
                semaphore.release(width * height);
                throw throwable;
            }
        }

        public Builder<I> add(Generator<I> generator) {
            for (var x = 0; x < width; x++) {
                for (var y = 0; y < height; y++) {
                    addBuffer(x, y, generator.generate(x, y));
                }
            }
            return this;
        }

        public Builder<I> add(Grid<I> grid) {
            if (grid.getWidth() == width && grid.getHeight() == height) {
                for (var entry : grid.iterable()) {
                    addBuffer(entry.x, entry.y, entry.value);
                }
            } else {
                throw new GinsuException.IllegalArgument("Invalid grid size!");
            }

            return this;
        }

        private void addBuffer(int x, int y, I value) {
            final var buffer = getBufferAndLock(x, y);
            var added = 0;
            try {
                final var zero = buffer.size();
                wkbWriter.get().write(value, new OutputStreamOutStream(buffer));
                added = buffer.size() - zero;
            } catch (Exception e) {
                throw new GinsuException.IllegalState("It's impossible to add value buffer to [" + x + ", " + y + "]!", e);
            } finally {
                unlock(x, y);
            }

            if (bufferSize.addAndGet(added) > maxBuffer) {
                tryDump();
            }
        }

        public <G extends Geometry> CompletionStage<FileGrid<G>> build(String id, Calculator<I, CompletionStage<G>> calculator) {
            return _build(id, calculator, NOOB_EXECUTOR);
        }

        public <G extends Geometry> CompletionStage<FileGrid<G>> build(String id, Calculator<I, CompletionStage<G>> calculator, Executor executor) {
            return _build(id, calculator, executor);
        }

        public <G extends Geometry> FileGrid<G> buildSync(String id, Calculator<I, G> calculator) {
            return buildSync(id, calculator, NOOB_EXECUTOR);
        }

        public <G extends Geometry> FileGrid<G> buildSync(String id, Calculator<I, G> calculator, Executor executor) {
            return _build(id, (x, y, values) -> {
                try {
                    return CompletableFuture.completedFuture(calculator.calculate(x, y, values));
                } catch (Exception e) {
                    return CompletableFuture.failedFuture(e);
                }
            }, executor).join();
        }

        private <G extends Geometry> CompletableFuture<Void> calculate(File outputDirectory, int x, int y, Calculator<I, CompletionStage<G>> calculator, Executor executor) {
            return CompletableFuture
                    .completedFuture(null)
                    .thenComposeAsync(any -> {
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

                        return calculator.calculate(x, y, values);
                    }, executor)
                    .thenApplyAsync(newValues -> {
                        try (var output = openToWrite(outputDirectory, x, y, false)) {
                            wkbWriter.get().write(newValues, new OutputStreamOutStream(output));
                            return null;
                        } catch (Exception e) {
                            throw new GinsuException.IllegalState("It's impossible create grid-cell [" + x + "," + y + "]!", e);
                        }
                    }, executor);
        }

        private void doDump() {
            for (var entry : buffers.entrySet()) {
                final var buffer = entry.getValue();
                if (buffer.size() > 0) {

                    final var key = entry.getKey();
                    try (var output = openToWrite(directory, key.x, key.y, true)) {
                        buffer.writeTo(output);
                        buffer.reset();
                        output.flush();
                    } catch (Exception e) {
                        throw new GinsuException.IllegalState("Impossible to dump buffer of [" + key.x + ", " + key.y + "]!", e);
                    }
                }
            }

            bufferSize.set(0L);
        }

        private ByteArrayOutputStream getBufferAndLock(int x, int y) {
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                throw new GinsuException.IllegalState("It's impossible to acquire a buffer!", e);
            }
            final var key = new Key(x, y);
            locks.get(key).lock();
            return buffers.get(key);
        }

        private void tryDump() {
            try {
                semaphore.acquire(width * height);
            } catch (InterruptedException e) {
                throw new GinsuException.IllegalState("It's impossible to acquire all resources!", e);
            }


            try {
                if (bufferSize.get() > maxBuffer) {
                    doDump();
                }
            } finally {
                semaphore.release(width * height);
            }
        }

        private void unlock(int x, int y) {
            locks.get(new Key(x, y)).unlock();
            semaphore.release();
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
            return x * x + y;
        }
    }
}
