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
    
    int connect(String dbfile);
    int disconnect();
    
    int insertRecords(String sql, List<List<String>> records);
    int insertRecords(String sql, List<List<String>> records, String dbfile);
}
