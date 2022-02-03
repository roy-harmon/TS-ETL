/**
 * 
 */
package com.royharmon.twinstrand.instrumentdataparser;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * @author Roy
 *
 */
public class SQLiteDataAdapter implements IDataAdapter {

	private Connection conn = null;
	
	@Override
	public int connect(String dbfile) {
		try {
			conn = DriverManager.getConnection(dbfile);
			// Check to see if the Results table exists.
			try (Statement stmt = conn.createStatement()){
				stmt.setQueryTimeout(10);
				ResultSet qryResults = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='Results';");
				if (!qryResults.next()) {
					// The table doesn't exist. Create the table.
					try (Statement tableStatement = conn.createStatement()){
						String creationString = "CREATE TABLE Results(ID INT PRIMARY KEY NOT NULL, Well TEXT, DIN REAL, 'Conc. [ng/µl]' REAL, 'Sample Description' TEXT, Alert TEXT, Observations TEXT);";
						
					}
				}
			}
		} catch (SQLException e) {
			// If the database doesn't exist, it should be created automatically.
			// If there's an error, it may be "out of memory."
			System.err.println(e.getMessage());
		}
		return 0;
	}

	@Override
	public int disconnect() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int insertRecords(String sql, List<List<String>> records) {
		int recordCount = 0;
		try (PreparedStatement stmt = conn.prepareStatement(sql)) {
			for (List<String> rowValues : records) {
				for (int i = 0; i < rowValues.size(); i++) {
					String val = rowValues.get(i);
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
				recordCount += stmt.executeUpdate();
				
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}

	@Override
	public int getNewRecordId() {
		// TODO Auto-generated method stub
		return 0;
	}

}
