import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.time.LocalDate;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class MySQLBenchmark {
    private final HikariDataSource dataSource;
    private final List<String> nationalities = Arrays.asList("Polska", "Niemcy", "Ukraina", "Białoruś", "Czechy", "Słowacja", "Litwa", "Rosja");
    private final List<String> directions = Arrays.asList("Wjazd", "Wyjazd");
    private final List<String> firstNames = Arrays.asList("Jan", "Anna", "Piotr", "Maria", "Andrzej", "Katarzyna", "Tomasz", "Aleksandra");
    private final List<String> lastNames = Arrays.asList("Nowak", "Kowalski", "Wiśniewski", "Wójcik", "Kowalczyk", "Kamiński", "Lewandowski", "Zieliński");
    private final Random random = new Random();

    private Map<String, Integer> cachedOddzialyIds = new HashMap<>();
    private Map<String, Integer> cachedRodzajeIds = new HashMap<>();
    private Map<String, Integer> cachedDokumentyIds = new HashMap<>();
    private Map<String, Integer> cachedPlacowkiIds = new HashMap<>();
    private Map<String, Integer> cachedPrzejsciaIds = new HashMap<>();
    private List<Integer> cachedOsobyIds = new ArrayList<>();

    public MySQLBenchmark(String database, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setJdbcUrl("jdbc:mysql://mysql:3306/" + database + "?useSSL=false&allowPublicKeyRetrieval=true");
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");

        dataSource = new HikariDataSource(config);
        initializeDatabase();
    }


    private void initializeDatabase() {
        try {
            addOddzialStrazyGranicznej("Bieszczadzki Oddział SG");
            addOddzialStrazyGranicznej("Podlaski Oddział SG");
            addOddzialStrazyGranicznej("Warmińsko-Mazurski Oddział SG");
            addOddzialStrazyGranicznej("Nadbużański Oddział SG");

            addRodzajDokumentu("Paszport");
            addRodzajDokumentu("Dowód osobisty");
            addRodzajDokumentu("Wiza");
            addRodzajDokumentu("Karta pobytu");

            addRodzajPrzejscia("Drogowe");
            addRodzajPrzejscia("Kolejowe");
            addRodzajPrzejscia("Lotnicze");
            addRodzajPrzejscia("Rzeczne");

            addPlacowkaStrazyGranicznej("Placówka SG w Medyce", "Bieszczadzki Oddział SG");
            addPlacowkaStrazyGranicznej("Placówka SG w Terespolu", "Nadbużański Oddział SG");
            addPlacowkaStrazyGranicznej("Placówka SG w Kuźnicy", "Podlaski Oddział SG");
            addPlacowkaStrazyGranicznej("Placówka SG w Bezledach", "Warmińsko-Mazurski Oddział SG");

            addPrzejscieGraniczne("Medyka-Szeginie", "Polska-Ukraina", "Placówka SG w Medyce", Arrays.asList("Drogowe"));
            addPrzejscieGraniczne("Terespol-Brześć", "Polska-Białoruś", "Placówka SG w Terespolu", Arrays.asList("Drogowe", "Kolejowe"));
            addPrzejscieGraniczne("Kuźnica-Grodno", "Polska-Białoruś", "Placówka SG w Kuźnicy", Arrays.asList("Drogowe"));
            addPrzejscieGraniczne("Bezledy-Bagrationowsk", "Polska-Rosja", "Placówka SG w Bezledach", Arrays.asList("Drogowe"));

            for (int i = 0; i < 100; i++) {
                addOsoba(
                        firstNames.get(random.nextInt(firstNames.size())),
                        lastNames.get(random.nextInt(lastNames.size())),
                        nationalities.get(random.nextInt(nationalities.size()))
                );
            }
        } catch (SQLException e) {
            System.err.println("Błąd podczas inicjalizacji bazy danych MySQL: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void addOddzialStrazyGranicznej(String nazwa) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO oddzialy_strazy_granicznej (nazwa) VALUES (?) ON DUPLICATE KEY UPDATE nazwa=nazwa",
                    Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, nazwa);
                pstmt.executeUpdate();

                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        cachedOddzialyIds.put(nazwa, rs.getInt(1));
                    } else {
                        try (PreparedStatement selectStmt = conn.prepareStatement(
                                "SELECT id FROM oddzialy_strazy_granicznej WHERE nazwa = ?")) {
                            selectStmt.setString(1, nazwa);
                            try (ResultSet selectRs = selectStmt.executeQuery()) {
                                if (selectRs.next()) {
                                    cachedOddzialyIds.put(nazwa, selectRs.getInt("id"));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void addRodzajDokumentu(String nazwa) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO rodzaje_dokumentow (nazwa) VALUES (?) ON DUPLICATE KEY UPDATE nazwa=nazwa",
                    Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, nazwa);
                pstmt.executeUpdate();

                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        cachedDokumentyIds.put(nazwa, rs.getInt(1));
                    } else {
                        try (PreparedStatement selectStmt = conn.prepareStatement(
                                "SELECT id FROM rodzaje_dokumentow WHERE nazwa = ?")) {
                            selectStmt.setString(1, nazwa);
                            try (ResultSet selectRs = selectStmt.executeQuery()) {
                                if (selectRs.next()) {
                                    cachedDokumentyIds.put(nazwa, selectRs.getInt("id"));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void addRodzajPrzejscia(String nazwa) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO rodzaje_przejsc (nazwa) VALUES (?) ON DUPLICATE KEY UPDATE nazwa=nazwa",
                    Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, nazwa);
                pstmt.executeUpdate();

                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        cachedRodzajeIds.put(nazwa, rs.getInt(1));
                    } else {
                        try (PreparedStatement selectStmt = conn.prepareStatement(
                                "SELECT id FROM rodzaje_przejsc WHERE nazwa = ?")) {
                            selectStmt.setString(1, nazwa);
                            try (ResultSet selectRs = selectStmt.executeQuery()) {
                                if (selectRs.next()) {
                                    cachedRodzajeIds.put(nazwa, selectRs.getInt("id"));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void addPlacowkaStrazyGranicznej(String nazwa, String oddzialNazwa) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            Integer oddzialId = cachedOddzialyIds.get(oddzialNazwa);
            if (oddzialId == null) {
                throw new SQLException("Oddział o nazwie " + oddzialNazwa + " nie istnieje");
            }

            try (PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO placowki_strazy_granicznej (nazwa, oddzial_id) VALUES (?, ?) ON DUPLICATE KEY UPDATE nazwa=nazwa",
                    Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, nazwa);
                pstmt.setInt(2, oddzialId);
                pstmt.executeUpdate();

                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        cachedPlacowkiIds.put(nazwa, rs.getInt(1));
                    } else {
                        try (PreparedStatement selectStmt = conn.prepareStatement(
                                "SELECT id FROM placowki_strazy_granicznej WHERE nazwa = ?")) {
                            selectStmt.setString(1, nazwa);
                            try (ResultSet selectRs = selectStmt.executeQuery()) {
                                if (selectRs.next()) {
                                    cachedPlacowkiIds.put(nazwa, selectRs.getInt("id"));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void addOsoba(String imie, String nazwisko, String narodowosc) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO osoby (imie, nazwisko, narodowosc) VALUES (?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, imie);
            pstmt.setString(2, nazwisko);
            pstmt.setString(3, narodowosc);
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    cachedOsobyIds.add(rs.getInt(1));
                }
            }
        }
    }

    private void addPrzejscieGraniczne(String nazwa, String granica, String placowkaNazwa, List<String> rodzajePrzejscia) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try {
                Integer placowkaId = cachedPlacowkiIds.get(placowkaNazwa);
                if (placowkaId == null) {
                    throw new SQLException("Placówka o nazwie " + placowkaNazwa + " nie istnieje");
                }

                int przejscieId;
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT INTO przejscia_graniczne (nazwa, granica, placowka_id) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE nazwa=nazwa",
                        Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setString(1, nazwa);
                    pstmt.setString(2, granica);
                    pstmt.setInt(3, placowkaId);
                    pstmt.executeUpdate();

                    try (ResultSet rs = pstmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            przejscieId = rs.getInt(1);
                            cachedPrzejsciaIds.put(nazwa, przejscieId);
                        } else {
                            try (PreparedStatement selectStmt = conn.prepareStatement(
                                    "SELECT id FROM przejscia_graniczne WHERE nazwa = ?")) {
                                selectStmt.setString(1, nazwa);
                                try (ResultSet selectRs = selectStmt.executeQuery()) {
                                    if (selectRs.next()) {
                                        przejscieId = selectRs.getInt("id");
                                        cachedPrzejsciaIds.put(nazwa, przejscieId);
                                    } else {
                                        throw new SQLException("Nie można pobrać ID przejścia granicznego");
                                    }
                                }
                            }
                        }
                    }
                }

                try (PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT IGNORE INTO przejscia_rodzaje (przejscie_id, rodzaj_id) VALUES (?, ?)")) {
                    for (String rodzajNazwa : rodzajePrzejscia) {
                        Integer rodzajId = cachedRodzajeIds.get(rodzajNazwa);
                        if (rodzajId == null) {
                            throw new SQLException("Rodzaj przejścia o nazwie " + rodzajNazwa + " nie istnieje");
                        }

                        pstmt.setInt(1, przejscieId);
                        pstmt.setInt(2, rodzajId);
                        pstmt.addBatch();
                    }
                    pstmt.executeBatch();
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public long benchmarkBatchInsertKontroleGraniczne(int numberOfControls, int batchSize) {
        if (cachedOsobyIds.isEmpty()) {
            throw new IllegalStateException("Brak osób w bazie danych. Najpierw dodaj osoby.");
        }

        List<String> przejsciaNazwy = new ArrayList<>(cachedPrzejsciaIds.keySet());
        List<String> oddzialyNazwy = new ArrayList<>(cachedOddzialyIds.keySet());
        List<String> dokumentyNazwy = new ArrayList<>(cachedDokumentyIds.keySet());

        long startTime = System.currentTimeMillis();

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO kontrole_graniczne (przejscie_id, oddzial_id, data, kto, kierunek, osoba_id, dokument_id) VALUES (?, ?, ?, ?, ?, ?, ?)")) {

                int count = 0;
                for (int i = 0; i < numberOfControls; i++) {
                    String przejscieNazwa = przejsciaNazwy.get(random.nextInt(przejsciaNazwy.size()));
                    String oddzialNazwa = oddzialyNazwy.get(random.nextInt(oddzialyNazwy.size()));
                    LocalDate data = LocalDate.now().minusDays(random.nextInt(365));
                    String kto = "Funkcjonariusz SG";
                    String kierunek = directions.get(random.nextInt(directions.size()));
                    int osobaId = cachedOsobyIds.get(random.nextInt(cachedOsobyIds.size()));
                    String dokumentNazwa = dokumentyNazwy.get(random.nextInt(dokumentyNazwy.size()));

                    Integer przejscieId = cachedPrzejsciaIds.get(przejscieNazwa);
                    Integer oddzialId = cachedOddzialyIds.get(oddzialNazwa);
                    Integer dokumentId = cachedDokumentyIds.get(dokumentNazwa);

                    pstmt.setInt(1, przejscieId);
                    pstmt.setInt(2, oddzialId);
                    pstmt.setDate(3, java.sql.Date.valueOf(data));
                    pstmt.setString(4, kto);
                    pstmt.setString(5, kierunek);
                    pstmt.setInt(6, osobaId);
                    pstmt.setInt(7, dokumentId);
                    pstmt.addBatch();

                    if (++count % batchSize == 0 || i == numberOfControls - 1) {
                        pstmt.executeBatch();
                        conn.commit();
                    }
                }
            }

            conn.setAutoCommit(true);
        } catch (SQLException e) {
            System.err.println("Błąd podczas wstawiania kontroli: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        System.out.println("Benchmark operacji INSERT zakończony. Czas wykonania: " + executionTime + " ms");
        return executionTime;
    }

    public long benchmarkBatchSelectKontroleGraniczne(int numberOfQueries, int batchSize) {
        long startTime = System.currentTimeMillis();

        try (Connection conn = dataSource.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                try {
                    stmt.execute("CREATE INDEX idx_kontrole_data ON kontrole_graniczne(data)");
                    System.out.println("Dodano indeks na kolumnie data");
                } catch (SQLException e) {}
            }

            String sql = "SELECT k.id, p.nazwa as przejscie, o.imie, o.nazwisko, o.narodowosc, " +
                    "rd.nazwa as dokument, k.data, k.kierunek " +
                    "FROM kontrole_graniczne k " +
                    "JOIN przejscia_graniczne p ON k.przejscie_id = p.id " +
                    "JOIN osoby o ON k.osoba_id = o.id " +
                    "JOIN rodzaje_dokumentow rd ON k.dokument_id = rd.id " +
                    "WHERE k.data BETWEEN ? AND ? " +
                    "ORDER BY k.data DESC LIMIT 100";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                int batchSizeDates = 1000000;
                List<Date> startDates = new ArrayList<>(batchSizeDates);
                List<Date> endDates = new ArrayList<>(batchSizeDates);

                for (int i = 0; i < Math.min(batchSizeDates, numberOfQueries); i++) {
                    int daysAgo = random.nextInt(365);
                    LocalDate endDate = LocalDate.now().minusDays(daysAgo);
                    LocalDate startDate = endDate.minusDays(30);
                    startDates.add(java.sql.Date.valueOf(startDate));
                    endDates.add(java.sql.Date.valueOf(endDate));
                }

                for (int i = 0; i < numberOfQueries; i++) {

                    int dateIndex = i % startDates.size();
                    pstmt.setDate(1, startDates.get(dateIndex));
                    pstmt.setDate(2, endDates.get(dateIndex));

                    try (ResultSet rs = pstmt.executeQuery()) {
                        int rowCount = 0;
                        while (rs.next()) {
                            rowCount++;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Błąd podczas wykonywania zapytań SELECT: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        System.out.println("Benchmark operacji SELECT zakończony. Czas wykonania: " + executionTime + " ms");
        return executionTime;
    }


    public long benchmarkBatchUpdateKontroleGraniczne(int numberOfUpdates, int batchSize) {
        long startTime = System.currentTimeMillis();

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            List<Integer> kontroleIds = new ArrayList<>();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id FROM kontrole_graniczne ORDER BY id LIMIT " + numberOfUpdates)) {
                while (rs.next()) {
                    kontroleIds.add(rs.getInt("id"));
                }
            }

            if (kontroleIds.isEmpty()) {
                throw new IllegalStateException("Brak kontroli w bazie danych. Najpierw dodaj kontrole.");
            }

            try (PreparedStatement pstmt = conn.prepareStatement(
                    "UPDATE kontrole_graniczne SET kierunek = ? WHERE id = ?")) {

                int count = 0;
                for (int i = 0; i < Math.min(numberOfUpdates, kontroleIds.size()); i++) {
                    int kontrolaId = kontroleIds.get(i % kontroleIds.size());
                    String nowyKierunek = directions.get(random.nextInt(directions.size()));

                    pstmt.setString(1, nowyKierunek);
                    pstmt.setInt(2, kontrolaId);
                    pstmt.addBatch();

                    if (++count % batchSize == 0 || i == Math.min(numberOfUpdates, kontroleIds.size()) - 1) {
                        pstmt.executeBatch();
                        conn.commit();
                    }
                }
            }

            conn.setAutoCommit(true);
        } catch (SQLException e) {
            System.err.println("Błąd podczas aktualizacji kontroli: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        System.out.println("Benchmark operacji UPDATE zakończony. Czas wykonania: " + executionTime + " ms");
        return executionTime;
    }

    public long benchmarkBatchDeleteKontroleGraniczne(int numberOfDeletes, int batchSize) {
        long startTime = System.currentTimeMillis();

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try {
                int deletedTotal = 0;
                while (deletedTotal < numberOfDeletes) {
                    try (PreparedStatement pstmt = conn.prepareStatement(
                            "DELETE FROM kontrole_graniczne WHERE id IN " +
                                    "(SELECT id FROM (SELECT id FROM kontrole_graniczne LIMIT ?) as t)")) {

                        pstmt.setInt(1, batchSize);
                        int deleted = pstmt.executeUpdate();
                        if (deleted == 0) break;

                        deletedTotal += deleted;
                        conn.commit();
                    }
                }
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("Błąd podczas usuwania: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        System.out.println("Benchmark operacji DELETE zakończony. Czas wykonania: " + executionTime + " ms");
        return executionTime;
    }

    public void runBenchmark(
            int batchSize,
            int numberOfInitialControls,
            int numberOfBatchUpdates,
            int numberOfBatchDeletes,
            int numberOfQueries) {

        long batchInsertTime = 0, batchSelectTime = 0, batchUpdateTime = 0, batchDeleteTime = 0;

        if (numberOfInitialControls > 0) {
            System.out.println("\nOperacje INSERT:");
            batchInsertTime = benchmarkBatchInsertKontroleGraniczne(numberOfInitialControls, batchSize);
        }

        if (numberOfQueries > 0) {
            System.out.println("\nOperacje SELECT:");
            batchSelectTime = benchmarkBatchSelectKontroleGraniczne(numberOfQueries, batchSize);
        }

        if (numberOfBatchUpdates > 0) {
            System.out.println("\nOperacje UPDATE:");
            batchUpdateTime = benchmarkBatchUpdateKontroleGraniczne(numberOfBatchUpdates, batchSize);
        }

        if (numberOfBatchDeletes > 0) {
            System.out.println("\nOperacje DELETE:");
            batchDeleteTime = benchmarkBatchDeleteKontroleGraniczne(numberOfBatchDeletes, batchSize);
        }

        System.out.println("\nPodsumowanie czasów wykonania:");
        System.out.println("Insert: " + batchInsertTime + "ms");
        System.out.println("Select: " + batchSelectTime + "ms");
        System.out.println("Update: " + batchUpdateTime + "ms");
        System.out.println("Delete: " + batchDeleteTime + "ms");
    }

    public void close() {
        if (dataSource != null) {
            dataSource.close();
            System.out.println("Połączenie z bazą danych MySQL zostało zamknięte.");
        }
    }
}