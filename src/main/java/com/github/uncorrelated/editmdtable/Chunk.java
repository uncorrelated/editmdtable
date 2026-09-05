package com.github.uncorrelated.editmdtable;

import java.io.ByteArrayOutputStream;

/*
    ファイル中の非テーブルのテキストブロックと、テーブルのテキストブロックの位置を保持する
 */
public final class Chunk {

    private long sp, ep;
    private boolean IsTable;
    private String[][] table = null;
    private String heading = null;

    public Chunk(long sp, long ep, boolean IsTable, String heading) {
	this.sp = sp;
	this.ep = ep;
	this.IsTable = IsTable;
	this.heading = heading;
    }

    public long sp() {
	return sp;
    }

    public long ep() {
	return ep;
    }

    public boolean IsTable() {
	return IsTable;
    }

    public String heading() {
	return heading;
    }

    public void setTable(String[][] table) {
	this.table = table;
    }

    public String[][] getTable() {
	return table;
    }

    private static final void writeEscaped(ByteArrayOutputStream out, byte[] b) {
	byte[] stack = new byte[2 * b.length];
	int sp = 0;
	boolean is_in_backquote = false;
	for (int i = 0; i < b.length; i++) {
	    switch (b[i]) {
		case '`':
		    is_in_backquote = !is_in_backquote;
		    stack[sp++] = b[i];
		    break;
		case '\\':
		case '|':
		    if (!is_in_backquote) {
			stack[sp++] = '\\';
		    }
		default:
		    stack[sp++] = b[i];
		    break;
	    }
	}
	if (is_in_backquote) {
	    stack[sp++] = '`';
	}
	out.write(stack, 0, sp);
    }

    private static final byte[] lf = "\n".getBytes();
    private static final byte[] vbar = "|".getBytes();

    private ByteArrayOutputStream toByteArrayOutputStream() {
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	for (int i = 0; i < table[0].length; i++) {
	    if(0 < i)
		out.write(lf, 0, lf.length);
	    for (int j = 0; j < table.length; j++) {
		out.write(vbar, 0, vbar.length);
		writeEscaped(out, table[j][i].getBytes());
	    }
	    out.write(vbar, 0, vbar.length);
	}
	ep = sp + out.size();
	return out;
    }

    public byte[] toByteArray() {
	ByteArrayOutputStream out = toByteArrayOutputStream();
	return out.toByteArray();
    }

    @Override
    public String toString() {
	if (!IsTable || null == table) {
	    return null;
	}
	ByteArrayOutputStream out = toByteArrayOutputStream();
	return out.toString();
    }
}
