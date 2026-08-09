// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.character;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.SurvivorDesc;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.core.znet.SteamUtils;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.iso.IsoCamera;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoDirections;
import zombie.iso.IsoWorld;
import zombie.iso.SpawnPoints;
import zombie.iso.areas.SafeHouse;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.ServerOptions;
import zombie.network.ServerWorldDatabase;
import zombie.network.packets.INetworkPacket;
import zombie.savefile.ServerPlayerDB;
import zombie.scripting.objects.CharacterProfession;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.ResourceLocation;
import zombie.util.AddCoopPlayer;

@PacketSetting(ordering = 0, priority = 1, reliability = 3, requiredCapability = Capability.LoginOnServer, handlingType = 7)
public class CreatePlayerPacket implements INetworkPacket {
    private String spawnRegionName = "";
    private SurvivorDesc survivorDescriptor;
    private byte playerIndex;
    private final List<CharacterTrait> characterTraits = new ArrayList<>();
    private IsoPlayer player;

    @Override
    public void setData(Object... values) {
        if (values.length == 1 && values[0] instanceof Byte) {
            this.set((Byte)values[0]);
        } else {
            DebugType.Multiplayer.warn(this.getClass().getSimpleName() + ".set get invalid arguments");
        }
    }

    private void set(byte playerIndex) {
        this.playerIndex = playerIndex;
        this.spawnRegionName = IsoWorld.instance.getSpawnRegion();
        this.survivorDescriptor = IsoWorld.instance.getLuaPlayerDesc();
        this.characterTraits.clear();
        this.characterTraits.addAll(IsoWorld.instance.getLuaTraits());
    }

    @Override
    public void processClientLoading(UdpConnection connection) {
        this.processClient(connection);
    }

