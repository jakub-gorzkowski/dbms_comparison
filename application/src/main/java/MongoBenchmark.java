import com.mongodb.*;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class MongoBenchmark {
    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final List<String> nationalities = Arrays.asList("Polska", "Niemcy", "Ukraina", "Białoruś", "Czechy", "Słowacja", "Litwa", "Rosja");
    private final List<String> directions = Arrays.asList("Wjazd", "Wyjazd");
    private final List<String> firstNames = Arrays.asList("Jan", "Anna", "Piotr", "Maria", "Andrzej", "Katarzyna", "Tomasz", "Aleksandra");
    private final List<String> lastNames = Arrays.asList("Nowak", "Kowalski", "Wiśniewski", "Wójcik", "Kowalczyk", "Kamiński", "Lewandowski", "Zieliński");
    private final Random random = new Random();

    private Map<String, ObjectId> cachedOddzialyIds = new HashMap<>();
    private Map<String, ObjectId> cachedRodzajeIds = new HashMap<>();
    private Map<String, ObjectId> cachedDokumentyIds = new HashMap<>();
    private Map<String, ObjectId> cachedPlacowkiIds = new HashMap<>();
    private Map<String, ObjectId> cachedPrzejsciaIds = new HashMap<>();
    private List<ObjectId> cachedOsobyIds = new ArrayList<>();

    public MongoBenchmark(String database) {
        String mongoHost = System.getenv("MONGO_HOST") != null ?
                System.getenv("MONGO_HOST") : "mongodb";
        String mongoPort = System.getenv("MONGO_INNER_PORT") != null ?
                System.getenv("MONGO_INNER_PORT") : "27017";

        String connectionString = String.format("mongodb://%s:%s", mongoHost, mongoPort);

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .applyToSocketSettings(builder ->
                        builder.connectTimeout(10000, TimeUnit.MILLISECONDS))
                .applyToServerSettings(builder ->
                        builder.heartbeatFrequency(20000, TimeUnit.MILLISECONDS))
                .applyToClusterSettings(builder ->
                        builder.serverSelectionTimeout(10000, TimeUnit.MILLISECONDS))
                .build();

        try {
            this.mongoClient = MongoClients.create(settings);
            this.database = mongoClient.getDatabase(database);

            initializeDatabase();
        } catch (MongoTimeoutException e) {
            System.err.println("Timeout podczas łączenia z MongoDB: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("Błąd podczas inicjalizacji MongoDB: " + e.getMessage());
            throw e;
        }
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
        } catch (Exception e) {
            System.err.println("Błąd podczas inicjalizacji bazy danych: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void addOddzialStrazyGranicznej(String nazwa) {
        MongoCollection<Document> collection = database.getCollection("oddzialy_strazy_granicznej");
        Document oddzial = collection.find(new Document("nazwa", nazwa)).first();
        if (oddzial == null) {
            oddzial = new Document("nazwa", nazwa);
            collection.insertOne(oddzial);
        }
        cachedOddzialyIds.put(nazwa, oddzial.getObjectId("_id"));
    }

    private void addRodzajDokumentu(String nazwa) {
        MongoCollection<Document> collection = database.getCollection("rodzaje_dokumentow");
        Document rodzaj = collection.find(new Document("nazwa", nazwa)).first();
        if (rodzaj == null) {
            rodzaj = new Document("nazwa", nazwa);
            collection.insertOne(rodzaj);
        }
        cachedDokumentyIds.put(nazwa, rodzaj.getObjectId("_id"));
    }

    private void addRodzajPrzejscia(String nazwa) {
        MongoCollection<Document> collection = database.getCollection("rodzaje_przejsc");
        Document rodzaj = collection.find(new Document("nazwa", nazwa)).first();
        if (rodzaj == null) {
            rodzaj = new Document("nazwa", nazwa);
            collection.insertOne(rodzaj);
        }
        cachedRodzajeIds.put(nazwa, rodzaj.getObjectId("_id"));
    }

    private void addPlacowkaStrazyGranicznej(String nazwa, String oddzialNazwa) {
        MongoCollection<Document> collection = database.getCollection("placowki_strazy_granicznej");
        ObjectId oddzialId = cachedOddzialyIds.get(oddzialNazwa);

        Document placowka = collection.find(new Document("nazwa", nazwa)).first();
        if (placowka == null) {
            placowka = new Document()
                    .append("nazwa", nazwa)
                    .append("oddzial_id", oddzialId);
            collection.insertOne(placowka);
        }
        cachedPlacowkiIds.put(nazwa, placowka.getObjectId("_id"));
    }

    private void addOsoba(String imie, String nazwisko, String narodowosc) {
        MongoCollection<Document> collection = database.getCollection("osoby");
        Document osoba = new Document()
                .append("imie", imie)
                .append("nazwisko", nazwisko)
                .append("narodowosc", narodowosc);
        collection.insertOne(osoba);
        cachedOsobyIds.add(osoba.getObjectId("_id"));
    }

    private void addPrzejscieGraniczne(String nazwa, String granica, String placowkaNazwa, List<String> rodzajePrzejscia) {
        MongoCollection<Document> collection = database.getCollection("przejscia_graniczne");
        ObjectId placowkaId = cachedPlacowkiIds.get(placowkaNazwa);

        List<ObjectId> rodzajeIds = new ArrayList<>();
        for (String rodzajNazwa : rodzajePrzejscia) {
            rodzajeIds.add(cachedRodzajeIds.get(rodzajNazwa));
        }

        Document przejscie = collection.find(new Document("nazwa", nazwa)).first();
        if (przejscie == null) {
            przejscie = new Document()
                    .append("nazwa", nazwa)
                    .append("granica", granica)
                    .append("placowka_id", placowkaId)
                    .append("rodzaje", rodzajeIds);
            collection.insertOne(przejscie);
        }
        cachedPrzejsciaIds.put(nazwa, przejscie.getObjectId("_id"));
    }

    public long benchmarkBatchInsertKontroleGraniczne(int numberOfControls, int batchSize) {
        if (cachedOsobyIds.isEmpty()) {
            throw new IllegalStateException("Brak osób w bazie danych");
        }

        long startTime = System.currentTimeMillis();
        MongoCollection<Document> collection = database.getCollection("kontrole_graniczne");

        List<Document> batch = new ArrayList<>();
        for (int i = 0; i < numberOfControls; i++) {
            String przejscieNazwa = new ArrayList<>(cachedPrzejsciaIds.keySet()).get(random.nextInt(cachedPrzejsciaIds.size()));
            String oddzialNazwa = new ArrayList<>(cachedOddzialyIds.keySet()).get(random.nextInt(cachedOddzialyIds.size()));

            Document kontrola = new Document()
                    .append("przejscie_id", cachedPrzejsciaIds.get(przejscieNazwa))
                    .append("oddzial_id", cachedOddzialyIds.get(oddzialNazwa))
                    .append("data", Date.from(LocalDate.now().minusDays(random.nextInt(365))
                            .atStartOfDay(ZoneId.systemDefault()).toInstant()))
                    .append("kto", "Funkcjonariusz SG")
                    .append("kierunek", directions.get(random.nextInt(directions.size())))
                    .append("osoba_id", cachedOsobyIds.get(random.nextInt(cachedOsobyIds.size())))
                    .append("dokument_id", cachedDokumentyIds.get(new ArrayList<>(cachedDokumentyIds.keySet())
                            .get(random.nextInt(cachedDokumentyIds.size()))));

            batch.add(kontrola);

            if (batch.size() >= batchSize || i == numberOfControls - 1) {
                collection.insertMany(batch);
                batch.clear();
            }
        }

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        System.out.println("Benchmark operacji INSERT zakończony. Czas wykonania: " + executionTime + " ms");
        return executionTime;
    }

    public long benchmarkBatchFindKontroleGraniczne(int numberOfQueries, int batchSize) {
        long startTime = System.currentTimeMillis();
        MongoCollection<Document> collection = database.getCollection("kontrole_graniczne");

        List<Document> pipeline = Arrays.asList(
                new Document("$sample", new Document("size", 100)),
                new Document("$lookup", new Document()
                        .append("from", "przejscia_graniczne")
                        .append("localField", "przejscie_id")
                        .append("foreignField", "_id")
                        .append("as", "przejscie")),
                new Document("$lookup", new Document()
                        .append("from", "osoby")
                        .append("localField", "osoba_id")
                        .append("foreignField", "_id")
                        .append("as", "osoba")),
                new Document("$lookup", new Document()
                        .append("from", "rodzaje_dokumentow")
                        .append("localField", "dokument_id")
                        .append("foreignField", "_id")
                        .append("as", "dokument"))
        );

        for (int i = 0; i < numberOfQueries; i += batchSize) {
            collection.aggregate(pipeline)
                    .allowDiskUse(true)
                    .batchSize(100)
                    .into(new ArrayList<>());
        }

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        System.out.println("Benchmark operacji FIND zakończony. Czas wykonania: " + executionTime + " ms");
        return executionTime;
    }

    public long benchmarkBatchUpdateKontroleGraniczne(int numberOfUpdates, int batchSize) {
        long startTime = System.currentTimeMillis();
        MongoCollection<Document> collection = database.getCollection("kontrole_graniczne");

        List<Document> kontrole = collection.find()
                .limit(numberOfUpdates)
                .into(new ArrayList<>());

        int count = 0;
        List<WriteModel<Document>> updates = new ArrayList<>();
        for (Document kontrola : kontrole) {
            String nowyKierunek = directions.get(random.nextInt(directions.size()));
            updates.add(new UpdateOneModel<>(
                    Filters.eq("_id", kontrola.getObjectId("_id")),
                    Updates.set("kierunek", nowyKierunek)
            ));

            if (++count % batchSize == 0 || count == kontrole.size()) {
                if (!updates.isEmpty()) {
                    collection.bulkWrite(updates);
                    updates.clear();
                }
            }
        }

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        System.out.println("Benchmark operacji UPDATE zakończony. Czas wykonania: " + executionTime + " ms");
        return executionTime;
    }

    public long benchmarkBatchDeleteKontroleGraniczne(int numberOfDeletes, int batchSize) {
        long startTime = System.currentTimeMillis();
        MongoCollection<Document> collection = database.getCollection("kontrole_graniczne");

        collection.createIndex(Indexes.ascending("data", "przejscie_id"));

        for (int i = 0; i < numberOfDeletes; i += batchSize) {
            int currentBatchSize = Math.min(batchSize, numberOfDeletes - i);

            List<ObjectId> idsToDelete = collection.find()
                    .limit(currentBatchSize)
                    .projection(Projections.include("_id"))
                    .map(doc -> doc.getObjectId("_id"))
                    .into(new ArrayList<>());

            if (idsToDelete.isEmpty()) {
                break;
            }
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

        long batchInsertTime = 0, batchFindTime = 0, batchUpdateTime = 0, batchDeleteTime = 0;

        if (numberOfInitialControls > 0) {
            System.out.println("\nOperacje INSERT:");
            batchInsertTime = benchmarkBatchInsertKontroleGraniczne(numberOfInitialControls, batchSize);
        }

        if (numberOfQueries > 0) {
            System.out.println("\nOperacje FIND:");
            batchFindTime = benchmarkBatchFindKontroleGraniczne(numberOfQueries, batchSize);
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
        System.out.println("Find: " + batchFindTime + "ms");
        System.out.println("Update: " + batchUpdateTime + "ms");
        System.out.println("Delete: " + batchDeleteTime + "ms");
    }

    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
            System.out.println("Połączenie z bazą danych zostało zamknięte.");
        }
    }
}