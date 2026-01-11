// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util;

import zombie.SoundManager;
import zombie.Lua.LuaEventManager;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.physics.WorldSimulation;
import zombie.debug.DebugLog;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.LightingJNI;
import zombie.iso.LosUtil;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.connection.ConnectCoopPacket;
import zombie.popman.ZombiePopulationManager;
import zombie.ui.UIManager;

public final class AddCoopPlayer {
    private AddCoopPlayer.Stage stage;
    private final IsoPlayer player;

    public AddCoopPlayer(IsoPlayer player, boolean newPlayer) {
        this.player = player;
        if (newPlayer) {
            this.stage = AddCoopPlayer.Stage.SendCoopConnect;
        } else {
            this.stage = AddCoopPlayer.Stage.Init;
        }
    }

    public int getPlayerIndex() {
        return this.player == null ? -1 : this.player.playerIndex;
    }

    public IsoPlayer getPlayer() {
        return this.player;
    }

    public void update() {
        switch (this.stage) {
            case SendCoopConnect:
                if (GameClient.client) {
                    GameClient.sendCreatePlayer((byte)this.player.playerIndex);
                    this.stage = AddCoopPlayer.Stage.ReceiveCoopConnect;
                } else {
                    this.stage = AddCoopPlayer.Stage.Init;
                }
            case ReceiveCoopConnect:
            case ReceiveClientConnect:
            case ReceivePlayerConnect:
            case Finished:
            default:
                break;
            case Init:
                if (GameClient.client) {
                    ConnectCoopPacket packetx = new ConnectCoopPacket();
                    packetx.setInit(this.player);
                    ByteBufferWriter bx = GameClient.connection.startPacket();
                    PacketTypes.PacketType.ConnectCoop.doPacket(bx);
                    packetx.write(bx);
                    PacketTypes.PacketType.ConnectCoop.send(GameClient.connection);
                    this.stage = AddCoopPlayer.Stage.ReceiveClientConnect;
                } else {
                    this.stage = AddCoopPlayer.Stage.StartMapLoading;
                }
                break;
            case StartMapLoading:
                IsoCell cellx = IsoWorld.instance.currentCell;
                int playerIndex = this.player.playerIndex;
                IsoChunkMap chunkMap = cellx.chunkMap[playerIndex];
                IsoChunkMap.bSettingChunk.lock();

                try {
                    chunkMap.Unload();
                    chunkMap.ignore = false;
                    int wx = (int)(this.player.getX() / 8.0F);
                    int wy = (int)(this.player.getY() / 8.0F);

                    try {
                        if (LightingJNI.init) {
                            LightingJNI.teleport(playerIndex, wx - IsoChunkMap.chunkGridWidth / 2, wy - IsoChunkMap.chunkGridWidth / 2);
                        }
                    } catch (Exception var16) {
                    }

                    if (!GameServer.server && !GameClient.client) {
                        ZombiePopulationManager.instance
                            .playerSpawnedAt(PZMath.fastfloor(this.player.getX()), PZMath.fastfloor(this.player.getY()), PZMath.fastfloor(this.player.getZ()));
                    }

                    chunkMap.worldX = wx;
                    chunkMap.worldY = wy;
                    if (!GameServer.server) {
                        WorldSimulation.instance.activateChunkMap(playerIndex);
                    }

                    int minwx = wx - IsoChunkMap.chunkGridWidth / 2;
                    int minwy = wy - IsoChunkMap.chunkGridWidth / 2;
                    int maxwx = wx + IsoChunkMap.chunkGridWidth / 2 + 1;
                    int maxwy = wy + IsoChunkMap.chunkGridWidth / 2 + 1;

                    for (int xx = minwx; xx < maxwx; xx++) {
                        for (int y = minwy; y < maxwy; y++) {
                            if (IsoWorld.instance.getMetaGrid().isValidChunk(xx, y)) {
                                IsoChunk chunk = chunkMap.LoadChunkForLater(xx, y, xx - minwx, y - minwy);
                                if (chunk != null && chunk.loaded) {
                                    cellx.setCacheChunk(chunk, playerIndex);
                                }
                            }
                        }
                    }

                    chunkMap.SwapChunkBuffers();
                } finally {
                    IsoChunkMap.bSettingChunk.unlock();
                }

                this.stage = AddCoopPlayer.Stage.CheckMapLoading;
                break;
            case CheckMapLoading:
                IsoCell cellx = IsoWorld.instance.currentCell;
                IsoChunkMap chunkMap = cellx.chunkMap[this.player.playerIndex];
                chunkMap.update();

                for (int y = 0; y < IsoChunkMap.chunkGridWidth; y++) {
                    for (int x = 0; x < IsoChunkMap.chunkGridWidth; x++) {
                        if (IsoWorld.instance.getMetaGrid().isValidChunk(chunkMap.getWorldXMin() + x, chunkMap.getWorldYMin() + y)
                            && chunkMap.getChunk(x, y) == null) {
                            return;
                        }
                    }
                }

                chunkMap.calculateZExtentsForChunkMap();
                IsoGridSquare sq = cellx.getGridSquare(
                    PZMath.fastfloor(this.player.getX()), PZMath.fastfloor(this.player.getY()), PZMath.fastfloor(this.player.getZ())
                );
                if (sq != null && sq.getRoom() != null) {
                    sq.getRoom().def.setExplored(true);
                    sq.getRoom().building.setAllExplored(true);
                }

                this.stage = GameClient.client ? AddCoopPlayer.Stage.SendPlayerConnect : AddCoopPlayer.Stage.AddToWorld;
                break;
            case SendPlayerConnect:
                GameClient.connection.username = this.player.username;
                ConnectCoopPacket packet = new ConnectCoopPacket();
                packet.setPlayerConnect(this.player);
                ByteBufferWriter b = GameClient.connection.startPacket();
                PacketTypes.PacketType.ConnectCoop.doPacket(b);
                packet.write(b);
                PacketTypes.PacketType.ConnectCoop.send(GameClient.connection);
                this.stage = AddCoopPlayer.Stage.ReceivePlayerConnect;
                break;
            case AddToWorld:
                IsoPlayer.players[this.player.playerIndex] = this.player;
                LosUtil.cachecleared[this.player.playerIndex] = true;
                this.player.updateLightInfo();
                IsoCell cell = IsoWorld.instance.currentCell;
                this.player.setCurrentSquareFromPosition();
                this.player.updateUsername();
                this.player.setSceneCulled(false);
                if (cell.isSafeToAdd()) {
                    cell.getObjectList().add(this.player);
                } else {
                    cell.getAddList().add(this.player);
                }

                this.player.getInventory().addItemsToProcessItems();
                LuaEventManager.triggerEvent("OnCreatePlayer", this.player.playerIndex, this.player);
                if (this.player.isAsleep()) {
                    UIManager.setFadeBeforeUI(this.player.playerIndex, true);
                    UIManager.FadeOut(this.player.playerIndex, 2.0);
                    UIManager.setFadeTime(this.player.playerIndex, 0.0);
                }

                this.stage = AddCoopPlayer.Stage.Finished;
                SoundManager.instance.stopMusic("PlayerDied");
        }
    }

