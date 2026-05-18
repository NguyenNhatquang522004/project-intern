import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class TestDb {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/camunda";
        String user = "postgres";
        String password = "password";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("Connected to DB!");
            Statement stmt = conn.createStatement();
            
            ResultSet rs = stmt.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema='public'");
            List<String> tables = new ArrayList<>();
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
            
            if (tables.isEmpty()) {
                System.out.println("No tables found in the database.");
            } else {
                System.out.println("Tables found: " + tables);
                for (String table : tables) {
                    try {
                        ResultSet countRs = conn.createStatement().executeQuery("SELECT count(*) FROM \"" + table + "\"");
                        if (countRs.next()) {
                            System.out.println("Table " + table + " has " + countRs.getInt(1) + " rows.");
                        }
                    } catch (Exception e) {
                        System.out.println("Could not count rows for table: " + table);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
