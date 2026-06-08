// SITM-MIO Slice contracts (V3 distribuida).
// Spec 02-slice-contracts. Política: añadir campos al final; no renombrar.

module SITM {

    // ── Datagrama crudo (alineado con Diccionario_De_Datos-OkGTM.pdf) ──
    struct Datagram {
        int eventType;
        string registerDate;     // "DD-MON-YY"
        int stopId;
        int odometer;
        int latitude;            // entero x10^7
        int longitude;           // entero x10^7
        int taskId;
        int lineId;
        int tripId;
        int unknown1;
        string datagramDate;     // "YYYY-MM-DD HH:MM:SS"
        int busId;
    };

    // ── Tiempo real (R4) ──
    struct Location { double latitude; double longitude; };

    struct BusUpdate {
        int busId;
        Location pos;
        int lineId;
        string timestamp;
        int zoneId;              // filtro server-side R9.2
        string operationalState; // R23.1: EN_RUTA|PARADO|SIN_SENAL|CRITICO
    };
    sequence<BusUpdate> BusUpdateSeq;

    struct CriticAlert {         // R19
        int busId;
        int lineId;
        int zoneId;
        int eventType;
        string priority;         // ALTA|MEDIA|BAJA
        string timestamp;
        string description;
    };

    // ── Analítica (R7) ──
    struct SpeedReport {
        int lineId;
        string shortName;
        string description;
        int year;
        int month;
        double totalDistanceKm;
        double totalTimeHours;
        double averageSpeedKmH;
        long validSegments;
        long skippedSegments;
        string status;           // OK|NO_DATA
    };
    sequence<SpeedReport> SpeedReportSeq;

    // ── Master-Worker ──
    struct PartitionKey { int lineId; int year; int month; };
    sequence<PartitionKey> PartitionKeySeq;

    struct WorkerMetrics {
        long datagramsRead;
        long validSegments;
        long outliersDropped;
        long minusOneFiltered;
        long elapsedMillis;
    };

    // ── Excepciones ──
    exception InvalidDatagram { string reason; };
    exception NoDataForPartition {};
    exception Unauthorized {};

    // ── Sesión y auth ──
    struct AuthToken { string jwt; long expiresAtEpochMs; };
    struct UserContext { string userId; string role; int assignedZoneId; };

    // ── Catálogo de rutas (R25) ──
    struct RouteInfo { int lineId; string shortName; string description; };
    sequence<RouteInfo> RouteInfoSeq;

    // ───── Interfaces ─────

    interface MonitoringSubscriber {
        void updateLocation(BusUpdate u);
        void updateLocations(BusUpdateSeq us);
        void onCriticAlert(CriticAlert a);
    };

    // R6.1, OrderPipeline
    interface DatagramReceiver {
        void postDatagram(Datagram d) throws InvalidDatagram;
        void subscribe(MonitoringSubscriber* sub);
    };

    // Reliable Messaging (ADR-2)
    interface DatagramQueue {
        void enqueueDatagram(Datagram d);
        Datagram dequeueDatagram();
        long queueSize();
    };

    // Cold storage (R6.2, R30)
    interface ArchiveService {
        ["ami"] void archiveDatagram(Datagram d);
        SpeedReport getReport(int lineId, int year, int month) throws NoDataForPartition;
    };

    // EDA pub-sub
    interface DatagramEventBus {
        void publishUpdate(BusUpdate u);
        void publishAlert(CriticAlert a);
        void subscribe(MonitoringSubscriber* sub, int zoneId);
        void unsubscribe(MonitoringSubscriber* sub);
    };

    // Master-Worker (R7)
    interface IBatchWorker {
        SpeedReport computePartition(PartitionKey k) throws NoDataForPartition;
        WorkerMetrics lastMetrics();
        string workerId();
    };

    interface BatchMaster {
        void registerWorker(IBatchWorker* w);
        SpeedReportSeq runMonth(int year, int month);
        SpeedReportSeq runRange(int yearFrom, int monthFrom, int yearTo, int monthTo);
    };

    // R7.4 / R27
    interface ReportProvider {
        SpeedReport getAverageSpeed(int lineId, int year, int month) throws NoDataForPartition;
        SpeedReportSeq getMonthlyReports(int year, int month);
        SpeedReportSeq getRangeReports(int yf, int mf, int yt, int mt);
    };

    // R10
    interface AuthService {
        AuthToken login(string user, string password) throws Unauthorized;
        UserContext validate(string jwt) throws Unauthorized;
    };

    // R14
    interface PublicAPI {
        SpeedReport publicSpeed(int lineId, int year, int month) throws NoDataForPartition;
        string systemStatus();
    };

    // R8/R9 lookup
    interface SessionContextController {
        int zoneOfLine(int lineId);
        int zoneOfUser(string userId);
    };
};