    public boolean isFinished() {
        return this.stage == AddCoopPlayer.Stage.Finished;
    }

    public void playerCreated(int playerIndex) {
        if (this.player.playerIndex == playerIndex) {
            DebugLog.Multiplayer.debugln("created player=%d", playerIndex, (short)4);
            this.stage = AddCoopPlayer.Stage.Init;
        }
    }

    public void accessGranted(int playerIndex) {
        if (this.player.playerIndex == playerIndex) {
            DebugLog.log("coop player=" + (playerIndex + 1) + "/4 access granted");
            this.stage = AddCoopPlayer.Stage.StartMapLoading;
        }
    }

    public void accessDenied(int playerIndex, String reason) {
        if (this.player.playerIndex == playerIndex) {
            DebugLog.log("coop player=" + (playerIndex + 1) + "/4 access denied: " + reason);
            IsoCell cell = IsoWorld.instance.currentCell;
            int n = this.player.playerIndex;
            IsoChunkMap chunkMap = cell.chunkMap[n];
            chunkMap.Unload();
            chunkMap.ignore = true;
            this.stage = AddCoopPlayer.Stage.Finished;
            LuaEventManager.triggerEvent("OnCoopJoinFailed", playerIndex);
        }
    }

    public void receivePlayerConnect(int playerIndex) {
        if (this.player.playerIndex == playerIndex) {
            this.stage = AddCoopPlayer.Stage.AddToWorld;
            this.update();
        }
    }

    public boolean isLoadingThisSquare(int x, int y) {
        int wx = (int)(this.player.getX() / 8.0F);
        int wy = (int)(this.player.getY() / 8.0F);
        int minX = wx - IsoChunkMap.chunkGridWidth / 2;
        int minY = wy - IsoChunkMap.chunkGridWidth / 2;
        int maxX = minX + IsoChunkMap.chunkGridWidth;
        int maxY = minY + IsoChunkMap.chunkGridWidth;
        x /= 8;
        y /= 8;
        return x >= minX && x < maxX && y >= minY && y < maxY;
    }

    public static enum Stage {
        SendCoopConnect,
        ReceiveCoopConnect,
        Init,
        ReceiveClientConnect,
        StartMapLoading,
        CheckMapLoading,
        SendPlayerConnect,
        ReceivePlayerConnect,
        AddToWorld,
        Finished;
    }
}
