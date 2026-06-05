#!/bin/bash
cd "$(/usr/bin/dirname "$0")"
mkdir -p experiment

cat > experiment/RunR7.java <<'JAVA'
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.Ice.Util;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RunR7 {
    public static void main(String[] args) throws java.lang.Exception {
        int year = 2019, month = 5;
        if (args.length >= 2) { year = Integer.parseInt(args[0]); month = Integer.parseInt(args[1]); }
        String masterHost = System.getenv().getOrDefault("SITM_MASTER_HOST", "127.0.0.1");
        String masterPort = System.getenv().getOrDefault("SITM_MASTER_PORT", "10050");
        try (Communicator c = Util.initialize(new String[0])) {
            ObjectPrx base = c.stringToProxy("BatchMaster:default -h " + masterHost + " -p " + masterPort);
            SITM.BatchMasterPrx master = SITM.BatchMasterPrx.checkedCast(base);
            if (master == null) { System.err.println("Master proxy inválido"); System.exit(1); }

            System.out.println();
            System.out.println("==================================================================");
            System.out.println("   V3 DISTRIBUIDA - runMonth(" + year + ", " + month + ")");
            System.out.println("==================================================================");
            System.out.println("Dispatching to BatchMaster... (workers procesarán en paralelo)");

            long t0 = System.currentTimeMillis();
            SITM.SpeedReport[] results = master.runMonth(year, month);
            long elapsed = System.currentTimeMillis() - t0;

            List<SITM.SpeedReport> sorted = new ArrayList<>(Arrays.asList(results));
            sorted.sort((a, b) -> Double.compare(b.averageSpeedKmH, a.averageSpeedKmH));

            int ok = 0, noData = 0;
            try (PrintWriter pw = new PrintWriter(new FileWriter("experiment/v3-results.csv"))) {
                pw.println("lineId,shortName,description,yearMonth,totalDistanceKm,totalTimeHours,averageSpeedKmH,validSegments,skippedSegments,status");
                for (SITM.SpeedReport r : sorted) {
                    pw.printf("%d,\"%s\",\"%s\",%04d-%02d,%.6f,%.9f,%.6f,%d,%d,%s%n",
                        r.lineId, r.shortName, r.description, r.year, r.month,
                        r.totalDistanceKm, r.totalTimeHours, r.averageSpeedKmH,
                        r.validSegments, r.skippedSegments, r.status);
                    if ("OK".equals(r.status)) ok++; else noData++;
                }
            }

            List<SITM.SpeedReport> okOnly = new ArrayList<>();
            for (SITM.SpeedReport r : sorted) if ("OK".equals(r.status)) okOnly.add(r);

            System.out.println();
            System.out.println("+-----------+------------+-----------+----------+--------------+");
            System.out.println("| lineId    | shortName  |  km/h     | segments | status       |");
            System.out.println("+-----------+------------+-----------+----------+--------------+");

            int show = Math.min(15, okOnly.size());
            for (int i = 0; i < show; i++) {
                SITM.SpeedReport r = okOnly.get(i);
                System.out.printf("| %-9d | %-10s | %8.4f  | %8d | %-12s |%n",
                    r.lineId, trunc(r.shortName, 10), r.averageSpeedKmH, r.validSegments, r.status);
            }
            if (okOnly.size() > 30) {
                System.out.println("|    ...    |    ...     |   ...     |   ...    |     ...      |");
                for (int i = okOnly.size() - 15; i < okOnly.size(); i++) {
                    SITM.SpeedReport r = okOnly.get(i);
                    System.out.printf("| %-9d | %-10s | %8.4f  | %8d | %-12s |%n",
                        r.lineId, trunc(r.shortName, 10), r.averageSpeedKmH, r.validSegments, r.status);
                }
            }
            System.out.println("+-----------+------------+-----------+----------+--------------+");

            System.out.println();
            System.out.printf ("[V3] Tiempo total:        %d ms%n", elapsed);
            System.out.printf ("[V3] Rutas procesadas:    %d (OK: %d, NO_DATA: %d)%n", results.length, ok, noData);
            if (!okOnly.isEmpty()) {
                double max = okOnly.get(0).averageSpeedKmH;
                double min = okOnly.get(okOnly.size()-1).averageSpeedKmH;
                double avg = okOnly.stream().mapToDouble(r -> r.averageSpeedKmH).average().orElse(0);
                System.out.printf("[V3] Velocidad MAX:       %.4f km/h (%s)%n", max, trunc(okOnly.get(0).shortName, 10));
                System.out.printf("[V3] Velocidad MIN:       %.4f km/h (%s)%n", min, trunc(okOnly.get(okOnly.size()-1).shortName, 10));
                System.out.printf("[V3] Velocidad promedio:  %.4f km/h%n", avg);
            }
            System.out.println("[V3] Output:              experiment/v3-results.csv");
        }
    }

    private static String trunc(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n);
    }
}
JAVA
javac -d experiment -cp "contracts/build/runtime-libs/*:contracts/build/classes/java/main" experiment/RunR7.java

java -cp "experiment:contracts/build/runtime-libs/*:contracts/build/classes/java/main" RunR7 "$@"

if [ -f data/output/monolith-results.csv ] && [ -f experiment/v3-results.csv ]; then
  echo ""
  echo "==================================================================="
  echo " VERIFICACION CONTRA ORACLE V1"
  echo "==================================================================="
  DIFF=$(diff <(/usr/bin/sort data/output/monolith-results.csv | /usr/bin/grep -v "^lineId") \
              <(/usr/bin/sort experiment/v3-results.csv | /usr/bin/grep -v "^lineId") 2>&1)
  if [ -z "$DIFF" ]; then
    echo " V3 == V1 (correctness verificada)"
  else
    LINES=$(echo "$DIFF" | /usr/bin/wc -l | /usr/bin/tr -d ' ')
    echo " V3 difiere en $LINES lineas. Primeras divergencias:"
    echo "$DIFF" | /usr/bin/head -10
  fi
  echo "==================================================================="
fi
