// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.areas;

import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.characters.Invite;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.core.utils.UpdateLimit;
import zombie.debug.DebugType;
import zombie.gameStates.ChooseGameInfo;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoWorld;
import zombie.iso.SpawnPoints;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.ServerOptions;
import zombie.network.WarManager;
import zombie.network.chat.ChatServer;
import zombie.network.packets.INetworkPacket;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public class SafeHouse extends Invite {
    private static final long HOUR_IN_MILLISECONDS = 3600000L;
    private static final UpdateLimit updateLimit = new UpdateLimit(1000L);
    private int x;
    private int y;
    private int w;
    private int h;
    private static final int diffError = 2;
    private String owner;
    private long lastVisited;
    private long datetimeCreated;
    private String location;
    private String title = "Safehouse";
    private int playerConnected;
    private int openTimer;
    private int hitPoints;
    private final String id;
    private ArrayList<String> players = new ArrayList<>();
    private final ArrayList<String> playersRespawn = new ArrayList<>();
    private static final ArrayList<SafeHouse> safehouseList = new ArrayList<>();
    private int onlineId;

    public static void init() {
        clearSafehouseList();
    }

    public static SafeHouse addSafeHouse(int x, int y, int w, int h, String player) {
        SafeHouse newsafe = new SafeHouse(x, y, w, h, player);
        newsafe.setOwner(player);
        newsafe.setLastVisited(Calendar.getInstance().getTimeInMillis());
        newsafe.setHitPoints(0);
        newsafe.setDatetimeCreated(0L);
        newsafe.setLocation(null);
        WarManager.removeWar(newsafe.getOnlineID(), null);
        safehouseList.add(newsafe);
        DebugType.Multiplayer
            .debugln(
                "[%03d] Safehouse=%d added (%d;%d) owner=%s", safehouseList.size(), newsafe.getOnlineID(), newsafe.getX(), newsafe.getY(), newsafe.getOwner()
            );
        updateSafehousePlayersConnected();
        if (GameClient.client) {
            LuaEventManager.triggerEvent("OnSafehousesChanged");
        }

        return newsafe;
    }

    public static SafeHouse addSafeHouse(IsoGridSquare square, IsoPlayer player) {
        return addSafeHouse(
            square.getBuilding().def.getX() - 2,
            square.getBuilding().def.getY() - 2,
            square.getBuilding().def.getW() + 4,
            square.getBuilding().def.getH() + 4,
            player.getUsername()
        );
    }

    public static SafeHouse hasSafehouse(String username) {
        for (SafeHouse safe : safehouseList) {
            if (safe.getPlayers().contains(username) || safe.getOwner().equals(username)) {
                return safe;
            }
        }

        return null;
    }

    public static SafeHouse getSafehouseByOwner(String username) {
        for (SafeHouse safe : safehouseList) {
            if (safe.getOwner().equals(username)) {
                return safe;
            }
        }

        return null;
    }

    public static SafeHouse hasSafehouse(IsoPlayer player) {
        return hasSafehouse(player.getUsername());
    }

    public static void updateSafehousePlayersConnected() {
        for (SafeHouse safe : safehouseList) {
            safe.updatePlayersConnected();
        }
    }

    public void updatePlayersConnected() {
        this.setPlayerConnected(0);
        if (GameClient.client) {
            for (IsoPlayer player : GameClient.IDToPlayerMap.values()) {
                if (this.getPlayers().contains(player.getUsername()) || this.getOwner().equals(player.getUsername())) {
                    this.setPlayerConnected(this.getPlayerConnected() + 1);
                }
            }
        } else if (GameServer.server) {
            for (IsoPlayer playerx : GameServer.IDToPlayerMap.values()) {
                if (this.getPlayers().contains(playerx.getUsername()) || this.getOwner().equals(playerx.getUsername())) {
                    this.setPlayerConnected(this.getPlayerConnected() + 1);
                }
            }
        }
    }

    private static SafeHouse findSafeHouse(IsoGridSquare square) {
        SafeHouse result = null;

        for (SafeHouse safeHouse : safehouseList) {
            if (square.getX() >= safeHouse.getX()
                && square.getX() < safeHouse.getX2()
                && square.getY() >= safeHouse.getY()
                && square.getY() < safeHouse.getY2()) {
                result = safeHouse;
                break;
            }
        }

        return result;
    }

    public static SafeHouse getSafeHouse(IsoGridSquare square) {
        return isSafeHouse(square, null, false);
    }

    public static SafeHouse getSafeHouse(String title) {
        for (SafeHouse safe : safehouseList) {
            if (safe.getTitle().equals(title)) {
                return safe;
            }
        }

        return null;
    }

    public static SafeHouse getSafeHouse(int x, int y, int w, int h) {
        for (SafeHouse safe : safehouseList) {
            if (x == safe.getX() && w == safe.getW() && y == safe.getY() && h == safe.getH()) {
                return safe;
            }
        }

        return null;
    }

    public static SafeHouse getSafehouseOverlapping(int x1, int y1, int x2, int y2) {
        for (SafeHouse safe : safehouseList) {
            if (x1 < safe.getX2() && x2 > safe.getX() && y1 < safe.getY2() && y2 > safe.getY()) {
                return safe;
            }
        }

        return null;
    }

    public static SafeHouse getSafehouseOverlapping(int x1, int y1, int x2, int y2, SafeHouse ignore) {
        for (SafeHouse safe : safehouseList) {
            if (safe != ignore && x1 < safe.getX2() && x2 > safe.getX() && y1 < safe.getY2() && y2 > safe.getY()) {
                return safe;
            }
        }

        return null;
    }

    /**
     * Return if the square is a safehouse non allowed for the player You need to be
     *  on a safehouse AND not be allowed to return the safe If you're allowed,
     *  you'll have null in return If username is null, you basically just return if
     *  there's a safehouse here
     */
    public static SafeHouse isSafeHouse(IsoGridSquare square, String username, boolean doDisableSafehouse) {
        if (square == null) {
            return null;
        } else {
            if (GameClient.client && username != null) {
                IsoPlayer player = GameClient.instance.getPlayerFromUsername(username);
                if (player != null && player.role.hasCapability(Capability.CanGoInsideSafehouses)) {
                    return null;
                }
            }

            SafeHouse found = findSafeHouse(square);
            if (found == null
                || !doDisableSafehouse
                || !ServerOptions.instance.disableSafehouseWhenOwnerConnected.getValue()
                || found.getPlayerConnected() <= 0 && found.getOpenTimer() <= 0) {
                return found == null || username != null && (found.getPlayers().contains(username) || found.getOwner().equals(username)) ? null : found;
            } else {
                return null;
            }
        }
    }

    public static boolean isSafehouseAllowTrepass(IsoGridSquare square, IsoPlayer player) {
        if (square == null) {
            return true;
        } else if (player == null) {
            return true;
        } else {
            SafeHouse found = findSafeHouse(square);
            if (found == null) {
                return true;
            } else if (player.role.hasCapability(Capability.CanGoInsideSafehouses)) {
                return true;
            } else if (ServerOptions.getInstance().safehouseAllowTrepass.getValue()) {
                return true;
            } else if (!ServerOptions.getInstance().disableSafehouseWhenOwnerConnected.getValue()
                || found.getPlayerConnected() <= 0 && found.getOpenTimer() <= 0) {
                return WarManager.isWarStarted(found.getOnlineID(), player.getUsername()) ? true : found.playerAllowed(player.getUsername());
            } else {
                return true;
            }
        }
    }

    public static boolean isSafehouseAllowInteract(IsoGridSquare square, IsoPlayer player) {
        if (square == null) {
            return true;
        } else if (player == null) {
            return true;
        } else {
            SafeHouse found = findSafeHouse(square);
            if (found == null) {
                return true;
            } else if (player.role != null && player.role.hasCapability(Capability.CanGoInsideSafehouses)) {
                return true;
            } else {
                return WarManager.isWarStarted(found.getOnlineID(), player.getUsername()) ? true : found.playerAllowed(player.getUsername());
            }
        }
    }

    public static boolean isSafehouseAllowLoot(IsoGridSquare square, IsoPlayer player) {
        return ServerOptions.getInstance().safehouseAllowLoot.getValue() ? true : isSafehouseAllowInteract(square, player);
    }

    public static boolean isSafehouseAllowClaimWar(SafeHouse safehouse, IsoPlayer player) {
        if (!ServerOptions.getInstance().war.getValue()) {
            return false;
        } else if (safehouse == null) {
            return false;
        } else if (player == null) {
            return false;
        } else {
            return WarManager.isWarClaimed(player.getUsername()) ? false : !safehouse.playerAllowed(player.getUsername());
        }
    }

    public static void clearSafehouseList() {
        safehouseList.clear();
    }

    public boolean playerAllowed(IsoPlayer player) {
        return this.players.contains(player.getUsername())
            || this.owner.equals(player.getUsername())
            || player.role.hasCapability(Capability.CanGoInsideSafehouses);
    }

    public boolean playerAllowed(String name) {
        return this.players.contains(name) || this.owner.equals(name);
    }

    public void addPlayer(String player) {
        if (!this.players.contains(player)) {
            this.players.add(player);
            updateSafehousePlayersConnected();
        }
    }

    public void removePlayer(String player) {
        if (this.players.contains(player)) {
            this.players.remove(player);
            this.playersRespawn.remove(player);
        }
    }

    public static void removeSafeHouse(SafeHouse safeHouse) {
        safehouseList.remove(safeHouse);
        DebugType.Multiplayer
            .debugln(
                "[%03d] Safehouse=%d removed (%d;%d) owner=%s",
                safehouseList.size(),
                safeHouse.getOnlineID(),
                safeHouse.getX(),
                safeHouse.getY(),
                safeHouse.getOwner()
            );
        if (GameClient.client) {
            LuaEventManager.triggerEvent("OnSafehousesChanged");
        }
    }

    public void save(ByteBuffer output) {
        output.putInt(this.getX());
        output.putInt(this.getY());
        output.putInt(this.getW());
        output.putInt(this.getH());
        GameWindow.WriteString(output, this.getOwner());
        output.putInt(this.getHitPoints());
        output.putInt(this.getPlayers().size());

        for (String players : this.getPlayers()) {
            GameWindow.WriteString(output, players);
        }

        output.putLong(this.getLastVisited());
        GameWindow.WriteString(output, this.getTitle());
        output.putLong(this.getDatetimeCreated());
        GameWindow.WriteString(output, this.getLocation());
        output.putInt(this.playersRespawn.size());

        for (int i = 0; i < this.playersRespawn.size(); i++) {
            GameWindow.WriteString(output, this.playersRespawn.get(i));
        }
    }

    public static SafeHouse load(ByteBuffer bb, int worldVersion) {
        SafeHouse safeHouse = new SafeHouse(bb.getInt(), bb.getInt(), bb.getInt(), bb.getInt(), GameWindow.ReadString(bb));
        if (worldVersion >= 216) {
            safeHouse.setHitPoints(bb.getInt());
        }

        int index = bb.getInt();

        for (int i = 0; i < index; i++) {
            safeHouse.addPlayer(GameWindow.ReadString(bb));
        }

        safeHouse.setLastVisited(bb.getLong());
        safeHouse.setTitle(GameWindow.ReadString(bb));
        if (worldVersion >= 223) {
            safeHouse.setDatetimeCreated(bb.getLong());
            safeHouse.setLocation(GameWindow.ReadString(bb));
        }

        if (ChatServer.isInited()) {
            ChatServer.getInstance().createSafehouseChat(safeHouse.getId());
        }

        safehouseList.add(safeHouse);
        int size = bb.getInt();

        for (int i = 0; i < size; i++) {
            safeHouse.playersRespawn.add(GameWindow.ReadString(bb));
        }

        return safeHouse;
    }

    public static String canBeSafehouse(IsoGridSquare clickedSquare, IsoPlayer player) {
        if (!GameClient.client && !GameServer.server) {
            return null;
        } else if (!ServerOptions.instance.playerSafehouse.getValue() && !ServerOptions.instance.adminSafehouse.getValue()) {
            return null;
        } else {
            String reason = "";
            if (ServerOptions.instance.playerSafehouse.getValue() && hasSafehouse(player) != null) {
                reason = reason + Translator.getText("IGUI_Safehouse_AlreadyHaveSafehouse") + System.lineSeparator();
            }

            int daysSurvived = ServerOptions.instance.safehouseDaySurvivedToClaim.getValue();
            if (!ServerOptions.instance.playerSafehouse.getValue() && ServerOptions.instance.adminSafehouse.getValue() && GameClient.client) {
                if (!player.role.hasCapability(Capability.CanSetupSafehouses)) {
                    return null;
                }

                daysSurvived = 0;
            }

            if (daysSurvived > 0 && player.getHoursSurvived() < daysSurvived * 24) {
                reason = reason + Translator.getText("IGUI_Safehouse_DaysSurvivedToClaim", daysSurvived) + System.lineSeparator();
            }

            if (GameClient.client) {
                KahluaTableIterator it = GameClient.instance.getServerSpawnRegions().iterator();

                while (it.advance()) {
                    KahluaTable spawnRegion = (KahluaTable)it.getValue();
                    KahluaTableIterator mapIt = ((KahluaTableImpl)spawnRegion.rawget("points")).iterator();

                    while (mapIt.advance()) {
                        KahluaTable spawnPoint = (KahluaTable)mapIt.getValue();
                        KahluaTableIterator pointIt = spawnPoint.iterator();

                        while (pointIt.advance()) {
                            KahluaTable point = (KahluaTable)pointIt.getValue();
                            IsoGridSquare square;
                            if (point.rawget("worldX") != null) {
                                Double worldX = (Double)point.rawget("worldX");
                                Double worldY = (Double)point.rawget("worldY");
                                Double posX = (Double)point.rawget("posX");
                                Double posY = (Double)point.rawget("posY");
                                square = IsoWorld.instance.getCell().getGridSquare(posX + worldX * 300.0, posY + worldY * 300.0, 0.0);
                            } else {
                                Double posX = (Double)point.rawget("posX");
                                Double posY = (Double)point.rawget("posY");
                                if (posX != null && posY != null) {
                                    square = IsoWorld.instance.getCell().getGridSquare(posX.doubleValue(), posY.doubleValue(), 0.0);
                                } else {
                                    square = null;
                                }
                            }

                            if (square != null && square.getBuilding() != null && square.getBuilding().getDef() != null) {
                                BuildingDef buildingDef = square.getBuilding().getDef();
                                if (clickedSquare.getX() >= buildingDef.getX()
                                    && clickedSquare.getX() < buildingDef.getX2()
                                    && clickedSquare.getY() >= buildingDef.getY()
                                    && clickedSquare.getY() < buildingDef.getY2()) {
                                    return Translator.getText("IGUI_Safehouse_IsSpawnPoint");
                                }
                            }
                        }
                    }
                }
            }

            boolean safehouseIsFree = true;
            boolean playerIsInSafeHouse = false;
            boolean bedroom = false;
            boolean notResidence = false;
            ArrayList<String> residentialRooms = new ArrayList<>();
            residentialRooms.add("bathroom");
            residentialRooms.add("bedroom");
            residentialRooms.add("closet");
            residentialRooms.add("fishingstorage");
            residentialRooms.add("garage");
            residentialRooms.add("hall");
            residentialRooms.add("kidsbedroom");
            residentialRooms.add("kitchen");
            residentialRooms.add("laundry");
            residentialRooms.add("livingroom");
            residentialRooms.add("diningroom");
            residentialRooms.add("sewingroom");
            residentialRooms.add("office");
            residentialRooms.add("emptyoutside");
            residentialRooms.add("empty");
            BuildingDef buildingDef = clickedSquare.getBuilding().getDef();
            if (clickedSquare.getBuilding().rooms != null) {
                for (IsoRoom room : clickedSquare.getBuilding().rooms) {
                    String name = room.getName();
                    if (!residentialRooms.contains(name)) {
                        notResidence = true;
                        break;
                    }

                    if (name.equals("bedroom") || room.getName().equals("livingroom")) {
                        bedroom = true;
                    }
                }
            }

            if (!bedroom) {
                notResidence = true;
            }

            IsoCell cell = IsoWorld.instance.getCell();

            for (IsoMovingObject object : cell.getObjectList()) {
                if (object != player
                    && object instanceof IsoGameCharacter
                    && !(object instanceof IsoAnimal)
                    && object.getX() >= buildingDef.getX() - 2
                    && object.getX() < buildingDef.getX2() + 2
                    && object.getY() >= buildingDef.getY() - 2
                    && object.getY() < buildingDef.getY2() + 2
                    && !(object instanceof BaseVehicle)) {
                    safehouseIsFree = false;
                    break;
                }
            }

            if (player.getX() >= buildingDef.getX() - 2
                && player.getX() < buildingDef.getX2() + 2
                && player.getY() >= buildingDef.getY() - 2
                && player.getY() < buildingDef.getY2() + 2
                && player.getCurrentSquare() != null
                && !player.getCurrentSquare().has(IsoFlagType.exterior)) {
                playerIsInSafeHouse = true;
            }

            if (!playerIsInSafeHouse) {
                reason = reason + Translator.getText("IGUI_Safehouse_NotInside") + System.lineSeparator();
            } else if (!safehouseIsFree) {
                reason = reason + Translator.getText("IGUI_Safehouse_SomeoneInside") + System.lineSeparator();
            }

            if (notResidence && !ServerOptions.instance.safehouseAllowNonResidential.getValue()) {
                reason = reason + Translator.getText("IGUI_Safehouse_NotHouse") + System.lineSeparator();
            }

            boolean intersects = intersects(
                buildingDef.getX() - 2,
                buildingDef.getY() - 2,
                buildingDef.getX() - 2 + buildingDef.getW() + 4,
                buildingDef.getY() - 2 + buildingDef.getH() + 4
            );
            if (intersects) {
                reason = reason + Translator.getText("IGUI_Safehouse_Intersects") + System.lineSeparator();
            }

            if (!WarManager.isWarClaimed(getOnlineID(buildingDef.getX() - 2, buildingDef.getY() - 2))) {
                reason = reason + Translator.getText("IGUI_Safehouse_War") + System.lineSeparator();
            }

            return reason;
        }
    }

    public void checkTrespass(IsoPlayer player) {
        if (GameServer.server && player.getVehicle() == null && !isSafehouseAllowTrepass(player.getCurrentSquare(), player)) {
            GameServer.sendTeleport(player, this.x - 1, this.y - 1, 0.0F);
            player.updateDisguisedState();
            if (player.isAsleep()) {
                player.setAsleep(false);
                player.setAsleepTime(0.0F);
                INetworkPacket.sendToAll(PacketTypes.PacketType.WakeUpPlayer, player);
            }
        }
    }

    public SafeHouse alreadyHaveSafehouse(String username) {
        return ServerOptions.instance.playerSafehouse.getValue() ? hasSafehouse(username) : null;
    }

    public SafeHouse alreadyHaveSafehouse(IsoPlayer player) {
        return ServerOptions.instance.playerSafehouse.getValue() ? hasSafehouse(player) : null;
    }

    public static boolean allowSafeHouse(IsoPlayer player) {
        boolean allowed = false;
        if ((GameClient.client || GameServer.server) && (ServerOptions.instance.playerSafehouse.getValue() || ServerOptions.instance.adminSafehouse.getValue())
            )
         {
            if (ServerOptions.instance.playerSafehouse.getValue()) {
                allowed = hasSafehouse(player) == null;
            }

            if (allowed
                && ServerOptions.instance.safehouseDaySurvivedToClaim.getValue() > 0
                && player.getHoursSurvived() < ServerOptions.instance.safehouseDaySurvivedToClaim.getValue() * 24) {
                allowed = false;
            }

            if (ServerOptions.instance.adminSafehouse.getValue()) {
                allowed = player.role.hasCapability(Capability.CanSetupSafehouses);
            }
        }

        return allowed;
    }

    public static int getOnlineID(int x, int y) {
        return (x + y) * (x + y + 1) / 2 + x;
    }

    public SafeHouse(int x, int y, int w, int h, String player) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.players.add(player);
        this.owner = player;
        this.id = x + "," + y + " at " + Calendar.getInstance().getTimeInMillis();
        this.onlineId = getOnlineID(x, y);
    }

    public String getId() {
        return this.id;
    }

    public int getX() {
        return this.x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return this.y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getW() {
        return this.w;
    }

    public void setW(int w) {
        this.w = w;
    }

    public int getH() {
        return this.h;
    }

    public void setH(int h) {
        this.h = h;
    }

    public int getX2() {
        return this.x + this.w;
    }

    public int getY2() {
        return this.y + this.h;
    }

    public boolean containsLocation(float x, float y) {
        return x >= this.getX() && x < this.getX2() && y >= this.getY() && y < this.getY2();
    }

    public ArrayList<String> getPlayers() {
        return this.players;
    }

    public void setPlayers(ArrayList<String> players) {
        this.players = players;
    }

    public ArrayList<String> getPlayersRespawn() {
        return this.playersRespawn;
    }

    public static ArrayList<SafeHouse> getSafehouseList() {
        return safehouseList;
    }

    public String getOwner() {
        return this.owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
        this.players.remove(owner);
    }

    public boolean isOwner(IsoPlayer player) {
        return this.isOwner(player.getUsername());
    }

    public boolean isOwner(String username) {
        return this.getOwner().equals(username);
    }

    public long getLastVisited() {
        return this.lastVisited;
    }

    public void setLastVisited(long lastVisited) {
        this.lastVisited = lastVisited;
    }

    public long getDatetimeCreated() {
        return this.datetimeCreated;
    }

    public String getDatetimeCreatedStr() {
        DateFormat time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return time.format(this.datetimeCreated);
    }

    public void setDatetimeCreated(long datetimeCreated) {
        if (datetimeCreated == 0L) {
            this.datetimeCreated = Calendar.getInstance().getTimeInMillis();
        } else {
            this.datetimeCreated = datetimeCreated;
        }
    }

    public String getLocation() {
        return this.location;
    }

    public void setLocation(String location) {
        if (location != null && location != "") {
            this.location = location;
        } else {
            KahluaTable spawnRegions = SpawnPoints.instance.getSpawnRegions();
            if (spawnRegions != null) {
                float minDistance = 100000.0F;
                KahluaTableIterator it = spawnRegions.iterator();

                while (it.advance()) {
                    KahluaTable spawnRegion = (KahluaTable)it.getValue();
                    String regionName = spawnRegion.getString("name");
                    ChooseGameInfo.Map map = ChooseGameInfo.getMapDetails(regionName);
                    if (map != null) {
                        float distance = PZMath.sqrt(PZMath.pow(map.getZoomX() - this.getX(), 2.0F) + PZMath.pow(map.getZoomY() - this.getY(), 2.0F));
                        if (distance < minDistance) {
                            minDistance = distance;
                            this.location = regionName;
                        }
                    }
                }
            }
        }
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getPlayerConnected() {
        return this.playerConnected;
    }

    public void setPlayerConnected(int playerConnected) {
        this.playerConnected = playerConnected;
    }

    public int getOpenTimer() {
        return this.openTimer;
    }

    public void setOpenTimer(int openTimer) {
        this.openTimer = openTimer;
    }

    public int getHitPoints() {
        return this.hitPoints;
    }

    public void setHitPoints(int hitPoints) {
        this.hitPoints = hitPoints;
    }

    public void setRespawnInSafehouse(boolean b, String username) {
        if (b) {
            if (!this.playersRespawn.contains(username)) {
                this.playersRespawn.add(username);
            }
        } else if (this.playersRespawn.contains(username)) {
            this.playersRespawn.remove(username);
        }
    }

    public boolean isRespawnInSafehouse(String username) {
        return this.playersRespawn.contains(username);
    }

    public static boolean isPlayerAllowedOnSquare(IsoPlayer player, IsoGridSquare sq) {
        return !ServerOptions.instance.safehouseAllowTrepass.getValue() ? isSafeHouse(sq, player.getUsername(), true) == null : true;
    }

    public int getOnlineID() {
        return this.onlineId;
    }

    public void setOnlineID(int value) {
        this.onlineId = value;
    }

    public static SafeHouse getSafeHouse(int onlineID) {
        for (SafeHouse safe : safehouseList) {
            if (safe.getOnlineID() == onlineID) {
                return safe;
            }
        }

        return null;
    }

    public static boolean isInSameSafehouse(String player1, String player2) {
        for (SafeHouse safeHouse : safehouseList) {
            if (safeHouse.playerAllowed(player1) && safeHouse.playerAllowed(player2)) {
                return true;
            }
        }

        return false;
    }

    public static boolean intersects(int startX, int startY, int endX, int endY) {
        for (int x = startX; x < endX; x++) {
            for (int y = startY; y < endY; y++) {
                IsoGridSquare square = IsoWorld.instance.currentCell.getOrCreateGridSquare(x, y, 0.0);
                if (getSafeHouse(square) != null) {
                    return true;
                }
            }
        }

        return false;
    }

    public static void hitPoint(int onlineID) {
        SafeHouse safeHouse = getSafeHouse(onlineID);
        if (safeHouse != null) {
            int hitPoints = safeHouse.getHitPoints() + 1;
            if (hitPoints == ServerOptions.instance.warSafehouseHitPoints.getValue()) {
                removeSafeHouse(safeHouse);
                INetworkPacket.sendToAll(PacketTypes.PacketType.SafehouseRelease, safeHouse);
            } else {
                safeHouse.setHitPoints(hitPoints);
                INetworkPacket.sendToAll(PacketTypes.PacketType.SafehouseSync, safeHouse);
            }
        }
    }

    public static void kickUserFromSafehouse(SafeHouse safeHouse, String username) {
        safeHouse.removePlayer(username);
        INetworkPacket.sendToAll(PacketTypes.PacketType.SafehouseSync, safeHouse);
        if (!ServerOptions.instance.safehouseAllowTrepass.getValue()) {
            IsoPlayer isoPlayer = GameServer.getPlayerByUserName(username);
            if (isoPlayer != null) {
                if (safeHouse.containsLocation(isoPlayer.getX(), isoPlayer.getY())) {
                    if (GameServer.isPlayerConnected(isoPlayer)) {
                        GameServer.sendTeleport(isoPlayer, safeHouse.getX() - 1.0F, safeHouse.getY() - 1.0F, 0.0F);
                        if (isoPlayer.isAsleep()) {
                            isoPlayer.setAsleep(false);
                            isoPlayer.setAsleepTime(0.0F);
                            INetworkPacket.sendToAll(PacketTypes.PacketType.WakeUpPlayer, isoPlayer);
                        }
                    }
                }
            }
        }
    }

    public static boolean hasNotSurvivedEnoughToClaim(IsoPlayer player) {
        if (player.getRole().hasCapability(Capability.CanSetupSafehouses)) {
            return false;
        } else {
            int daysSurvived = ServerOptions.instance.safehouseDaySurvivedToClaim.getValue();
            return daysSurvived > 0 && player.getHoursSurvived() < daysSurvived * 24;
        }
    }

    private static boolean isSafeHouseExpired(SafeHouse safeHouse, long currentTime, long removalTime) {
        if (safeHouse == null) {
            return false;
        } else {
            long expirationTime = safeHouse.getLastVisited() + removalTime;
            return currentTime > expirationTime;
        }
    }

    public static void update() {
        long currentTime = System.currentTimeMillis();
        long removalTime = 3600000L * ServerOptions.instance.safeHouseRemovalTime.getValue();
        if (removalTime > 0L && updateLimit.Check()) {
            Iterator<SafeHouse> iterator = safehouseList.iterator();

            while (iterator.hasNext()) {
                SafeHouse safeHouse = iterator.next();
                if (isSafeHouseExpired(safeHouse, currentTime, removalTime)) {
                    iterator.remove();
                    INetworkPacket.sendToAll(PacketTypes.PacketType.SafehouseRelease, safeHouse);
                }
            }
        }
    }
}
