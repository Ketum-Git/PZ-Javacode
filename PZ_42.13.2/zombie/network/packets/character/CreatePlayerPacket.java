// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.character;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.GameWindow;
import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.SurvivorDesc;
import zombie.core.Core;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.iso.IsoCamera;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoDirections;
import zombie.iso.IsoWorld;
import zombie.iso.SpawnPoints;
import zombie.iso.areas.SafeHouse;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.ServerOptions;
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
            DebugLog.Multiplayer.warn(this.getClass().getSimpleName() + ".set get invalid arguments");
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
    public void parse(ByteBuffer b, UdpConnection connection) {
        if (GameServer.server) {
            this.playerIndex = b.get();
            this.spawnRegionName = GameWindow.ReadString(b);

            try {
                this.survivorDescriptor = new SurvivorDesc();
                this.survivorDescriptor.load(b, IsoWorld.getWorldVersion(), null);
                this.survivorDescriptor.getWornItems().load(b, IsoWorld.getWorldVersion());
                this.survivorDescriptor.getHumanVisual().load(b, IsoWorld.getWorldVersion());
            } catch (IOException var5) {
                var5.printStackTrace();
            }

            this.characterTraits.clear();
            short traitsSize = b.getShort();

            for (int i = 0; i < traitsSize; i++) {
                this.characterTraits.add(CharacterTrait.get(ResourceLocation.of(GameWindow.ReadString(b))));
            }
        }

        if (GameClient.client) {
            this.playerIndex = b.get();

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

                this.player.load(b, IsoWorld.getWorldVersion());
                if (this.playerIndex != 0) {
                    IsoWorld.instance.currentCell.getAddList().remove(this.player);
                    IsoWorld.instance.currentCell.getObjectList().remove(this.player);
                }
            } catch (IOException var6) {
                var6.printStackTrace();
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
                var5.printStackTrace();
            }

            b.putShort((short)this.characterTraits.size());

            for (CharacterTrait characterTrait : this.characterTraits) {
                b.putUTF(characterTrait.toString());
            }
        }

        if (GameServer.server) {
            b.putByte(this.playerIndex);

            try {
                this.player.save(b.bb);
            } catch (IOException var4) {
                var4.printStackTrace();
            }
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        int SpawnPointX = 0;
        int SpawnPointY = 0;
        int SpawnPointZ = 0;
        if (!ServerOptions.instance.spawnPoint.getValue().isEmpty()
            && !ServerOptions.instance.spawnPoint.getDefaultValue().equals(ServerOptions.instance.spawnPoint.getValue())) {
            String[] spawnPoint = ServerOptions.instance.spawnPoint.getValue().split(",");
            if (spawnPoint.length == 3) {
                try {
                    SpawnPointX = Integer.parseInt(spawnPoint[0].trim());
                    SpawnPointY = Integer.parseInt(spawnPoint[1].trim());
                    SpawnPointZ = Integer.parseInt(spawnPoint[2].trim());
                } catch (NumberFormatException var17) {
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
                                    Double SpawnCellX = (Double)point.rawget("worldX");
                                    Double SpawnCellY = (Double)point.rawget("worldY");
                                    Double PosX = (Double)point.rawget("posX");
                                    Double PosY = (Double)point.rawget("posY");
                                    Double PosZ = (Double)point.rawget("posZ");
                                    if (PosZ == null) {
                                        PosZ = 0.0;
                                    }

                                    SpawnPointX = (int)(SpawnCellX * 300.0 + PosX);
                                    SpawnPointY = (int)(SpawnCellY * 300.0 + PosY);
                                    SpawnPointZ = PosZ.intValue();
                                } else {
                                    Double PosX = (Double)point.rawget("posX");
                                    Double PosY = (Double)point.rawget("posY");
                                    Double PosZ = (Double)point.rawget("posZ");
                                    if (PosZ == null) {
                                        PosZ = 0.0;
                                    }

                                    SpawnPointX = PosX.intValue();
                                    SpawnPointY = PosY.intValue();
                                    SpawnPointZ = PosZ.intValue();
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (SpawnPointX == 0 && SpawnPointY == 0) {
            ArrayList<IsoGameCharacter.Location> spawnPoints = SpawnPoints.instance.getSpawnPoints();
            IsoGameCharacter.Location location = spawnPoints.get(Rand.Next(0, spawnPoints.size() - 1));
            SpawnPointX = location.x;
            SpawnPointY = location.y;
            SpawnPointZ = location.z;
        }

        if (ServerOptions.instance.safehouseAllowRespawn.getValue()) {
            SafeHouse safe = SafeHouse.hasSafehouse(connection.username);
            if (safe != null && safe.isRespawnInSafehouse(connection.username)) {
                SpawnPointX = safe.getX() + safe.getH() / 2;
                SpawnPointY = safe.getY() + safe.getW() / 2;
                SpawnPointZ = 0;
            }
        }

        IsoChunkMap.worldXa = SpawnPointX;
        IsoChunkMap.worldYa = SpawnPointY;
        IsoChunkMap.worldZa = SpawnPointZ;
        DebugLog.General.warn("position:" + SpawnPointX + "," + SpawnPointY + "," + SpawnPointZ);
        this.player = new IsoPlayer(IsoWorld.instance.currentCell, this.survivorDescriptor, 0, 0, 0);
        this.player.setX(SpawnPointX + 0.5F);
        this.player.setY(SpawnPointY + 0.5F);
        this.player.setZ(SpawnPointZ);
        this.player.setDir(IsoDirections.SE);
        this.player.applyTraits(this.characterTraits);
        this.player.applyProfessionRecipes();
        this.player.applyCharacterTraitsRecipes();
        if (!GameClient.client) {
            Core.getInstance().initPoisonousBerry();
            Core.getInstance().initPoisonousMushroom();
        }

        this.player.setUsername(connection.username);
        LuaEventManager.triggerEvent("OnNewGame", this.player, null);
        ServerPlayerDB.getInstance().serverUpdateNetworkCharacter(this.player, this.playerIndex, connection);
        ServerPlayerDB.getInstance().process();
        ByteBufferWriter b = connection.startPacket();
        PacketTypes.PacketType.CreatePlayer.doPacket(b);
        this.write(b);
        PacketTypes.PacketType.CreatePlayer.send(connection);
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
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
