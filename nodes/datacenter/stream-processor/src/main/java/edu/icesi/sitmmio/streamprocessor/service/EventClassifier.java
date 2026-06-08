package edu.icesi.sitmmio.streamprocessor.service;

import edu.icesi.sitmmio.domain.Datagram;

public final class EventClassifier {
    public enum Category { ROUTINE, EXCEPTIONAL }

    public Category classify(Datagram d) {
        int t = d.eventType();
        return (t == 0 || t == 1 || t == 2) ? Category.ROUTINE : Category.EXCEPTIONAL;
    }
}
