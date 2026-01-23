// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.chat;

import java.util.ArrayList;
import java.util.HashMap;
import zombie.characters.IsoPlayer;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.core.raknet.UdpConnection;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerOptions;
import zombie.network.chat.ChatType;

public final class ChatUtility {
    private static final boolean useEuclidean = true;
    private static final HashMap<String, String> allowedChatIcons = new HashMap<>();
    private static final HashMap<String, String> allowedChatIconsFull = new HashMap<>();
    private static final StringBuilder builder = new StringBuilder();
    private static final StringBuilder builderTest = new StringBuilder();

    private ChatUtility() {
    }

    public static float getScrambleValue(IsoObject src, IsoPlayer dest, float baseRange) {
        return getScrambleValue(src.getX(), src.getY(), src.getZ(), src.getSquare(), dest, baseRange);
    }

    public static float getScrambleValue(float srcX, float srcY, float srcZ, IsoGridSquare srcSquare, IsoPlayer dest, float baseRange) {
        float mod = 1.0F;
        boolean forceScramble = false;
        boolean forceDisplay = false;
        if (srcSquare != null && dest.getSquare() != null) {
            if (dest.getBuilding() != null && srcSquare.getBuilding() != null && dest.getBuilding() == srcSquare.getBuilding()) {
                if (dest.getSquare().getRoom() == srcSquare.getRoom()) {
                    mod = (float)(mod * 2.0);
                    forceDisplay = true;
                } else if (Math.abs(dest.getZ() - srcZ) < 1.0F) {
                    mod = (float)(mod * 2.0);
                }
            } else if (dest.getBuilding() != null || srcSquare.getBuilding() != null) {
                mod = (float)(mod * 0.5);
                forceScramble = true;
            }

            if (Math.abs(dest.getZ() - srcZ) >= 1.0F) {
                mod = (float)(mod - mod * (Math.abs(dest.getZ() - srcZ) * 0.25));
                forceScramble = true;
            }
        }

        float range = baseRange * mod;
        float scramble = 1.0F;
        if (mod > 0.0F && playerWithinBounds(srcX, srcY, dest, range)) {
            float dist = getDistance(srcX, srcY, dest);
            if (dist >= 0.0F && dist < range) {
                float fullRange = range * 0.6F;
                if (!forceDisplay && (forceScramble || !(dist < fullRange))) {
                    if (range - fullRange != 0.0F) {
                        scramble = (dist - fullRange) / (range - fullRange);
                        if (scramble < 0.2F) {
                            scramble = 0.2F;
                        }
                    }
                } else {
                    scramble = 0.0F;
                }
            }
        }

        return scramble;
    }

    public static boolean playerWithinBounds(IsoObject source, IsoObject dest, float dist) {
        return playerWithinBounds(source.getX(), source.getY(), dest, dist);
    }

    public static boolean playerWithinBounds(float srcX, float srcY, IsoObject dest, float dist) {
        return dest == null ? false : dest.getX() > srcX - dist && dest.getX() < srcX + dist && dest.getY() > srcY - dist && dest.getY() < srcY + dist;
    }

    public static float getDistance(IsoObject source, IsoPlayer dest) {
        return dest == null ? -1.0F : (float)Math.sqrt(Math.pow(source.getX() - dest.getX(), 2.0) + Math.pow(source.getY() - dest.getY(), 2.0));
    }

    public static float getDistance(float srcX, float srcY, IsoPlayer dest) {
        return dest == null ? -1.0F : (float)Math.sqrt(Math.pow(srcX - dest.getX(), 2.0) + Math.pow(srcY - dest.getY(), 2.0));
    }

    public static UdpConnection findConnection(short playerOnlineID) {
        UdpConnection targetConnection = null;
        if (GameServer.udpEngine != null) {
            for (int i = 0; i < GameServer.udpEngine.connections.size(); i++) {
                UdpConnection connection = GameServer.udpEngine.connections.get(i);

                for (int j = 0; j < connection.playerIds.length; j++) {
                    if (connection.playerIds[j] == playerOnlineID) {
                        targetConnection = connection;
                        break;
                    }
                }
            }
        }

        if (targetConnection == null) {
            DebugLog.log("Connection with PlayerID ='" + playerOnlineID + "' not found!");
        }

        return targetConnection;
    }

