package edu.icesi.sitmmio.datagramqueue.io;

import edu.icesi.sitmmio.datagramqueue.domain.QueuePolicy;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cola FIFO durable. Memoria + append-only log para reanudación tras crash.
 * Serialización CSV-like del datagram para reusar el parser cuando se recarga.
 */
public final class AppendOnlyStore implements AutoCloseable {
    private final Path logPath;
    private final Path offsetPath;
    private final Deque<String> buffer = new ArrayDeque<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final AtomicLong enqueued = new AtomicLong();
    private final AtomicLong dequeued = new AtomicLong();
    private final QueuePolicy policy;
    private final BufferedWriter logWriter;
    private int sinceFsync = 0;

    public AppendOnlyStore(Path dir, QueuePolicy policy) throws IOException {
        Files.createDirectories(dir);
        this.logPath = dir.resolve("queue.log");
        this.offsetPath = dir.resolve("consumer.offset");
        this.policy = policy;
        recoverIfPresent();
        this.logWriter = Files.newBufferedWriter(logPath, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private void recoverIfPresent() throws IOException {
        if (!Files.exists(logPath)) return;
        long startOffset = 0L;
        if (Files.exists(offsetPath)) {
            try { startOffset = Long.parseLong(Files.readString(offsetPath).trim()); }
            catch (Exception ignored) {}
        }
        var lines = Files.readAllLines(logPath, StandardCharsets.UTF_8);
        for (long i = startOffset; i < lines.size(); i++) {
            buffer.addLast(lines.get((int) i));
        }
        enqueued.set(lines.size());
        dequeued.set(startOffset);
    }

    public void enqueue(String serialized) throws IOException {
        lock.lock();
        try {
            logWriter.write(serialized);
            logWriter.newLine();
            sinceFsync++;
            if (sinceFsync >= policy.fsyncEveryN) {
                logWriter.flush();
                sinceFsync = 0;
            }
            buffer.addLast(serialized);
            enqueued.incrementAndGet();
            notEmpty.signal();
        } finally { lock.unlock(); }
    }

    public String dequeueBlocking() throws InterruptedException {
        lock.lock();
        try {
            long remaining = policy.dequeueTimeoutMs * 1_000_000L;
            while (buffer.isEmpty()) {
                if (remaining <= 0) return null;
                remaining = notEmpty.awaitNanos(remaining);
            }
            String item = buffer.pollFirst();
            dequeued.incrementAndGet();
            try { Files.writeString(offsetPath, String.valueOf(dequeued.get())); }
            catch (IOException ignored) {}
            return item;
        } finally { lock.unlock(); }
    }

    public long size() { return buffer.size(); }
    public long enqueuedTotal() { return enqueued.get(); }
    public long dequeuedTotal() { return dequeued.get(); }

    @Override
    public void close() throws IOException {
        lock.lock();
        try {
            logWriter.flush();
            logWriter.close();
        } finally { lock.unlock(); }
    }
}
