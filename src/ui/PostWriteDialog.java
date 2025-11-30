package ui;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import ui.theme.RoundedButton;
import ui.theme.Theme;

/**
 * 카드형 UI의 글쓰기 다이얼로그 (수정됨: 파일명 자동 생성 로직 제거)
 */
public class PostWriteDialog extends JDialog {

    private final JTextField tfPath = new JTextField();
    private final JTextField tfName = new JTextField();
    private final JLabel preview = new JLabel("", SwingConstants.CENTER);
    private final JTextArea taContent = new JTextArea();

    private boolean approved = false;

    public PostWriteDialog(Window owner) {
        super(owner, "글쓰기", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(720, 520);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(Theme.WHITE);
        setLayout(new BorderLayout());

        /* ===== 헤더 ===== */
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.WHITE);
        JLabel title = new JLabel("새 게시글");
        title.setFont(Theme.fontBold(18));
        title.setBorder(new EmptyBorder(12, 16, 6, 16));
        header.add(title, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        /* ===== 카드 본문 ===== */
        JPanel card = new JPanel(new GridBagLayout());
        card.setOpaque(true);
        card.setBackground(Theme.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(new LineBorder(Theme.GRAY_200, 1, true),
                new EmptyBorder(16, 16, 16, 16)));
        add(wrap(card, 12, 16, 8, 16), BorderLayout.CENTER);

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8, 10, 8, 10);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;

        // 라벨
        JLabel lPath = label("파일 경로:");
        JLabel lName = label("파일명:");
        JLabel lPick = label("파일 선택:");
        JLabel lContent = label("내용:");

        // 입력 필드
        styleInput(tfPath);
        tfPath.setEditable(false);
        styleInput(tfName);

        // [수정] 여기가 문제였습니다! 자동으로 파일명을 채우는 코드를 삭제했습니다.
        // 이제 파일을 선택하지 않으면 tfName은 비어있게 됩니다.

        // 파일 선택 버튼
        RoundedButton btnPick = new RoundedButton("파일 선택...", true);
        btnPick.setMargin(new Insets(10, 16, 10, 16));
        btnPick.addActionListener(e -> openChooser());

        // 미리보기
        preview.setPreferredSize(new Dimension(220, 220));
        preview.setHorizontalAlignment(SwingConstants.CENTER);
        preview.setVerticalAlignment(SwingConstants.CENTER);
        preview.setOpaque(true);
        preview.setBackground(Theme.WHITE);
        preview.setBorder(new LineBorder(Theme.GRAY_200, 1, true));
        preview.setText("<html><div style='color:#999;'>미리보기</div></html>");

        // ===== 좌측 폼 배치
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 0;
        card.add(lPath, gc);
        gc.gridx = 1;
        gc.gridy = 0;
        gc.weightx = 1;
        card.add(tfPath, gc);
        gc.gridx = 0;
        gc.gridy = 1;
        gc.weightx = 0;
        card.add(lName, gc);
        gc.gridx = 1;
        gc.gridy = 1;
        gc.weightx = 1;
        card.add(tfName, gc);
        gc.gridx = 0;
        gc.gridy = 2;
        gc.weightx = 0;
        card.add(lPick, gc);

        JPanel pickLine = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pickLine.setOpaque(false);
        pickLine.add(btnPick);
        gc.gridx = 1;
        gc.gridy = 2;
        gc.weightx = 1;
        card.add(pickLine, gc);

        // 본문 라벨
        gc.gridx = 0;
        gc.gridy = 3;
        gc.weightx = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        card.add(lContent, gc);

        // 본문 입력
        taContent.setLineWrap(true);
        taContent.setWrapStyleWord(true);
        taContent.setFont(Theme.fontRegular(13));
        taContent.setBorder(BorderFactory.createCompoundBorder(new LineBorder(Theme.GRAY_200, 1, true),
                new EmptyBorder(8, 10, 8, 10)));
        taContent.setRows(8);
        JScrollPane spContent = new JScrollPane(taContent, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        gc.gridx = 1;
        gc.gridy = 3;
        gc.weightx = 1;
        gc.weighty = 1;
        gc.fill = GridBagConstraints.BOTH;
        gc.gridwidth = 1;
        card.add(spContent, gc);

        // ===== 우측 미리보기
        GridBagConstraints pv = new GridBagConstraints();
        pv.gridx = 2;
        pv.gridy = 0;
        pv.gridheight = 4;
        pv.insets = new Insets(0, 24, 0, 0);
        pv.weightx = 0;
        pv.weighty = 0;
        pv.fill = GridBagConstraints.NONE;
        pv.anchor = GridBagConstraints.CENTER;
        card.add(preview, pv);

        /* ===== 하단 버튼 ===== */
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        south.setOpaque(false);
        RoundedButton btnCancel = new RoundedButton("취소", true);
        RoundedButton btnSave = new RoundedButton("저장");
        btnCancel.setMargin(new Insets(10, 16, 10, 16));
        btnSave.setMargin(new Insets(10, 20, 10, 20));
        south.add(btnCancel);
        south.add(btnSave);
        add(wrap(south, 0, 16, 12, 16), BorderLayout.SOUTH);

        // 액션
        btnCancel.addActionListener(e -> {
            approved = false;
            dispose();
        });
        btnSave.addActionListener(e -> {
            approved = true;
            dispose();
        });

        // 단축키
        getRootPane().registerKeyboardAction(e -> btnCancel.doClick(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().registerKeyboardAction(e -> btnSave.doClick(),
                KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().registerKeyboardAction(e -> btnSave.doClick(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().setDefaultButton(btnSave);
    }

    /* ===================== public API ===================== */
    public boolean isApproved() {
        return approved;
    }

    // [수정] 파일 경로가 비어있으면(파일 선택 안함) 파일명도 무조건 null로 리턴
    public String getFilePath() {
        return blankToNull(tfPath.getText());
    }

    public String getFileName() {
        if (blankToNull(tfPath.getText()) == null)
            return null; // 경로 없으면 이름도 무효
        return blankToNull(tfName.getText());
    }

    public String getContent() {
        String s = taContent.getText();
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    /* ===================== helpers ===================== */
    private void openChooser() {
        if (GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(this, "headless 모드에서는 파일 선택 창을 열 수 없습니다.");
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("이미지 선택");
        int r = fc.showOpenDialog(this);
        if (r == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            tfPath.setText(f.getParent());
            tfName.setText(f.getName());
            loadPreview(new File(tfPath.getText(), tfName.getText()).getAbsolutePath());
        }
    }

    private void loadPreview(String fullPath) {
        try {
            BufferedImage src = ImageIO.read(new File(fullPath));
            if (src == null)
                throw new Exception("not image");

            int boxW = preview.getPreferredSize().width;
            int boxH = preview.getPreferredSize().height;
            double scale = Math.min((double) boxW / src.getWidth(), (double) boxH / src.getHeight());
            int w = Math.max(1, (int) Math.round(src.getWidth() * scale));
            int h = Math.max(1, (int) Math.round(src.getHeight() * scale));

            Image scaled = src.getScaledInstance(w, h, Image.SCALE_SMOOTH);
            preview.setIcon(new ImageIcon(scaled));
            preview.setText(null);
        } catch (Exception ex) {
            preview.setIcon(null);
            preview.setText("<html><div style='color:#c00;'>이미지를 불러올 수 없음</div></html>");
        }
    }

    private static JPanel wrap(JComponent c, int t, int l, int b, int r) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(t, l, b, r));
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    private static JLabel label(String s) {
        JLabel l = new JLabel(s);
        l.setFont(Theme.fontRegular(13));
        return l;
    }

    private static void styleInput(JTextField tf) {
        tf.setFont(Theme.fontRegular(13));
        tf.setBackground(Color.WHITE);
        tf.setBorder(BorderFactory.createCompoundBorder(new LineBorder(Theme.GRAY_200, 1, true),
                new EmptyBorder(8, 10, 8, 10)));
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}