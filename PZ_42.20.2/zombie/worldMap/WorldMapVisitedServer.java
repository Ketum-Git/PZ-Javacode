// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.iso.IsoWorld;
import zombie.iso.SliceY;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.packets.character.PlayerVisitedPacket;
import zombie.scripting.FileName;

@UsedFromLua
public class WorldMapVisitedServer {
    private static WorldMapVisitedServer instance;
    private final HashMap<String, byte[]> dictionary = new HashMap<>();

    public static WorldMapVisitedServer getInstance() {
        if (instance == null) {
            instance = new WorldMapVisitedServer();
            instance.init();
        }

        return instance;
    }

    private void init() {
        File folder = new File(this.getFolderName());
        if (!folder.exists()) {
            folder.mkdir();
        }
    }

    public void update() {
        for (IConnection connection : GameServer.udpEngine.connections) {
            if (this.dictionary.containsKey(connection.getUserName())) {
                for (int i = 0; i < 4; i++) {
                    IsoPlayer player = connection.getPlayerAt(i);
                    if (player != null && !player.isDead() && !player.isAsleep()) {
                        WorldMapVisited.updatePlayer(player, this.dictionary.get(connection.getUserName()));
                    }
                }
            }
        }
    }

    public void setKnownInSquares(IsoPlayer player, int minX, int minY, int maxX, int maxY) {
        IConnection connection = GameServer.getConnectionFromPlayer(player);
        if (connection != null && this.dictionary.containsKey(connection.getUserName())) {
            WorldMapVisited.setKnownInSquares(minX, minY, maxX, maxY, this.dictionary.get(connection.getUserName()));
        }
    }

    public void forget(IsoPlayer player) {
        IConnection connection = GameServer.getConnectionFromPlayer(player);
        if (connection != null && this.dictionary.containsKey(connection.getUserName())) {
            this.dictionary.put(connection.getUserName(), new byte[WorldMapVisited.getVisitedLength()]);
        }
    }

    public void loadUser(IConnection connection) {
        if (!this.dictionary.containsKey(connection.getUserName())) {
            String safeName = FileName.sanitize(connection.getUserName());
            File oldFile = new File(this.getFolderName() + safeName);
            File zipFile = new File(this.getFolderName() + safeName + ".zip");
            Path zipPath = FileSystems.getDefault().getPath(zipFile.getPath()).toAbsolutePath();
            byte[] entity = new byte[WorldMapVisited.getVisitedLength()];
            if (oldFile.exists()) {
                Path oldFilePath = FileSystems.getDefault().getPath(oldFile.getPath()).toAbsolutePath();
                Map<String, String> env = new HashMap<>();
                env.put("create", String.valueOf(Files.notExists(zipPath)));

                try (FileSystem zipFS = FileSystems.newFileSystem(zipPath, env)) {
                    Path to = zipFS.getPath("").resolve(zipPath.relativize(oldFilePath).toString());
                    Files.copy(oldFilePath, to);
                    oldFile.delete();
                } catch (IOException var23) {
                    DebugLog.log("Error repacking visited data saves for user " + connection.getUserName());
                }
            }

            if (zipFile.exists()) {
                try (
                    FileSystem zipFS = FileSystems.newFileSystem(zipPath);
                    InputStream iS = Files.newInputStream(zipFS.getPath(safeName));
                    BufferedInputStream bIS = new BufferedInputStream(iS);
                ) {
                    ByteBuffer in = SliceY.SliceBuffer;
                    in.clear();
                    bIS.read(in.array(), 0, 4);
                    int version = in.getInt();

                    for (int i = 0; i < entity.length; i += in.capacity()) {
                        in.clear();
                        int sizeRead = bIS.read(in.array(), 0, in.capacity());
                        System.arraycopy(in.array(), 0, entity, i, sizeRead);
                    }

                    PlayerVisitedPacket.HandleSendPacket(entity, connection);
                } catch (IOException var21) {
                    DebugLog.log("Error parsing visited data saves for user " + connection.getUserName());
                }
            }

            this.dictionary.put(connection.getUserName(), entity);
        }
    }

    private void saveUser(String user) {
        String safeName = FileName.sanitize(user);
        File zipFile = new File(this.getFolderName() + safeName + ".zip");
        Path zipPath = FileSystems.getDefault().getPath(zipFile.getPath()).toAbsolutePath();
        Map<String, String> env = new HashMap<>();
        env.put("create", String.valueOf(Files.notExists(zipPath)));

        try (
            FileSystem zipFS = FileSystems.newFileSystem(zipPath, env);
            OutputStream oS = Files.newOutputStream(
                zipFS.getPath(safeName), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING
            );
            BufferedOutputStream bOS = new BufferedOutputStream(oS);
        ) {
            byte[] entity = this.dictionary.get(user);
            ByteBuffer out = SliceY.SliceBuffer;
            out.clear();
            out.putInt(IsoWorld.getWorldVersion());
            bOS.write(out.array(), 0, out.position());

            for (int i = 0; i < entity.length; i += out.capacity()) {
                bOS.write(Arrays.copyOfRange(entity, i, Math.min(i + out.capacity(), entity.length)));
            }
        } catch (IOException var18) {
            DebugLog.log("Error saving visited data for user " + user);
        }
    }

    public void unloadUser(String user) {
        if (this.dictionary.containsKey(user)) {
            this.saveUser(user);
            this.dictionary.remove(user);
        }
    }

    public void deleteUser(String user) {
        String safeName = FileName.sanitize(user);
        File file = new File(this.getFolderName() + safeName + ".zip");
        File oldFile = new File(this.getFolderName() + safeName);
        if (file.exists()) {
            file.delete();
        }

        if (oldFile.exists()) {
            oldFile.delete();
        }

        this.dictionary.remove(user);
    }

    public void save() {
        for (String user : this.dictionary.keySet()) {
            this.saveUser(user);
        }
    }

    private String getFolderName() {
        return ZomboidFileSystem.instance.getGameModeCacheDir() + File.separator + Core.gameSaveWorld + File.separator + "map_visited_server" + File.separator;
    }
}
