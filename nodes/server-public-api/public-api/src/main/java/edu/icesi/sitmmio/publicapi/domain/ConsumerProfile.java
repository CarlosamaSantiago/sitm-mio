package edu.icesi.sitmmio.publicapi.domain;

public enum ConsumerProfile {
    ANONYMOUS(10), PARTNER(100), GOV(Integer.MAX_VALUE);
    public final int rpm;
    ConsumerProfile(int rpm) { this.rpm = rpm; }

    public static ConsumerProfile from(String role) {
        if (role == null) return ANONYMOUS;
        return switch (role.toUpperCase()) {
            case "ADMIN", "GOV" -> GOV;
            case "PARTNER" -> PARTNER;
            default -> ANONYMOUS;
        };
    }
}
