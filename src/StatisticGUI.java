/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
 
/*
 * Table example snippet: place arbitrary controls in a table
 *
 * For a list of all SWT example snippets see
 * http://www.eclipse.org/swt/snippets/
 */
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.*;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

class FileStatistics {
	String fileName;
	String comment;
	ChordAnalysis chordAnalysis;
	
	Map<ChordMark, Integer> chordMarkLengths = new HashMap<ChordMark, Integer>();
	
	private ChordMark getRandomChordMark() {
		int tone = (int) (Math.random() * 12);
		Chord chord = Chord.values()[(int) (Math.random() * Chord.values().length)];
		
		return new ChordMark(chord, tone);
	}
	
	public Map<Chord, Integer> getChordLengths() {
		Map<Chord, Integer> chordLengths = new HashMap<Chord, Integer>();
		for (ChordMark chM: chordMarkLengths.keySet()) {
			Chord ch = chM.getChord();
			
			if(chM == null || ch == null) {
				continue;
			}
			//System.out.println(chM);
			
			boolean containsKey = chordLengths.containsKey(ch);
			int chCount = containsKey ? (chordLengths.get(ch) + 1) : 1; 
			chordLengths.put(ch, chCount);
		}
		
		return chordLengths;
	}
	
	public Map<Integer, Integer> getChordMarkLengths(Chord chord) {
		Map<Integer, Integer> chordLengths = new HashMap<Integer, Integer>();
		for (ChordMark chM : chordMarkLengths.keySet()) {
			if(chM.getChord() != null && chM.getChord().equals(chord)) {
				int midiNum = chM.getMidiNum();
				
				boolean containsKey = chordLengths.containsKey(midiNum);
				int chMlength = chordMarkLengths.get(chM);
				int chCount = containsKey ? (chordLengths.get(midiNum) + chMlength) : 1;
				chordLengths.put(midiNum, chCount);	
			}			
		}
		
		return chordLengths;
	}
	
	public long chordPercentage(Chord chord) {
		int sum = 0;
		
		Map<Chord, Integer> chordLengths = getChordLengths();
		for (Chord ch: chordLengths.keySet()) {
			sum += chordLengths.get(ch);
		}
		
		if(chordLengths.get(chord) == null) {
			return 0;
		}
		
		return Math.round((( (double) chordLengths.get(chord) ) / sum ) * 100);
	}
	
	private void fillChordMarkLengths() {
		int count = 100;
		
		for (int i = 0; i < count; i++) {
			ChordMark chM = getRandomChordMark();
			
			boolean containsKey = chordMarkLengths.containsKey(chM);
			int chCount = containsKey ? (chordMarkLengths.get(chM) + 1) : 1; 
			
			chordMarkLengths.put(chM, chCount);
		}
	}
	
	public void init(String fileName) {
		chordAnalysis = new ChordAnalysis(fileName);
		chordAnalysis.process();
		
		chordMarkLengths.clear();
		
		//System.out.println(chAnalysis.song.beats.size());
		
		for(Beat b: chordAnalysis.song.beats) {
			//String line = "[" + (b.measure+1) + "] " + (b.orderInSong+1) + ": " + b.chordMark + ' ' + b.getScale() + "\n";
			//System.out.print(line);
			
			ChordMark chM = b.getChordMark();
			
			if(chM == null) {
				continue;
			}
			
			boolean containsKey = chordMarkLengths.containsKey(chM);
			int chCount = containsKey ? (chordMarkLengths.get(chM) + 1) : 1; 
			
			chordMarkLengths.put(chM, chCount);
		}
	}
	
	public FileStatistics(String fileName) {
		this.fileName = fileName;
		
		String[] fSplits = fileName.split("/"); 
		this.comment = fSplits[fSplits.length - 1];
		
		// tu sa to sprocesuje
		this.init(fileName);
		//fillChordMarkLengths();
		
		//System.out.println(toCSV());
	}
	
	public String getFileName() {
		return fileName;
	}
	
	public static String getCSVheader() {
		List<String> headers = new ArrayList<String>();
		headers.add("File name");
		headers.add("Comment");
		headers.add("Tone");
		for (int i = 0; i < Chord.values().length; i++) {
			headers.add(Chord.values()[i].toString());
		}
		
		return getJoinedStringList(headers, ",");
	}
	
