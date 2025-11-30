package ui;

import ui.theme.RoundedButton;
import ui.theme.Theme;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;

/** 줌/패닝 가능한 이미지 뷰어 (파일경로/BufferedImage 모두 지원) */
public class ImageViewerDialog extends JDialog {
    private final String fullPath; // 파일일 때만 사용
    private final String displayTitle; // 상단 제목
    private BufferedImage original; // 원본 이미지
    private final JLabel view = new JLabel("", SwingConstants.CENTER);
    private final JScrollPane scroll = new JScrollPane(view);

    private double zoom = 1.0;
    private final double MIN_ZOOM = 0.1, MAX_ZOOM = 8.0, STEP = 1.25;
    private boolean fitMode = true;

    /* 파일 경로 버전 */
    public ImageViewerDialog(Window owner, String fullPath, String titleText) {
        super(owner, titleText == null ? "이미지 미리보기" : titleText, ModalityType.APPLICATION_MODAL);
        this.fullPath = fullPath;
        this.displayTitle = titleText == null ? new File(fullPath).getName() : titleText;
        commonInit();
        loadFromPath();
    }

    /* BufferedImage 직접 전달 버전 */
    public ImageViewerDialog(Window owner, BufferedImage image, String titleText) {
        super(owner, titleText == null ? "이미지 미리보기" : titleText, ModalityType.APPLICATION_MODAL);
        this.fullPath = null;
        this.displayTitle = titleText == null ? "미리보기" : titleText;
        commonInit();
        this.original = image;
        if (original == null) {
            view.setIcon(null);
            view.setText("표시할 이미지가 없습니다.");
            fitMode = false;
            zoom = 1.0;
        } else {
            fitToWindow();
        }
    }

    private void commonInit() {
        setSize(900, 680);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.WHITE);

        add(buildToolbar(), BorderLayout.NORTH);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.WHITE);
        add(scroll, BorderLayout.CENTER);

        // 리사이즈 대응
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (fitMode)
                    fitToWindow();
                else
                    renderZoom();
            }
        });

        // Ctrl + 휠 줌
        scroll.addMouseWheelListener(e -> {
            if (e.isControlDown()) {
                if (e.getWheelRotation() < 0)
                    zoomIn();
                else
                    zoomOut();
                e.consume();
            }
        });

        // 드래그 패닝
        final Point[] start = { null };
        view.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        view.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                start[0] = e.getPoint();
                view.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                view.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            }
        });
        view.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (start[0] == null)
                    return;
                JViewport vp = scroll.getViewport();
                Point p = vp.getViewPosition();
                int dx = e.getX() - start[0].x, dy = e.getY() - start[0].y;
                p.translate(-dx, -dy);
                if (p.x < 0)
                    p.x = 0;
                if (p.y < 0)
                    p.y = 0;
                if (p.x + vp.getWidth() > view.getWidth())
                    p.x = Math.max(0, view.getWidth() - vp.getWidth());
                if (p.y + vp.getHeight() > view.getHeight())
                    p.y = Math.max(0, view.getHeight() - vp.getHeight());
                vp.setViewPosition(p);
            }
        });

        // 단축키
        getRootPane().registerKeyboardAction(e -> zoomIn(),
                KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().registerKeyboardAction(e -> zoomOut(),
                KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().registerKeyboardAction(e -> zoom100(),
                KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.CTRL_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().registerKeyboardAction(e -> toggleFit(), KeyStroke.getKeyStroke(KeyEvent.VK_F, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private JComponent buildToolbar() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.WHITE);

        var title = new JLabel(
                displayTitle == null ? (fullPath == null ? "미리보기" : new File(fullPath).getName()) : displayTitle);
        title.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        p.add(title, BorderLayout.WEST);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        btns.setOpaque(false);
        RoundedButton btnFit = new RoundedButton("창에 맞추기(F)", true);
        RoundedButton btn100 = new RoundedButton("100%(Ctrl+0)", true);
        RoundedButton btnOut = new RoundedButton("－ (Ctrl+-)", true);
        RoundedButton btnIn = new RoundedButton("＋ (Ctrl+=)", true);
        btns.add(btnFit);
        btns.add(btn100);
        btns.add(btnOut);
        btns.add(btnIn);
        p.add(btns, BorderLayout.EAST);

        btnFit.addActionListener(e -> toggleFit());
        btn100.addActionListener(e -> zoom100());
        btnOut.addActionListener(e -> zoomOut());
        btnIn.addActionListener(e -> zoomIn());

        p.add(new JSeparator(SwingConstants.HORIZONTAL), BorderLayout.SOUTH);
        return p;
    }

    private void loadFromPath() {
        try {
            original = ImageIO.read(new File(fullPath));
        } catch (Exception e) {
            original = null;
        }
        if (original == null) {
            view.setIcon(null);
            view.setText("이미지를 불러올 수 없습니다.\n" + fullPath);
            fitMode = false;
            zoom = 1.0;
            return;
        }
        fitToWindow();
    }

    private void renderZoom() {
        if (original == null)
            return;
        int w = (int) Math.max(1, Math.round(original.getWidth() * zoom));
        int h = (int) Math.max(1, Math.round(original.getHeight() * zoom));
        Image scaled = original.getScaledInstance(w, h, Image.SCALE_SMOOTH);
        view.setIcon(new ImageIcon(scaled));
        view.setText(null);
        view.setPreferredSize(new Dimension(w, h));
        view.revalidate();
    }

    private void fitToWindow() {
        if (original == null)
            return;
        Dimension vp = scroll.getViewport().getExtentSize();
        int availW = Math.max(100, vp.width - 16);
        int availH = Math.max(100, vp.height - 16);
        double rw = (double) availW / original.getWidth();
        double rh = (double) availH / original.getHeight();
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, Math.min(rw, rh)));
        fitMode = true;
        renderZoom();
    }

    private void zoomIn() {
        fitMode = false;
        zoom = Math.min(MAX_ZOOM, zoom * STEP);
        renderZoom();
    }

    private void zoomOut() {
        fitMode = false;
        zoom = Math.max(MIN_ZOOM, zoom / STEP);
        renderZoom();
    }

    private void zoom100() {
        fitMode = false;
        zoom = 1.0;
        renderZoom();
        SwingUtilities.invokeLater(() -> {
            JViewport vp = scroll.getViewport();
            int x = (view.getWidth() - vp.getWidth()) / 2, y = (view.getHeight() - vp.getHeight()) / 2;
            vp.setViewPosition(new Point(Math.max(0, x), Math.max(0, y)));
        });
    }

    private void toggleFit() {
        if (fitMode)
            zoom100();
        else
            fitToWindow();
    }
}
