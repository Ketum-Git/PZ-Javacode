// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import zombie.core.logger.ExceptionLogger;

public class PZSQLUtils {
    public static void init() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException var1) {
            ExceptionLogger.logException(var1);
            System.exit(1);
        }

        setupSqliteVariables();
    }

    private static void setupSqliteVariables() {
        if (System.getProperty("os.name").contains("OS X")) {
            System.setProperty("org.sqlite.lib.path", searchPathForSqliteLib("libsqlitejdbc.dylib"));
            System.setProperty("org.sqlite.lib.name", "libsqlitejdbc.dylib");
        } else if (System.getProperty("os.name").startsWith("Win")) {
            System.setProperty("org.sqlite.lib.path", searchPathForSqliteLib("sqlitejdbc.dll"));
            System.setProperty("org.sqlite.lib.name", "sqlitejdbc.dll");
        } else {
            System.setProperty("org.sqlite.lib.path", searchPathForSqliteLib("libsqlitejdbc.so"));
            System.setProperty("org.sqlite.lib.name", "libsqlitejdbc.so");
        }
    }

    private static String searchPathForSqliteLib(String library) {
        for (String path : System.getProperty("java.library.path", "").split(File.pathSeparator)) {
            File file = new File(path, library);
            if (file.exists()) {
                return path;
            }
        }

        return "";
    }

    public static Connection getConnection(String absolutePath) throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + absolutePath);
    }
}
