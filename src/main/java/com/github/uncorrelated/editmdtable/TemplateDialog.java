package com.github.uncorrelated.editmdtable;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

public class TemplateDialog extends JDialog {
    public TemplateDialog(Frame owner, String title){
	super(owner, title, true);
    }

    protected void enableEnterESC(JButton jb_ok, JButton jb_cancel) {
	JRootPane jrp = getRootPane();
	jrp.setDefaultButton(jb_ok);
	jrp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape");
	getRootPane().getActionMap().put("escape", new AbstractAction() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		dispose();
	    }
	});
	enableEnter(jb_ok);
	enableEnter(jb_cancel);
    }

    protected void enableEnter(JButton jb) {
	jb.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false), "pressed");
	jb.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, true), "released");
    }
   
}
