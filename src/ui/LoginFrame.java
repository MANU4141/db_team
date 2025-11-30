package ui;

import service.port.SnsPort;
import ui.theme.RoundedButton;
import ui.theme.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.lang.reflect.Method; // 리플렉션을 사용하여 동적으로 메서드 존재 여부를 확인

/** 로그인 화면 + 회원가입 버튼(포트에 register 없으면 안내) */
public class LoginFrame extends JFrame { // Swing 기반의 최상위 윈도우

    private final SnsPort port; // ★ 핵심: SnsPort 인터페이스에 의존하여 기능을 호출
    private final JTextField usernameField = new JTextField(); // 사용자 ID/이메일 입력 필드
    private final JPasswordField pwField = new JPasswordField(); // 비밀번호 입력 필드

    public LoginFrame(SnsPort port) {
        super("로그인");
        this.port = port; // 외부에서 주입된 SnsPort 구현체 (MySnsPort 또는 MemorySnsPort 등)

        // 윈도우 기본 설정
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(420, 360);
        setLocationRelativeTo(null); // 화면 중앙에 배치
        getContentPane().setBackground(Theme.WHITE);
        setLayout(new BorderLayout()); // 전체 레이아웃 설정

        // 1. 상단 헤더 추가
        var header = Theme.header("SNS 로그인");
        add(header, BorderLayout.NORTH);

        // 2. 입력 폼 영역 (중앙)
        var form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(new EmptyBorder(16, 16, 16, 16));
        var c = new GridBagConstraints(); // 레이아웃 제약조건 설정
        c.insets = new Insets(8, 8, 8, 8);
        c.fill = GridBagConstraints.HORIZONTAL;

        // 사용자명/이메일 레이블 및 필드 추가
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        form.add(new JLabel("사용자명 / 이메일"), c);
        c.gridx = 1;
        c.weightx = 1;
        decorate(usernameField);
        form.add(usernameField, c);

        // 비밀번호 레이블 및 필드 추가
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0;
        form.add(new JLabel("비밀번호"), c);
        c.gridx = 1;
        c.weightx = 1;
        decorate(pwField);
        form.add(pwField, c);

        add(form, BorderLayout.CENTER);

        // 3. 하단 버튼 영역 (SOUTH)
        var bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        bottom.setOpaque(false);
        var signupBtn = new RoundedButton("회원가입", true);
        var loginBtn = new RoundedButton("로그인");
        bottom.add(signupBtn);
        bottom.add(loginBtn);
        add(bottom, BorderLayout.SOUTH);

        // 엔터 키 입력 시 로그인 버튼 작동하도록 설정
        getRootPane().setDefaultButton(loginBtn);
        // 버튼 리스너 등록
        loginBtn.addActionListener(e -> doLogin());
        signupBtn.addActionListener(e -> openSignup());
    }

    // 텍스트 필드 등의 컴포넌트 스타일링을 위한 헬퍼 메서드
    private void decorate(JComponent c) {
        c.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.GRAY_200),
                new EmptyBorder(8, 10, 8, 10)));
        c.setFont(Theme.fontRegular(13));
    }

    /** 사용자가 '로그인' 버튼을 눌렀을 때 실행되는 핵심 로직 */
    private void doLogin() {
        String id = usernameField.getText().trim();
        String pw = new String(pwField.getPassword());
        if (id.isEmpty() || pw.isEmpty()) {
            JOptionPane.showMessageDialog(this, "아이디(또는 이메일)와 비밀번호를 입력하세요.");
            return;
        }
        try {
            SnsPort.UserView user = null;

            // ★ 중요 로직: 동적 기능 확인 (리플렉션 사용)
            // 1. ID에 '@'가 포함되어 있으면 loginByEmail 시도 (확장성 고려)
            if (id.contains("@")) {
                try {
                    // 구현체에 loginByEmail(String, String) 메서드가 있는지 확인
                    Method m = port.getClass().getMethod("loginByEmail", String.class, String.class);
                    // 메서드가 있다면 리플렉션으로 호출 (없으면 NoSuchMethodException 발생)
                    user = (SnsPort.UserView) m.invoke(port, id, pw);
                } catch (NoSuchMethodException ignore) {
                    // loginByEmail이 구현 안 되어 있으면 무시하고 username 로그인으로 폴백
                }
            }
            // 2. loginByEmail 시도가 없었거나(null) 실패하면 port.login(ID/PW) 시도
            if (user == null)
                user = port.login(id, pw);

            // 로그인 결과 처리
            if (user == null) {
                JOptionPane.showMessageDialog(this, "로그인 실패: 아이디/이메일 또는 비밀번호 확인");
                return;
            }
            // 3. 로그인 성공: 다음 화면(HomeFrame)을 띄우고 현재 창 닫기
            new HomeFrame(port, user.userId(), user.userName()).setVisible(true);
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "로그인 오류: " + ex.getMessage());
        }
    }

    /** '회원가입' 버튼을 눌렀을 때 실행되는 로직 */
    private void openSignup() {
        // 회원가입 다이얼로그를 띄우고, 가입이 완료되면 registerIfSupported 메서드를 호출하도록 콜백 등록
        new SignupDialog(this).onSubmit((u, em, p) -> registerIfSupported(u, em, p)).setVisible(true);
    }

    /** SnsPort 구현체의 지원 여부를 확인하여 회원가입 메서드 호출 */
    private void registerIfSupported(String username, String email, String password) {
        try {
            // ★ 중요 로직: 동적 기능 확인 및 호환성 처리 (리플렉션 사용)

            // 1) 최신 규격 (3-인자: username, email, password) 지원 확인
            try {
                Method m3 = port.getClass().getMethod("register", String.class, String.class, String.class);
                Object ret = m3.invoke(port, username, email, password);
                handleRegisterResult(username, ret);
                return;
            } catch (NoSuchMethodException ignore) {
                // 3-인자 메서드가 없으면 2)로 이동
            }

            // 2) 레거시 규격 (2-인자: username, password) 지원 확인
            try {
                Method m2 = port.getClass().getMethod("register", String.class, String.class);
                Object ret = m2.invoke(port, username, password);
                handleRegisterResult(username, ret);
                return;
            } catch (NoSuchMethodException ignore2) {
                // 2-인자 메서드도 없으면 회원가입 API가 없음을 사용자에게 안내
                JOptionPane.showMessageDialog(this,
                        "현재 포트 구현에는 회원가입 API가 없습니다.\n관리자에게 문의하거나 포트에 register 메서드를 추가해 주세요.");
            } catch (Exception ex2) {
                JOptionPane.showMessageDialog(this, "가입 중 오류: " + ex2.getMessage());
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "가입 중 오류: " + ex.getMessage());
        }
    }

    /** 회원가입 결과에 따라 성공/실패 메시지를 표시하고 로그인 화면을 정리하는 헬퍼 메서드 */
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