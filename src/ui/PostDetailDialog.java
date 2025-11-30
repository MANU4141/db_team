package ui;

import service.port.SnsPort;
import ui.theme.RoundedButton;
import ui.theme.Theme;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;

public class PostDetailDialog extends JDialog {

    private final SnsPort port;
    private final int myUserId;
    private final SnsPort.PostView post;
    private final Runnable onUpdate; // 삭제/수정 시 목록 새로고침을 위한 콜백

    // [수정] 생성자에 port, myUserId, onUpdate 추가
    public PostDetailDialog(Window owner, SnsPort port, int myUserId, SnsPort.PostView post, Runnable onUpdate) {
        super(owner, "게시글 상세", ModalityType.MODELESS);
        this.port = port;
        this.myUserId = myUserId;
        this.post = post;
        this.onUpdate = onUpdate;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(600, 700);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.WHITE);

        // --- 1. 메인 콘텐츠 ---
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(Theme.WHITE);
        contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // 작성자 정보
        JLabel metaLabel = new JLabel("<html><b>" + post.authorName() + "</b> · " + post.uploadTime() + "</html>");
        metaLabel.setFont(Theme.fontRegular(14));
        metaLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(metaLabel);
        contentPanel.add(Box.createVerticalStrut(10));

        // 이미지 로드
        String fullPath = findExistingPath(post.filePath(), post.fileName());
        BufferedImage img = null;
        if (fullPath != null) {
            try {
                img = ImageIO.read(new File(fullPath));
            } catch (Exception ignored) {
            }
        } else if (post.fileName() != null && !post.fileName().isBlank()) {
            img = ui.ImageUtils.placeholderImage(600, 400, "이미지 없음", post.fileName());
        }

        if (img != null) {
            int maxWidth = 540;
            int newW = maxWidth;
            int newH = (int) ((double) img.getHeight() / img.getWidth() * maxWidth);
            Image scaled = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
            JLabel imgLabel = new JLabel(new ImageIcon(scaled));
            imgLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            contentPanel.add(imgLabel);
            contentPanel.add(Box.createVerticalStrut(15));
        }

        // 본문 텍스트
        JTextArea textArea = new JTextArea(post.content());
        textArea.setFont(Theme.fontRegular(15));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);
        textArea.setOpaque(false);
        textArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(textArea);

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        // --- 2. 하단 버튼 영역 ---
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setBackground(Theme.WHITE);

        // ★ [핵심] 내가 쓴 글이면 수정/삭제 버튼 표시
        if (post.authorId() == myUserId) {
            RoundedButton btnEdit = new RoundedButton("수정", true);
            RoundedButton btnDelete = new RoundedButton("삭제", true);

            btnEdit.setForeground(new Color(0, 100, 200)); // 파란색
            btnDelete.setForeground(Color.RED); // 빨간색

            btnEdit.addActionListener(e -> doEdit());
            btnDelete.addActionListener(e -> doDelete());

            bottom.add(btnEdit);
            bottom.add(btnDelete);
        }

        RoundedButton btnClose = new RoundedButton("닫기");
        btnClose.addActionListener(e -> dispose());
        bottom.add(btnClose);
        add(bottom, BorderLayout.SOUTH);

        // ESC 닫기
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    // --- 동작 구현 ---

    private void doDelete() {
        int confirm = JOptionPane.showConfirmDialog(this, "정말로 이 게시글을 삭제하시겠습니까?", "삭제 확인", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            port.deletePost(myUserId, post.postId());
            JOptionPane.showMessageDialog(this, "삭제되었습니다.");
            dispose();
            if (onUpdate != null)
                onUpdate.run(); // 목록 새로고침
        }
    }

    private void doEdit() {
        // 간단하게 입력 다이얼로그로 수정 (더 복잡한 UI 원하면 JDialog 새로 만드셔도 됨)
        String newContent = JOptionPane.showInputDialog(this, "수정할 내용을 입력하세요:", post.content());

        if (newContent != null && !newContent.trim().isEmpty()) {
            port.updatePost(myUserId, post.postId(), newContent.trim());
            JOptionPane.showMessageDialog(this, "수정되었습니다.");
            dispose();
            if (onUpdate != null)
                onUpdate.run(); // 목록 새로고침
        }
    }

    private String findExistingPath(String path, String name) {
        if (name == null || name.isBlank())
            return null;
        java.util.List<String> candidates = new java.util.ArrayList<>();
        if (path != null && !path.isBlank())
            candidates.add(path + File.separator + name);
        candidates.add("./images/" + name);
        candidates.add("./img/" + name);
        candidates.add("./" + name);
        for (String c : candidates) {
            File f = new File(c);
            if (f.exists() && f.isFile())
                return f.getAbsolutePath();
        }
        return null;
    }
}