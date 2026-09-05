package com.github.uncorrelated.editmdtable;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.ButtonGroup;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.undo.UndoManager;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

public class Table extends Container {

    private class FocusCellRenderer extends DefaultTableCellRenderer {

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
		boolean isSelected, boolean hasFocus, int row, int column) {

	    Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

	    // フォーカスがあるセルの背景色を設定
	    if (hasFocus) {
		c.setBackground(Color.YELLOW); // フォーカス時の背景色
		c.setForeground(Color.BLACK);
	    } else if (isSelected) {
		c.setBackground(table.getSelectionBackground());
		c.setForeground(table.getSelectionForeground());
	    } else {
		c.setBackground(table.getBackground());
		c.setForeground(table.getForeground());
	    }

	    if (c instanceof JLabel) {
		// 列から右並びと左並びを判別
		int index = jt.convertColumnIndexToModel(column);
		switch (HorizonalAlign[index]) {
		    case JLabel.LEFT:
			((JLabel) c).setHorizontalAlignment(SwingConstants.LEFT);
			break;
		    case JLabel.CENTER:
			((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
			break;
		    case JLabel.RIGHT:
			((JLabel) c).setHorizontalAlignment(SwingConstants.RIGHT);
			break;
		}
	    }

	    return c;
	}
    }

    protected JTable jt = null;
    // ViewではなくModelのインデックス順
    protected int[] HorizonalAlign = null;
    protected String[] identifiers = null;
    private final UndoManager undoManager = new UndoManager();
    private boolean IsOnGoingUndoRedo = false;
    private final JTextField filter_tf = new JTextField("");
    private final Container container_filter = new Container(), container_replace = new Container(), container_fr = new Container();
    private boolean isFilterd = false;
    private final TableRowSorter<DefaultTableModel> sorter;
    private final JCheckBox filter_not;
    private final JRadioButton movement_v, movement_h;
    private final JTextField search_text, replace_text;

    private final Table self;
    private final GUI gui;

    public Table(GUI gui, DirectionToAddDialog add_dialog, DirectionToDeleteDialog delete_dialog, ColumnPropertiesDialog cp_dialog, String[][] table) {

	self = this;
	this.gui = gui;
	ResourceBundle rb = gui.getResourceBundle();

	/*
	    JScrollPaneにJTableをありつける
	    BorderLayoutのContainerのCENTERにJScrollPane、NORTHにTableHeaderを入れる
	    JTabbedPaneにBorderLayoutのContainerを貼り付ける
	 */
//	UIManager.put("Table.selectionBackground", Color.LIGHT_GRAY);
	jt = setTable(table);

// セルの表示をカスタマイズ（カーソル位置の背景色を変更）
	jt.setDefaultRenderer(Object.class, new FocusCellRenderer());

// 矩形選択可能にする
	jt.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	jt.setCellSelectionEnabled(true); // 行と列の両方の選択を連動させる

// 列ラベルを表示
	DefaultTableModel model = (DefaultTableModel) jt.getModel();
	model.setColumnIdentifiers(identifiers);
// JTable からヘッダーを取得
	JTableHeader tableHeader = jt.getTableHeader();
//	tableHeader.setPreferredSize(new Dimension(tableHeader.getPreferredSize().width, Pt2Pixel(tableHeader.getFont().getSize())));
// 列サイズの変更を許可
	tableHeader.setResizingAllowed(true);
// 列ラベル名や文字寄せの変更ダイアログを呼ぶ
	tableHeader.addMouseListener(new MouseAdapter() {
	    @Override
	    public void mouseClicked(MouseEvent e) {
		int view_index = jt.columnAtPoint(e.getPoint());
		gui.moveCenter(cp_dialog);
		cp_dialog.renameColumn(self, view_index);
	    }
	});

// 列ラベルの文字寄せを設定
	TableColumnModel columnModel = jt.getColumnModel();
	for (int i = 0; i < HorizonalAlign.length; i++) {
	    DefaultTableCellRenderer headerRenderer0 = new DefaultTableCellRenderer();
	    headerRenderer0.setHorizontalAlignment(HorizonalAlign[i]);
	    columnModel.getColumn(i).setHeaderRenderer(headerRenderer0);
	}

	setLayout(new BorderLayout());

// 列の順番変更をキャッチする
	jt.getColumnModel().addColumnModelListener(new TableColumnModelListener() {
	    @Override
	    public void columnAdded(TableColumnModelEvent e) {
	    }

	    @Override
	    public void columnRemoved(TableColumnModelEvent e) {
	    }

	    @Override
	    public void columnMoved(TableColumnModelEvent e) {
		gui.dataChanged();
	    }

	    @Override
	    public void columnMarginChanged(ChangeEvent e) {
	    }

	    @Override
	    public void columnSelectionChanged(ListSelectionEvent e) {
	    }
	});

// 無駄に二重BorderLayout Containerにする気がするが、JTableと同じBorderLayout Containerの他のComonentはうまく描画されない
	Container c_table = new Container();
	c_table.setLayout(new BorderLayout());

	c_table.add(jt.getTableHeader(), BorderLayout.NORTH);
	c_table.add(new JScrollPane(jt), BorderLayout.CENTER);

	add(c_table, BorderLayout.CENTER);

// 表示フィルター用の入力フォーム
	JButton filter_button = new JButton(rb.getString("button.filter"));
	filter_button.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		filter();
	    }
	});
	setESCtoFilterComponent(filter_button);

	container_fr.setLayout(new BorderLayout());

	// 置換用のフォームのコンポーネントを並べる
	ButtonGroup replace_bg = new ButtonGroup();
	JButton btn_search, btn_backward, btn_replace, btn_replaceAll;

	container_replace.setLayout(new BorderLayout(4, 4));
	Container container_r_west = new Container();
	container_r_west.setLayout(new GridLayout(2, 1));
	container_r_west.add(movement_v = new JRadioButton(rb.getString("row")));
	container_r_west.add(movement_h = new JRadioButton(rb.getString("column")));
	movement_v.setSelected(true);
	replace_bg.add(movement_v);
	replace_bg.add(movement_h);
	container_replace.add(container_r_west, BorderLayout.WEST);
	Container container_r_center = new Container();
	container_r_center.setLayout(new BorderLayout());
	Container container_r_center_west = new Container();
	container_r_center_west.setLayout(new GridLayout(2, 1));
	container_r_center_west.add(new JLabel(rb.getString("find.what")));
	container_r_center_west.add(new JLabel(rb.getString("replace.with")));
	container_r_center.add(container_r_center_west, BorderLayout.WEST);
	Container container_r_center_center = new Container();
	container_r_center_center.setLayout(new GridLayout(2, 1));
	container_r_center_center.add(search_text = new JTextField());
	container_r_center_center.add(replace_text = new JTextField());
	container_r_center.add(container_r_center_center, BorderLayout.CENTER);
	container_replace.add(container_r_center, BorderLayout.CENTER);
	Container container_r_east = new Container();
	container_r_east.setLayout(new GridLayout(2, 2));
	container_r_east.add(btn_search = new JButton(rb.getString("search")));
	container_r_east.add(btn_replace = new JButton(rb.getString("replace")));
	container_r_east.add(btn_backward = new JButton(rb.getString("backward")));
	container_r_east.add(btn_replaceAll = new JButton(rb.getString("replace.all")));
	container_replace.add(container_r_east, BorderLayout.EAST);
	container_replace.setVisible(false);
	container_fr.add(container_replace, BorderLayout.NORTH);

	setESCtoReplaceComponent(movement_v);
	setESCtoReplaceComponent(movement_h);

	btn_search.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		search(false, movement_h.isSelected(), false, false);
	    }
	});
	setESCtoReplaceComponent(btn_search);

	btn_replace.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		search(true, movement_h.isSelected(), false, true);
	    }
	});
	setESCtoReplaceComponent(btn_replace);

	btn_backward.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		search(false, movement_h.isSelected(), true, true);
	    }
	});
	setESCtoReplaceComponent(btn_backward);

	btn_replaceAll.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		search(true, movement_h.isSelected(), false, false);
	    }
	});
	setESCtoReplaceComponent(btn_replaceAll);

	container_filter.setLayout(new BorderLayout(4, 0));
	container_filter.add(filter_not = new JCheckBox(rb.getString("search.not")), BorderLayout.WEST);
	setESCtoFilterComponent(filter_not);
	container_filter.add(filter_tf, BorderLayout.CENTER);
	container_filter.add(filter_button, BorderLayout.EAST);
	container_filter.setVisible(false);
	container_fr.setVisible(false);
	container_fr.add(container_filter, BorderLayout.SOUTH);
	add(container_fr, BorderLayout.SOUTH);

