public class Main {
    public static void main(String[] args) {
        String database = System.getenv("DATABASE");
        String username = System.getenv("USER");
        String password = System.getenv("PASSWORD");
        int numberOfTests = 1_000_000;

        try {
            PostgresBenchmark benchmark = new PostgresBenchmark(database, username, password);
            int batchSize = 1_000;
            int initialControls = numberOfTests;
            int batchInserts = numberOfTests;
            int updateOperations = numberOfTests;
            int batchUpdates = numberOfTests;

            benchmark.runBenchmark(
                    batchSize,
                    initialControls,
                    batchInserts,
                    updateOperations,
                    batchUpdates
            );

            benchmark.close();
        } catch (Exception e) {
            System.err.println("Wystąpił błąd podczas benchmarku: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
