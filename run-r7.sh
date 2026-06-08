#!/bin/bash
# run-r7.sh — dispara el cálculo R7 vía BatchMaster.
#
# Uso:
#   ./run-r7.sh                        # default: rango completo 2018-05..2019-05
#   ./run-r7.sh --all-months           # explícito, mismo efecto
#   ./run-r7.sh <year> <month>         # un solo mes (ej.  ./run-r7.sh 2019 5)
#   ./run-r7.sh --range YYYY M YYYY M  # rango personalizado
#
# Variables de entorno:
#   SITM_MASTER_HOST   default 127.0.0.1
#   SITM_MASTER_PORT   default 10050
#   SITM_R7_YEAR       default 2019   (fallback para arg single-month)
#   SITM_R7_MONTH      default 5      (fallback para arg single-month)
#   SITM_R7_RANGE_FROM default 2018-05
#   SITM_R7_RANGE_TO   default 2019-05

cd "$(/usr/bin/dirname "$0")"
mkdir -p experiment

MASTER_HOST="${SITM_MASTER_HOST:-127.0.0.1}"
MASTER_PORT="${SITM_MASTER_PORT:-10050}"
DEFAULT_FROM="${SITM_R7_RANGE_FROM:-2018-05}"
DEFAULT_TO="${SITM_R7_RANGE_TO:-2019-05}"

# Determinar modo (single-month | range | all-months)
MODE="range"
YEAR=""
MONTH=""
YEAR_FROM=$(echo "$DEFAULT_FROM" | /usr/bin/cut -d- -f1)
MONTH_FROM=$(echo "$DEFAULT_FROM" | /usr/bin/cut -d- -f2 | /usr/bin/sed 's/^0*//')
YEAR_TO=$(echo "$DEFAULT_TO" | /usr/bin/cut -d- -f1)
MONTH_TO=$(echo "$DEFAULT_TO" | /usr/bin/cut -d- -f2 | /usr/bin/sed 's/^0*//')

if [ "$#" -eq 0 ] || [ "$1" = "--all-months" ]; then
    MODE="range"
elif [ "$1" = "--range" ] && [ "$#" -eq 5 ]; then
    MODE="range"
    YEAR_FROM=$2; MONTH_FROM=$3; YEAR_TO=$4; MONTH_TO=$5
elif [ "$#" -eq 2 ]; then
    MODE="single"; YEAR=$1; MONTH=$2
else
    echo "Uso: $0 [--all-months] [--range Y M Y M] [year month]"
    exit 2
fi

# Compilar cliente Ice si no existe (o si lo regeneramos)
COMPILE_FLAG="${SITM_R7_REBUILD:-0}"
if [ ! -f experiment/RunR7.class ] || [ "$COMPILE_FLAG" = "1" ]; then
    cat > experiment/RunR7.java <<'JAVA'
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.Ice.Util;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class RunR7 {
    public static void main(String[] args) throws java.lang.Exception {
        String host = System.getenv("SITM_MASTER_HOST"); if (host == null) host = "127.0.0.1";
        String port = System.getenv("SITM_MASTER_PORT"); if (port == null) port = "10050";
        String mode = args.length > 0 ? args[0] : "range";

        try (Communicator c = Util.initialize(new String[0])) {
            ObjectPrx base = c.stringToProxy("BatchMaster:default -h " + host + " -p " + port);
            SITM.BatchMasterPrx master = SITM.BatchMasterPrx.checkedCast(base);
            if (master == null) { System.err.println("Master proxy invalido"); System.exit(1); }

            SITM.SpeedReport[] results;
            long t0 = System.currentTimeMillis();

            if ("single".equals(mode)) {
                int year = Integer.parseInt(args[1]);
                int month = Integer.parseInt(args[2]);
                System.out.println("[V3] runMonth(" + year + ", " + month + ")");
                results = master.runMonth(year, month);
            } else {
                int yf = Integer.parseInt(args[1]);
                int mf = Integer.parseInt(args[2]);
                int yt = Integer.parseInt(args[3]);
                int mt = Integer.parseInt(args[4]);
                System.out.printf("[V3] runRange(%d-%02d .. %d-%02d)%n", yf, mf, yt, mt);
                results = master.runRange(yf, mf, yt, mt);
            }
            long elapsed = System.currentTimeMillis() - t0;

            List<SITM.SpeedReport> sorted = new ArrayList<>(Arrays.asList(results));
            sorted.sort(Comparator
                .<SITM.SpeedReport>comparingInt(r -> r.year)
                .thenComparingInt(r -> r.month)
                .thenComparingInt(r -> r.lineId));

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

            System.out.println();
            System.out.println("+-----------+------------+---------+-----------+----------+--------+");
            System.out.println("| lineId    | shortName  | yearMo  |  km/h     | segments | status |");
            System.out.println("+-----------+------------+---------+-----------+----------+--------+");
            int n = Math.min(30, sorted.size());
            for (int i = 0; i < n; i++) {
                SITM.SpeedReport r = sorted.get(i);
                System.out.printf("| %-9d | %-10s | %04d-%02d | %8.4f  | %8d | %-6s |%n",
                    r.lineId, trunc(r.shortName, 10), r.year, r.month,
                    r.averageSpeedKmH, r.validSegments, r.status);
            }
            if (sorted.size() > 30) System.out.println("|  ...      |    ...     |   ...   |   ...     |   ...    |   ... |");
            System.out.println("+-----------+------------+---------+-----------+----------+--------+");

            System.out.println();
            System.out.printf("[V3] Reportes:      %d (OK=%d, NO_DATA=%d)%n", results.length, ok, noData);
            System.out.printf("[V3] Tiempo total:  %d ms%n", elapsed);
            System.out.println("[V3] Output:        experiment/v3-results.csv");
        }
    }

    private static String trunc(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n);
    }
}
JAVA
    javac -d experiment -cp "contracts/build/runtime-libs/*:contracts/build/classes/java/main" experiment/RunR7.java || exit 1
fi

# Construir args para la JVM
if [ "$MODE" = "single" ]; then
    JARGS=("single" "$YEAR" "$MONTH")
else
    JARGS=("range" "$YEAR_FROM" "$MONTH_FROM" "$YEAR_TO" "$MONTH_TO")
fi

SITM_MASTER_HOST="$MASTER_HOST" SITM_MASTER_PORT="$MASTER_PORT" \
    java -cp "experiment:contracts/build/runtime-libs/*:contracts/build/classes/java/main" RunR7 "${JARGS[@]}"

# Diff contra oracle (si existe)
ORACLE="${SITM_ORACLE:-data/output/monolith-results.csv}"
if [ -f "$ORACLE" ] && [ -f experiment/v3-results.csv ]; then
  echo ""
  echo "==================================================="
  echo " DIFF (lineId, yearMonth) contra oracle $ORACLE"
  echo "==================================================="
  # Construye llave (lineId, yearMonth) y compara por esa llave
  DIFF=$(diff \
    <(/usr/bin/sort -t, -k1,1 -k4,4 "$ORACLE" | /usr/bin/grep -v "^lineId") \
    <(/usr/bin/sort -t, -k1,1 -k4,4 experiment/v3-results.csv | /usr/bin/grep -v "^lineId") 2>&1)
  if [ -z "$DIFF" ]; then
    echo " V3 == V1 (correctness verificada)"
  else
    LINES=$(echo "$DIFF" | /usr/bin/wc -l | /usr/bin/tr -d ' ')
    echo " V3 difiere en $LINES lineas (primeras 10):"
    echo "$DIFF" | /usr/bin/head -10
  fi
  echo "==================================================="
fi
