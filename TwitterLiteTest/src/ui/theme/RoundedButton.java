package ui.theme;

import java.awt.*;
import javax.swing.*;

public class RoundedButton extends JButton {
    private final boolean outlined;

    public RoundedButton(String text){ this(text, false); }
    public RoundedButton(String text, boolean outlined){
        super(text);
        this.outlined = outlined;
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(Theme.fontBold(13));
        setForeground(Theme.WHITE);
        setMargin(new Insets(10,16,10,16));
        setToolTipText(text);
    }

    @Override protected void paintComponent(Graphics g){
        Graphics2D g2=(Graphics2D)g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w=getWidth(), h=getHeight();

        if (outlined){
            g2.setColor(Theme.WHITE);
            g2.fillRoundRect(0,0,w,h, Theme.RADIUS, Theme.RADIUS);
            g2.setColor(Theme.BLUE);
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(1,1,w-2,h-2, Theme.RADIUS, Theme.RADIUS);
            setForeground(Theme.BLUE);
        }else{
            g2.setColor(getModel().isRollover()? Theme.BLUE_DARK : Theme.BLUE);
            g2.fillRoundRect(0,0,w,h, Theme.RADIUS, Theme.RADIUS);
            setForeground(Theme.WHITE);
        }
        super.paintComponent(g);
        g2.dispose();
    }
    @Override public boolean isOpaque(){ return false; }
}
