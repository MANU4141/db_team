package ui.theme;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public final class RoundedFields {
    private RoundedFields(){}

    public static JTextField textField(String placeholder){
        JTextField f = new JTextField();
        style(f, placeholder);
        return f;
    }
    public static JPasswordField passwordField(String placeholder){
        JPasswordField f = new JPasswordField();
        style(f, placeholder);
        return f;
    }

    private static void style(JTextField f, String placeholder){
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.GRAY_200, 1, true),
                new EmptyBorder(8,12,8,12)
        ));
        f.setBackground(Theme.WHITE);
        f.setFont(Theme.fontRegular(13));
        f.setToolTipText(placeholder);
    }

    public static JScrollPane scroll(Component comp){
        JScrollPane sp = new JScrollPane(comp);
        sp.setBorder(BorderFactory.createLineBorder(Theme.GRAY_200, 1, true));
        sp.getViewport().setBackground(Theme.WHITE);
        return sp;
    }
}
