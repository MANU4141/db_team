package ui;

import service.port.SnsPort;
import ui.theme.RoundedButton;
import ui.theme.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.lang.reflect.Method;

/** 로그인 화면 + 회원가입 버튼(포트에 register 없으면 안내) */
public class LoginFrame extends JFrame {

    private final SnsPort port;
    private final JTextField usernameField = new JTextField();
    private final JPasswordField pwField = new JPasswordField();

    public LoginFrame(SnsPort port) {
        super("로그인");
        this.port = port;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(420, 360);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Theme.WHITE);
        setLayout(new BorderLayout());

        var header = Theme.header("SNS 로그인");
        add(header, BorderLayout.NORTH);

        var form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(new EmptyBorder(16,16,16,16));
        var c = new GridBagConstraints();
        c.insets = new Insets(8,8,8,8);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx=0; c.gridy=0; c.weightx=0; form.add(new JLabel("사용자명 / 이메일"), c);
        c.gridx=1; c.weightx=1; decorate(usernameField); form.add(usernameField, c);

        c.gridx=0; c.gridy=1; c.weightx=0; form.add(new JLabel("비밀번호"), c);
        c.gridx=1; c.weightx=1; decorate(pwField); form.add(pwField, c);

        add(form, BorderLayout.CENTER);

        var bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        bottom.setOpaque(false);
        var signupBtn = new RoundedButton("회원가입", true);
        var loginBtn  = new RoundedButton("로그인");
        bottom.add(signupBtn); bottom.add(loginBtn);
        add(bottom, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(loginBtn);
        loginBtn.addActionListener(e -> doLogin());
        signupBtn.addActionListener(e -> openSignup());
    }

    private void decorate(JComponent c){
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.GRAY_200),
                new EmptyBorder(8,10,8,10)
        ));
        c.setFont(Theme.fontRegular(13));
    }

    private void doLogin() {
        String id = usernameField.getText().trim();
        String pw = new String(pwField.getPassword());
        if (id.isEmpty() || pw.isEmpty()) {
            JOptionPane.showMessageDialog(this, "아이디(또는 이메일)와 비밀번호를 입력하세요.");
            return;
        }
        try {
            SnsPort.UserView user = null;

            // 이메일 형식으로 보이면 loginByEmail 우선 시도(미구현이면 null)
            if (id.contains("@")) {
                try {
                    Method m = port.getClass().getMethod("loginByEmail", String.class, String.class);
                    user = (SnsPort.UserView) m.invoke(port, id, pw);
                } catch (NoSuchMethodException ignore) {
                    // 구현 안 되어 있으면 아래 username 로그인으로 폴백
                }
            }
            if (user == null) user = port.login(id, pw);

            if (user == null) {
                JOptionPane.showMessageDialog(this, "로그인 실패: 아이디/이메일 또는 비밀번호 확인");
                return;
            }
            new HomeFrame(port, user.userId(), user.userName()).setVisible(true);
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "로그인 오류: " + ex.getMessage());
        }
    }

    private void openSignup() {
        // 이메일 필드가 포함된 SignupDialog(최신본) 사용 가정: onSubmit((u, em, p) -> ...)
        new SignupDialog(this)
            .onSubmit((u, em, p) -> registerIfSupported(u, em, p))
            .setVisible(true);
    }

    /** SnsPort에 register가 있으면 호출(3→2 인자 순), 없으면 안내 */
    private void registerIfSupported(String username, String email, String password) {
        try {
            // 1) (String username, String email, String password)
            Method m3 = port.getClass().getMethod("register", String.class, String.class, String.class);
            Object ret = m3.invoke(port, username, email, password);
            handleRegisterResult(username, ret);
            return;
        } catch (NoSuchMethodException ignore) {
            try {
                // 2) (String username, String password)
                Method m2 = port.getClass().getMethod("register", String.class, String.class);
                Object ret = m2.invoke(port, username, password);
                handleRegisterResult(username, ret);
                return;
            } catch (NoSuchMethodException ignore2) {
                JOptionPane.showMessageDialog(this,
                        "현재 포트 구현에는 회원가입 API가 없습니다.\n관리자에게 문의하거나 포트에 register 메서드를 추가해 주세요.");
            } catch (Exception ex2) {
                JOptionPane.showMessageDialog(this, "가입 중 오류: " + ex2.getMessage());
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "가입 중 오류: " + ex.getMessage());
        }
    }

    private void handleRegisterResult(String username, Object ret) {
        if (ret == null) {
            JOptionPane.showMessageDialog(this, "이미 존재하는 사용자명(또는 이메일)입니다.");
            return;
        }
        JOptionPane.showMessageDialog(this, "가입 완료! 이제 로그인해 주세요.");
        usernameField.setText(username);
        pwField.setText("");
    }
}
