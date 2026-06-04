package edu.icesi.sitmmio.authservice;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Util;
import edu.icesi.sitmmio.authservice.adapter.AuthServiceI;
import edu.icesi.sitmmio.authservice.domain.Jwt;
import edu.icesi.sitmmio.authservice.io.UserRepository;

import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        int port = 10040;
        String usersPath = "admin/users.example.json";
        String secret = "change-me-in-cfg";
        String salt = "sitm-mio-salt";
        long ttlMs = 3_600_000L;
        for (int i = 0; i < args.length - 1; i++) {
            if ("--port".equals(args[i])) port = Integer.parseInt(args[i + 1]);
            if ("--users".equals(args[i])) usersPath = args[i + 1];
            if ("--secret".equals(args[i])) secret = args[i + 1];
            if ("--salt".equals(args[i])) salt = args[i + 1];
            if ("--ttl-ms".equals(args[i])) ttlMs = Long.parseLong(args[i + 1]);
        }
        try (Communicator c = Util.initialize(args)) {
            UserRepository repo = new UserRepository(salt);
            repo.loadFromJson(Path.of(usersPath));
            Jwt jwt = new Jwt(secret);
            AuthServiceI servant = new AuthServiceI(repo, jwt, ttlMs);
            ObjectAdapter adapter = c.createObjectAdapterWithEndpoints(
                    "AuthServiceAdapter", "default -p " + port);
            adapter.add(servant, Util.stringToIdentity("AuthService"));
            adapter.activate();
            System.out.println("[auth-service] listening on " + port);
            c.waitForShutdown();
        } catch (Exception e) {
            System.err.println("[auth-service] FATAL: " + e);
            System.exit(1);
        }
    }
}
