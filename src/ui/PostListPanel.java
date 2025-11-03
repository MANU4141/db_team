package ui;

import service.port.SnsPort;
import ui.theme.RoundedButton;
import ui.theme.Theme;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * 피드 목록 패널
 * - 어떤 작성자의 글이든 더블클릭/Enter/우클릭/버튼/작성자/파일명 클릭으로 열기
 * - 댓글 버튼/우클릭 메뉴로 댓글 창 열기
 * - 좋아요/싫어요 토글
 * - 검색/새로고침은 HomeFrame에서 호출
 */
public class PostListPanel extends JPanel {

    private final SnsPort port;
    private final int myUserId;

    private final JTable table;
    private final PostTableModel model;
    private final TableRowSorter<PostTableModel> sorter;

    public PostListPanel(SnsPort port, int myUserId) {
        this.port = port;
        this.myUserId = myUserId;

        setLayout(new BorderLayout(8, 8));
        setBackground(Theme.WHITE);

        // 상단 액션 바
        JPanel action = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        action.setOpaque(false);
        var previewBtn = new RoundedButton("미리보기", true);
        var commentBtn = new RoundedButton("댓글…", true);   // ➜ 추가
        var dislikeBtn = new RoundedButton("싫어요", true);
        var likeBtn    = new RoundedButton("좋아요");
        action.add(previewBtn);
        action.add(commentBtn);                                // ➜ 추가
        action.add(dislikeBtn);
        action.add(likeBtn);
        action.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        add(action, BorderLayout.NORTH);

        model = new PostTableModel();
        table = new JTable(model);
        styleTable(table);

        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        sorter.setSortKeys(Arrays.asList(new RowSorter.SortKey(0, SortOrder.ASCENDING))); // ID 오름차순

        JScrollPane sp = new JScrollPane(table);
        add(sp, BorderLayout.CENTER);

        // 우클릭 메뉴
        JPopupMenu menu = new JPopupMenu();
        JMenuItem openItem = new JMenuItem("열기");
        JMenuItem commentItem = new JMenuItem("댓글…");
        menu.add(openItem);
        menu.add(commentItem);
        table.setComponentPopupMenu(menu);
        sp.getViewport().setComponentPopupMenu(menu);

        // ----- 행 선택 고정 -----
        MouseAdapter rowSelector = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0) table.setRowSelectionInterval(row, row);
                table.requestFocusInWindow();
            }
        };
        table.addMouseListener(rowSelector);
        sp.getViewport().addMouseListener(rowSelector);

        // 단일 클릭: 작성자/파일명 클릭 시 열기
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseReleased(MouseEvent e) {
                int r = table.rowAtPoint(e.getPoint());
                int c = table.columnAtPoint(e.getPoint());
                if (r >= 0 && (c == 1 || c == 2)) {
                    openPreview();
                }
            }
        });

        // 더블클릭 → 열기
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) openPreview();
            }
        });

        // Enter → 열기
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "open");
        table.getActionMap().put("open", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { openPreview(); }
        });

        // 버튼/메뉴 액션
        previewBtn.addActionListener(e -> openPreview());
        commentBtn.addActionListener(e -> openComments());     // ➜ 추가
        likeBtn.addActionListener(e -> toggle("LIKE"));
        dislikeBtn.addActionListener(e -> toggle("DISLIKE"));
        openItem.addActionListener(e -> openPreview());
        commentItem.addActionListener(e -> openComments());

        // 초기 로드
        reload();
    }

    /* ========== 외부에서 호출 ========== */

    public void reload() { model.setRows(port.listRecent(myUserId, 100)); }
    public void search(String keyword) { model.setRows(port.search(myUserId, keyword, 100)); }

    /* ========== 내부 동작 ========== */

    private int ensureSelected() {
        int vr = table.getSelectedRow();
        if (vr == -1 && table.getRowCount() > 0) {
            table.setRowSelectionInterval(0, 0);
            vr = 0;
        }
        if (vr == -1) {
            JOptionPane.showMessageDialog(this, "표시할 게시물이 없습니다.");
        }
        return vr;
    }

    private void toggle(String type) {
        int vr = ensureSelected(); if (vr < 0) return;
        int mr = table.convertRowIndexToModel(vr);
        var p = model.getAt(mr);
        var state = port.toggleReaction(myUserId, p.postId(), type);
        reload();
        if (vr < table.getRowCount()) table.setRowSelectionInterval(vr, vr);
        JOptionPane.showMessageDialog(this, "현재 내 상태: " + state);
    }

    /** 어떤 작성자의 글이든 미리보기 (파일이 없어도 반드시 뜨게) */
    private void openPreview() {
        int vr = ensureSelected(); if (vr < 0) return;
        int mr = table.convertRowIndexToModel(vr);
        var p = model.getAt(mr);

        // 파일 유무 판단
        boolean noAttach = (isBlank(p.filePath()) && isBlank(p.fileName()));

        // 경로 후보에서 실제 파일 찾기
        String full = findExistingPath(p.filePath(), p.fileName());

        if (noAttach && full == null) {
            // 완전 텍스트 글이면 댓글 창으로 유도
            int choice = JOptionPane.showConfirmDialog(this,
                    "이 게시물에는 첨부 이미지가 없습니다.\n댓글 창을 여시겠어요?",
                    "미리보기", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) openComments();
            return;
        }

        if (full == null) {
            // 파일명은 있으나 실제 파일을 못 찾으면 플레이스홀더로라도 반드시 보여준다
            var ph = ui.image.ImageUtils.placeholderImage(
                    800, 600,
                    p.authorName() + " 의 게시글",
                    isBlank(p.fileName()) ? "(첨부 없음)" : p.fileName()
            );
            new ImageViewerDialog(SwingUtilities.getWindowAncestor(this), ph,
                    p.authorName() + " · " + (isBlank(p.fileName()) ? "no-file" : p.fileName()))
                    .setVisible(true);
            return;
        }

        // 실제 파일 열기
        new ImageViewerDialog(SwingUtilities.getWindowAncestor(this), full,
                p.authorName() + " · " + p.fileName()).setVisible(true);
    }

    /** 댓글 창 열기 (작성자와 무관) */
    private void openComments() {
        int vr = ensureSelected(); if (vr < 0) return;
        int mr = table.convertRowIndexToModel(vr);
        var p = model.getAt(mr);

        new CommentDialog(SwingUtilities.getWindowAncestor(this),
                port, myUserId, p.postId(), p.authorName(), p.fileName())
                .setVisible(true);
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }

    /** filePath/fileName 조합 + 관용 폴더에서 실제 존재 파일을 찾는다 */
    private String findExistingPath(String path, String name) {
        java.util.List<String> candidates = new java.util.ArrayList<>();
        if (!isBlank(path) && !isBlank(name)) candidates.add(path + File.separator + name);
        if (!isBlank(path) && isBlank(name))  candidates.add(path);
        if (isBlank(path) && !isBlank(name)) {
            candidates.add("./images/" + name);
            candidates.add("./img/" + name);
            candidates.add("./assets/" + name);
            candidates.add("./" + name);
        }
        for (String c : candidates) {
            File f = new File(c);
            if (f.exists() && f.isFile()) return f.getAbsolutePath();
        }
        return null;
    }

    private static void styleTable(JTable t) {
        t.setRowHeight(32);
        t.setFillsViewportHeight(true);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setDefaultEditor(Object.class, null);
        t.getTableHeader().setFont(Theme.fontBold(13));
        ((DefaultTableCellRenderer) t.getTableHeader()
                .getDefaultRenderer()).setHorizontalAlignment(SwingConstants.CENTER);

        var center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        t.getColumnModel().getColumn(0).setCellRenderer(center); // ID
        t.getColumnModel().getColumn(4).setCellRenderer(center); // 좋아요
        t.getColumnModel().getColumn(5).setCellRenderer(center); // 싫어요
        t.getColumnModel().getColumn(6).setCellRenderer(center); // 내상태
    }

    /* ===== TableModel ===== */

    static class PostTableModel extends AbstractTableModel {
        private final String[] cols = {"ID", "작성자", "파일명", "업로드", "좋아요", "싫어요", "내상태"};
        private List<SnsPort.PostView> rows = List.of();

        public void setRows(List<SnsPort.PostView> list) { rows = list; fireTableDataChanged(); }
        public SnsPort.PostView getAt(int r) { return rows.get(r); }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }

        @Override public Object getValueAt(int r, int c) {
            var p = rows.get(r);
            return switch (c) {
                case 0 -> p.postId();
                case 1 -> p.authorName();
                case 2 -> p.fileName();
                case 3 -> p.uploadTime();
                case 4 -> p.likeCount();
                case 5 -> p.dislikeCount();
                case 6 -> p.myState();
                default -> "";
            };
        }
    }
}
