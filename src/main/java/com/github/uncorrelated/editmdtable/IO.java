package com.github.uncorrelated.editmdtable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Iterator;

public class IO {

    private RandomAccessFile mdFileHandle = null, tmpFileHandle = null;
    private ArrayList<Chunk> chunkList = null;
    private File mdFile = null, tmpFile = null;

    public IO(String fname) throws IOException {
	open(new File(fname));
    }

    private static String getPrefix(String fname) {
	int index = fname.lastIndexOf('.');
	if (0 >= index) {
	    return fname;
	}
	return fname.substring(0, index - 1);
    }

    public void open(File file) throws IOException {
	if (file.exists()) {
	    if (file.isFile()) {
		mdFile = file;
		try {
		    tmpFile = File.createTempFile(getPrefix(file.getName()), ".tmp");
		    tmpFile.deleteOnExit();
		    tmpFileHandle = new RandomAccessFile(tmpFile, "rw");
		} catch (IOException e) {
		    // 一時ファイルをオープンできなくてもリードはする
		    tmpFileHandle = null;
		}
		mdFileHandle = new RandomAccessFile(file, "rw");
		chunkList = listChunk(mdFileHandle, tmpFileHandle);
	    } else {
		throw new java.io.IOException(file.getName() + " isn't a file.");
	    }
	} else {
	    throw new java.io.FileNotFoundException(file.getName() + " doesn't exist.");
	}
    }

    public int[] listTable() {
	if (null == mdFileHandle || null == chunkList) {
	    return null;
	}
	Iterator<Chunk> it = chunkList.iterator();
	int[] r = new int[chunkList.size()];
	int index = 0, not = 0;
	while (it.hasNext()) {
	    Chunk c = it.next();
	    if (c.IsTable()) {
		r[not++] = index;
	    }
	    index++;
	}
	int[] s = new int[not];
	System.arraycopy(r, 0, s, 0, not);
	return s;
    }

    public String[][] readTable(int index) throws IOException {
	if (null == mdFileHandle || null == chunkList || chunkList.size() <= index) {
	    return null;
	}
	Chunk c = chunkList.get(index);
	if (!c.IsTable()) {
	    return null;
	}
	// 可能な限り、一時ファイルに退避したデータを読む
	if (null == tmpFileHandle) {
	    return readTable(mdFileHandle, c);
	}
	return readTable(tmpFileHandle, c);
    }

    public void update(int index, String[][] newdata) {
	Chunk c = chunkList.get(index);
	if (c.IsTable()) {
	    c.setTable(newdata);
	}
    }

    public void add(String[][] newdata) {
	Chunk c = chunkList.get(chunkList.size() - 1);
	Chunk a = new Chunk(c.ep() + 1, -1, true, "");
	a.setTable(newdata);
	chunkList.add(a);
    }

    private void copyChunk(RandomAccessFile input_fh, long sp, long ep, RandomAccessFile output_fh) throws IOException {
	input_fh.seek(sp);
	byte[] b = new byte[BUFSIZE];
	long cp = sp;
	while (cp < ep) {
	    int len = input_fh.read(b, 0, (int) Long.min(ep - cp + 1, BUFSIZE));
	    cp += len;
	    output_fh.write(b, 0, len);
	}
    }

    public void write() throws IOException {
	if (null == tmpFileHandle) {
	    throw new IOException("No temporary file!");
	}
	mdFileHandle.setLength(0);
	mdFileHandle.seek(0);
	Iterator<Chunk> it = chunkList.iterator();
	while (it.hasNext()) {
	    Chunk c = it.next();
	    if (!c.IsTable() || null == c.getTable()) {
		copyChunk(tmpFileHandle, c.sp(), c.ep(), mdFileHandle);
	    } else {
		mdFileHandle.write(c.toByteArray());
	    }
	}
    }

    public String getLastHeading(int index) {
	String r = chunkList.get(index).heading();
	return null == r ? "" : r;
    }

