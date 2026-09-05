package com.github.uncorrelated.editmdtable;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JRadioButton;

public class DirectionToDeleteDialog extends TemplateDialog {

    private ButtonGroup bg = new ButtonGroup();
    private JRadioButton jrb_column = null, jrb_row = null;
    private Table t = null;

    public DirectionToDeleteDialog(Frame owner, ResourceBundle rb) {
	super(owner, rb.getString("add.column"));
	setLayout(new FlowLayout());

	bg.add(jrb_row = new JRadioButton(rb.getString("row")));
	bg.add(jrb_column = new JRadioButton(rb.getString("column")));
	jrb_row.setSelected(true);

	Container c = new Container();
	c.setLayout(new FlowLayout());
	c.add(jrb_row);
	c.add(jrb_column);
	add(c);

	JButton jb_cancel = new JButton(rb.getString("cancel"));
	jb_cancel.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		cancel();
	    }
	});
	add(jb_cancel);

	JButton jb_ok = new JButton(rb.getString("ok"));
	jb_ok.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		ok();
	    }
	});
	add(jb_ok);

	enableEnterESC(jb_ok, jb_cancel);

	pack();
    }


    private void cancel() {
	setVisible(false);
    }

    private void ok() {
	if (jrb_row.isSelected()) {
	    t.deleteSelectedRow();
	} else if (jrb_column.isSelected()) {
	    t.deleteSelectedColumn();;
	}
	setVisible(false);
    }
    
    public void askDirection(Table t) {
	this.t = t;
	setVisible(true);
    }
}
