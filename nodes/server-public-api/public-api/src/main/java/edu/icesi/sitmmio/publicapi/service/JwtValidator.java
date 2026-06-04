package edu.icesi.sitmmio.publicapi.service;

public final class JwtValidator {
    private final SITM.AuthServicePrx authService;
    public JwtValidator(SITM.AuthServicePrx authService) { this.authService = authService; }

    public SITM.UserContext validate(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        if (authService == null) return null;
        try { return authService.validate(authHeader.substring(7).trim()); }
        catch (Exception e) { return null; }
    }
}
