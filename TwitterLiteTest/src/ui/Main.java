package ui;

import service.port.MySnsPort;
import service.port.SnsPort;

import java.sql.Connection;
import java.sql.DriverManager;

public class Main {
    public static void main(String[] args) {
        try {
            // 1️⃣ DB 연결
            Connection conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/sns?serverTimezone=Asia/Seoul",
                    "root",
                    "12345"
            );

            // 2️⃣ MySnsPort 생성
            SnsPort port = new MySnsPort(conn);

            // 3️⃣ 사용자 ID/이름 (임시로 1번 유저)
            int myUserId = 1;
            String myName = "Alice";

            // 4️⃣ HomeFrame 실행
            HomeFrame home = new HomeFrame(port, myUserId, myName);
            home.setVisible(true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
