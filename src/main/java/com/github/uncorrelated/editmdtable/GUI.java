package com.github.uncorrelated.editmdtable;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputListener;
import javax.swing.filechooser.FileFilter;

public class GUI extends JFrame implements MouseInputListener, WindowListener, DropTargetListener {

    private static final ResourceBundle rb = ResourceBundle.getBundle("com.github.uncorrelated.editmdtable.GUI");
    private JTextField jtf = new JTextField();
    private final JTabbedPane jtp = new JTabbedPane();
    private DirectionToAddDialog add_dialog = null;
    private DirectionToDeleteDialog delete_dialog = null;
    private ColumnPropertiesDialog cp_dialog = null;
    private JMenu[] jm;
    private JMenuItem jmi_save;
    private JButton btn_save;
    private IO io;
    private int[] table_indexes;
    private boolean IsUpdate = false;
    private volatile boolean IsDoingIO = false;
    private JPopupMenu popup_menu = new JPopupMenu();

    public GUI() {
	super(rb.getString("application.name") + " " + rb.getString("application.version"));

	setComponentSize(this, 0.8);

	try {
	    setIconImage(ImageIO.read(this.getClass().getResource("icon.png")));
	} catch (IOException ex) {
	    Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, ex);
	}

	setLayout(new BorderLayout());

