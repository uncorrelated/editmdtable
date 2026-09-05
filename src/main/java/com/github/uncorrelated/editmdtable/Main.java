package com.github.uncorrelated.editmdtable;

import java.io.File;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {

	for(int i = 0; i < args.length; i++){
	    File f = new File(args[i]);
	    if(!f.exists()){
		System.err.println(f.getAbsolutePath() + " doesn't exist.");
		System.exit(-1);
	    }
	    if(!f.isFile()){
		System.err.println(f.getAbsolutePath() + " is not a file.");
		System.exit(-2);
	    }
	    if(!f.canRead()){
		System.err.println(f.getAbsolutePath() + " is not readable.");
		System.exit(-3);
	    }
	}

	javax.swing.SwingUtilities.invokeLater(new Runnable() {
	    public void run() {
		if (args.length < 1) {
		    GUI gui = new GUI();
		} else {
		    for (int i = 0; i < args.length && i < 3; i++) {
			GUI gui = new GUI(args[i]);
		    }
		}
	    }
	});
    }
}