	public String toCSV() {
		List<String> values = new ArrayList<String>();
		values.add(fileName);
		values.add(comment);
		values.add("SUM");
		
		
		Map<Chord, Integer> chordLenghts = getChordLengths();
		for (int i = 0; i < Chord.values().length; i++) {
			Chord curChord = Chord.values()[i];
			values.add(chordLenghts.get(curChord) + " (" +  chordPercentage(curChord) + "%)");
		}
		
		String result = getJoinedStringList(values, ",");
		
		for(int midiNum=0; midiNum<12; midiNum++) {
			values.clear();
			values.add(fileName);
			values.add(comment);
			values.add(MidiReader.NOTE_NAMES[midiNum]);
			
			for (int i = 0; i < Chord.values().length; i++) {
				
				Chord curChord = Chord.values()[i];
				Map<Integer,Integer> chMarkLengths = getChordMarkLengths(curChord);
				Integer length = chMarkLengths.get(midiNum);
				
				if(length == null) {
					length = 0;
				}
				
				values.add(length.toString());
			}
			result += "\n" + getJoinedStringList(values, ",");
		}
		
		
		
		return result;			
	}
	
	public static String getJoinedStringList(List list, String delimiter) {
		if(list == null) return "";
		StringBuilder sb = new StringBuilder();
		
		for (Iterator iterator = list.iterator(); iterator.hasNext();) {
			Object object = (Object) iterator.next();
			
			sb.append(object.toString());
			if(iterator.hasNext()) {
				sb.append(delimiter);
			}
		}
		
		return sb.toString();
	}
	
	
}


