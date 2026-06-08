package edu.icesi.sitmmio.datagramqueue.domain;

public final class QueuePolicy {
    public final long dequeueTimeoutMs;
    public final int fsyncEveryN;

    public QueuePolicy(long dequeueTimeoutMs, int fsyncEveryN) {
        this.dequeueTimeoutMs = dequeueTimeoutMs;
        this.fsyncEveryN = fsyncEveryN;
    }
    public static QueuePolicy defaults() { return new QueuePolicy(5000, 1000); }
}
