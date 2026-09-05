package com.github.uncorrelated.editmdtable;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;

public class FontUtils {

    public static void changeFontAll(Container container, Font baseFont) {
	if (container == null) {
	    return;
	}

	applyCustomFont(container, baseFont);

	if (container instanceof JMenu) {
	    JMenu menu = (JMenu) container;
	    for (int i = 0; i < menu.getItemCount(); i++) {
		JMenuItem item = menu.getItem(i);
		if (item != null) {
		    changeFontAll(item, baseFont);
		}
	    }
	}

	for (Component comp : container.getComponents()) {
	    applyCustomFont(comp, baseFont);

	    if (comp instanceof Container) {
		changeFontAll((Container) comp, baseFont);
	    }
	}
    }

    private static void applyCustomFont(Component comp, Font baseFont) {
	// JTable および JTableHeader は 0.9倍
	if (comp instanceof JTable || comp instanceof JTableHeader) {
	    float scaledSize = baseFont.getSize2D() * 1.0f;
	    comp.setFont(baseFont.deriveFont(scaledSize));
	} else {
	    comp.setFont(baseFont);
	}
    }
}
