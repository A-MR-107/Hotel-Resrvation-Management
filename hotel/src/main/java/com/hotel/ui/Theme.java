package com.hotel.ui;

import java.awt.Color;
import javax.swing.UIManager;

/** Installs a Metal look and feel and sets colours for Swing's standard components. */
public final class Theme {

    private Theme() {
    }

    public static void install() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
            // Keep the default look and feel.
        }
        Color panel = new Color(0x141414);
        Color field = new Color(0x0B0B0B);
        Color text = new Color(0xF2F2F2);
        Color accent = new Color(0xE4E4E4);
        Color onAccent = new Color(0x0A0B0C);
        Color button = new Color(0x262626);
        Color line = new Color(0x3A3A3A);
        Object[][] keys = {
                {"Panel.background", panel}, {"Panel.foreground", text},
                {"control", panel}, {"controlText", text},
                {"controlShadow", line}, {"controlHighlight", line}, {"controlDkShadow", new Color(0x000000)},
                {"Label.foreground", text},
                {"Button.background", button}, {"Button.foreground", text}, {"Button.select", new Color(0x4A4A4A)},
                {"TextField.background", field}, {"TextField.foreground", text},
                {"TextField.caretForeground", text}, {"TextField.selectionBackground", accent},
                {"TextField.selectionForeground", onAccent},
                {"FormattedTextField.background", field}, {"FormattedTextField.foreground", text},
                {"TextArea.background", field}, {"TextArea.foreground", text}, {"TextArea.caretForeground", text},
                {"ComboBox.background", field}, {"ComboBox.foreground", text},
                {"ComboBox.selectionBackground", accent}, {"ComboBox.selectionForeground", onAccent},
                {"ComboBox.buttonBackground", button}, {"ComboBox.buttonHighlight", button},
                {"ComboBox.buttonShadow", panel}, {"ComboBox.buttonDarkShadow", new Color(0x000000)},
                {"List.background", field}, {"List.foreground", text},
                {"List.selectionBackground", accent}, {"List.selectionForeground", onAccent},
                {"Table.background", field}, {"Table.foreground", text}, {"Table.gridColor", line},
                {"Table.selectionBackground", accent}, {"Table.selectionForeground", onAccent},
                {"TableHeader.background", button}, {"TableHeader.foreground", text},
                {"ScrollPane.background", panel}, {"Viewport.background", panel},
                {"ScrollBar.background", panel}, {"ScrollBar.track", field},
                {"ScrollBar.thumb", new Color(0x4A4A4A)}, {"ScrollBar.thumbShadow", new Color(0x4A4A4A)},
                {"ScrollBar.thumbHighlight", new Color(0x6A6A6A)}, {"ScrollBar.thumbDarkShadow", new Color(0x333333)},
                {"TitledBorder.titleColor", new Color(0xD0D5D9)},
                {"OptionPane.background", panel}, {"OptionPane.messageForeground", text},
                {"ToolTip.background", field}, {"ToolTip.foreground", text},
        };
        for (Object[] kv : keys) {
            UIManager.put(kv[0], kv[1]);
        }
    }
}
