// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import java.util.Arrays;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.raknet.UdpConnection;
import zombie.core.textures.Texture;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.network.packets.INetworkPacket;

public final class ClientServerMap {
    private static final int ChunksPerServerCell = 8;
    private static final int SquaresPerServerCell = 64;
    public int playerIndex;
    public int centerX;
    public int centerY;
    int chunkGridWidth;
    public int width;
    public boolean[] loaded;
    private static boolean[] isLoaded;
    private static Texture trafficCone;

    public ClientServerMap(int playerIndex, int squareX, int squareY, int chunkGridWidth) {
        this.playerIndex = playerIndex;
        this.centerX = squareX;
        this.centerY = squareY;
        this.chunkGridWidth = chunkGridWidth;
        this.width = (chunkGridWidth - 1) * 8 / 64;
        if ((chunkGridWidth - 1) * 8 % 64 != 0) {
            this.width++;
        }

        this.width++;
        this.loaded = new boolean[this.width * this.width];
    }

    public int getMinX() {
        return PZMath.coorddivision(PZMath.coorddivision(this.centerX, 8) - this.chunkGridWidth / 2, 8);
    }

    public int getMinY() {
        return PZMath.coorddivision(PZMath.coorddivision(this.centerY, 8) - this.chunkGridWidth / 2, 8);
    }

    public int getMaxX() {
        return this.getMinX() + this.width - 1;
    }

    public int getMaxY() {
        return this.getMinY() + this.width - 1;
    }

    public boolean isValidCell(int x, int y) {
        return x >= 0 && y >= 0 && x < this.width && y < this.width;
    }

    public boolean setLoaded() {
        if (!GameServer.server) {
            return false;
        } else {
            int serverMapMinX = ServerMap.instance.getMinX();
            int serverMapMinY = ServerMap.instance.getMinY();
            int minX = this.getMinX();
            int minY = this.getMinY();
            boolean changed = false;

            for (int y = 0; y < this.width; y++) {
                for (int x = 0; x < this.width; x++) {
                    ServerMap.ServerCell cell = ServerMap.instance.getCell(minX + x - serverMapMinX, minY + y - serverMapMinY);
                    boolean isLoaded = cell == null ? false : cell.isLoaded;
                    changed |= this.loaded[x + y * this.width] != isLoaded;
                    this.loaded[x + y * this.width] = isLoaded;
                }
            }

            return changed;
        }
    }

    public boolean setPlayerPosition(int squareX, int squareY) {
        if (!GameServer.server) {
            return false;
        } else {
            int oldMinX = this.getMinX();
            int oldMinY = this.getMinY();
            this.centerX = squareX;
            this.centerY = squareY;
            return this.setLoaded() || oldMinX != this.getMinX() || oldMinY != this.getMinY();
        }
    }

