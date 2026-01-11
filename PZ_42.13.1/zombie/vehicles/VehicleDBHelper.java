// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicles;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import zombie.core.logger.ExceptionLogger;
import zombie.debug.DebugLog;
import zombie.util.PZSQLUtils;

public final class VehicleDBHelper {
    public static boolean isPlayerAlive(String saveDir, int playerSqlId) {
        File file = new File(saveDir + File.separator + "map_p.bin");
        if (file.exists()) {
            return true;
        } else if (playerSqlId == -1) {
            return false;
        } else {
            Connection connection = null;
            File dbFile = new File(saveDir + File.separator + "vehicles.db");
            dbFile.setReadable(true, false);
            if (!dbFile.exists()) {
                return false;
            } else {
                try {
                    connection = PZSQLUtils.getConnection(dbFile.getAbsolutePath());
                } catch (Exception var20) {
                    DebugLog.log("failed to get connection to vehicles database: " + dbFile.getAbsolutePath());
                    ExceptionLogger.logException(var20);
                    return false;
                }

                boolean isAlive = false;
                String sql = "SELECT isDead FROM localPlayers WHERE id=?";
                PreparedStatement pstmt = null;

                boolean var9;
                try {
                    pstmt = connection.prepareStatement("SELECT isDead FROM localPlayers WHERE id=?");
                    pstmt.setInt(1, playerSqlId);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        isAlive = !rs.getBoolean(1);
                    }

                    return isAlive;
                } catch (SQLException var21) {
                    var9 = false;
                } finally {
                    try {
                        if (pstmt != null) {
                            pstmt.close();
                        }

                        connection.close();
                    } catch (SQLException var19) {
                        System.out.println(var19.getMessage());
                    }
                }

                return var9;
            }
        }
    }
}
