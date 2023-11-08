package com.gssystems.datomic.docker;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;

public class TestCassandraConnection {
    public static void main(String[] args) {    
           
        try (CqlSession session = CqlSession.builder().withAuthCredentials("datomic", "datomic").build()) {
            ResultSet rs = session.execute("select count(*) as cnt from datomic.datomic");
            // Extract the first row (which is the only one in this case).
            Row row = rs.one();

            // Extract the value of the first (and only) column from the row.
            assert row != null;
            Long recCount = row.getLong("cnt");
            System.out.printf("Datomic table contains row count : %s%n", recCount);
        } 
    }
}
