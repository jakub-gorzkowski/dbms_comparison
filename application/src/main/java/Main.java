public class Main {
    public static void main(String[] args) {
        String database = System.getenv("DATABASE");
        String username = System.getenv("USER");
        String password = System.getenv("PASSWORD");
        int numberOfTests = 1_000_000;
        int batchSize = 1_000;

        System.out.println("\n=== PostgreSQL Benchmark ===");
        try {
            PostgresBenchmark postgresBenchmark = new PostgresBenchmark(database, username, password);
            postgresBenchmark.runBenchmark(
                    batchSize,
                    numberOfTests,  // initialControls
                    numberOfTests,  // batchUpdates
                    numberOfTests,  // deleteOperations
                    numberOfTests   // queries
            );
            postgresBenchmark.close();
        } catch (Exception e) {
            System.err.println("Wystąpił błąd podczas benchmarku PostgreSQL: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n=== MongoDB Benchmark ===");
        try {
            MongoBenchmark mongoBenchmark = new MongoBenchmark(database);
            mongoBenchmark.runBenchmark(
                    batchSize,
                    numberOfTests,  // initialControls
                    numberOfTests,  // batchUpdates
                    numberOfTests,  // deleteOperations
                    numberOfTests   // queries
            );
            mongoBenchmark.close();
        } catch (Exception e) {
            System.err.println("Wystąpił błąd podczas benchmarku MongoDB: " + e.getMessage());
            e.printStackTrace();
        }
    }
}