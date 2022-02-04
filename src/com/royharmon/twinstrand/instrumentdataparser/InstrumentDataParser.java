/**
 * 
 */
package com.royharmon.twinstrand.instrumentdataparser;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The InstrumentDataParser is a CLI tool that accepts a list of directories to monitor 
 * for new CSV files. When such a file is found, its data is extracted and loaded into 
 * a relational database. 
 * The InstrumentDataParser class accepts directories as command-line arguments and passes
 * the full list of directories to the DataMonitor that watches those directories for new
 * files. When those files are found, the data is extracted and inserted into a database.
 * 
 * @author Roy Harmon
 * @version 1.0
 * @since	2022-01-31
 *	
 */
public class InstrumentDataParser {

	protected static final String ERROR_ACCESSING_FILE = "Error accessing file {}.";
	static Logger logger = Logger.getLogger(InstrumentDataParser.class.getSimpleName());

	/**
	 * This main method determines the directories to monitor based on provided arguments.
	 * To specify directories, use the "-d <path\to\directory>" option.
	 * To use a text file containing a list of directories, use the "-f <file-path.txt>" option.
	 * To specify multiple directories or text files, use the "-d" or "-f" option 
	 * followed by the various paths, separated with spaces (e.g. "-f file1.txt file2.txt").
	 * 
	 * @param args Contains command-line arguments. 
	 */
	public static void main(String[] args) {
		final HashMap<String, List<String>> parms = new HashMap<>();
		List<String> opts = null;
		for (int i = 0; i < args.length; i++) {
			String a = args[i];
			if (a.startsWith("-")) {
				while (a.startsWith("-")) {
					a = a.replaceFirst("-", "");
				}
				if (a.isEmpty()) {
					logger.log(Level.SEVERE, "Argument {} is invalid.", i);
					return;
				}
				opts = new ArrayList<>();
				parms.put(a, opts);
			} else if (opts != null) {
				opts.add(a);
			} else {
				logger.log(Level.SEVERE, "Argument {} is invalid.", i);
				return;
			}
		}
		// All arguments have been parsed to the parms HashMap. 
		// Create a unified list of directories from the input provided.
		List<String> folderList = combineFolderLists(parms);
		// Use the DataMonitor class to register a WatchService for all of the folder paths.
		DataMonitor.main(folderList.toArray(new String[0]));
	}
	
	

	/**
	 * @param parms -HashMap whose keys are options and whose values are arguments for those options.
	 * @return List<String> combining folder paths from arguments with folder paths from files given as arguments.
	 */
	private static List<String> combineFolderLists(final HashMap<String, List<String>> parms) {
		List<String> dirList = new ArrayList<>();
		if (parms.containsKey("d")) {
			for (String filePath : parms.get("d")) {
				dirList.add(filePath);
			}
		}
		if (parms.containsKey("f")) {
			// Read directory list(s) from parms.
			for (String filePath : parms.get("f")) {
				File fileEntry = new File(filePath);
				if (fileEntry.canRead()) {
					try (Scanner fileScanner = new Scanner(fileEntry)) {
						extractFolderPaths(dirList, fileScanner);
					} catch (FileNotFoundException e) {
						logger.log(Level.WARNING, ERROR_ACCESSING_FILE, filePath);
						e.printStackTrace();
					}
				}
			}
		}
		return dirList;
	}

	/**
	 * @param dirList -List of strings containing directories to monitor
	 * @param fileScanner -Scanner that returns directory paths to add to the list
	 * @throws FileNotFoundException when path list contains entries that are neither folders nor files containing lists of folders
	 */
	private static void extractFolderPaths(List<String> dirList, Scanner fileScanner) throws FileNotFoundException {
		while (fileScanner.hasNextLine()) {
			String listFile = fileScanner.nextLine();
			File filePath = new File(listFile);
			if (filePath.isDirectory()) {
				dirList.add(listFile);
			} else if (filePath.canRead()) {
				try (Scanner scanner = new Scanner(filePath)) {
					extractFolderPaths(dirList, scanner);
				} catch (FileNotFoundException e) {
					logger.log(Level.WARNING, ERROR_ACCESSING_FILE, listFile);
					e.printStackTrace();
				}
			} else {
				logger.log(Level.WARNING, ERROR_ACCESSING_FILE, listFile);
				throw new FileNotFoundException();
			}
		}
	}

}
