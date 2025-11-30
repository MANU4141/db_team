import javax.swing.*;
import service.port.MySnsPort;
import service.port.SnsPort;
import ui.LoginFrame;

import java.sql.Connection;
import java.sql.DriverManager;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // 윈도우 룩앤필 (선택)
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }

            try {
                // ✅ 1️⃣ MySQL DB 연결
                Connection conn = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/sns?serverTimezone=Asia/Seoul", "root", // ← MySQL 아이디
                        "12345" // ← MySQL 비밀번호
                );

                // ✅ 2️⃣ DB 버전 포트(MySnsPort) 사용
                SnsPort port = new MySnsPort(conn);

                // ✅ 3️⃣ 로그인 프레임 실행
                new LoginFrame(port).setVisible(true);

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "DB 연결 실패: " + e.getMessage());
            }
        });
    }
}
