package ui;

import ui.theme.RoundedButton;
import ui.theme.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.function.Consumer;

/** 사용자명 + 이메일 + 비밀번호 회원가입 폼 (포트 의존 X, 콜백으로 결과 전달) */
public class SignupDialog extends JDialog {
    private final JTextField username = new JTextField();
    private final JTextField email = new JTextField();
    private final JPasswordField pw1  = new JPasswordField();
    private final JPasswordField pw2  = new JPasswordField();

    /** (username, email, password) -> 성공 시 호출 */
    private TriConsumer<String,String,String> onSubmit = (u, em, p) -> {};

    public SignupDialog(Window owner) {
        super(owner, "회원가입", ModalityType.APPLICATION_MODAL);
        setSize(420, 380);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.WHITE);

        var form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(new EmptyBorder(16,16,16,16));
        var c = new GridBagConstraints();
        c.insets = new Insets(8,8,8,8);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx=0; c.gridy=0; c.weightx=0; form.add(new JLabel("사용자명"), c);
        c.gridx=1; c.weightx=1; decorate(username); form.add(username, c);

        c.gridx=0; c.gridy=1; c.weightx=0; form.add(new JLabel("이메일"), c);
        c.gridx=1; c.weightx=1; decorate(email); form.add(email, c);

        c.gridx=0; c.gridy=2; c.weightx=0; form.add(new JLabel("비밀번호"), c);
        c.gridx=1; c.weightx=1; decorate(pw1); form.add(pw1, c);

        c.gridx=0; c.gridy=3; c.weightx=0; form.add(new JLabel("비밀번호 확인"), c);
        c.gridx=1; c.weightx=1; decorate(pw2); form.add(pw2, c);

        add(form, BorderLayout.CENTER);

        var south = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,8));
        south.setOpaque(false);
        var cancel = new RoundedButton("취소", true);
        var submit = new RoundedButton("가입하기");
        south.add(cancel); south.add(submit);
        add(south, BorderLayout.SOUTH);

        cancel.addActionListener(e -> dispose());
        submit.addActionListener(e -> submit());
        getRootPane().setDefaultButton(submit);
    }

    private void decorate(JComponent comp){
        comp.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.GRAY_200),
                new EmptyBorder(8,10,8,10)
        ));
        comp.setFont(Theme.fontRegular(13));
    }

    private void submit(){
        var u = username.getText().trim();
        var em = email.getText().trim();
        var p1 = new String(pw1.getPassword());
        var p2 = new String(pw2.getPassword());

        if(u.isEmpty() || em.isEmpty() || p1.isEmpty() || p2.isEmpty()){
            JOptionPane.showMessageDialog(this, "모든 칸을 입력해 주세요.");
            return;
        }
        if(!p1.equals(p2)){
            JOptionPane.showMessageDialog(this, "비밀번호가 일치하지 않습니다.");
            return;
        }
        if(p1.length() < 4){
            JOptionPane.showMessageDialog(this, "비밀번호는 4자 이상으로 설정해 주세요.");
            return;
        }
        onSubmit.accept(u, em, p1);
        dispose();
    }

    /** 가입 버튼 눌렀을 때 실행할 처리 지정 */
    public SignupDialog onSubmit(TriConsumer<String,String,String> cb){
        this.onSubmit = (cb==null) ? (u, em, p) -> {} : cb;
        return this;
    }

    /** 3인자 람다용 functional interface */
    @FunctionalInterface
    public interface TriConsumer<A,B,C> {
        void accept(A a, B b, C c);
    }
}
