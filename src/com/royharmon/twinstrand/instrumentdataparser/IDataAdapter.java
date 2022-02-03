/**
 * 
 */
package com.royharmon.twinstrand.instrumentdataparser;

import java.util.List;

/**
 * @author Roy
 *
 */
public interface IDataAdapter {
	int CONNECTION_OPEN_OK = 100;
    int CONNECTION_OPEN_FAILED = 101;

    int CONNECTION_CLOSE_OK = 200;
    int CONNECTION_CLOSE_FAILED = 201;
    
    int connect(String dbfile);
    int disconnect();
    
    int insertRecords(String sql, List<List<String>> records);
    int getNewRecordId();
}
