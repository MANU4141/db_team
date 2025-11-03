import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/sns?serverTimezone=Asia/Seoul";
        String user = "root";
        String password = "12345";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("✅ 연결 성공!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
