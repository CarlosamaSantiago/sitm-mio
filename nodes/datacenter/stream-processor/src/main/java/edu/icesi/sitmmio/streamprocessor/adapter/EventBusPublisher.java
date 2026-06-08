package edu.icesi.sitmmio.streamprocessor.adapter;

public final class EventBusPublisher {
    private final SITM.DatagramEventBusPrx bus;

    public EventBusPublisher(SITM.DatagramEventBusPrx bus) { this.bus = bus; }

    public void publishUpdate(SITM.BusUpdate u) {
        if (bus != null) {
            try { bus.publishUpdate(u); } catch (Exception ignored) {}
        }
    }
    public void publishAlert(SITM.CriticAlert a) {
        if (bus != null) {
            try { bus.publishAlert(a); } catch (Exception ignored) {}
        }
    }
}