    public static boolean isChunkLoaded(int wx, int wy) {
        if (!GameClient.client) {
            return false;
        } else {
            for (int pn = 0; pn < IsoPlayer.numPlayers; pn++) {
                ClientServerMap csm = GameClient.loadedCells[pn];
                if (csm != null) {
                    int cx = PZMath.coorddivision(wx, 8) - csm.getMinX();
                    int cy = PZMath.coorddivision(wy, 8) - csm.getMinY();
                    if (csm.isValidCell(cx, cy) && csm.loaded[cx + cy * csm.width]) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    public static void characterIn(UdpConnection connection, int playerIndex) {
        if (GameServer.server) {
            ClientServerMap csm = connection.loadedCells[playerIndex];
            if (csm != null) {
                IsoPlayer player = connection.players[playerIndex];
                if (player != null) {
                    if (csm.setPlayerPosition(PZMath.fastfloor(player.getX()), PZMath.fastfloor(player.getY()))) {
                        csm.sendPacket(connection);
                    }
                }
            }
        }
    }

    public void sendPacket(UdpConnection connection) {
        if (GameServer.server) {
            INetworkPacket.send(connection, PacketTypes.PacketType.ServerMap, this);
        }
    }

    public static void render(int playerIndex) {
        if (GameClient.client) {
            IsoChunkMap chunkMap = IsoWorld.instance.currentCell.getChunkMap(playerIndex);
            if (chunkMap != null && !chunkMap.ignore) {
                int SCL = Core.tileScale;
                int CPW = 8;
                float R = 0.1F;
                float G = 0.1F;
                float B = 0.1F;
                float A = 0.75F;
                float z = 0.0F;
                if (trafficCone == null) {
                    trafficCone = Texture.getSharedTexture("street_decoration_01_26");
                }

                Texture tex = trafficCone;
                if (isLoaded == null || isLoaded.length < IsoChunkMap.chunkGridWidth * IsoChunkMap.chunkGridWidth) {
                    isLoaded = new boolean[IsoChunkMap.chunkGridWidth * IsoChunkMap.chunkGridWidth];
                }

                for (int y = 0; y < IsoChunkMap.chunkGridWidth; y++) {
                    for (int x = 0; x < IsoChunkMap.chunkGridWidth; x++) {
                        IsoChunk chunk = chunkMap.getChunk(x, y);
                        if (chunk != null) {
                            isLoaded[x + y * IsoChunkMap.chunkGridWidth] = isChunkLoaded(chunk.wx, chunk.wy);
                        }
                    }
                }

                for (int y = 0; y < IsoChunkMap.chunkGridWidth; y++) {
                    for (int xx = 0; xx < IsoChunkMap.chunkGridWidth; xx++) {
                        IsoChunk chunk = chunkMap.getChunk(xx, y);
                        if (chunk != null) {
                            boolean loaded = isLoaded[xx + y * IsoChunkMap.chunkGridWidth];
                            if (loaded && tex != null) {
                                IsoChunk chunkN = chunkMap.getChunk(xx, y - 1);
                                if (chunkN != null && !isLoaded[xx + (y - 1) * IsoChunkMap.chunkGridWidth]) {
                                    for (int xxx = 0; xxx < 8; xxx++) {
                                        float sx = IsoUtils.XToScreenExact(chunk.wx * 8 + xxx, chunk.wy * 8, 0.0F, 0);
                                        float sy = IsoUtils.YToScreenExact(chunk.wx * 8 + xxx, chunk.wy * 8, 0.0F, 0);
                                        SpriteRenderer.instance
                                            .render(tex, sx - tex.getWidth() / 2, sy, tex.getWidth(), tex.getHeight(), 1.0F, 1.0F, 1.0F, 1.0F, null);
                                    }
                                }

                                IsoChunk chunkS = chunkMap.getChunk(xx, y + 1);
                                if (chunkS != null && !isLoaded[xx + (y + 1) * IsoChunkMap.chunkGridWidth]) {
                                    for (int xxx = 0; xxx < 8; xxx++) {
                                        float sx = IsoUtils.XToScreenExact(chunk.wx * 8 + xxx, chunk.wy * 8 + 8 - 1, 0.0F, 0);
                                        float sy = IsoUtils.YToScreenExact(chunk.wx * 8 + xxx, chunk.wy * 8 + 8 - 1, 0.0F, 0);
                                        SpriteRenderer.instance
                                            .render(tex, sx - tex.getWidth() / 2, sy, tex.getWidth(), tex.getHeight(), 1.0F, 1.0F, 1.0F, 1.0F, null);
                                    }
                                }

                                IsoChunk chunkW = chunkMap.getChunk(xx - 1, y);
                                if (chunkW != null && !isLoaded[xx - 1 + y * IsoChunkMap.chunkGridWidth]) {
                                    for (int yy = 0; yy < 8; yy++) {
                                        float sx = IsoUtils.XToScreenExact(chunk.wx * 8, chunk.wy * 8 + yy, 0.0F, 0);
                                        float sy = IsoUtils.YToScreenExact(chunk.wx * 8, chunk.wy * 8 + yy, 0.0F, 0);
                                        SpriteRenderer.instance
                                            .render(tex, sx - tex.getWidth() / 2, sy, tex.getWidth(), tex.getHeight(), 1.0F, 1.0F, 1.0F, 1.0F, null);
                                    }
                                }

                                IsoChunk chunkE = chunkMap.getChunk(xx + 1, y);
                                if (chunkE != null && !isLoaded[xx + 1 + y * IsoChunkMap.chunkGridWidth]) {
                                    for (int yy = 0; yy < 8; yy++) {
                                        float sx = IsoUtils.XToScreenExact(chunk.wx * 8 + 8 - 1, chunk.wy * 8 + yy, 0.0F, 0);
                                        float sy = IsoUtils.YToScreenExact(chunk.wx * 8 + 8 - 1, chunk.wy * 8 + yy, 0.0F, 0);
                                        SpriteRenderer.instance
                                            .render(tex, sx - tex.getWidth() / 2, sy, tex.getWidth(), tex.getHeight(), 1.0F, 1.0F, 1.0F, 1.0F, null);
                                    }
                                }
                            }

                            if (!loaded) {
                                float left = chunk.wx * 8;
                                float top = chunk.wy * 8;
                                float sx = IsoUtils.XToScreenExact(left, top + 8.0F, 0.0F, 0);
                                float sy = IsoUtils.YToScreenExact(left, top + 8.0F, 0.0F, 0);
                                SpriteRenderer.instance
                                    .renderPoly(
                                        (int)sx,
                                        (int)sy,
                                        (int)(sx + 256 * SCL),
                                        (int)(sy - 128 * SCL),
                                        (int)(sx + 512 * SCL),
                                        (int)sy,
                                        (int)(sx + 256 * SCL),
                                        (int)(sy + 128 * SCL),
                                        0.1F,
                                        0.1F,
                                        0.1F,
                                        0.75F
                                    );
                            }
                        }
                    }
                }
            }
        }
    }

    public static void Reset() {
        Arrays.fill(GameClient.loadedCells, null);
        trafficCone = null;
    }
}
