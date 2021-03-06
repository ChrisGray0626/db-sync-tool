package pers.chris.dbSync.util;

import pers.chris.dbSync.common.typeEnum.DBTypeEnum;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ConnectUtil {

    private ConnectUtil() {}

    private static final Map<DBTypeEnum, String> drivers;
    private static final Logger logger = Logger.getLogger(ConnectUtil.class);

    static {
        drivers = new HashMap<>();
        drivers.put(DBTypeEnum.MYSQL, "com.mysql.cj.jdbc.Driver");
        drivers.put(DBTypeEnum.POSTGRESQL, "org.postgresql.Driver");
        drivers.put(DBTypeEnum.SQLSERVER, "com.microsoft.sqlserver.jdbc.SQLServerDriver");
    }

    public static Connection connect(DBTypeEnum dbType, String url, String user, String password) {
        String driverName = drivers.get(dbType);
        try {
            Class.forName(driverName);
        } catch (ClassNotFoundException e) {
            logger.error(e);
        }

        Connection connection = null;
        try {
            connection = DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            logger.error(e);
        }
        return connection;
    }

    public static String getUrl(DBTypeEnum dbType, String hostname, String port, String dbName) {
            return "jdbc:" + dbType.toString().toLowerCase() + "://" + hostname + ":" + port + "/" + dbName;
    }

}
