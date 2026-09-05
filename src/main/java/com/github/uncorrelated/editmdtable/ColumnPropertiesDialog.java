package com.github.uncorrelated.editmdtable;

import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ResourceBundle;
import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JRootPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

public class ColumnPropertiesDialog extends TemplateDialog {

    private JTextField jtf = null;
    private ButtonGroup halign = new ButtonGroup();
    private JRadioButton jrb_left = null;
    private JRadioButton jrb_center = null;
    private JRadioButton jrb_right = null;
    private Table t;
    private int target_column;

    public ColumnPropertiesDialog(Frame owner, ResourceBundle rb) {
	super(owner, rb.getString("rename.column"));
	setLayout(new GridLayout(3, 1));
	add(jtf = new JTextField(Integer.parseInt(rb.getString("textfield.size"))));

	halign.add(jrb_left = new JRadioButton(rb.getString("left")));
	halign.add(jrb_center = new JRadioButton(rb.getString("center")));
	halign.add(jrb_right = new JRadioButton(rb.getString("right")));

	Container rc = new Container();
	rc.setLayout(new FlowLayout());
	rc.add(jrb_left);
	rc.add(jrb_center);
	rc.add(jrb_right);
	add(rc);

	Container bc = new Container();
	bc.setLayout(new FlowLayout());
	JButton jb_cancel = new JButton(rb.getString("cancel"));
	jb_cancel.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		cancel();
	    }
	});
	JButton jb_ok = new JButton(rb.getString("ok"));
	jb_ok.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		ok();
	    }
	});
	bc.add(jb_cancel);
	bc.add(jb_ok);
	add(bc);

	enableEnterESC(jb_ok, jb_cancel);

	pack();
    }

    public void setVisible(boolean b) {
	super.setVisible(b);
	if (b) {
	    jtf.requestFocusInWindow();
	}
    }

    private void cancel() {
	setVisible(false);
    }

    private void ok() {
	if (jrb_left.isSelected()) {
	    t.setColumnAlign(target_column, JLabel.LEFT);
	} else if (jrb_center.isSelected()) {
	    t.setColumnAlign(target_column, JLabel.CENTER);
	} else {
	    t.setColumnAlign(target_column, JLabel.RIGHT);
	}
	t.setColumnName(target_column, jtf.getText());
	setVisible(false);
    }

    private void checkRadioButton(int align) {
	switch (align) {
	    case JLabel.LEFT:
		jrb_left.setSelected(true);
		break;
	    case JLabel.CENTER:
		jrb_center.setSelected(true);
		break;
	    case JLabel.RIGHT:
		jrb_right.setSelected(true);
		break;
	}
    }

    public void renameColumn(Table t, int col) {
	if(0 > col)
	    return;
	this.t = t;
	this.target_column = col;
	checkRadioButton(t.getColumnAlign(col));
	jtf.setText(t.getColumnName(col));
	setVisible(true);
    }

    public void renameSelectedColumn(Table t) {
	this.t = t;
	renameColumn(t, t.getSelectedColumn());
    }
}
