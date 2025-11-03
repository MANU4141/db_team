import javax.swing.*;
import service.fake.FakeSnsAdapter;
import service.port.SnsPort;
import ui.LoginFrame;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // 윈도우 룩앤필(선택)
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            SnsPort port = new FakeSnsAdapter();
            new LoginFrame(port).setVisible(true);
        });
    }
}
