package com.ecfront.dew.core.jdbc.dialect;

public interface Dialect {

    String paging(String sql, long pageNumber, int pageSize);

    String count(String sql);

    String getTableInfo(String tableName);

    String getDriver();

    DialectType getDialectType();
}
