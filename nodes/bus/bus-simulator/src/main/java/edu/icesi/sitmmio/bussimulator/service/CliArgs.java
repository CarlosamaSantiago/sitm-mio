package edu.icesi.sitmmio.bussimulator.service;

import java.util.HashMap;
import java.util.Map;

/** Parser CLI minimalista: pares --key value. */
public final class CliArgs {
    private final Map<String, String> kv = new HashMap<>();

    private CliArgs(Map<String, String> kv) { this.kv.putAll(kv); }

    public static CliArgs parse(String[] args) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].startsWith("--")) {
                String key = args[i].substring(2);
                String val = args[i + 1];
                if (!val.startsWith("--")) { m.put(key, val); i++; }
            }
        }
        return new CliArgs(m);
    }

    public String str(String k, String def)   { return kv.getOrDefault(k, def); }
    public int    intv(String k, int def)     { return kv.containsKey(k) ? Integer.parseInt(kv.get(k)) : def; }
    public long   longv(String k, long def)   { return kv.containsKey(k) ? Long.parseLong(kv.get(k)) : def; }
    public double dbl(String k, double def)   { return kv.containsKey(k) ? Double.parseDouble(kv.get(k)) : def; }
}
