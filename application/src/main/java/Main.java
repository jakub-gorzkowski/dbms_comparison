public class Main {
    public static void main(String[] args) {
        String database = System.getenv("DATABASE");
        String username = System.getenv("USER");
        String password = System.getenv("PASSWORD");
        int numberOfTests = 1_000_000;
        int batchSize = 1_000;

        try {
            Thread.sleep(20_000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

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

        System.out.println("\n=== MySQL Benchmark ===");
        try {
            MySQLBenchmark mySQLBenchmark = new MySQLBenchmark(database, username, password);
            mySQLBenchmark.runBenchmark(
                    batchSize,
                    numberOfTests,  // initialControls
                    numberOfTests,  // batchUpdates
                    numberOfTests,  // deleteOperations
                    numberOfTests   // queries
            );
            mySQLBenchmark.close();
        } catch (Exception e) {
            System.err.println("Wystąpił błąd podczas benchmarku MySQL: " + e.getMessage());
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
        System.out.println("\n=== DynamoDB Benchmark ===");
        try {
            DynamoBenchmark dynamoBenchmark = new DynamoBenchmark();
            dynamoBenchmark.runBenchmark(
                    batchSize,
                    numberOfTests / 200,  // initialControls
                    numberOfTests / 200,  // batchUpdates
                    numberOfTests / 200,  // deleteOperations
                    numberOfTests / 200   // queries
            );
            dynamoBenchmark.close();
        } catch (Exception e) {
            System.err.println("Wystąpił błąd podczas benchmarku DynamoDB: " + e.getMessage());
            e.printStackTrace();
        }
    }
}