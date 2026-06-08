package edu.icesi.sitmmio.datagrameventbus.adapter;

import com.zeroc.Ice.Current;
import edu.icesi.sitmmio.datagrameventbus.domain.SubscriberRegistry;
import edu.icesi.sitmmio.datagrameventbus.service.Dispatcher;

public class DatagramEventBusI implements SITM.DatagramEventBus {

    private final SubscriberRegistry registry;
    private final Dispatcher dispatcher;

    public DatagramEventBusI(SubscriberRegistry registry, Dispatcher dispatcher) {
        this.registry = registry;
        this.dispatcher = dispatcher;
    }

    @Override
    public void publishUpdate(SITM.BusUpdate u, Current current) { dispatcher.dispatchUpdate(u); }

    @Override
    public void publishAlert(SITM.CriticAlert a, Current current) { dispatcher.dispatchAlert(a); }

    @Override
    public void subscribe(SITM.MonitoringSubscriberPrx sub, int zoneId, Current current) {
        if (sub != null) registry.subscribe(sub, zoneId);
    }

    @Override
    public void unsubscribe(SITM.MonitoringSubscriberPrx sub, Current current) {
        if (sub != null) registry.unsubscribe(sub);
    }
}
