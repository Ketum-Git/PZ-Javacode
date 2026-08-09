// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap.network;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map.Entry;
import zombie.ZomboidFileSystem;
import zombie.core.logger.ExceptionLogger;
import zombie.network.GameClient;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

public final class HiddenAuthors {
    private static final String FILE_NAME = "hidden_authors.ini";
    private static final HashMap<String, HiddenAuthors.UserData> userData = new HashMap<>();
    private static final Set<String> localHidden = new HashSet<>();

    public static void clearLocal() {
        localHidden.clear();
    }

    public static void read() {
        userData.clear();
        File file = ZomboidFileSystem.instance.getFileInCurrentSave("hidden_authors.ini");
        if (file.exists()) {
            try (BufferedReader input = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
                while (true) {
                    String curUserName = input.readLine();
                    if (curUserName == null || curUserName.isEmpty()) {
                        break;
                    }

                    String userNames = input.readLine();
                    if (userNames == null) {
                        break;
                    }

                    HiddenAuthors.UserData userData1 = new HiddenAuthors.UserData();
                    userData1.userName = curUserName;
                    String[] userNamesArray = userNames.split(",");

                    for (String userName : userNamesArray) {
                        String userName1 = userName.replaceAll("&comma;", ",");
                        if (!userName1.isEmpty()) {
                            userData1.hidden.add(userName1);
                        }
                    }

                    userData.put(curUserName, userData1);
                }
            } catch (IOException var13) {
                ExceptionLogger.logException(var13);
            }
        }
    }

    public static void write() {
        String fileName = ZomboidFileSystem.instance.getFileNameInCurrentSave("hidden_authors.ini");

        try (
            FileOutputStream fos = new FileOutputStream(fileName, false);
            OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
        ) {
            for (Entry<String, HiddenAuthors.UserData> it : userData.entrySet()) {
                if (!it.getValue().hidden.isEmpty()) {
                    osw.write(it.getKey());
                    osw.append(System.lineSeparator());
                    StringBuilder sb = new StringBuilder();

                    for (String userName : it.getValue().hidden) {
                        String userName2 = userName.replaceAll(",", "&comma;");
                        sb.append(userName2);
                        sb.append(",");
                    }

                    osw.write(sb.toString());
                    osw.append(System.lineSeparator());
                }
            }
        } catch (IOException var13) {
            ExceptionLogger.logException(var13);
        }
    }

    public static void serverSetAuthorHidden(String userName, String authorName, boolean hidden) {
        HiddenAuthors.UserData userData1 = userData.computeIfAbsent(userName, k -> new HiddenAuthors.UserData());
        if (hidden) {
            userData1.hidden.add(authorName);
        } else {
            userData1.hidden.remove(authorName);
        }
    }

    public static void clientSetAuthorHidden(String authorName, boolean hidden) {
        if (hidden) {
            localHidden.add(authorName);
        } else {
            localHidden.remove(authorName);
        }
    }

    public static void setAuthorHidden(String authorName, boolean hidden) {
        clientSetAuthorHidden(authorName, hidden);
        if (GameClient.client) {
            INetworkPacket.send(PacketTypes.PacketType.HiddenAuthors, hidden, authorName);
        }
    }

    public static boolean isAuthorHidden(String authorName) {
        return localHidden.contains(authorName);
    }

    public static Set<String> getSetForUser(String userName) {
        HiddenAuthors.UserData userData1 = userData.get(userName);
        return userData1 != null ? userData1.hidden : null;
    }

    private static final class UserData {
        String userName;
        final Set<String> hidden = new HashSet<>();
    }
}