    public static UdpConnection findConnection(String playerName) {
        UdpConnection founded = null;
        if (GameServer.udpEngine != null) {
            for (int i = 0; i < GameServer.udpEngine.connections.size() && founded == null; i++) {
                UdpConnection connection = GameServer.udpEngine.connections.get(i);

                for (int j = 0; j < connection.players.length; j++) {
                    if (connection.players[j] != null && connection.players[j].username.equalsIgnoreCase(playerName)) {
                        founded = connection;
                        break;
                    }
                }
            }
        }

        if (founded == null) {
            DebugLog.DetailedInfo.trace("Player with nickname = '" + playerName + "' not found!");
        }

        return founded;
    }

    public static IsoPlayer findPlayer(int playerOnlineID) {
        IsoPlayer foundedPlayer = null;
        if (GameServer.udpEngine != null) {
            for (int i = 0; i < GameServer.udpEngine.connections.size(); i++) {
                UdpConnection connection = GameServer.udpEngine.connections.get(i);

                for (int j = 0; j < connection.playerIds.length; j++) {
                    if (connection.playerIds[j] == playerOnlineID) {
                        foundedPlayer = connection.players[j];
                        break;
                    }
                }
            }
        }

        if (foundedPlayer == null) {
            DebugLog.log("Player with PlayerID ='" + playerOnlineID + "' not found!");
        }

        return foundedPlayer;
    }

    public static String findPlayerName(int playerOnlineID) {
        return findPlayer(playerOnlineID).getUsername();
    }

    public static IsoPlayer findPlayer(String playerNickname) {
        IsoPlayer foundedPlayer = null;
        if (GameClient.client) {
            foundedPlayer = GameClient.instance.getPlayerFromUsername(playerNickname);
        } else if (GameServer.server) {
            foundedPlayer = GameServer.getPlayerByUserName(playerNickname);
        }

        if (foundedPlayer == null) {
            DebugLog.DetailedInfo.trace("Player with nickname = '" + playerNickname + "' not found!");
        }

        return foundedPlayer;
    }

    public static ArrayList<ChatType> getAllowedChatStreams() {
        String optionValue = ServerOptions.getInstance().chatStreams.getValue();
        optionValue = optionValue.replaceAll("\"", "");
        String[] enabledStreams = optionValue.split(",");
        ArrayList<ChatType> allowedStreams = new ArrayList<>();
        allowedStreams.add(ChatType.server);

        for (String streamShortName : enabledStreams) {
            switch (streamShortName) {
                case "s":
                    allowedStreams.add(ChatType.say);
                    break;
                case "r":
                    allowedStreams.add(ChatType.radio);
                    break;
                case "a":
                    allowedStreams.add(ChatType.admin);
                    break;
                case "w":
                    allowedStreams.add(ChatType.whisper);
                    break;
                case "y":
                    allowedStreams.add(ChatType.shout);
                    break;
                case "sh":
                    allowedStreams.add(ChatType.safehouse);
                    break;
                case "f":
                    allowedStreams.add(ChatType.faction);
                    break;
                case "all":
                    if (ServerOptions.getInstance().globalChat.getValue()) {
                        allowedStreams.add(ChatType.general);
                    }
            }
        }

        return allowedStreams;
    }

    public static boolean chatStreamEnabled(ChatType type) {
        ArrayList<ChatType> enabledStreams = getAllowedChatStreams();
        return enabledStreams.contains(type);
    }

    public static void InitAllowedChatIcons() {
        allowedChatIcons.clear();
        Texture.collectAllIcons(allowedChatIcons, allowedChatIconsFull);
    }

    private static String getColorString(String input, boolean doFloat) {
        if (Colors.ColorExists(input)) {
            Color c = Colors.GetColorByName(input);
            return doFloat ? c.getRedFloat() + "," + c.getGreenFloat() + "," + c.getBlueFloat() : c.getRed() + "," + c.getGreen() + "," + c.getBlue();
        } else {
            if (input.length() <= 11 && input.contains(",")) {
                String[] split = input.split(",");
                if (split.length == 3) {
                    int r = parseColorInt(split[0]);
                    int g = parseColorInt(split[1]);
                    int b = parseColorInt(split[2]);
                    if (r != -1 && g != -1 && b != -1) {
                        if (doFloat) {
                            return r / 255.0F + "," + g / 255.0F + "," + b / 255.0F;
                        }

                        return r + "," + g + "," + b;
                    }
                }
            }

            return null;
        }
    }

    private static int parseColorInt(String str) {
        try {
            int i = Integer.parseInt(str);
            return i >= 0 && i <= 255 ? i : -1;
        } catch (Exception var2) {
            return -1;
        }
    }

