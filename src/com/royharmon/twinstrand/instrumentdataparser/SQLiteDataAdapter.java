/**
 * 
 */
package com.royharmon.twinstrand.instrumentdataparser;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * The SQLiteDataAdapter provides methods for inserting files into a SQLite database file.
 * 
 * @author Roy Harmon
 * @version 1.0
 * @since	2022-02-03
 *
 */
public class SQLiteDataAdapter implements IDataAdapter {

	private Connection conn = null;
	
	@Override
	public int connect(String dbfile) {
		try {
			Class.forName("org.sqlite.JDBC");
			if (!dbfile.startsWith("jdbc:sqlite:")) {
				dbfile = "jdbc:sqlite:".concat(dbfile);
			}
			conn = DriverManager.getConnection(dbfile);
			// Check to see if the Results table exists.
			try (Statement stmt = conn.createStatement()){
				stmt.setQueryTimeout(10);
				String creationString = "CREATE TABLE IF NOT EXISTS Results(ID INTEGER PRIMARY KEY NOT NULL, Well TEXT, DIN REAL, 'Conc. [ng/µl]' REAL, 'Sample Description' TEXT, Alert TEXT, Observations TEXT, SourceFile TEXT, ImportDateTime TEXT);";
				stmt.execute(creationString);
				String triggerString = "CREATE TRIGGER IF NOT EXISTS [InsertedOn] AFTER INSERT ON Results BEGIN UPDATE Results SET ImportDateTime = datetime('now', 'localtime') WHERE ID = NEW.ID; END";
				stmt.execute(triggerString);
			}
		} catch (SQLException | ClassNotFoundException e) {
			// If the database doesn't exist, it should be created automatically.
			// If there's an error, it may be "out of memory," 
			// or dbfile could contain an invalid path.
			System.err.println(e.getMessage());
			return 0;
		}
		return 1;
	}

	@Override
	public int disconnect() {
		try {
			if (conn != null) {
				conn.close();
			}
			return 1;
		} catch (SQLException e) {
			System.err.println(e.getMessage());
			return 0;
		}
	}

	@Override
	public int insertRecords(String sql, List<List<String>> records) {
		int recordCount = 0;
		try (PreparedStatement stmt = conn.prepareStatement(sql)) {
			for (List<String> rowValues : records) {
				for (int i = 0; i < rowValues.size(); i++) {
					String val = rowValues.get(i);
					parsePrepParams(stmt, i+1, val);
				}
				recordCount += stmt.executeUpdate();
				stmt.clearParameters();
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		return recordCount;
	}

	/**
	 * @param stmt -java.sql.PreparedStatement whose parameters need to be set to the appropriate data types.
	 * @param i -The parameter index. 
	 * @param val -The field value to be inserted as a parameter in the statement.
	 * @throws SQLException
	 */
	private void parsePrepParams(PreparedStatement stmt, int i, String val) throws SQLException {
		try {
			if (val.contains(".")) {
				Double dub = Double.parseDouble(val);
				stmt.setDouble(i, dub);
			}
			else {
				int intvalue = Integer.parseInt(val);
				stmt.setInt(i, intvalue);
			}
		} catch (NumberFormatException e) {
			stmt.setString(i, val);
		}
	}

	public int insertRecords(String sql, List<List<String>> records, String dbfile) {
		int recordCount = 0;
		try {
			connect(dbfile);
			recordCount = insertRecords(sql, records);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			disconnect();
		}
		return recordCount;
	}

}
