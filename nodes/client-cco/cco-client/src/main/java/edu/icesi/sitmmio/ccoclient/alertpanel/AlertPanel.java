package edu.icesi.sitmmio.ccoclient.alertpanel;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.LongAdder;

public final class AlertPanel {
    private final Deque<SITM.CriticAlert> history = new ConcurrentLinkedDeque<>();
    private final int maxHistory = 50;
    private final LongAdder received = new LongAdder();

    public void show(SITM.CriticAlert a) {
        received.increment();
        history.addFirst(a);
        while (history.size() > maxHistory) history.removeLast();
        System.out.println("[alert-panel] ALERTA bus=" + a.busId + " lineId=" + a.lineId
                + " priority=" + a.priority + " desc=" + a.description);
    }

    public long receivedTotal() { return received.sum(); }
    public int historySize() { return history.size(); }
}
