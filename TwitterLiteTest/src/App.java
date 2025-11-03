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
                // 윈도우 룩앤필(선택)
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            try {
                // 1️⃣ DB 연결
                Connection conn = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/sns?serverTimezone=Asia/Seoul",
                        "root",
                        "12345"
                );

                // 2️⃣ MySnsPort 생성
                SnsPort port = new MySnsPort(conn); // 수정됨: FakeSnsAdapter → MySnsPort

                // 3️⃣ LoginFrame 실행
                new LoginFrame(port).setVisible(true);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
