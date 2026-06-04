package edu.icesi.sitmmio.datagramqueue;

import edu.icesi.sitmmio.datagramqueue.domain.QueuePolicy;
import edu.icesi.sitmmio.datagramqueue.io.AppendOnlyStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AppendOnlyStoreTest {

    @Test
    void fifoOrderPreserved(@TempDir Path dir) throws Exception {
        AppendOnlyStore store = new AppendOnlyStore(dir, new QueuePolicy(100, 1));
        store.enqueue("a");
        store.enqueue("b");
        store.enqueue("c");
        assertEquals("a", store.dequeueBlocking());
        assertEquals("b", store.dequeueBlocking());
        assertEquals("c", store.dequeueBlocking());
    }

    @Test
    void recoversFromOffset(@TempDir Path dir) throws Exception {
        AppendOnlyStore s1 = new AppendOnlyStore(dir, new QueuePolicy(100, 1));
        s1.enqueue("x");
        s1.enqueue("y");
        s1.enqueue("z");
        assertEquals("x", s1.dequeueBlocking());
        // Simula reinicio: nuevo store sobre el mismo dir
        AppendOnlyStore s2 = new AppendOnlyStore(dir, new QueuePolicy(100, 1));
        assertEquals("y", s2.dequeueBlocking());
        assertEquals("z", s2.dequeueBlocking());
    }

    @Test
    void timeoutReturnsNull(@TempDir Path dir) throws Exception {
        AppendOnlyStore store = new AppendOnlyStore(dir, new QueuePolicy(50, 1));
        assertNull(store.dequeueBlocking());
    }
}
