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

public class DirectionToAddDialog extends TemplateDialog {

    private ButtonGroup bg = new ButtonGroup();
    private JRadioButton jrb_left = null, jrb_right = null, jrb_upper = null, jrb_lower = null;
    private Table t = null;

    public DirectionToAddDialog(Frame owner, ResourceBundle rb) {
	super(owner, rb.getString("add.column"));
	setLayout(new FlowLayout());

	bg.add(jrb_right = new JRadioButton(rb.getString("right")));
	bg.add(jrb_upper = new JRadioButton(rb.getString("upper")));
	bg.add(jrb_left = new JRadioButton(rb.getString("left")));
	bg.add(jrb_lower = new JRadioButton(rb.getString("lower")));

	Container c = new Container();
	c.setLayout(new BorderLayout());
	c.add(jrb_left, BorderLayout.WEST);
	c.add(jrb_right, BorderLayout.EAST);
	c.add(jrb_upper, BorderLayout.NORTH);
	c.add(jrb_lower, BorderLayout.SOUTH);
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
	if (jrb_right.isSelected()) {
	    t.insertColumn(Direction.RIGHT);
	} else if (jrb_left.isSelected()) {
	    t.insertColumn(Direction.LEFT);
	} else if (jrb_upper.isSelected()) {
	    t.insertRow(Direction.UPPER);
	} else if (jrb_lower.isSelected()) {
	    t.insertRow(Direction.LOWER);
	}
	setVisible(false);
    }
    
    public void askDirectionToAddARow(Table t) {
	if(0 > t.getSelectedRow())
	    return;
	this.t = t;
	jrb_left.setEnabled(false);
	jrb_right.setEnabled(false);
	jrb_upper.setEnabled(true);
	jrb_lower.setEnabled(true);
	if (!(jrb_upper.isSelected() || jrb_lower.isSelected())) {
	    jrb_upper.setSelected(true);
	}
	
	setVisible(true);
    }

    public void askDirectionToAddAColumn(Table t) {
	if(0 > t.getSelectedColumn())
	    return;
	this.t = t;
	jrb_left.setEnabled(true);
	jrb_right.setEnabled(true);
	jrb_upper.setEnabled(false);
	jrb_lower.setEnabled(false);
	if (!(jrb_right.isSelected() || jrb_left.isSelected())) {
	    jrb_left.setSelected(true);
	}
	setVisible(true);
    }

    public void askDirection(Table t) {
	this.t = t;
	jrb_left.setEnabled(true);
	jrb_right.setEnabled(true);
	jrb_upper.setEnabled(true);
	jrb_lower.setEnabled(true);
	if (!(jrb_upper.isSelected() || jrb_lower.isSelected() || jrb_right.isSelected() || jrb_left.isSelected())) {
	    jrb_left.setSelected(true);
	}
	setVisible(true);
    }
}
