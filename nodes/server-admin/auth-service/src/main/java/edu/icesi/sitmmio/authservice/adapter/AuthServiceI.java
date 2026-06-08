package edu.icesi.sitmmio.authservice.adapter;

import com.zeroc.Ice.Current;
import edu.icesi.sitmmio.authservice.domain.Jwt;
import edu.icesi.sitmmio.authservice.io.UserRepository;

public class AuthServiceI implements SITM.AuthService {

    private final UserRepository repo;
    private final Jwt jwt;
    private final long ttlMs;

    public AuthServiceI(UserRepository repo, Jwt jwt, long ttlMs) {
        this.repo = repo; this.jwt = jwt; this.ttlMs = ttlMs;
    }

    @Override
    public SITM.AuthToken login(String user, String password, Current current) throws SITM.Unauthorized {
        if (!repo.authenticate(user, password)) throw new SITM.Unauthorized();
        UserRepository.User u = repo.get(user);
        SITM.AuthToken token = new SITM.AuthToken();
        token.jwt = jwt.issue(u.userId(), u.role(), u.zoneId(), ttlMs);
        token.expiresAtEpochMs = System.currentTimeMillis() + ttlMs;
        return token;
    }

    @Override
    public SITM.UserContext validate(String token, Current current) throws SITM.Unauthorized {
        try {
            Jwt.Claims c = jwt.parseAndValidate(token);
            SITM.UserContext ctx = new SITM.UserContext();
            ctx.userId = c.sub(); ctx.role = c.role(); ctx.assignedZoneId = c.zoneId();
            return ctx;
        } catch (Exception e) {
            throw new SITM.Unauthorized();
        }
    }
}
