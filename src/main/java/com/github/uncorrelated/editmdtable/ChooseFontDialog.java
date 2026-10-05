package com.github.uncorrelated.editmdtable;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.ResourceBundle;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;

public class ChooseFontDialog extends TemplateDialog {

    private int fontSize = 16;
    private JList<String>jlst = null;

    public ChooseFontDialog(GUI owner, ResourceBundle rb) {
	super(owner, rb.getString("choose.font"));
	setLayout(new BorderLayout());

	GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
	String[] fontNames = ge.getAvailableFontFamilyNames();

	ArrayList<String>al = new ArrayList(); 
	final String alphabets = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
	for(String fn : fontNames){
	    Font f = new Font(fn, Font.PLAIN, 12);
	    if(0 > f.canDisplayUpTo(alphabets)){
		al.add(fn);
	    }
	}
	
	jlst = new JList(al.toArray());
	jlst.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	jlst.setSelectedIndex(0);

	Font f = owner.getFont();
	if (f != null) {
	    jlst.setSelectedValue(f.getFamily(), true);
	    if (jlst.getSelectedIndex() == -1) {
		jlst.setSelectedIndex(0);
	    }
	}

	JScrollPane jsp = new JScrollPane(jlst);

	add(jsp, BorderLayout.CENTER);

	Container c = new Container();
	c.setLayout(new FlowLayout());

	JButton jb_cancel = new JButton(rb.getString("cancel"));
	jb_cancel.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		setVisible(false);
	    }
	});
	c.add(jb_cancel);

	JButton jb_ok = new JButton(rb.getString("select"));
	jb_ok.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		String selected = jlst.getSelectedValue();
		Font f = owner.getFont();
		owner.changeFont(new Font(selected, f.getStyle(), f.getSize()));
		dispose();
	    }
	});
	c.add(jb_ok);

	JButton jb_save = new JButton(rb.getString("save"));
	jb_save.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		String selected = jlst.getSelectedValue();
		Font f = owner.getFont();
		owner.changeFont(new Font(selected, f.getStyle(), f.getSize()));
		owner.saveFontSetting();
		dispose();
	    }
	});
	c.add(jb_save);

	add(c, BorderLayout.SOUTH);

	enableEnterESC(jb_ok, jb_cancel);
    }

    @Override
    public void setVisible(boolean f) {
	if (f) {
	    GUI owner = (GUI)getOwner();
	    fontSize = owner.getFont().getSize();
	    jlst.setCellRenderer(new FontPreviewCellRenderer());
	    pack();
	    owner.setComponentSize(this, 0.7);
	    owner.moveCenter(this);
	}
	super.setVisible(f);
    }

    private class FontPreviewCellRenderer implements ListCellRenderer<String> {

	private final DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

	@Override
	public Component getListCellRendererComponent(
		JList<? extends String> list,
		String fontName,
		int index,
		boolean isSelected,
		boolean cellHasFocus) {

	    // 標準の選択色や背景処理などを適用したJLabelを取得
	    JLabel label = (JLabel) defaultRenderer.getListCellRendererComponent(
		    list, fontName, index, isSelected, cellHasFocus);

	    // 表示するフォント名を使って、該当書体のFontオブジェクトを設定 (サイズ16pt)
	    label.setFont(new Font(fontName, Font.PLAIN, fontSize));

	    return label;
	}
    }
}
