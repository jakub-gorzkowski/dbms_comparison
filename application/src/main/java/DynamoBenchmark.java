import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class DynamoBenchmark {
    private final DynamoDbClient dynamoDbClient;
    private final Random random = new Random();
    private final List<String> nationalities = Arrays.asList("Polska", "Niemcy", "Ukraina", "Białoruś", "Czechy", "Słowacja", "Litwa", "Rosja");
    private final List<String> directions = Arrays.asList("Wjazd", "Wyjazd");
    private final List<String> firstNames = Arrays.asList("Jan", "Anna", "Piotr", "Maria", "Andrzej", "Katarzyna", "Tomasz", "Aleksandra");
    private final List<String> lastNames = Arrays.asList("Nowak", "Kowalski", "Wiśniewski", "Wójcik", "Kowalczyk", "Kamiński", "Lewandowski", "Zieliński");

    private Map<String, String> cachedOddzialyIds = new HashMap<>();
    private Map<String, String> cachedRodzajeIds = new HashMap<>();
    private Map<String, String> cachedDokumentyIds = new HashMap<>();
    private Map<String, String> cachedPlacowkiIds = new HashMap<>();
    private Map<String, String> cachedPrzejsciaIds = new HashMap<>();
    private List<String> cachedOsobyIds = new ArrayList<>();
    private List<String> cachedKontroleIds = new ArrayList<>();

    public DynamoBenchmark() {
        String host = "dynamodb";
        String port = System.getenv("DYNAMO_OUTER_PORT") != null ?
                System.getenv("DYNAMO_OUTER_PORT") : "8000";

        String endpoint = String.format("http://%s:%s", host, port);

        this.dynamoDbClient = DynamoDbClient.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("dummy", "dummy")))
                .region(Region.US_EAST_1)
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallTimeout(Duration.ofMinutes(5))
                        .apiCallAttemptTimeout(Duration.ofMinutes(2))
                        .retryPolicy(RetryPolicy.builder()
                                .numRetries(3)
                                .build())
                        .build())
                .build();

        setupDatabase();
        initializeDatabase();

    }

    private void setupDatabase() {
        List<String> tables = Arrays.asList(
                "oddzialy_strazy_granicznej",
                "rodzaje_dokumentow",
                "rodzaje_przejsc",
                "placowki_strazy_granicznej",
                "osoby",
                "przejscia_graniczne",
                "kontrole_graniczne"
        );

        for (String tableName : tables) {
            createTableIfNotExists(tableName);
        }
    }

    private void createTableIfNotExists(String tableName) {
        try {
            ListTablesResponse listTablesResponse = dynamoDbClient.listTables();
            if (!listTablesResponse.tableNames().contains(tableName)) {
                CreateTableRequest request = CreateTableRequest.builder()
                        .attributeDefinitions(AttributeDefinition.builder()
                                .attributeName("id")
                                .attributeType(ScalarAttributeType.S)
                                .build())
                        .keySchema(KeySchemaElement.builder()
                                .attributeName("id")
                                .keyType(KeyType.HASH)
                                .build())
                        .provisionedThroughput(ProvisionedThroughput.builder()
                                .readCapacityUnits(5000L)
                                .writeCapacityUnits(5000L)
                                .build())
                        .tableName(tableName)
                        .build();

                dynamoDbClient.createTable(request);
                waitForTableToBeActive(tableName);
            }
        } catch (Exception e) {
            System.err.println("Error creating table " + tableName + ": " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void waitForTableToBeActive(String tableName) {
        try {
            DescribeTableRequest request = DescribeTableRequest.builder()
                    .tableName(tableName)
                    .build();

            int attempts = 0;
            boolean tableIsActive = false;
            while (!tableIsActive && attempts < 30) {
                DescribeTableResponse response = dynamoDbClient.describeTable(request);
                tableIsActive = response.table().tableStatus() == TableStatus.ACTIVE;
                if (!tableIsActive) {
                    Thread.sleep(1000);
                    attempts++;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private void initializeDatabase() {
        List<String> oddzialy = Arrays.asList(
                "Bieszczadzki Oddział SG",
                "Podlaski Oddział SG",
                "Warmińsko-Mazurski Oddział SG",
                "Nadbużański Oddział SG"
        );

        List<String> dokumenty = Arrays.asList(
                "Paszport",
                "Dowód osobisty",
                "Wiza",
                "Karta pobytu"
        );

        List<String> rodzajePrzejsc = Arrays.asList(
                "Drogowe",
                "Kolejowe",
                "Lotnicze",
                "Rzeczne"
        );

        for (String oddzial : oddzialy) {
            addOddzialStrazyGranicznej(oddzial);
        }

        for (String dokument : dokumenty) {
            addRodzajDokumentu(dokument);
        }

        for (String rodzaj : rodzajePrzejsc) {
            addRodzajPrzejscia(rodzaj);
        }

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
    }

    private void addOddzialStrazyGranicznej(String nazwa) {
        String id = UUID.randomUUID().toString();
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s(id).build());
        item.put("nazwa", AttributeValue.builder().s(nazwa).build());

        PutItemRequest request = PutItemRequest.builder()
                .tableName("oddzialy_strazy_granicznej")
                .item(item)
                .build();

        dynamoDbClient.putItem(request);
        cachedOddzialyIds.put(nazwa, id);
    }

    private void addRodzajDokumentu(String nazwa) {
        String id = UUID.randomUUID().toString();
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s(id).build());
        item.put("nazwa", AttributeValue.builder().s(nazwa).build());

        PutItemRequest request = PutItemRequest.builder()
                .tableName("rodzaje_dokumentow")
                .item(item)
                .build();

        dynamoDbClient.putItem(request);
        cachedDokumentyIds.put(nazwa, id);
    }

    private void addRodzajPrzejscia(String nazwa) {
        String id = UUID.randomUUID().toString();
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s(id).build());
        item.put("nazwa", AttributeValue.builder().s(nazwa).build());

        PutItemRequest request = PutItemRequest.builder()
                .tableName("rodzaje_przejsc")
                .item(item)
                .build();

        dynamoDbClient.putItem(request);
        cachedRodzajeIds.put(nazwa, id);
    }

    private void addPlacowkaStrazyGranicznej(String nazwa, String oddzialNazwa) {
        String id = UUID.randomUUID().toString();
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s(id).build());
        item.put("nazwa", AttributeValue.builder().s(nazwa).build());
        item.put("oddzial_id", AttributeValue.builder().s(cachedOddzialyIds.get(oddzialNazwa)).build());

        PutItemRequest request = PutItemRequest.builder()
                .tableName("placowki_strazy_granicznej")
                .item(item)
                .build();

        dynamoDbClient.putItem(request);
        cachedPlacowkiIds.put(nazwa, id);
    }

    private void addOsoba(String imie, String nazwisko, String narodowosc) {
        String id = UUID.randomUUID().toString();
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s(id).build());
        item.put("imie", AttributeValue.builder().s(imie).build());
        item.put("nazwisko", AttributeValue.builder().s(nazwisko).build());
        item.put("narodowosc", AttributeValue.builder().s(narodowosc).build());

        PutItemRequest request = PutItemRequest.builder()
                .tableName("osoby")
                .item(item)
                .build();

        dynamoDbClient.putItem(request);
        cachedOsobyIds.add(id);
    }

    private void addPrzejscieGraniczne(String nazwa, String granica, String placowkaNazwa, List<String> rodzajePrzejscia) {
        String id = UUID.randomUUID().toString();
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s(id).build());
        item.put("nazwa", AttributeValue.builder().s(nazwa).build());
        item.put("granica", AttributeValue.builder().s(granica).build());
        item.put("placowka_id", AttributeValue.builder().s(cachedPlacowkiIds.get(placowkaNazwa)).build());

        List<AttributeValue> rodzajeIds = rodzajePrzejscia.stream()
                .map(rodzaj -> AttributeValue.builder().s(cachedRodzajeIds.get(rodzaj)).build())
                .collect(Collectors.toList());
        item.put("rodzaje", AttributeValue.builder().l(rodzajeIds).build());

        PutItemRequest request = PutItemRequest.builder()
                .tableName("przejscia_graniczne")
                .item(item)
                .build();

        dynamoDbClient.putItem(request);
        cachedPrzejsciaIds.put(nazwa, id);
    }

    public long benchmarkBatchInsertKontroleGraniczne(int numberOfControls, int batchSize) {
        long startTime = System.currentTimeMillis();
        cachedKontroleIds.clear();
        final int MAX_BATCH_SIZE = 25;

        try {
            // Przygotuj wszystkie dane z góry
            List<Map<String, AttributeValue>> allItems = new ArrayList<>(numberOfControls);
            for (int i = 0; i < numberOfControls; i++) {
                Map<String, AttributeValue> item = createKontrolaItem();
                allItems.add(item);
                cachedKontroleIds.add(item.get("id").s());
            }

            int processedItems = 0;
            List<WriteRequest> currentBatch = new ArrayList<>(MAX_BATCH_SIZE);

            for (Map<String, AttributeValue> item : allItems) {
                currentBatch.add(WriteRequest.builder()
                        .putRequest(PutRequest.builder().item(item).build())
                        .build());

                if (currentBatch.size() == MAX_BATCH_SIZE) {
                    processBatch(currentBatch);
                    processedItems += currentBatch.size();
                    currentBatch.clear();
                }
            }

            if (!currentBatch.isEmpty()) {
                processBatch(currentBatch);
                processedItems += currentBatch.size();
            }

        } catch (Exception e) {
            System.err.println("Błąd podczas operacji INSERT: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        System.out.println("Benchmark operacji INSERT zakończony. Czas wykonania: " + executionTime + " ms");
        return executionTime;
    }

    private void processBatch(List<WriteRequest> batch) {
        Map<String, List<WriteRequest>> requestItems = new HashMap<>();
        requestItems.put("kontrole_graniczne", new ArrayList<>(batch));

        int retries = 0;
        int maxRetries = 10;
        Map<String, List<WriteRequest>> unprocessedItems;

        do {
            if (retries > 0) {
                try {
                    Thread.sleep((long) (Math.pow(2, retries) * 50));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }

            BatchWriteItemRequest batchWriteItemRequest = BatchWriteItemRequest.builder()
                    .requestItems(requestItems)
                    .build();

            BatchWriteItemResponse response = dynamoDbClient.batchWriteItem(batchWriteItemRequest);
            unprocessedItems = response.unprocessedItems();

            if (!unprocessedItems.isEmpty()) {
                requestItems = unprocessedItems;
            }

            retries++;
        } while (!unprocessedItems.isEmpty() && retries < maxRetries);

        if (!unprocessedItems.isEmpty()) {
            throw new RuntimeException("Nie udało się przetworzyć wszystkich elementów po " + maxRetries + " próbach");
        }
    }



    public long benchmarkBatchFindKontroleGraniczne(int numberOfQueries, int batchSize) {
        long startTime = System.currentTimeMillis();
        int totalResults = 0;

        try {
            for (int i = 0; i < numberOfQueries; i++) {
                ScanRequest scanRequest = ScanRequest.builder()
                        .tableName("kontrole_graniczne")
                        .limit(batchSize)
                        .build();

                ScanResponse response = dynamoDbClient.scan(scanRequest);
                totalResults += response.items().size();
            }
        } catch (Exception e) {
            System.err.println("Error during FIND operation: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        System.out.println("Benchmark operacji FIND zakończony. Czas wykonania: " + executionTime + " ms");
        return executionTime;
    }


    public long benchmarkBatchUpdateKontroleGraniczne(int numberOfUpdates, int batchSize) {
        long startTime = System.currentTimeMillis();
        final int DYNAMO_BATCH_LIMIT = 25; // Limit DynamoDB dla BatchWriteItem

        try {
            List<String> kontroleToUpdate = new ArrayList<>(cachedKontroleIds);
            if (kontroleToUpdate.isEmpty()) {
                throw new IllegalStateException("No kontrole to update");
            }

            for (int i = 0; i < Math.min(numberOfUpdates, kontroleToUpdate.size()); i += DYNAMO_BATCH_LIMIT) {
                List<WriteRequest> writeRequests = new ArrayList<>();
                int currentBatchSize = Math.min(DYNAMO_BATCH_LIMIT, Math.min(numberOfUpdates - i, kontroleToUpdate.size() - i));

                for (int j = 0; j < currentBatchSize; j++) {
                    String kontrolaId = kontroleToUpdate.get(i + j);
                    Map<String, AttributeValue> item = createUpdateKontrolaItem(kontrolaId);
                    writeRequests.add(WriteRequest.builder()
                            .putRequest(PutRequest.builder().item(item).build())
                            .build());
                }

                if (!writeRequests.isEmpty()) {
                    Map<String, List<WriteRequest>> requestItems = new HashMap<>();
                    requestItems.put("kontrole_graniczne", writeRequests);

                    BatchWriteItemRequest batchWriteItemRequest = BatchWriteItemRequest.builder()
                            .requestItems(requestItems)
                            .build();

                    BatchWriteItemResponse response = dynamoDbClient.batchWriteItem(batchWriteItemRequest);
                    handleUnprocessedItems(response);
                }
            }
        } catch (Exception e) {
            System.err.println("Error during UPDATE operation: " + e.getMessage());
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
        final int DYNAMO_BATCH_LIMIT = 25; // Limit DynamoDB dla BatchWriteItem

        try {
            List<String> kontroleToDelete = new ArrayList<>(cachedKontroleIds);
            if (kontroleToDelete.isEmpty()) {
                throw new IllegalStateException("No kontrole to delete");
            }

            for (int i = 0; i < Math.min(numberOfDeletes, kontroleToDelete.size()); i += DYNAMO_BATCH_LIMIT) {
                List<WriteRequest> writeRequests = new ArrayList<>();
                int currentBatchSize = Math.min(DYNAMO_BATCH_LIMIT, Math.min(numberOfDeletes - i, kontroleToDelete.size() - i));

                for (int j = 0; j < currentBatchSize; j++) {
                    String kontrolaId = kontroleToDelete.get(i + j);
                    writeRequests.add(WriteRequest.builder()
                            .deleteRequest(DeleteRequest.builder()
                                    .key(Collections.singletonMap("id", AttributeValue.builder().s(kontrolaId).build()))
                                    .build())
                            .build());
                }

                if (!writeRequests.isEmpty()) {
                    Map<String, List<WriteRequest>> requestItems = new HashMap<>();
                    requestItems.put("kontrole_graniczne", writeRequests);

                    BatchWriteItemRequest batchWriteItemRequest = BatchWriteItemRequest.builder()
                            .requestItems(requestItems)
                            .build();

                    BatchWriteItemResponse response = dynamoDbClient.batchWriteItem(batchWriteItemRequest);
                    handleUnprocessedItems(response);
                }
            }
        } catch (Exception e) {
            System.err.println("Error during DELETE operation: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        System.out.println("Benchmark operacji DELETE zakończony. Czas wykonania: " + executionTime + " ms");
        return executionTime;
    }

    private void handleUnprocessedItems(BatchWriteItemResponse response) {
        Map<String, List<WriteRequest>> unprocessedItems = response.unprocessedItems();
        int retries = 0;
        int maxRetries = 10;

        while (!unprocessedItems.isEmpty() && retries < maxRetries) {
            try {
                Thread.sleep((long) (Math.pow(2, retries) * 50)); // Exponential backoff
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }

            BatchWriteItemResponse retryResponse = dynamoDbClient.batchWriteItem(
                    BatchWriteItemRequest.builder().requestItems(unprocessedItems).build());
            unprocessedItems = retryResponse.unprocessedItems();
            retries++;
        }

        if (!unprocessedItems.isEmpty()) {
            throw new RuntimeException("Could not process all items after " + maxRetries + " retries");
        }
    }

    private Map<String, AttributeValue> createKontrolaItem() {
        String id = UUID.randomUUID().toString();
        String przejscieNazwa = new ArrayList<>(cachedPrzejsciaIds.keySet()).get(random.nextInt(cachedPrzejsciaIds.size()));
        String oddzialNazwa = new ArrayList<>(cachedOddzialyIds.keySet()).get(random.nextInt(cachedOddzialyIds.size()));
        String dokumentNazwa = new ArrayList<>(cachedDokumentyIds.keySet()).get(random.nextInt(cachedDokumentyIds.size()));

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s(id).build());
        item.put("przejscie_id", AttributeValue.builder().s(cachedPrzejsciaIds.get(przejscieNazwa)).build());
        item.put("oddzial_id", AttributeValue.builder().s(cachedOddzialyIds.get(oddzialNazwa)).build());
        item.put("data", AttributeValue.builder().s(LocalDate.now().minusDays(random.nextInt(365)).toString()).build());
        item.put("kto", AttributeValue.builder().s("Funkcjonariusz SG").build());
        item.put("kierunek", AttributeValue.builder().s(directions.get(random.nextInt(directions.size()))).build());
        item.put("osoba_id", AttributeValue.builder().s(cachedOsobyIds.get(random.nextInt(cachedOsobyIds.size()))).build());
        item.put("dokument_id", AttributeValue.builder().s(cachedDokumentyIds.get(dokumentNazwa)).build());

        return item;
    }

    private Map<String, AttributeValue> createUpdateKontrolaItem(String id) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s(id).build());
        item.put("kierunek", AttributeValue.builder().s(directions.get(random.nextInt(directions.size()))).build());
        return item;
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
            batchSelectTime = benchmarkBatchFindKontroleGraniczne(numberOfQueries, batchSize);
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
        System.out.println("Find: " + batchSelectTime + "ms");
        System.out.println("Update: " + batchUpdateTime + "ms");
        System.out.println("Delete: " + batchDeleteTime + "ms");
    }

    public void close() {
        if (dynamoDbClient != null) {
            dynamoDbClient.close();
            System.out.println("Połączenie z bazą danych zostało zamknięte.");
        }
    }
}