	Container tf_btn = new Container();
	JButton jb = new JButton(rb.getString("button.load"));
	jb.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		openFileChooser();
	    }
	});
	tf_btn.setLayout(new BorderLayout(4, 0));
	tf_btn.add(new JLabel(rb.getString("markdown.file")), BorderLayout.WEST);
	jtf.setEditable(false);
	tf_btn.add(jtf, BorderLayout.CENTER);
	Container tf_btn_east = new Container();
	tf_btn_east.setLayout(new FlowLayout());
	tf_btn_east.add(jb);
	tf_btn_east.add(btn_save = new JButton(rb.getString("button.save")));
	btn_save.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		save();
	    }
	});
	btn_save.setEnabled(false);
	tf_btn.add(tf_btn_east, BorderLayout.EAST);
	add(tf_btn, BorderLayout.NORTH);

	Container js_c = new Container();
	js_c.setLayout(new BorderLayout());
	js_c.add(new JLabel(rb.getString("font.size")), BorderLayout.WEST);
	JSlider js_fontsize = new JSlider(50, 250, 100);
	js_fontsize.addChangeListener(new ChangeListener() {
	    @Override
	    public void stateChanged(ChangeEvent e) {
		setFontSize(js_fontsize.getValue());
	    }
	});
	js_c.add(js_fontsize, BorderLayout.CENTER);
	add(js_c, BorderLayout.SOUTH);

	add_dialog = new DirectionToAddDialog(this, rb);
	delete_dialog = new DirectionToDeleteDialog(this, rb);
	cp_dialog = new ColumnPropertiesDialog(this, rb);

	add(jtp, BorderLayout.CENTER);

	JMenuBar jmb = new JMenuBar();
	String[] jmenu_str = new String[]{
	    "menu.file", "menu.column", "menu.row", "menu.edit", "menu.view"
	};
	for (int i = 0; i < jmenu_str.length; i++) {
	    jmenu_str[i] = rb.getString(jmenu_str[i]);
	}
	int[] jmenu_ke = new int[]{
	    KeyEvent.VK_F,
	    KeyEvent.VK_C,
	    KeyEvent.VK_R,
	    KeyEvent.VK_E,
	    KeyEvent.VK_V
	};
	jm = new JMenu[jmenu_str.length];
	for (int i = 0; i < jm.length; i++) {
	    jmb.add(jm[i] = new JMenu(jmenu_str[i]));
	    jm[i].setMnemonic(jmenu_ke[i]);
	}
	setJMenuBar(jmb);

	setUI();

	addWindowListener(this);
	moveCenter(this);
	setVisible(true);
	initialFont = getFont();

	JMenuItem jmi_load = new JMenuItem(rb.getString("button.load"));
	jmi_load.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		openFileChooser();
	    }
	});
	jm[0].add(jmi_load);
	jmi_save = new JMenuItem(rb.getString("button.save"));
	jmi_save.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		save();
	    }
	});
	jmi_save.setEnabled(false);
	jm[0].add(jmi_save);
	JMenuItem jmi_close = new JMenuItem(rb.getString("menu.close"));
	jmi_close.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		System.exit(0);
	    }
	});
	jm[0].add(jmi_close);

	JMenuItem jmi_edit = new JMenuItem(rb.getString("menu.rename"));
	jm[1].add(jmi_edit);
	jmi_edit.addActionListener((ActionEvent e) -> {
	    Table t1 = (Table) jtp.getSelectedComponent();
	    if (null != t1) {
		cp_dialog.renameSelectedColumn(t1);
	    }
	});

	JMenuItem jmi_insert_column = new JMenuItem(rb.getString("menu.insert"));
	jmi_insert_column.addActionListener((ActionEvent e) -> {
	    Table t1 = (Table) jtp.getSelectedComponent();
	    if (null != t1) {
		moveCenter(add_dialog);
		add_dialog.askDirectionToAddAColumn(t1);
	    }
	});
	jm[1].add(jmi_insert_column);

	JMenuItem jmi_delete = new JMenuItem(rb.getString("menu.delete"));
	jmi_delete.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		Table t1 = (Table) jtp.getSelectedComponent();
		if (null != t1) {
		    t1.deleteSelectedColumn();
		    t1.clearSelection();
		}
	    }
	});
	jm[1].add(jmi_delete);

	// メニューバーには登録せず、あとでポップアップで使う
	JMenuItem jmi_insert = new JMenuItem(rb.getString("menu.insert"));
	jmi_insert.addActionListener((ActionEvent e) -> {
	    Table t1 = (Table) jtp.getSelectedComponent();
	    if (null != t1) {
		moveCenter(add_dialog);
		add_dialog.askDirection(t1);
	    }
	});

	JMenuItem jmi_insert_raw = new JMenuItem(rb.getString("menu.insert"));
	jm[2].add(jmi_insert_raw);
	jmi_insert_raw.addActionListener((ActionEvent e) -> {
	    Table t1 = (Table) jtp.getSelectedComponent();
	    if (null != t1) {
		moveCenter(add_dialog);
		add_dialog.askDirectionToAddARow(t1);
	    }
	});

	JMenuItem jmi_delete_row = new JMenuItem(rb.getString("menu.delete"));
	jm[2].add(jmi_delete_row);
	jmi_delete_row.addActionListener((ActionEvent e) -> {
	    Table t1 = (Table) jtp.getSelectedComponent();
	    if (null != t1) {
		t1.deleteSelectedRow();
		t1.clearSelection();
	    }
	});

	JMenuItem jmi_undo = new JMenuItem(rb.getString("menu.undo"));
	jmi_undo.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		Table t1 = (Table) jtp.getSelectedComponent();
		if (null != t1) {
		    t1.undo();
		}
	    }
	});
	jm[3].add(jmi_undo);

	JMenuItem jmi_redo = new JMenuItem(rb.getString("menu.redo"));
	jmi_redo.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		Table t1 = (Table) jtp.getSelectedComponent();
		if (null != t1) {
		    t1.redo();
		}
	    }
	});
	jm[3].add(jmi_redo);

	JMenuItem jmi_copy = new JMenuItem(rb.getString("menu.copy"));
	jmi_copy.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		Table t1 = (Table) jtp.getSelectedComponent();
		if (null != t1) {
		    t1.copy();
		}
	    }
	});
	jm[3].add(jmi_copy);
	popup_menu.add(jmi_copy);

	JMenuItem jmi_cut = new JMenuItem(rb.getString("menu.cut"));
	jmi_cut.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		Table t1 = (Table) jtp.getSelectedComponent();
		if (null != t1) {
		    t1.cut();
		}
	    }
	});
	jm[3].add(jmi_cut);
	popup_menu.add(jmi_cut);

	JMenuItem jmi_paste = new JMenuItem(rb.getString("menu.paste"));
	jmi_paste.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(ActionEvent e) {
		Table t1 = (Table) jtp.getSelectedComponent();
		if (null != t1) {
		    t1.paste();
		}
	    }
	});
	jm[3].add(jmi_paste);
	popup_menu.add(jmi_paste);
	popup_menu.add(jmi_insert);

	JMenuItem jmi_replace = new JMenuItem(rb.getString("menu.replace"));
	jmi_replace.addActionListener((e) -> {
	    Table t1 = (Table) jtp.getSelectedComponent();
	    t1.toggleVisibilityReplaceForm();
	});
	jm[3].add(jmi_replace);

	JMenuItem jmi_filter = new JMenuItem(rb.getString("menu.filter"));
	jmi_filter.addActionListener((ActionEvent e) -> {
	    Table t1 = (Table) jtp.getSelectedComponent();
	    if (null != t1) {
		t1.toggleVisibilityFilterForm();
	    }
	});
	jm[4].add(jmi_filter);
    }

    public GUI(String fname) {
	this();
	openFile(fname);
    }

    private final Font initialFont;

    public void setFontSize(int size) {
	double fsize = ((double) size / 100) * initialFont.getSize();
	Font font = new Font(initialFont.getFamily(), initialFont.getStyle(), (int) fsize);
	FontUtils.changeFontAll(this, font);
	FontUtils.changeFontAll(add_dialog, font);
	FontUtils.changeFontAll(delete_dialog, font);
	FontUtils.changeFontAll(cp_dialog, font);
	add_dialog.pack();
	delete_dialog.pack();
	cp_dialog.pack();
	changeTabFont();
	if (null == table_indexes) {
	    return;
	}
	for (int i = 0; i < table_indexes.length; i++) {
	    Table t = (Table) jtp.getComponentAt(i);
	    t.modifyCellHeight();
	}
    }

    public ResourceBundle getResourceBundle() {
	return rb;
    }

    public void dataChanged() {
	if (!IsUpdate && null != io && io.isWritable()) {
	    IsUpdate = true;
	    btn_save.setEnabled(true);
	    jmi_save.setEnabled(true);
	}
    }

    public void setEditEnable(boolean flag) {
	jm[1].setEnabled(flag);
    }

    public void moveCenter(Component c) {
	Rectangle screen = getGraphicsConfiguration().getBounds();
	if (this == c) {
	    c.setLocation(screen.x + screen.width / 2 - c.getSize().width / 2,
		    screen.y + screen.height / 2 - c.getSize().height / 2);
	} else {
	    Point p = this.getLocation();
	    Dimension dp = this.getSize();
	    int cx = p.x + (int) (dp.width / 2);
	    int cy = p.y + (int) (dp.height / 2);
	    Dimension dc = c.getSize();
	    int x = cx - (int) (dc.width / 2);
	    int y = cy - (int) (dc.height / 2);
	    c.setLocation(x, y);
	}
    }

    private void setUI() {
	try {
	    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
	} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
	    e.printStackTrace();
	}
	SwingUtilities.updateComponentTreeUI(this);
    }

    private void setComponentSize(Component c, double coef) {
	Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	int size_w = (int) (coef * screenSize.width);
	int size_h = (int) (coef * screenSize.height);
	c.setSize(size_w, size_h);
    }

    public void openFileChooser() {
	synchronized (jtp) {
	    if (IsDoingIO) {
		return;
	    }
	    IsDoingIO = true;
	}

	JFileChooser fc = new JFileChooser() {
	    @Override
	    protected JDialog createDialog(Component parent) throws HeadlessException {
		JDialog dialog = super.createDialog(parent);
		setComponentSize(dialog, 0.7);
		moveCenter(dialog);
		Dimension d = getSize();
		dialog.setMinimumSize(new Dimension((int) (d.getWidth() / 2), (int) (d.getHeight() / 2)));
		return dialog;
	    }
	};

	fc.setFileFilter(new FileFilter() {
	    String[] exts = new String[]{"md", "qmd", "Rmd", "txt"};

	    public boolean accept(File f) {
		if (f.isDirectory()) {
		    return true;
		}
		String name = f.getName();
		int lastDot = name.lastIndexOf('.');
		if (lastDot == -1) {
		    return false;
		}
		String ext = name.substring(lastDot + 1).toLowerCase();
		for (int c = 0; c < exts.length; c++) {
		    if (0 == ext.compareToIgnoreCase(exts[c])) {
			return true;
		    }
		}
		return false;
	    }

	    public String getDescription() {
		return "Markdown Files";
	    }
	});
	int selected = fc.showOpenDialog(this);
	if (selected == JFileChooser.APPROVE_OPTION) {
	    openFile(fc.getSelectedFile().getPath());
	} else {
	    IsDoingIO = false;
	}
    }

    private void setNoUpdate() {
	SwingUtilities.invokeLater(new Runnable() {
	    public void run() {
		IsUpdate = false;
		btn_save.setEnabled(false);
		jmi_save.setEnabled(false);
	    }
	});
    }

    private void openFile(String fpath) {

	jtf.setText(fpath);
	jtp.removeAll();

	new Thread(new Runnable() {
	    @Override
	    public void run() {
		synchronized (jtp) {
		    try {
			if (null != io) {
			    io.close();
			}
			io = new IO(fpath);
			table_indexes = io.listTable();

			SwingUtilities.invokeLater(new Runnable() {
			    public void run() {
				changeTabFont();
			    }
			});

			for (int i = 0; i < table_indexes.length; i++) {
			    String[][] table = io.readTable(table_indexes[i]);
			    String heading = io.getLastHeading(table_indexes[i]);
			    SwingUtilities.invokeLater(new Runnable() {
				public void run() {
				    addTableTab(heading, table);
				}
			    });
			}

			setNoUpdate();

		    } catch (IOException ex) {
			Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, ex);
		    } finally {
			IsDoingIO = false;
		    }
		}
	    }
	}
	).start();
    }

    public void changeTabFont() {
	FontUtils.changeFontAll(jtp, this.getFont());
    }

    public void addTableTab(String heading, String[][] table) {
	Table t = new Table(this, add_dialog, delete_dialog, cp_dialog, table);
	jtp.addTab(heading, t);

	FontUtils.changeFontAll(t, this.getFont());
	t.modifyCellHeight();
    }

    public void save() {

	if (null == io || !io.isWritable()) {
	    JOptionPane.showMessageDialog(
		    null,
		    rb.getString("msg.not_writable.1"),
		    rb.getString("msg.not_writable.2"),
		    JOptionPane.ERROR_MESSAGE
	    );
	    return;
	}

	synchronized (jtp) {
	    if (IsDoingIO || !IsUpdate) {
		return;
	    }
	    IsDoingIO = true;
	}

	String[][][] array = new String[table_indexes.length][][];
	for (int i = 0; i < table_indexes.length; i++) {
	    Table t = (Table) jtp.getComponentAt(i);
	    array[i] = t.getTable();
	}

	new Thread(new Runnable() {
	    public void run() {
		synchronized (jtp) {
		    if (null == io || 0 >= jtp.getComponentCount()) {
			return;
		    }
		    for (int i = 0; i < table_indexes.length; i++) {
			io.update(table_indexes[i], array[i]);
		    }
		    try {
			io.write();
			setNoUpdate();
		    } catch (IOException ex) {
			Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, ex);
		    } finally {
			IsDoingIO = false;
		    }
		}
	    }
	}).start();
    }

    @Override
    public void windowOpened(WindowEvent e) {

    }

    @Override
    public void windowClosing(WindowEvent e) {
	try {
	    if (null != io) {
		io.close();
	    }
	} catch (IOException ex) {
	    Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, ex);
	}
	System.exit(0);
    }

    @Override
    public void windowClosed(WindowEvent e) {
    }

    @Override
    public void windowIconified(WindowEvent e) {

    }

    @Override
    public void windowDeiconified(WindowEvent e) {

    }

    @Override
    public void windowActivated(WindowEvent e) {

    }

    @Override
    public void windowDeactivated(WindowEvent e) {

    }

    private void showPopupMenu(MouseEvent e) {
	if (e.isPopupTrigger()) {
	    popup_menu.show(e.getComponent(), e.getX(), e.getY());
	}
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
	showPopupMenu(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
	showPopupMenu(e);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void dragEnter(DropTargetDragEvent dtde) {
    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {
    }

    @Override
    public void dragExit(DropTargetEvent dte) {
    }

    private DropTarget dropTarget = new DropTarget(this,
	    DnDConstants.ACTION_COPY, this, true);

    @Override
    public void drop(DropTargetDropEvent dtde) {
	if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
	    dtde.acceptDrop(DnDConstants.ACTION_COPY);
	    Transferable trans = dtde.getTransferable();
	    java.util.List files;
	    try {
		files = (java.util.List) trans
			.getTransferData(DataFlavor.javaFileListFlavor);
		Iterator it = files.iterator();
		if (it.hasNext()) {
		    openFile(((File) it.next()).getAbsolutePath());
		}
	    } catch (UnsupportedFlavorException ex) {
		Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, ex);
	    } catch (IOException ex) {
		Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, ex);
	    }
	}
    }
}
