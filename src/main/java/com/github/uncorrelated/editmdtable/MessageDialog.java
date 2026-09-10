package com.github.uncorrelated.editmdtable;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

public class MessageDialog extends TemplateDialog {

    public MessageDialog(Frame owner, ResourceBundle rb, String title, String fpath) {
	super(owner, rb.getString("application.name") + " " + rb.getString("application.version"));
	SwingUtilities.updateComponentTreeUI(this);
	BorderLayout bl = new BorderLayout();
	bl.setVgap(8);
	setLayout(bl);
	JLabel jl = new JLabel(title, JLabel.CENTER);
	jl.setPreferredSize(new Dimension(300, 40));
	jl.setFont(new Font(rb.getString("font.dialog"), Font.BOLD, 18));
	add(jl, BorderLayout.NORTH);
	JEditorPane editorPane;
	try {
	    editorPane = new JEditorPane(getClass().getResource(fpath));
	    editorPane.setEditable(false);
	    editorPane.setContentType("text/html");
	    add(new JScrollPane(editorPane,
		    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
		    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
	} catch (IOException ex) {
	    Logger.getLogger(MessageDialog.class.getName()).log(Level.SEVERE, null, ex);
	}
	JButton jb = new JButton(rb.getString("close"));
	jb.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		JComponent c = (JComponent) e.getSource();
		JDialog jd = (JDialog)SwingUtilities.getWindowAncestor(c);
		jd.setVisible(false);
	    }
	});
	Container c = new Container();
	c.setLayout(new GridLayout(1, 3));
	c.add(new JPanel());
	c.add(jb);
	c.add(new JPanel());
	add(c, BorderLayout.SOUTH);

	pack();
    }
}