    @Override
    public void processClient(UdpConnection connection) {
        AddCoopPlayer coopPlayer = null;

        for (AddCoopPlayer cp : IsoWorld.instance.addCoopPlayers) {
            if (cp.getPlayerIndex() == this.playerIndex) {
                coopPlayer = cp;
            }
        }

        if (coopPlayer != null) {
            coopPlayer.playerCreated(this.playerIndex);
        } else {
            IsoPlayer.players[this.playerIndex] = this.player;
            IsoPlayer.setInstance(this.player);
            IsoCamera.setCameraCharacter(this.player);
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        if (GameServer.server) {
            this.playerIndex = b.getByte();
            this.spawnRegionName = b.getUTF();

            try {
                this.survivorDescriptor = new SurvivorDesc();
                this.survivorDescriptor.load(b.bb, IsoWorld.getWorldVersion(), null);
                this.survivorDescriptor.getWornItems().load(b.bb, IsoWorld.getWorldVersion());
                this.survivorDescriptor.getHumanVisual().load(b.bb, IsoWorld.getWorldVersion());
            } catch (IOException var5) {
                DebugType.General.printException(var5, LogSeverity.Error);
            }

            this.characterTraits.clear();
            short traitsSize = b.getShort();

            for (int i = 0; i < traitsSize; i++) {
                this.characterTraits.add(CharacterTrait.get(ResourceLocation.of(b.getUTF())));
            }
        }

        if (GameClient.client) {
            this.playerIndex = b.getByte();

            try {
                this.player = null;

                for (AddCoopPlayer p : IsoWorld.instance.addCoopPlayers) {
                    if (p != null && p.getPlayerIndex() == this.playerIndex) {
                        this.player = p.getPlayer();
                    }
                }

                if (this.player == null) {
                    this.player = new IsoPlayer(IsoWorld.instance.currentCell);
                }

                this.player.load(b.bb, IsoWorld.getWorldVersion());
                if (this.playerIndex != 0) {
                    IsoWorld.instance.currentCell.getAddList().remove(this.player);
                    IsoWorld.instance.currentCell.getObjectList().remove(this.player);
                }
            } catch (IOException var6) {
                DebugType.General.printException(var6, LogSeverity.Error);
            }
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        if (GameClient.client) {
            b.putByte(this.playerIndex);
            b.putUTF(this.spawnRegionName);

            try {
                this.survivorDescriptor.save(b.bb);
                this.survivorDescriptor.getWornItems().save(b.bb);
                this.survivorDescriptor.getHumanVisual().save(b.bb);
            } catch (IOException var5) {
                DebugType.General.printException(var5, LogSeverity.Error);
            }

            b.putShort(this.characterTraits.size());

            for (CharacterTrait characterTrait : this.characterTraits) {
                b.putUTF(characterTrait.toString());
            }
        }

        if (GameServer.server) {
            b.putByte(this.playerIndex);

            try {
                this.player.save(b.bb);
            } catch (IOException var4) {
                DebugType.General.printException(var4, LogSeverity.Error);
            }
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        int spawnPointX = 0;
        int spawnPointY = 0;
        int spawnPointZ = 0;
        String withPlayerPrefix = "WithPlayer";
        if (this.spawnRegionName != null && this.spawnRegionName.length() > "WithPlayer".length() && this.spawnRegionName.startsWith("WithPlayer")) {
            int playerId = this.spawnRegionName.charAt("WithPlayer".length()) - '1';
            if (playerId >= 0 && playerId < 4) {
                boolean indexIsValid = ServerOptions.getInstance().playerRespawnWithSelf.getValue() && this.playerIndex == playerId
                    || ServerOptions.getInstance().playerRespawnWithOther.getValue() && this.playerIndex != playerId;
                IsoPlayer player = connection.getPlayerAt(playerId);
                if (indexIsValid && player != null) {
                    spawnPointX = (int)player.getX();
                    spawnPointY = (int)player.getY();
                    spawnPointZ = (int)player.getZ();
                }
            }
        } else if (!ServerOptions.instance.spawnPoint.getValue().isEmpty()
            && !ServerOptions.instance.spawnPoint.getDefaultValue().equals(ServerOptions.instance.spawnPoint.getValue())) {
            String[] spawnPoint = ServerOptions.instance.spawnPoint.getValue().split(",");
            if (spawnPoint.length == 3) {
                try {
                    spawnPointX = Integer.parseInt(spawnPoint[0].trim());
                    spawnPointY = Integer.parseInt(spawnPoint[1].trim());
                    spawnPointZ = Integer.parseInt(spawnPoint[2].trim());
                } catch (NumberFormatException var18) {
                    DebugLog.log("ERROR: SpawnPoint must be x,y,z, got \"" + ServerOptions.instance.spawnPoint.getValue() + "\"");
                }
            } else {
                DebugLog.log("ERROR: SpawnPoint must be x,y,z, got \"" + ServerOptions.instance.spawnPoint.getValue() + "\"");
            }
        } else {
            KahluaTable spawnRegions = SpawnPoints.instance.getSpawnRegions();
            KahluaTableIterator it = spawnRegions.iterator();

            while (it.advance()) {
                KahluaTable spawnRegion = (KahluaTable)it.getValue();
                if (spawnRegion.getString("name").equals(this.spawnRegionName)) {
                    KahluaTable pointsByProfession = (KahluaTable)spawnRegion.rawget("points");
                    if (pointsByProfession != null && !pointsByProfession.isEmpty()) {
                        KahluaTable points = (KahluaTable)pointsByProfession.rawget(this.survivorDescriptor.getCharacterProfession().getName());
                        if (points == null) {
                            points = (KahluaTable)pointsByProfession.rawget(CharacterProfession.UNEMPLOYED.getName());
                        }

                        if (points != null && !points.isEmpty()) {
                            KahluaTable point = (KahluaTable)points.rawget(Rand.Next(1, points.len()));
                            if (point != null) {
                                if (point.rawget("worldX") != null) {
                                    Double spawnCellX = (Double)point.rawget("worldX");
                                    Double spawnCellY = (Double)point.rawget("worldY");
                                    Double posX = (Double)point.rawget("posX");
                                    Double posY = (Double)point.rawget("posY");
                                    Double posZ = (Double)point.rawget("posZ");
                                    if (posZ == null) {
                                        posZ = 0.0;
                                    }

                                    spawnPointX = (int)(spawnCellX * 300.0 + posX);
                                    spawnPointY = (int)(spawnCellY * 300.0 + posY);
                                    spawnPointZ = posZ.intValue();
                                } else {
                                    Double posX = (Double)point.rawget("posX");
                                    Double posY = (Double)point.rawget("posY");
                                    Double posZ = (Double)point.rawget("posZ");
                                    if (posZ == null) {
                                        posZ = 0.0;
                                    }

                                    spawnPointX = posX.intValue();
                                    spawnPointY = posY.intValue();
                                    spawnPointZ = posZ.intValue();
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (spawnPointX == 0 && spawnPointY == 0) {
            ArrayList<IsoGameCharacter.Location> spawnPoints = SpawnPoints.instance.getSpawnPoints();
            IsoGameCharacter.Location location = spawnPoints.get(Rand.Next(0, spawnPoints.size() - 1));
            spawnPointX = location.x;
            spawnPointY = location.y;
            spawnPointZ = location.z;
        }

        if (ServerOptions.instance.safehouseAllowRespawn.getValue()) {
            SafeHouse safe = SafeHouse.hasSafehouse(connection.getUserName());
            if (safe != null && safe.isRespawnInSafehouse(connection.getUserName())) {
                spawnPointX = safe.getX() + safe.getH() / 2;
                spawnPointY = safe.getY() + safe.getW() / 2;
                spawnPointZ = 0;
            }
        }

        if (!ServerOptions.getInstance().open.getValue()
            && !ServerWorldDatabase.instance.containsUser(connection.getUserName())
            && !connection.getRole().hasCapability(Capability.CanAlwaysJoinServer)) {
            GameServer.kick(connection, "UI_Policy_Kick", "UI_OnConnectFailed_AccessDenied");
            connection.forceDisconnect("UI_OnConnectFailed_AccessDenied");
        } else {
            IsoChunkMap.worldXa = spawnPointX;
            IsoChunkMap.worldYa = spawnPointY;
            IsoChunkMap.worldZa = spawnPointZ;
            DebugType.General.warn("position:" + spawnPointX + "," + spawnPointY + "," + spawnPointZ);
            this.player = new IsoPlayer(IsoWorld.instance.currentCell, this.survivorDescriptor, 0, 0, 0);
            this.player.setX(spawnPointX + 0.5F);
            this.player.setY(spawnPointY + 0.5F);
            this.player.setZ(spawnPointZ);
            this.player.setDir(IsoDirections.SE);
            this.player.applyTraits(this.characterTraits);
            this.player.applyProfessionRecipes();
            this.player.applyCharacterTraitsRecipes();
            this.player.setUsername(connection.getUserName());
            if (SteamUtils.isSteamModeEnabled()) {
                this.player.setSteamID(connection.getSteamId());
            }

            LuaEventManager.triggerEvent("OnNewGame", this.player, null);
            ServerPlayerDB.getInstance().serverUpdateNetworkCharacter(this.player, this.playerIndex, connection);
            ServerPlayerDB.getInstance().process();
            ByteBufferWriter b = connection.startPacket();
            PacketTypes.PacketType.CreatePlayer.doPacket(b);
            this.write(b);
            PacketTypes.PacketType.CreatePlayer.send(connection);
        }
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return GameServer.server && this.survivorDescriptor != null || GameClient.client && this.player != null;
    }

    @Override
    public String getDescription() {
        String newStr = "\n    ";
        if (!GameServer.server) {
            if (GameClient.client) {
                String s = "\n\t" + this.getClass().getSimpleName() + " [\n    ";
                s = s + "playerIndex=" + this.playerIndex + " | \n    ";
                return s + "player=" + this.player.getDescription("\n    ") + " ] ";
            } else {
                return null;
            }
        } else {
            StringBuilder s = new StringBuilder("CreatePlayer [\n    ");
            s.append("playerIndex=").append(this.playerIndex).append(" | ").append("\n    ");
            s.append("spawnRegionName=").append(this.spawnRegionName).append(" | ").append("\n    ");
            s.append("traits=");

            for (CharacterTrait characterTrait : this.characterTraits) {
                s.append(characterTrait.toString()).append(",");
            }

            s.append(" | ");
            s.append("descriptor=").append(this.survivorDescriptor.getDescription("\n    ")).append("] ");
            return s.toString();
        }
    }
}
