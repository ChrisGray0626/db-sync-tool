package com.chris.util;

import common.DBTypeEnum;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ConnectUtil {

    private static final Map<DBTypeEnum, String> map = new HashMap<>();
    private static final Logger logger = Logger.getLogger(ConnectUtil.class);

    static {
        map.put(DBTypeEnum.MYSQL, "com.mysql.cj.jdbc.Driver");
        map.put(DBTypeEnum.POSTGRESQL, "org.postgresql.Driver");
        map.put(DBTypeEnum.SQLSERVER, "com.microsoft.sqlserver.jdbc.SQLServerDriver");
    }

    public static Connection connect(DBTypeEnum dbType, String url, String user, String password) {
        String driverName = map.get(dbType);
        try {
            Class.forName(driverName);
        } catch (ClassNotFoundException e) {
            logger.error(e);
        }
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            logger.error(e);
        }
        return null;
    }
}