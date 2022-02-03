/**
 * 
 */
package com.royharmon.twinstrand.instrumentdataparser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import static java.nio.file.StandardWatchEventKinds.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Roy
 *
 */
class DataMonitor {

	private static WatchService watcher;
    private static final Map<WatchKey,Path> keys = new HashMap<>();
    private static IDataAdapter dataAdapter = new SQLiteDataAdapter();
	
	private static int processFile(String filePath) {
		List<List<String>> result = new ArrayList<>();
		StringBuilder sql1 = new StringBuilder();
		sql1.append("INSERT INTO Results(");
		StringBuilder sql2 = new StringBuilder();
		sql2.append(") VALUES (");
		try (BufferedReader reader = new BufferedReader(new FileReader(filePath))){
			String[] firstLine = reader.readLine().split(",");
			for (String field : firstLine) {
				if (!sql1.toString().endsWith("(")) {
					sql1.append(", ");
					sql2.append(", ");
				}
				sql1.append("'" + field + "'");
				sql2.append("?");
			}
			sql1.append(", SourceFile");
			sql2.append(", ?)");
			String line;
			while ((line = reader.readLine()) != null) {
				String[] values = line.split(",");
				List<String> resultValues = new ArrayList<>(Arrays.asList(values));
				for (int i = resultValues.size(); i < firstLine.length; i++) {
					resultValues.add(i, "");
				}
				resultValues.add(filePath);
				result.add(resultValues);
			}
		} catch (IOException e) {
			// Surely this would have been handled already, right?
			e.printStackTrace();
			return 0;
		}
		return insertRecords(sql1.append(sql2).toString(), result);
	}
	
	private static int insertRecords(String sql, List<List<String>> records) {
		String currentPath = Paths.get("").toAbsolutePath().toString();
		return dataAdapter.insertRecords(sql, records, currentPath + "\\Results.db");
	}
	
	public static void main(String[] args) {
		try {
			watcher = FileSystems.getDefault().newWatchService();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		for (String arg : args) {
			try {
				Path dir = FileSystems.getDefault().getPath(arg);
				WatchKey key = dir.register(watcher, ENTRY_CREATE);	
				keys.put(key, dir);
			} catch (InvalidPathException | IOException e) {
				e.printStackTrace();
			}
		}
		processEvents();
	}

	private static void processEvents() {
		for (;;) {
			WatchKey key;
			try {
				key = watcher.take();
			} catch (InterruptedException ex) {
				// End when interrupted.
				return;
			}
			Path dir = keys.get(key);
			if (dir == null) {
				continue;
			}
			for (WatchEvent<?> event: key.pollEvents()) {
				WatchEvent.Kind<?> kind = event.kind();
				// OVERFLOW occurs if events are lost or discarded.
				if (kind == OVERFLOW) {
					continue;
				}
				// Get filename from event context.
				WatchEvent<Path> evPath = (WatchEvent<Path>)event;
				Path filePath = evPath.context();
				// Resolve path against the directory.
				Path child = dir.resolve(filePath);
				try {
					String fileContentType = Files.probeContentType(child);
					switch (fileContentType) {
					case "text/plain":
					case "application/vnd.ms-excel":
					case "text/csv":
					case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet":
						// Insert new file data into database.
						int fileRecords = processFile(child.toString());
						// Do something with the number of records processed?
						System.out.println(String.format("Processed %d records from '%s'.", fileRecords, child.toString()));
						break;
					default:
						break;
					}
				} catch (IOException e) {
					// Should probably log this somehow.
					e.printStackTrace();
				}
			}
			// Reset the key! Invalid key means inaccessible directory; exit loop.
			boolean valid = key.reset();
			if (!valid) {
				break;
			}
		}
	}
	
}
