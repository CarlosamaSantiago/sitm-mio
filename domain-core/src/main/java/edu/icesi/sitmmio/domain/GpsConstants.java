package edu.icesi.sitmmio.domain;

/**
 * Constantes GPS/geográficas del SITM-MIO.
 * - COORD_SCALE: factor de des-escalado de lat/lon enteras del datagrama (Diccionario §1, RES-5).
 * - EARTH_RADIUS_KM: radio medio (consistente con monolith / V1).
 * - NULL_SENTINEL: centinela `-1` del Diccionario para identificadores ausentes (RES-6).
 */
public final class GpsConstants {
    public static final int COORD_SCALE = 10_000_000;
    public static final double EARTH_RADIUS_KM = 6371.0088;
    public static final int NULL_SENTINEL = -1;

    private GpsConstants() {}
}
