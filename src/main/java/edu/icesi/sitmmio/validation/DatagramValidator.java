package edu.icesi.sitmmio.validation;

import edu.icesi.sitmmio.domain.Datagram;
import edu.icesi.sitmmio.domain.Route;

import java.util.Map;

public class DatagramValidator {
    public boolean isValid(Datagram datagram, Map<Integer, Route> activeRoutes) {
        return datagram.rawLatitude() != -1
                && datagram.rawLongitude() != -1
                && datagram.busId() > 0
                && datagram.tripId() >= 0
                && activeRoutes.containsKey(datagram.lineId())
                && datagram.datagramDate() != null;
    }
}