public class StatisticGUI {
public static void main(String[] args) {
	Display display = new Display ();
	final Shell shell = new Shell (display);
	shell.setText("Chord analysis batch");
	
    Menu m = new Menu(shell, SWT.BAR);
    // create a file menu and add an exit item
    final MenuItem file = new MenuItem(m, SWT.CASCADE);
    file.setText("&File");
    final Menu filemenu = new Menu(shell, SWT.DROP_DOWN);
    file.setMenu(filemenu);
    final MenuItem saveCSVItem = new MenuItem(filemenu, SWT.PUSH);
    saveCSVItem.setText("&Save as CSV\tCTRL+S");
    saveCSVItem.setAccelerator(SWT.CTRL + 'S');
    final MenuItem saveAnalysisItem = new MenuItem(filemenu, SWT.PUSH);
    saveAnalysisItem.setText("&Save as Analysis\tCTRL+D");
    saveAnalysisItem.setAccelerator(SWT.CTRL + 'D');
    final MenuItem separator = new MenuItem(filemenu, SWT.SEPARATOR);
    final MenuItem exitItem = new MenuItem(filemenu, SWT.PUSH);
    exitItem.setText("E&xit");
    
    exitItem.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
          MessageBox messageBox = new MessageBox(shell, SWT.ICON_QUESTION
              | SWT.YES | SWT.NO);
          messageBox.setMessage("Do you really want to exit?");
          messageBox.setText("Exiting Application");
          int response = messageBox.open();
          if (response == SWT.YES)
            System.exit(0);
        }
      });
    

    
    shell.setMenuBar(m);
	
	FileDialog fd = new FileDialog(shell, SWT.MULTI);
    fd.setText("Open");
    fd.setFilterPath(".");
    String[] filterExt = { "*.mid", "*.midi" };
    fd.setFilterExtensions(filterExt);
    fd.open();
    
    String path = fd.getFilterPath();
    String[] fileNames = fd.getFileNames();
	
    if(fileNames == null || fileNames.length == 0) {
    	return;
    }
    
	final List<FileStatistics> fileStatisticsList = new LinkedList<FileStatistics>();
	for (String fileName : fileNames) {
		fileStatisticsList.add(new FileStatistics(path + "/" + fileName));
	}
	
	

	
	shell.setLayout(new GridLayout());
	Table table = new Table (shell, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);
	table.setLinesVisible (true);
	table.setHeaderVisible (true);
	GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
	data.heightHint = 200;
	table.setLayoutData(data);
	String[] titles = new String[Chord.values().length + 2];
	
	titles[0] = "File name";
	titles[1] = "Comment";
	
	for (int i = 2; i < titles.length; i++) {
		titles[i] = Chord.values()[i-2].toString();
	}
	
	for (int i=0; i<titles.length; i++) {
		TableColumn column = new TableColumn (table, SWT.NONE);
		column.setText (titles [i]);
	}	
	int count = fileStatisticsList.size();
	for (int i=0; i<count; i++) {
		FileStatistics curFs = fileStatisticsList.get(i);
		
		TableItem item = new TableItem (table, SWT.NONE);
		item.setText (0, curFs.fileName);
		
		TableEditor editor = new TableEditor (table);
		editor.grabHorizontal = true;
		Text text = new Text (table, SWT.NONE);
		text.setText(curFs.comment);
		editor.setEditor(text, item, 1);
		
		
		Map<Chord, Integer> chordLenghts = curFs.getChordLengths();
				
		//item.setText (1, fileStatisticsList.get(i).comment);
		/*item.setText (2, "!");
		item.setText (3, "this stuff behaves the way I expect");
		item.setText (4, "almost everywhere");
		item.setText (5, "some.folder");
		item.setText (6, "line " + i + " in nowhere");*/
		for (int j=2; j<titles.length; j++) {
			//item.setText (j, "" + (int)(Math.random() * 100));
			
			Tree tree = new Tree (table, SWT.BORDER);
			TreeItem iItem = new TreeItem (tree, 0);
			
			Chord curChord = Chord.valueOf(titles[j]);
			
			iItem.setText ("Total: " + chordLenghts.get(curChord) + " (" +  curFs.chordPercentage(curChord) + "%)");
			
			Map<Integer,Integer> chMarkLengths = curFs.getChordMarkLengths(curChord);
			
			for (Integer midiNum : chMarkLengths.keySet()) {
				//System.out.println("CH  : " + curChord);
				TreeItem jItem = new TreeItem (iItem, 0);
				Integer length = chMarkLengths.get(midiNum);
				jItem.setText (MidiReader.NOTE_NAMES[midiNum] + ": " + length);
			}
							
			//TreeItem jItem = new TreeItem (iItem, 0);
			//jItem.setText ("Total: " + chordLenghts.get(Chord.valueOf(titles[j])));
			/*for (int k=0; k<4; k++) {
				TreeItem kItem = new TreeItem (jItem, 0);
				kItem.setText ("TreeItem (2) -" + k);
				for (int l=0; l<4; l++) {
					TreeItem lItem = new TreeItem (kItem, 0);
					lItem.setText ("TreeItem (3) -" + l);
				}
			}*/
			
			TableEditor editorChord = new TableEditor (table);
			editorChord.grabHorizontal = true;
			//Text textChord = new Text (table, SWT.NONE);
			//textChord.setText("a");
			editorChord.setEditor(tree, item, j);
			
			/*Text text2 = new Text (table, SWT.NONE);
			text2.setText("a");
			editor.setEditor(text2, item, j);*/
			
			// resize the row height using a MeasureItem listener
			table.addListener(SWT.MeasureItem, new Listener() {
			   public void handleEvent(Event event) {
			      // height cannot be per row so simply set
			      event.height = 67;
			      event.width = 120;
			   }
			});
			
		}	
	}
	for (int i=0; i<titles.length; i++) {
		table.getColumn (i).pack ();
	}	
	
    saveCSVItem.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
        	System.out.println(FileStatistics.getCSVheader());
        	for (FileStatistics fs : fileStatisticsList) {
        		System.out.println(fs.toCSV());	
			}
        	
          }
        });
    
    saveAnalysisItem.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
        	for (Iterator<FileStatistics> iterator = fileStatisticsList.iterator(); iterator.hasNext();) {
				FileStatistics fs = (FileStatistics) iterator.next();
				
				String fileName = null;
				try {
        			fileName = fs.fileName + ".txt";
        			BufferedWriter out = new BufferedWriter(new FileWriter(fileName));
        			System.out.print("Processing file: " + fileName + " ...");
        			
        			int i=0;
        			for(Beat b: fs.chordAnalysis.song.beats) {
        				String line = i + ": " + b.chordMark + "\r\n";
        				out.write(line);
        				i++;
        			}
        			        			
                    out.close();
                    System.out.println("done");
                } catch (IOException ex) {
                	System.err.println("Unable to save file: " + fileName);
                }
        	}
          }
        });
	
	shell.pack ();
	shell.open ();
	while (!shell.isDisposed ()) {
		if (!display.readAndDispatch ()) display.sleep ();
	}
	display.dispose ();
}
}