    public static String parseStringForChatBubble(String str) {
        try {
            builder.delete(0, builder.length());
            builderTest.delete(0, builderTest.length());
            str = str.replaceAll("\\[br/]", "");
            str = str.replaceAll("\\[cdt=", "");
            char[] chars = str.toCharArray();
            boolean hasOpened = false;
            boolean hasOpenedColor = false;
            int maxImages = 10;
            int imgCount = 0;

            for (int i = 0; i < chars.length; i++) {
                char c = chars[i];
                if (c == '*') {
                    if (!hasOpened) {
                        hasOpened = true;
                    } else {
                        String s = builderTest.toString();
                        builderTest.delete(0, builderTest.length());
                        String colorStr = getColorString(s, false);
                        if (colorStr != null) {
                            if (hasOpenedColor) {
                                builder.append("[/]");
                            }

                            builder.append("[col=");
                            builder.append(colorStr);
                            builder.append(']');
                            hasOpened = false;
                            hasOpenedColor = true;
                        } else if (imgCount < 10 && (s.equalsIgnoreCase("music") || allowedChatIcons.containsKey(s.toLowerCase()))) {
                            if (hasOpenedColor) {
                                builder.append("[/]");
                                hasOpenedColor = false;
                            }

                            builder.append("[img=");
                            builder.append(s.equalsIgnoreCase("music") ? "music" : allowedChatIcons.get(s.toLowerCase()));
                            builder.append(']');
                            hasOpened = false;
                            imgCount++;
                        } else {
                            builder.append('*');
                            builder.append(s);
                        }
                    }
                } else if (hasOpened) {
                    builderTest.append(c);
                } else {
                    builder.append(c);
                }
            }

            if (hasOpened) {
                builder.append('*');
                String s = builderTest.toString();
                if (!s.isEmpty()) {
                    builder.append(s);
                }

                if (hasOpenedColor) {
                    builder.append("[/]");
                }
            }

            return builder.toString();
        } catch (Exception var10) {
            var10.printStackTrace();
            return str;
        }
    }

    public static String parseStringForChatLog(String str) {
        try {
            builder.delete(0, builder.length());
            builderTest.delete(0, builderTest.length());
            char[] chars = str.toCharArray();
            boolean hasOpened = false;
            boolean hasOpenedColor = false;
            int maxImages = 10;
            int imgCount = 0;

            for (int i = 0; i < chars.length; i++) {
                char c = chars[i];
                if (c == '*') {
                    if (!hasOpened) {
                        hasOpened = true;
                    } else {
                        String s = builderTest.toString();
                        builderTest.delete(0, builderTest.length());
                        String colorStr = getColorString(s, true);
                        if (colorStr != null) {
                            builder.append(" <RGB:");
                            builder.append(colorStr);
                            builder.append('>');
                            hasOpened = false;
                            hasOpenedColor = true;
                        } else {
                            if (imgCount < 10 && (s.equalsIgnoreCase("music") || allowedChatIconsFull.containsKey(s.toLowerCase()))) {
                                if (hasOpenedColor) {
                                    builder.append(" <RGB:");
                                    builder.append("1.0,1.0,1.0");
                                    builder.append('>');
                                    hasOpenedColor = false;
                                }

                                String texid = s.equalsIgnoreCase("music") ? "Icon_music_notes" : allowedChatIconsFull.get(s.toLowerCase());
                                Texture tex = Texture.getSharedTexture(texid);
                                if (Texture.getSharedTexture(texid) != null) {
                                    int w = (int)(tex.getWidth() * 0.5F);
                                    int h = (int)(tex.getHeight() * 0.5F);
                                    if (s.equalsIgnoreCase("music")) {
                                        w = (int)(tex.getWidth() * 0.75F);
                                        h = (int)(tex.getHeight() * 0.75F);
                                    }

                                    builder.append("<IMAGE:");
                                    builder.append(texid);
                                    builder.append("," + w + "," + h + ">");
                                    hasOpened = false;
                                    imgCount++;
                                    continue;
                                }
                            }

                            builder.append('*');
                            builder.append(s);
                        }
                    }
                } else if (hasOpened) {
                    builderTest.append(c);
                } else {
                    builder.append(c);
                }
            }

            if (hasOpened) {
                builder.append('*');
                String s = builderTest.toString();
                if (!s.isEmpty()) {
                    builder.append(s);
                }
            }

            return builder.toString();
        } catch (Exception var14) {
            var14.printStackTrace();
            return str;
        }
    }
}