    public void close() throws IOException {
	if (null != mdFileHandle) {
	    mdFileHandle.close();
	}
	if (null != tmpFileHandle) {
	    tmpFileHandle.close();
	}
    }

    private static final int BUFSIZE = 1024;
    private static final int SIZE_OF_HEADING = 128;

    public static ArrayList<Chunk> listChunk(RandomAccessFile raf, RandomAccessFile tmpfh) throws IOException {
	byte[] b = new byte[BUFSIZE];
	ArrayList<Chunk> al = new ArrayList();
	int clen = 0; // Chunkの長さ
	long sp = 0, ep = 0, fp;
	boolean is_table = false, is_line_head = true, is_heading = false, is_inline_block = false;
	byte[] last_heading = new byte[SIZE_OF_HEADING];
	int last_heading_char_num = 0, num_of_sharp = 0;
	int num_of_backquote = 0, num_of_backquote_begin = 0;

	while (raf.length() > (fp = raf.getFilePointer())) {
	    int len = raf.read(b);
	    if (null != tmpfh) {
		// 読み込んだファイルの中身は一時ファイルにコピーしておく
		tmpfh.write(b);
	    }
	    for (int i = 0; i < len; i++) {
		if (is_line_head) {
		    if ('|' == b[i] && !is_inline_block) {
			if (!is_table && 0 < fp + i) {
			    ep = sp + clen - 1;
			    al.add(new Chunk(sp, ep, is_table, null));
			    clen = 0;
			    sp = ep + 1;
			}
			is_table = true;
		    } else if ('#' == b[i] && !is_inline_block) {
			num_of_sharp = 1;
		    } else if ('`' == b[i]) {
			num_of_backquote = 1;
		    } else if ('\r' != b[i]) {
			if (is_table) {
			    ep = sp + clen - 1;
			    clen = 0;
			    if ('\n' == b[i]) {
				ep--;
				clen = 1;
			    }
			    al.add(new Chunk(sp, ep, is_table, new String(last_heading, 0, last_heading_char_num)));
			    sp = ep + 1;
			}
			is_table = false;
		    }
		    is_line_head = false;
		} else if (1 <= num_of_sharp && '#' == b[i]) {
		    num_of_sharp++;
		} else if (1 <= num_of_backquote) {
		    if ('`' == b[i]) {
			num_of_backquote++;
		    } else {
			if (3 <= num_of_backquote) {
			    if (is_inline_block) {
				if (num_of_backquote_begin == num_of_backquote) {
				    is_inline_block = false;
				}
			    } else {
				num_of_backquote_begin = num_of_backquote;
				is_inline_block = true;
			    }
			}
			num_of_backquote = 0;
		    }
		} else if (' ' == b[i] && 1 <= num_of_sharp && 3 >= num_of_sharp) {
		    num_of_sharp = 0;
		    is_heading = true;
		    last_heading_char_num = 0;
		} else if (is_heading && last_heading_char_num < SIZE_OF_HEADING) {
		    last_heading[last_heading_char_num++] = b[i];
		}
		if ('\n' == b[i]) {
		    is_line_head = true;
		    is_heading = false;
		}
		clen++;
	    }
	}
	if (0 < clen) {
	    ep = sp + clen - 1;
	    al.add(new Chunk(sp, ep, is_table, !is_table ? null : new String(last_heading, 0, last_heading_char_num)));
	}
	return al;
    }

    public static String readAsString(RandomAccessFile raf, long offset, int len) throws IOException {
	byte[] b = new byte[len];
	raf.read(b, 0, len);
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	out.write(b, 0, len);
	return out.toString();
    }