// 列の入れ替えを許可
	jt.getTableHeader().setReorderingAllowed(true);

// 列追加を自動にすると、列の追加時にビューの順序と列の横幅が初期化されるため、falseにする
	jt.setAutoCreateColumnsFromModel(false);

// フィルター機能のためにソーターをつける
	sorter = new TableRowSorter<DefaultTableModel>(model) {
	    @Override
	    public void toggleSortOrder(int column) {
		return;
	    }
	};
	jt.setRowSorter(sorter);

// ショートカットの登録
	InputMap im = jt.getInputMap();
	ActionMap am = jt.getActionMap();
	im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK, false), "Paste");
	am.put("Paste", new AbstractAction() {
	    public void actionPerformed(ActionEvent e) {
		paste();
	    }
	});
	im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, ActionEvent.SHIFT_MASK, false), "Clear");
	am.put("Clear", new AbstractAction() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		clear();
	    }
	});
	im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK, false), "Cut");
	am.put("Cut", new AbstractAction() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		cut();
	    }
	});

	im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, ActionEvent.CTRL_MASK, false), "Filter");
	AbstractAction aa_filter = new AbstractAction() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		toggleVisibilityFilterForm();
	    }
	};
	am.put("Filter", aa_filter);

	// 表示フィルターのテキストフィールドのショートカット
	InputMap im_ftf = filter_tf.getInputMap();
	ActionMap am_ftf = filter_tf.getActionMap();
	im_ftf.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, ActionEvent.CTRL_MASK, false), "Close");
	am_ftf.put("Close", aa_filter);

	im_ftf.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false), "Filter");
	am_ftf.put("Filter", new AbstractAction() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		filter();
	    }
	});

	im_ftf.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "ESC");
	am_ftf.put("ESC", new AbstractAction() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		isFilterd = false;
		toggleVisibilityFilterForm();
	    }
	});

	im.put(KeyStroke.getKeyStroke(KeyEvent.VK_H, ActionEvent.CTRL_MASK, false), "Replace");
	AbstractAction aa_replace = new AbstractAction() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		toggleVisibilityReplaceForm();
	    }
	};
	am.put("Replace", aa_replace);

	// 置換用のテキストフィールドのショートカット
	InputMap im_stf = search_text.getInputMap();
	ActionMap am_stf = search_text.getActionMap();
	im_stf.put(KeyStroke.getKeyStroke(KeyEvent.VK_H, ActionEvent.CTRL_MASK, false), "Close");
	im_stf.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "Close");
	am_stf.put("Close", aa_replace);

	InputMap im_rtf = replace_text.getInputMap();
	ActionMap am_rtf = replace_text.getActionMap();
	im_rtf.put(KeyStroke.getKeyStroke(KeyEvent.VK_H, ActionEvent.CTRL_MASK, false), "Close");
	im_rtf.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "Close");
	am_rtf.put("Close", aa_replace);

	im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, ActionEvent.CTRL_MASK, false), "GoToTableBottom");
	am.put("GoToTableBottom", new AbstractAction() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		moveCursor(jt.getRowCount() - 1, jt.getSelectedColumn());
	    }
	});
	im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, ActionEvent.CTRL_MASK, false), "GoToTableTop");
	am.put("GoToTableTop", new AbstractAction() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		moveCursor(0, jt.getSelectedColumn());
	    }
	});
	im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, ActionEvent.CTRL_MASK, false), "GoToTableLeft");
	am.put("GoToTableLeft", new AbstractAction() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		moveCursor(jt.getSelectedRow(), 0);
	    }
	});
	im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, ActionEvent.CTRL_MASK, false), "GoToTableRight");
	am.put("GoToTableRight", new AbstractAction() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		moveCursor(jt.getSelectedRow(), jt.getColumnCount() - 1);
	    }
	});

	im.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK, false), "Insert");
	im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SEMICOLON, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK, false), "Insert");
	am.put("Insert", new AbstractAction() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		gui.moveCenter(add_dialog);
		add_dialog.askDirection(self);
	    }
	});

	im.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK, false), "Delete");
	am.put("Delete", new AbstractAction() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		gui.moveCenter(delete_dialog);
		delete_dialog.askDirection(self);
	    }
	});

	im.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK, false), "Properties");
	am.put("Properties", new AbstractAction() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		gui.moveCenter(cp_dialog);
		cp_dialog.renameSelectedColumn(self);
	    }
	});

	im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK, false), "Undo");
	am.put("Undo", new AbstractAction() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		undo();
	    }
	});

	im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_DOWN_MASK, false), "Redo");
	am.put("Redo", new AbstractAction() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		redo();
	    }
	});

	im.put(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK, false), "Open");
	am.put("Open", new AbstractAction() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		gui.openFileChooser();
	    }
	});

	im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK, false), "Save");
	am.put("Save", new AbstractAction() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		gui.save();
	    }
	});

    }

    public void modifyCellHeight() {
// JTableのヘッダーとセルの高さを調整
	JTableHeader tableHeader = jt.getTableHeader();
	tableHeader.setPreferredSize(new Dimension(tableHeader.getPreferredSize().width, Pt2Pixel(tableHeader.getFont().getSize())));
	jt.setRowHeight(Pt2Pixel(tableHeader.getFont().getSize()));
// 列幅も変わるため、PreferredSizeの更新も行う
	resizeColumnWidth(jt);
    }

    public static void resizeColumnWidth(JTable jt) {
	final TableColumnModel columnModel = jt.getColumnModel();
	final int minimum = 50, margin = 10;

	for (int column = 0; column < jt.getColumnCount(); column++) {
	    int width = minimum; // 最小列幅
	    TableCellRenderer headerRenderer = jt.getTableHeader().getDefaultRenderer();
	    Component comp = headerRenderer.getTableCellRendererComponent(jt, jt.getColumnName(column), false, false, 0, column);
	    width = Math.max(comp.getPreferredSize().width + margin, width);

	    // 各行のセルの幅を計算して最大値を追う
	    for (int row = 0; row < jt.getRowCount(); row++) {
		TableCellRenderer renderer = jt.getCellRenderer(row, column);
		Component c = jt.prepareRenderer(renderer, row, column);
		width = Math.max(c.getPreferredSize().width + margin, width);
	    }

	    columnModel.getColumn(column).setPreferredWidth(width);
	}
    }

    public void toggleVisibilityFilterForm() {
	if (isFilterd) {
	    filter_tf.requestFocusInWindow();
	} else if (container_filter.isVisible()) {
	    sorter.setRowFilter(null);
	    jt.requestFocusInWindow();
	    container_filter.setVisible(false);
	    if (!container_replace.isVisible()) {
		container_fr.setVisible(false);
	    }
	} else {
	    container_filter.setVisible(true);
	    container_fr.setVisible(true);
	    filter_tf.requestFocusInWindow();
	}
	self.repaint();
    }

    private void setESCtoFilterComponent(JComponent jb) {
	jb.addKeyListener(new KeyAdapter() {
	    @Override
	    public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
		    toggleVisibilityFilterForm();
		}
	    }
	});
    }

    private void filter() {
	String search_text = filter_tf.getText();
	if (0 < search_text.length()) {
	    if (filter_not.isSelected()) {
		sorter.setRowFilter(RowFilter.notFilter(RowFilter.regexFilter(search_text)));
	    } else {
		sorter.setRowFilter(RowFilter.regexFilter(search_text));
	    }
	    isFilterd = true;
	} else {
	    sorter.setRowFilter(null);
	    isFilterd = false;
	    container_filter.setVisible(false);
	}
	jt.requestFocusInWindow();
    }

    public void toggleVisibilityReplaceForm() {
	if (container_replace.isVisible()) {
	    jt.requestFocusInWindow();
	    container_replace.setVisible(false);
	    if (!container_filter.isVisible()) {
		container_fr.setVisible(false);
	    }
	} else {
	    container_replace.setVisible(true);
	    container_fr.setVisible(true);
	    search_text.requestFocusInWindow();
	}
	self.repaint();
    }

    private void setESCtoReplaceComponent(JComponent jb) {
	jb.addKeyListener(new KeyAdapter() {
	    @Override
	    public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
		    toggleVisibilityReplaceForm();
		}
	    }
	});
    }

    private static int[] getHorizonalAlign(String[][] table, int row) {
	if (!(table[0].length > row)) {
	    return null;
	}
	int[] HorizonalAlign = new int[table.length];
	int ha = JLabel.LEFT;
	for (int j = 0; j < table.length; j++) {
	    char[] a = table[j][row].toCharArray();
	    for (int k = 0; k < a.length; k++) {
		if (':' != a[k] && '-' != a[k]) {
		    return null;
		}
	    }
	    if (a.length <= 0) {
		return null;
	    } else if (':' == a[0]) {
		if (':' == a[a.length - 1]) {
		    HorizonalAlign[j] = JLabel.CENTER;
		}
		HorizonalAlign[j] = JLabel.LEFT;
	    } else if (':' == a[a.length - 1]) {
		HorizonalAlign[j] = JLabel.RIGHT;
	    } else {
		HorizonalAlign[j] = JLabel.CENTER;
	    }
	}
	return HorizonalAlign;
    }

    public final JTable setTable(String[][] table) {
	int nr = table[0].length;
	int nc = table.length;
	int top_row = 0;
	HorizonalAlign = getHorizonalAlign(table, 0);
	if (null != HorizonalAlign) {
	    identifiers = new String[nc];
	    for (int j = 0; j < nc; j++) {
		identifiers[j] = "";
	    }
	    top_row = 1;
	} else {
	    HorizonalAlign = getHorizonalAlign(table, 1);
	    if (null == HorizonalAlign) {
		identifiers = new String[nc];
		HorizonalAlign = new int[nc];
		for (int j = 0; j < nc; j++) {
		    identifiers[j] = "";
		    HorizonalAlign[j] = JLabel.LEFT;
		}
		top_row = 0;
	    } else {
		identifiers = new String[nc];
		for (int j = 0; j < nc; j++) {
		    identifiers[j] = table[j][0];
		}
		top_row = 2;
	    }
	}
	JTable jt = new JTable(nr - top_row, nc) {
	    @Override
	    public Component prepareEditor(TableCellEditor editor, int row, int column) {
		Component comp = super.prepareEditor(editor, row, column);
		if (comp != null) {
		    comp.setFont(this.getFont());
		}
		return comp;
	    }

	    @Override
	    public boolean getScrollableTracksViewportWidth() {
		// JTableの横サイズがウィンドウのサイズよりも小さければ引き伸ばし、そうでなければ縮小しないようにしてスクロールバーを出す
		if (getPreferredSize().width < SwingUtilities.getUnwrappedParent(this).getWidth()) {
		    setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
		} else {
		    setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		}
		return super.getScrollableTracksViewportWidth();
	    }
	};
	String[][] t = new String[nr - top_row][nc];
	for (int j = 0; j < nc; j++) {
	    for (int i = top_row; i < nr; i++) {
		t[i - top_row][j] = table[j][i];
	    }
	}
	jt.setModel(new DefaultTableModel(t, identifiers) {
	    @Override
	    public void setValueAt(Object aValue, int row, int column) {
		// Undo用にメソッドをオーバーライド
		Object oldValue = getValueAt(row, column);
		if (oldValue == null ? aValue == null : oldValue.equals(aValue)) {
		    return;
		}
		super.setValueAt(aValue, row, column);
		gui.dataChanged();
		if (!IsOnGoingUndoRedo) {
		    undoManager.addEdit(new CellEdit(this, oldValue, aValue, row, column));
		}
	    }
	});
	jt.addMouseListener(gui);
	jt.enableInputMethods(false);
	return jt;
    }

    public final String[][] getTable() {
	/*
	行フィルターを一時的にオフにしてから保存用データを作成する
	表示をそのまま保存用データにしてしまうため
	 */
	RowFilter<? super DefaultTableModel, ? super Integer> rowFilter = sorter.getRowFilter();
	sorter.setRowFilter(null);

	int nr = jt.getRowCount();
	int nc = jt.getColumnCount();
	String[][] table = new String[nc][nr + 2];
	for (int c = 0; c < nc; c++) {
	    int mc = jt.convertColumnIndexToModel(c);
	    table[c][0] = identifiers[mc];
	    switch (HorizonalAlign[mc]) {
		case JLabel.LEFT:
		    table[c][1] = ":---";
		    break;
		case JLabel.CENTER:
		    table[c][1] = ":---:";
		    break;
		case JLabel.RIGHT:
		    table[c][1] = "---:";
		    break;
	    }
	    for (int r = 0; r < nr; r++) {
		table[c][r + 2] = (String) jt.getValueAt(r, c);
	    }
	}

	sorter.setRowFilter(rowFilter); // 行フィルターを元に戻す
	return table;
    }

    public void undo() {
	// セル編集中の場合は編集をキャンセルしてからUndo実行
	if (jt.isEditing()) {
	    jt.getCellEditor().cancelCellEditing();
	}
	if (undoManager.canUndo()) {
	    undoManager.undo();
	}
    }

    public void redo() {
	if (jt.isEditing()) {
	    jt.getCellEditor().cancelCellEditing();
	}
	if (undoManager.canRedo()) {
	    undoManager.redo();
	}
    }

    public class CellEdit extends AbstractUndoableEdit {

	private final TableModel model;
	private final Object oldValue;
	private final Object newValue;
	private final int row;
	private final int column;

	public CellEdit(TableModel model, Object oldValue, Object newValue, int row, int column) {
	    this.model = model;
	    this.oldValue = oldValue;
	    this.newValue = newValue;
	    this.row = row;
	    this.column = column;
	}

	@Override
	public void undo() throws CannotUndoException {
	    super.undo();
	    IsOnGoingUndoRedo = true;
	    model.setValueAt(oldValue, row, column);
	    IsOnGoingUndoRedo = false;
	}

	@Override
	public void redo() throws CannotRedoException {
	    super.redo();
	    IsOnGoingUndoRedo = true;
	    model.setValueAt(newValue, row, column);
	    IsOnGoingUndoRedo = false;
	}
    }

    private class InsertRowEdit extends AbstractUndoableEdit {

	private final int[] rows;

	public InsertRowEdit(int[] rows) {
	    this.rows = rows;
	}

	@Override
	public void undo() throws CannotUndoException {
	    super.undo();
	    DefaultTableModel model = (DefaultTableModel) jt.getModel();
	    for (int i = rows.length - 1; i >= 0; i--) {
		int row = rows[i];
//		row -= i; // 削除する度に位置がずれるため補正
		model.removeRow(row);
	    }
	}

	@Override
	public void redo() throws CannotRedoException {
	    super.redo();
	    insertRows(rows);
	}
    }

    private class InsertColumnEdit extends AbstractUndoableEdit {

	private final int[] view_cols;

	public InsertColumnEdit(int[] view_cols) {
	    this.view_cols = view_cols;
	}

	@Override
	public void undo() throws CannotUndoException {
	    super.undo();
	    for (int j = view_cols.length - 1; j >= 0; j--) {
		jt.removeColumn(jt.getColumnModel().getColumn(view_cols[j]));
	    }
	}

	@Override
	public void redo() throws CannotRedoException {
	    super.redo();
	    insertColumn(view_cols);
	}
    }

    private class ExtendTableEdit extends AbstractUndoableEdit {

	private final int row, col;
	private final int[] view_rows, view_cols;

	public ExtendTableEdit(int row, int col, int[] view_rows, int[] view_cols) {
	    this.row = row;
	    this.col = col;
	    this.view_cols = view_cols;
	    this.view_rows = view_rows;
	}

	@Override
	public void undo() throws CannotUndoException {
	    super.undo();
	    DefaultTableModel model = (DefaultTableModel) jt.getModel();
	    for (int i = view_rows.length - 1; i >= 0; i--) {
		int row = view_rows[i];
		model.removeRow(row);
	    }
	    for (int j = view_cols.length - 1; j >= 0; j--) {
		jt.removeColumn(jt.getColumnModel().getColumn(view_cols[j]));
	    }
	    moveCursor(row, col);
	}

	@Override
	public void redo() throws CannotRedoException {
	    super.redo();
	    insertColumn(view_cols);
	    insertRows(view_rows);
	    moveCursor(row, col);
	}
    }

    private class DeleteRowEdit extends AbstractUndoableEdit {

	private final List<DeletedRow> deleted;

	public DeleteRowEdit(List deleted) {
	    this.deleted = deleted;
	}

	@Override
	public void undo() throws CannotUndoException {
	    super.undo();
	    DefaultTableModel model = (DefaultTableModel) jt.getModel();
	    Iterator<DeletedRow> it = deleted.iterator();
	    while (it.hasNext()) {
		DeletedRow r = it.next();
		model.insertRow(r.row, r.rowdata);
	    }
	}

	@Override
	public void redo() throws CannotRedoException {
	    super.redo();
	    DefaultTableModel model = ((DefaultTableModel) jt.getModel());
	    for (int i = deleted.size() - 1; i >= 0; i--) {
		DeletedRow r = deleted.get(i);
		model.removeRow(r.row);
	    }
	}
    }

    private class DeleteViewColumnEdit extends AbstractUndoableEdit {

	private final TableColumn[] columns;
	private final int[] viewIndexes;

	public DeleteViewColumnEdit(int[] viewIndexex, TableColumn[] modelIndexes) {
	    this.viewIndexes = viewIndexex;
	    // 表示中の列オブジェクトを取得
	    this.columns = modelIndexes;
	}

	@Override
	public void undo() throws CannotUndoException {
	    super.undo();
	    // 削除されていた位置に TableColumn を差し戻すだけ（データはモデルに残っている）
	    TableColumnModel columnModel = jt.getColumnModel();
	    for (int j = 0; j < columns.length; j++) {
		columnModel.addColumn(columns[j]);
		int lastIndex = columnModel.getColumnCount() - 1;
		if (lastIndex != viewIndexes[j]) {
		    columnModel.moveColumn(lastIndex, viewIndexes[j]);
		}
	    }
	}

	@Override
	public void redo() throws CannotRedoException {
	    super.redo();
	    // 再度ビューから削除する
	    TableColumnModel columnModel = jt.getColumnModel();
	    for (int j = 0; j < columns.length; j++) {
		columnModel.removeColumn(columns[j]);
	    }
	}
    }

    private class PasteEdit extends AbstractUndoableEdit {

	private final char[] pastedText;
	private final List previousText;

	public PasteEdit(char[] pastedText, List previousText) {
	    this.pastedText = pastedText;
	    this.previousText = previousText;
	}

	@Override
	public void undo() throws CannotUndoException {
	    super.undo();
	    IsOnGoingUndoRedo = true;
	    Iterator it = previousText.iterator();
	    DefaultTableModel model = (DefaultTableModel) jt.getModel();
	    while (it.hasNext()) {
		Cell c = (Cell) it.next();
		model.setValueAt(c.value, c.r, c.c);
	    }
	    IsOnGoingUndoRedo = false;
	}

	@Override
	public void redo() throws CannotRedoException {
	    super.redo();
	    Cell c = (Cell) previousText.get(0);
	    pasteChar((DefaultTableModel) jt.getModel(), c.r, c.c, pastedText, '\t');
	}
    }

    private class ClearEdit extends AbstractUndoableEdit {

	private final int[] model_rows, model_cols;
	private final String[][] values;

	public ClearEdit(int[] model_rows, int[] model_cols, String[][] values) {
	    this.model_rows = model_rows;
	    this.model_cols = model_cols;
	    this.values = values;
	}

	@Override
	public void undo() throws CannotUndoException {
	    super.undo();
	    DefaultTableModel model = (DefaultTableModel) jt.getModel();
	    IsOnGoingUndoRedo = true;
	    for (int i = 0; i < model_rows.length; i++) {
		for (int j = 0; j < model_cols.length; j++) {
		    model.setValueAt(values[i][j], model_rows[i], model_cols[j]);
		}
	    }
	    IsOnGoingUndoRedo = false;
	}

	@Override
	public void redo() throws CannotRedoException {
	    super.redo();
	    DefaultTableModel model = (DefaultTableModel) jt.getModel();
	    IsOnGoingUndoRedo = true;
	    for (int i = 0; i < model_rows.length; i++) {
		for (int j = 0; j < model_cols.length; j++) {
		    model.setValueAt("", model_rows[i], model_cols[j]);
		}
	    }
	    IsOnGoingUndoRedo = false;
	}
    }

    private class ReplaceEdit extends AbstractUndoableEdit {

	private final List<ReplacedCell> Cells;

	public ReplaceEdit(List Cells) {
	    this.Cells = Cells;
	}

	@Override
	public void undo() throws CannotUndoException {
	    super.undo();
	    IsOnGoingUndoRedo = true;
	    Iterator<ReplacedCell> it = Cells.iterator();
	    DefaultTableModel model = (DefaultTableModel) jt.getModel();
	    while (it.hasNext()) {
		ReplacedCell c = (ReplacedCell) it.next();
		model.setValueAt(c.previousText, c.model_r, c.model_c);
	    }
	    IsOnGoingUndoRedo = false;
	}

	@Override
	public void redo() throws CannotRedoException {
	    super.redo();
	    IsOnGoingUndoRedo = true;
	    Iterator<ReplacedCell> it = Cells.iterator();
	    DefaultTableModel model = (DefaultTableModel) jt.getModel();
	    while (it.hasNext()) {
		ReplacedCell c = (ReplacedCell) it.next();
		model.setValueAt(c.currentText, c.model_r, c.model_c);
	    }
	    IsOnGoingUndoRedo = false;
	}
    }

    public String getColumnName(int col) {
	if (0 > col) {
	    return null;
	}
	TableColumn column = jt.getColumnModel().getColumn(col);
	return column.getHeaderValue().toString();
    }

    public int getSelectedRow() {
	return jt.getSelectedRow();
    }

    public int getSelectedColumn() {
	return jt.getSelectedColumn();
    }

    public String getSelectedColumnName() {
	return getColumnName(jt.getSelectedColumn());
    }

    public void setColumnName(int col, String name) {
	identifiers[jt.convertColumnIndexToModel(col)] = name;
	// Viewの方を更新
	JTableHeader header = jt.getTableHeader();
	TableColumn column = jt.getColumnModel().getColumn(col);
	column.setHeaderValue(name);
	header.repaint(header.getHeaderRect(col));
    }

    public int getColumnAlign(int col) {
	int index = jt.convertColumnIndexToModel(col);
	return HorizonalAlign[index];
    }

    public void setColumnAlign(int col, int align) {
	int index = jt.convertColumnIndexToModel(col);
	HorizonalAlign[index] = align;
	TableColumnModel columnModel = jt.getColumnModel();
	DefaultTableCellRenderer headerRenderer0 = new DefaultTableCellRenderer();
	headerRenderer0.setHorizontalAlignment(HorizonalAlign[index]);
	columnModel.getColumn(col).setHeaderRenderer(headerRenderer0);
	repaint();
    }

    public void insertColumn(int col) {
	if (0 > col || jt.getColumnCount() < col) {
	    return;
	}
	String colname = "";

	DefaultTableModel model = (DefaultTableModel) jt.getModel();
	// 追加する列のデータはすべての行を空文字列にする
	String[] columnData = new String[model.getRowCount()];
	for (int i = 0; i < columnData.length; i++) {
	    columnData[i] = "";
	}
	model.addColumn(colname, columnData);

	int model_index = HorizonalAlign.length;

	int[] newHorizonalAlign = new int[HorizonalAlign.length + 1];
	newHorizonalAlign[model_index] = JLabel.LEFT;
	System.arraycopy(HorizonalAlign, 0, newHorizonalAlign, 0, HorizonalAlign.length);
	HorizonalAlign = newHorizonalAlign;

	String[] new_indentifiers = new String[identifiers.length + 1];
	new_indentifiers[model_index] = colname;
	System.arraycopy(identifiers, 0, new_indentifiers, 0, identifiers.length);
	identifiers = new_indentifiers;

	// 設定によりビューへの列追加は自動では無い
	int new_index = model.getColumnCount() - 1;
	TableColumn newColumn = new TableColumn(new_index);
	newColumn.setHeaderValue(colname);
	jt.addColumn(newColumn);
	jt.moveColumn(jt.getColumnCount() - 1, col);
    }

    public void insertColumn(int[] cols) {
	for (int j = 0; j < cols.length; j++) {
	    insertColumn(cols[j]);
	}
    }

    public void insertColumn(Direction direction) {
	int[] cols = jt.getSelectedColumns();
	cols = ViewToOpt(cols, Direction.RIGHT == direction);
	insertColumn(cols);
	gui.dataChanged();
	undoManager.addEdit(new InsertColumnEdit(cols));
    }

    public void deleteSelectedColumn() {
	int[] cols = jt.getSelectedColumns();
	TableColumn[] indexes = new TableColumn[cols.length];
	TableColumnModel columnModel = jt.getColumnModel();
	for (int j = 0; j < cols.length; j++) {
	    if (0 > cols[j] || jt.getColumnCount() <= cols[j]) {
		continue;
	    }
	    indexes[j] = columnModel.getColumn(cols[j]);
	}
	for (int j = 0; j < cols.length; j++) {
	    try {
		jt.removeColumn(indexes[j]);
		gui.dataChanged();
	    } catch (java.lang.ArrayIndexOutOfBoundsException e) {
		System.err.println("index: " + indexes[j]);
		System.err.println("col: " + cols[j]);
		throw e;
	    }
	}
	undoManager.addEdit(new DeleteViewColumnEdit(cols, indexes));
	// Model内の列は消えず、Viewで不可視になるだけなので、indentifiersとHorizonalAlignは変化させない
    }

    public void insertRow(int row) {
	if (0 > row || jt.getRowCount() < row) {
	    return;
	}
	DefaultTableModel model = (DefaultTableModel) jt.getModel();
	int ncol = model.getColumnCount();
	String[] rowdata = new String[ncol];
	for (int j = 0; j < ncol; j++) {
	    rowdata[j] = "";
	}
	model.insertRow(row, rowdata);
    }

    public void insertRows(int[] rows) {
	for (int i = 0; i < rows.length; i++) {
	    insertRow(rows[i]);
	}
    }

    public void insertRow(Direction direction) {
	int[] rows = jt.getSelectedRows();
	rows = ViewToOpt(rows, Direction.LOWER == direction);
	insertRows(rows);
	gui.dataChanged();
	undoManager.addEdit(new InsertRowEdit(rows));
    }

    private int[] ViewToOpt(int[] view_n, boolean DirectionFlag) {
	int[] opt_n = new int[view_n.length];
	for (int i = 0; i < view_n.length; i++) {
	    if (DirectionFlag) {
		opt_n[i] = view_n[i] + 1;
		for (int j = i - 1; j >= 0; j--) {
		    if (1 >= view_n[j + 1] - view_n[j]) {
			opt_n[j] = opt_n[i];
		    } else {
			// 挿入する度に位置がずれるため補正
			opt_n[i] = view_n[i] + i + 1;
		    }
		}
	    } else {
		opt_n[i] = view_n[i];
		for (int j = i - 1; j >= 0; j--) {
		    if (1 >= view_n[j + 1] - view_n[j]) {
			opt_n[i] = opt_n[j];
		    } else {
			// 挿入する度に位置がずれるため補正
			opt_n[i] = view_n[i] + i;
		    }
		}
	    }
	}
	return opt_n;
    }

    private class DeletedRow {

	public int row;
	public String[] rowdata;

	public DeletedRow(int row, String[] rowdata) {
	    this.row = row;
	    this.rowdata = rowdata;
	}
    }

    public void deleteSelectedRow() {
	int[] rows = jt.getSelectedRows();
	DefaultTableModel model = (DefaultTableModel) jt.getModel();
	ArrayList<DeletedRow> al = new ArrayList();
	for (int j = 0; j < rows.length; j++) {
	    int row = rows[j];
	    if (0 > row || jt.getRowCount() <= row) {
		continue;
	    }
	    String[] rowdata = (String[]) ((Vector) ((Vector) model.getDataVector()).elementAt(row)).toArray(new String[0]);
	    al.add(new DeletedRow(row, rowdata));
	}
	for (int j = rows.length - 1; j >= 0; j--) {
	    int row = rows[j];
	    if (0 > row || jt.getRowCount() <= row) {
		continue;
	    }
	    model.removeRow(row);
	}
	undoManager.addEdit(new DeleteRowEdit(al));
    }

    private class Cell {

	public int r, c;
	public String value;

	public Cell(int r, int c, String value) {
	    this.r = r;
	    this.c = c;
	    this.value = value;
	}
    }

    private List pasteChar(DefaultTableModel model, int row, int column, final char[] a, final char separator) {
	IsOnGoingUndoRedo = true;
	ArrayList<Cell> previous = new ArrayList<Cell>();

	final int nr = jt.getRowCount();
	final int nc = jt.getColumnCount();
	char[] stack = new char[a.length];
	int sp = 0, i = 0, j = 0;
	boolean escaped = true, quoated = false;
	for (int k = 0; k < a.length; k++) {
	    if (separator == a[k]) {
		if (quoated) {
		    stack[sp++] = a[k];
		} else {
		    if (nc > column + j && nr > row + i) {
			int r = jt.convertRowIndexToModel(row + i);
			int c = jt.convertColumnIndexToModel(column + j);
			previous.add(new Cell(r, c, (String) model.getValueAt(r, c)));
			model.setValueAt(new String(stack, 0, sp), r, c);
		    }
		    sp = 0;
		    j++;
		}
	    } else if ('\n' == a[k]) {
		if (nc > column + j && nr > row + i) {
		    int r = jt.convertRowIndexToModel(row + i);
		    int c = jt.convertColumnIndexToModel(column + j);
		    previous.add(new Cell(r, c, (String) model.getValueAt(r, c)));
		    model.setValueAt(new String(stack, 0, sp), r, c);
		}
		sp = 0;
		j = 0;
		i++;
	    } else if ('\r' == a[k]) {
	    } else if ('\"' == a[k]) {
		if (escaped) {
		    stack[sp++] = a[k];
		    escaped = false;
		} else {
		    quoated = !quoated;
		}
	    } else if ('\\' == a[k]) {
		if (escaped) {
		    stack[sp++] = a[k];
		}
		escaped = !escaped;
	    } else {
		stack[sp++] = a[k];
		escaped = false;
	    }
	}
	if (0 < sp && nc > column + j && nr > row + i) {
	    int r = jt.convertRowIndexToModel(row + i);
	    int c = jt.convertColumnIndexToModel(column + j);
	    previous.add(new Cell(r, c, (String) model.getValueAt(r, c)));
	    model.setValueAt(new String(stack, 0, sp), r, c);
	}
	IsOnGoingUndoRedo = false;
	return previous;
    }

    public List pasteChar(final char[] a, final char separator) {

	DefaultTableModel model = (DefaultTableModel) jt.getModel();
	int column = jt.getSelectedColumn();
	int row = jt.getSelectedRow();

	return pasteChar(model, row, column, a, separator);
    }

    private static int[] pasteSize(final char[] a, final char separator) {
	int i = 1, j = 1, max_j = 1;
	for (int n = 0; n < a.length; n++) {
	    if ('\n' == a[n]) {
		i++;
		max_j = Integer.max(max_j, j);
		j = 0;
	    } else if (separator == a[n]) {
		j++;
	    }
	}
	char e = a[a.length - 1];
	if ('\n' == e || '\r' == e) {
	    i--;
	}
	max_j = Integer.max(max_j, j);
	return new int[]{i, max_j};
    }

    private void extendSizeToPaste(final char[] a) {
	int[] size = pasteSize(a, '\t');
	int nrow = jt.getRowCount();
	int ncol = jt.getColumnCount();
	int r = jt.getSelectedRow();
	int c = jt.getSelectedColumn();
	int dc = size[1] - ncol + c;
	int dr = size[0] - nrow + r;
	String msg = null;
	ResourceBundle rb = gui.getResourceBundle();
	if (dc > 0 && dr > 0) {
	    msg = String.format(rb.getString("paste.msg.1"), dr, dc) + rb.getString("paste.msg.0");
	} else if (dc > 0) {
	    msg = String.format(rb.getString("paste.msg.2"), dc) + rb.getString("paste.msg.0");
	} else if (dr > 0) {
	    msg = String.format(rb.getString("paste.msg.3"), dr) + rb.getString("paste.msg.0");
	} else {
	    return;
	}
	if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this, msg, "表の拡張", JOptionPane.YES_NO_OPTION)) {
	    DefaultTableModel model = (DefaultTableModel) jt.getModel();
	    int[] cols = new int[Integer.max(dc, 0)];
	    for(int j = 0; j < cols.length; j++){
		cols[j] = ncol++;
	    }
	    int[] rows = new int[Integer.max(dr, 0)];
	    for(int i=0; i<rows.length; i++){
		rows[i] = nrow++;
	    }
	    insertColumn(cols);
	    insertRows(rows);
	    moveCursor(r, c);
	    undoManager.addEdit(new ExtendTableEdit(r, c, rows, cols));
	}
    }

    public void paste() {
	Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(jt);
	if (null != t) {
	    try {
		char[] a = ((String) t.getTransferData(DataFlavor.stringFlavor)).toCharArray();
		extendSizeToPaste(a);
		gui.dataChanged();
		undoManager.addEdit(new PasteEdit(a, pasteChar(a, '\t')));
	    } catch (UnsupportedFlavorException ex) {
		Logger.getLogger(Table.class.getName()).log(Level.SEVERE, null, ex);
	    } catch (IOException ex) {
		Logger.getLogger(Table.class.getName()).log(Level.SEVERE, null, ex);
	    }
	}
    }

    public void clear() {
	DefaultTableModel model = (DefaultTableModel) jt.getModel();
	int[] rows = jt.getSelectedRows();
	int[] cols = jt.getSelectedColumns();
	int[] model_rows = new int[rows.length];
	int[] model_cols = new int[cols.length];
	String[][] values = new String[rows.length][cols.length];
	IsOnGoingUndoRedo = true;
	for (int i = 0; i < rows.length; i++) {
	    int r = jt.convertRowIndexToModel(rows[i]);
	    model_rows[i] = r;
	    for (int j = 0; j < cols.length; j++) {
		int c = jt.convertColumnIndexToModel(cols[j]);
		model_cols[j] = c;
		values[i][j] = (String) model.getValueAt(r, c);
		model.setValueAt("", r, c);
	    }
	}
	IsOnGoingUndoRedo = false;
	gui.dataChanged();
	undoManager.addEdit(new ClearEdit(model_rows, model_cols, values));
    }

    public void cut() {
	DefaultTableModel model = (DefaultTableModel) jt.getModel();
	int[] rows = jt.getSelectedRows();
	int[] cols = jt.getSelectedColumns();
	int[] model_rows = new int[rows.length];
	int[] model_cols = new int[cols.length];
	String[][] values = new String[rows.length][cols.length];
	StringBuilder sb = new StringBuilder();
	IsOnGoingUndoRedo = true;
	for (int i = 0; i < rows.length; i++) {
	    int r = jt.convertRowIndexToModel(rows[i]);
	    model_rows[i] = r;
	    for (int j = 0; j < cols.length; j++) {
		int c = jt.convertColumnIndexToModel(cols[j]);
		model_cols[j] = c;
		if (0 < j) {
		    sb.append('\t');
		}
		sb.append(model.getValueAt(r, c));
		values[i][j] = (String) model.getValueAt(r, c);
		model.setValueAt("", r, c);
	    }
	    sb.append('\n');
	    StringSelection selection = new StringSelection(sb.toString());
	    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	    clipboard.setContents(selection, null);
	}
	IsOnGoingUndoRedo = false;
	gui.dataChanged();
	undoManager.addEdit(new ClearEdit(model_rows, model_cols, values));
    }

    public void copy() {
	Action copyAction = jt.getActionMap().get("copy");
	if (copyAction != null) {
	    copyAction.actionPerformed(
		    new ActionEvent(jt, ActionEvent.ACTION_PERFORMED, "copy")
	    );
	}
    }

    private void moveCursor(int targetRow, int targetColumn) {
	// 1. 指定位置を選択状態にする
	jt.changeSelection(targetRow, targetColumn, false, false);
	// 2. 移動先セルを画面内に自動スクロール表示（テーブルがJScrollPaneに入っている場合）
	jt.scrollRectToVisible(jt.getCellRect(targetRow, targetColumn, true));
    }

    public boolean isSelected() {
	int row = jt.getSelectedColumn();
	if (0 > row) {
	    return false;
	}
	int col = jt.getSelectedColumn();
	if (0 > col) {
	    return false;
	}
	return true;
    }

    public void clearSelection() {
	jt.clearSelection();
	// Undo?
    }

    public static int Pt2Pixel(double pt) {
	int dpi = Toolkit.getDefaultToolkit().getScreenResolution();
	return (int) Math.round(pt * dpi / 72.0);
    }

    private class ReplacedCell {

	public int model_r, model_c;
	public String previousText, currentText;

	public ReplacedCell(int model_r, int model_c, String previousText, String currentText) {
	    this.model_r = model_r;
	    this.model_c = model_c;
	    this.previousText = previousText;
	    this.currentText = currentText;
	}
    }

    private void search(boolean IsReplace, boolean IsHorizonalMove, boolean IsBackward, boolean IsConfirming) {
	IsOnGoingUndoRedo = true;
	ArrayList<ReplacedCell> cells = new ArrayList<ReplacedCell>();

	DefaultTableModel model = (DefaultTableModel) jt.getModel();
	String target = search_text.getText();
	String replaced = replace_text.getText();
	Pattern p = Pattern.compile(target);

	int ncol = jt.getColumnCount();
	int nrow = jt.getRowCount();
	int c = jt.getSelectedColumn();
	int r = jt.getSelectedRow();
	int start_c = c, start_r = r;
	int n_cells = ncol * nrow; // 検索対象となるセルの数
	int n_replaced = 0, n_searched = 0;

	for (int n = 0; n < n_cells; n++) {
	    int mr = jt.convertRowIndexToModel(r);
	    int mc = jt.convertColumnIndexToModel(c);
	    String text = (String) model.getValueAt(mr, mc);
	    Matcher m = p.matcher(text);
	    if (m.find()) {
		n_searched++;
		if (IsReplace) {
		    if (IsConfirming && (start_c != c || start_r != r)) {
			moveCursor(r, c);
			break;
		    }
		    String text2 = m.replaceAll(replaced);
		    cells.add(new ReplacedCell(mr, mc, text, text2));
		    n_replaced++;
		    model.setValueAt(text2, mr, mc);
		} else {
		    if (start_c != c || start_r != r) {
			moveCursor(r, c);
			break;
		    }
		}
	    }
	    if (IsBackward) {
		if (IsHorizonalMove) {
		    if (0 > --c) {
			c = ncol - 1;
			if (0 > --r) {
			    r = nrow - 1;
			}
		    }
		} else {
		    if (0 > --r) {
			r = nrow - 1;
			if (0 > --c) {
			    c = ncol - 1;
			}
		    }
		}
	    } else {
		if (IsHorizonalMove) {
		    if (ncol <= ++c) {
			c = 0;
			if (nrow <= ++r) {
			    r = 0;
			}
		    }
		} else {
		    if (nrow <= ++r) {
			r = 0;
			if (ncol <= ++c) {
			    c = 0;
			}
		    }
		}
	    }
	}

	IsOnGoingUndoRedo = false;
	if (0 < n_replaced) {
	    gui.dataChanged();
	    undoManager.addEdit(new ReplaceEdit(cells));
	}
    }
}
