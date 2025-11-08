package ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import service.port.SnsPort;
import ui.theme.RoundedButton;
import ui.theme.Theme;

public class HomeFrame extends JFrame {
    private final SnsPort port;
    private final int myUserId;
    private final String myName;

    private final PostListPanel listPanel;

    public HomeFrame(SnsPort port, int myUserId, String myName){
        super("홈 · " + myName);
        this.port = port;
        this.myUserId = myUserId;
        this.myName = myName;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 640);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        /* ===== 헤더 ===== */
        var header = Theme.header("SNS 피드");
        var userLabel = new JLabel(myName + " 님");
        userLabel.setFont(Theme.fontRegular(14));
        header.add(userLabel, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        /* ===== 목록 ===== */
        listPanel = new PostListPanel(port, myUserId);
        add(listPanel, BorderLayout.CENTER);

        /* ===== 하단 ===== */
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        var searchField = new JTextField();
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.GRAY_200),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        var searchBtn = new RoundedButton("검색", true);

        JPanel left = new JPanel(new BorderLayout(6, 0));
        left.add(searchField, BorderLayout.CENTER);
        left.add(searchBtn, BorderLayout.EAST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        var refreshBtn = new RoundedButton("새로고침", true);
        var writeBtn   = new RoundedButton("글쓰기");
        right.add(refreshBtn);
        right.add(writeBtn);

        bottom.add(left, BorderLayout.CENTER);
        bottom.add(right, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        /* ===== 이벤트 ===== */
        refreshBtn.addActionListener(e -> listPanel.reload());
        searchBtn.addActionListener(e -> listPanel.search(searchField.getText().trim()));

        // 글쓰기: 파일+텍스트 본문을 함께 저장
        writeBtn.addActionListener(e -> {
            PostWriteDialog dlg = new PostWriteDialog(this);
            dlg.setVisible(true);                // 모달: 닫힐 때까지 대기
            if (!dlg.isApproved()) return;       // 취소 시 종료

            String path = dlg.getFilePath();     // null 가능
            String name = dlg.getFileName();     // null 가능
            String text = dlg.getContent();      // ★ 본문 (null 또는 빈 문자열 가능)

            // SnsPort에 추가한 오버로드 사용
            port.createPost(myUserId, path, name, text);

            listPanel.reload();                  // 새 글 반영
        });
    }
}
