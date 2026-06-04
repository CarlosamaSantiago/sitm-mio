package edu.icesi.sitmmio.streamprocessor.service;

import edu.icesi.sitmmio.domain.Datagram;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BusStateBuilder {

    public enum State { EN_RUTA, PARADO, SIN_SENAL, CRITICO }

    private final Map<Integer, Long> lastOdometer = new ConcurrentHashMap<>();
    private final EventClassifier classifier;

    public BusStateBuilder(EventClassifier classifier) { this.classifier = classifier; }

    public State build(Datagram d) {
        if (classifier.classify(d) == EventClassifier.Category.EXCEPTIONAL) return State.CRITICO;
        Long prev = lastOdometer.put(d.busId(), d.odometer());
        if (prev != null && prev.equals(d.odometer())) return State.PARADO;
        return State.EN_RUTA;
    }
}
