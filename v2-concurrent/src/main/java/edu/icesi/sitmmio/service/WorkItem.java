package edu.icesi.sitmmio.service;

import edu.icesi.sitmmio.domain.Datagram;

record WorkItem(Datagram datagram, boolean poison) {
    static WorkItem datagram(Datagram datagram) {
        return new WorkItem(datagram, false);
    }

    static WorkItem poisoned() {
        return new WorkItem(null, true);
    }
}