    public static String[][] readTable(RandomAccessFile raf, Chunk c) throws IOException {
	int len = (int) (c.ep() - c.sp() + 1);
	raf.seek(c.sp());
	String text = readAsString(raf, 0, len);
	char[] ca = text.toCharArray();
	int nr = 0, nc = 0, max_l = 0;
	int j = 0, l = 0;
	boolean escaped = false, is_head = true, is_in_backquote = false;
	// 縦横のセルサイズを計算
	j = l = 0;
	for (int n = 0; n < ca.length; n++) {
	    if (escaped) {
		escaped = false;
		continue;
	    }
	    if ('\\' == ca[n]) {
		escaped = true;
	    } else if ('|' == ca[n] && !is_in_backquote) {
		if (nc < ++j) {
		    nc++;
		}
		l = 0;
	    } else if ('\n' == ca[n]) {
		if (0 < l) {
		    if (nc < ++j) {
			nc++;
		    }
		    l = 0;
		}
		j = 0;
		is_in_backquote = false;
		nr++;
	    } else if ('\r' != ca[n]) {
		if ('`' == ca[n]) {
		    is_in_backquote = !is_in_backquote;
		}
		if (max_l < ++l) {
		    max_l = l;
		}
	    }
	}
	if (0 < ca.length) {
	    nc--; // 行頭の | の分はのぞいて | の数を列数にする
	    if (0 < l) {
		nc++; // 最後の行の行末が | で終わらないときは足す
	    }
	    /*
	     最後の行は \n がないので、改行の数は行数にならない
	     データ的には入らないはずだが、末尾に改行がついているだけのときは処理しない
	     */
	    if (0 < j) {
		nr++;
	    }
	}
	// 文字列型の配列に値を入れていく
	char[] cell = new char[max_l];
	String[][] r = new String[nc][nr];
	j = l = 0;
	escaped = is_in_backquote = false;
	for (int n = 0, i = 0; n < ca.length; n++) {
	    if (escaped) {
		escaped = false;
		cell[l++] = ca[n];
		continue;
	    }
	    if ('\\' == ca[n]) {
		escaped = true;
		continue;
	    }
	    if ('`' == ca[n]) {
		is_in_backquote = !is_in_backquote;
	    }
	    if ('|' == ca[n] && !is_in_backquote) {
		if (!is_head) {
		    r[j++][i] = new String(cell, 0, l);
		    l = 0;
		}
		continue;
	    }
	    if ('\n' == ca[n]) {
		if (0 < l) {
		    r[j++][i] = new String(cell, 0, l);
		    l = 0;
		}
		while (j < nc) {
		    r[j++][i] = "";
		}
		j = 0;
		i++;
		is_head = true;
		is_in_backquote = false;
		continue;
	    }
	    if ('\r' != ca[n]) {
		cell[l++] = ca[n];
		is_head = false;
	    }
	}
	if (0 < l) {
	    r[j++][nr - 1] = new String(cell, 0, l);
	}
	if (0 < j) {
	    while (j < nc) {
		r[j++][nr - 1] = "";
	    }
	}
	return r;
    }

    public static void read(File file) {
	if (file.exists()) {
	    if (file.isFile()) {
		try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
		    ArrayList<Chunk> al = listChunk(raf, null);
		    Iterator<Chunk> it = al.iterator();
		    while (it.hasNext()) {
			Chunk c = it.next();
			int len = (int) (c.ep() - c.sp() + 1);
			System.out.println("sp: " + c.sp() + " ep: " + c.ep() + " Table?: "
				+ c.IsTable());
			if (c.IsTable()) {
			    String[][] r = readTable(raf, c);
			    for (int i = 0; i < r[0].length; i++) {
				for (int j = 0; j < r.length; j++) {
				    System.out.println("(" + i + "," + j + ") " + r[j][i]);
				}
			    }
			    c.setTable(r);
			    System.out.println(c);
//			    System.out.println(readAsString(raf, 0, len) + "\n - - - ");
			}
		    }
		} catch (IOException e) {
		    System.err.println("Couldn't open/read the file: " + file);
		}
	    } else {
		System.err.println(file.getName() + " isn't a file.");
	    }
	} else {
	    System.err.println(file.getName() + " doesn't exist.");
	}
    }

    public static void read(String fname) {
	read(new File(fname));
    }
}
