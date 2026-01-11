// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import org.joml.Vector3f;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.network.packets.INetworkPacket;
import zombie.vehicles.BaseVehicle;

public final class PassengerMap {
    private static final int CHUNKS = 7;
    private static final int MAX_PASSENGERS = 16;
    private static final PassengerMap.PassengerLocal[] perPlayerPngr = new PassengerMap.PassengerLocal[4];
    private static final PassengerMap.DriverLocal[] perPlayerDriver = new PassengerMap.DriverLocal[4];

    public static void updatePassenger(IsoPlayer player) {
        if (player != null && player.getVehicle() != null && !player.getVehicle().isDriver(player)) {
            if (player.getVehicle().getDriver() instanceof IsoPlayer isoPlayer && !isoPlayer.isLocalPlayer()) {
                PassengerMap.PassengerLocal pngrLocal = perPlayerPngr[player.playerIndex];
                pngrLocal.chunkMap = IsoWorld.instance.currentCell.chunkMap[player.playerIndex];
                pngrLocal.updateLoaded();
            }
        }
    }

    public static void clientReceivePacket(int pn, int seat, int wx, int wy, long loaded) {
        PassengerMap.DriverLocal driverLocal = perPlayerDriver[pn];
        PassengerMap.PassengerRemote pngrRemote = driverLocal.passengers[seat];
        if (pngrRemote == null) {
            pngrRemote = driverLocal.passengers[seat] = new PassengerMap.PassengerRemote();
        }

        pngrRemote.setLoaded(wx, wy, loaded);
    }

    public static boolean isChunkLoaded(BaseVehicle vehicle, int wx, int wy) {
        if (!GameClient.client) {
            return false;
        } else if (vehicle == null) {
            return false;
        } else if (vehicle.getDriver() instanceof IsoPlayer player && player.isLocalPlayer()) {
            int playerIndex = player.playerIndex;
            PassengerMap.DriverLocal driverLocal = perPlayerDriver[playerIndex];

            for (int seat = 1; seat < vehicle.getMaxPassengers(); seat++) {
                PassengerMap.PassengerRemote pngrRemote = driverLocal.passengers[seat];
                if (pngrRemote != null && pngrRemote.wx != -1) {
                    if (vehicle.getCharacter(seat) instanceof IsoPlayer isoPlayer && !isoPlayer.isLocalPlayer()) {
                        int minX = pngrRemote.wx - 3;
                        int minY = pngrRemote.wy - 3;
                        if (wx >= minX && wy >= minY && wx < minX + 7 && wy < minY + 7 && (pngrRemote.loaded & 1L << wx - minX + (wy - minY) * 7) == 0L) {
                            return false;
                        }
                    } else {
                        pngrRemote.wx = -1;
                    }
                }
            }

            return true;
        } else {
            return false;
        }
    }

    public static void render(int playerIndex) {
        if (GameClient.client) {
            IsoPlayer player = IsoPlayer.players[playerIndex];
            if (player != null && player.getVehicle() != null) {
                BaseVehicle vehicle = player.getVehicle();
                int SCL = Core.tileScale;
                int CPW = 8;
                float R = 0.1F;
                float G = 0.1F;
                float B = 0.1F;
                float A = 0.75F;
                float z = 0.0F;
                PassengerMap.DriverLocal drvrLocal = perPlayerDriver[playerIndex];

                for (int seat = 1; seat < vehicle.getMaxPassengers(); seat++) {
                    PassengerMap.PassengerRemote pngrRemote = drvrLocal.passengers[seat];
                    if (pngrRemote != null && pngrRemote.wx != -1) {
                        if (vehicle.getCharacter(seat) instanceof IsoPlayer isoPlayer && !isoPlayer.isLocalPlayer()) {
                            for (int y = 0; y < 7; y++) {
                                for (int x = 0; x < 7; x++) {
                                    boolean bLoaded = (pngrRemote.loaded & 1L << x + y * 7) != 0L;
                                    if (!bLoaded) {
                                        float left = (pngrRemote.wx - 3 + x) * 8;
                                        float top = (pngrRemote.wy - 3 + y) * 8;
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
                        } else {
                            pngrRemote.wx = -1;
                        }
                    }
                }
            }
        }
    }

    public static void Reset() {
        for (int pn = 0; pn < 4; pn++) {
            PassengerMap.PassengerLocal pngrLocal = perPlayerPngr[pn];
            pngrLocal.wx = -1;
            PassengerMap.DriverLocal drvrLocal = perPlayerDriver[pn];

            for (int seat = 0; seat < 16; seat++) {
                PassengerMap.PassengerRemote pngrRemote = drvrLocal.passengers[seat];
                if (pngrRemote != null) {
                    pngrRemote.wx = -1;
                }
            }
        }
    }

    static {
        for (int i = 0; i < 4; i++) {
            perPlayerPngr[i] = new PassengerMap.PassengerLocal(i);
            perPlayerDriver[i] = new PassengerMap.DriverLocal();
        }
    }

    private static final class DriverLocal {
        final PassengerMap.PassengerRemote[] passengers = new PassengerMap.PassengerRemote[16];
    }

    private static final class PassengerLocal {
        final int playerIndex;
        IsoChunkMap chunkMap;
        int wx = -1;
        int wy = -1;
        long loaded;

        PassengerLocal(int playerIndex) {
            this.playerIndex = playerIndex;
        }

        boolean setLoaded() {
            int wx = this.chunkMap.worldX;
            int wy = this.chunkMap.worldY;
            Vector3f velocity = IsoPlayer.players[this.playerIndex].getVehicle().jniLinearVelocity;
            float absX = Math.abs(velocity.x);
            float absY = Math.abs(velocity.z);
            boolean moveW = velocity.x < 0.0F && absX > absY;
            boolean moveE = velocity.x > 0.0F && absX > absY;
            boolean moveN = velocity.z < 0.0F && absY > absX;
            boolean moveS = velocity.z > 0.0F && absY > absX;
            if (moveE) {
                wx++;
            } else if (moveW) {
                wx--;
            } else if (moveN) {
                wy--;
            } else if (moveS) {
                wy++;
            }

            long loaded = 0L;

            for (int y = 0; y < 7; y++) {
                for (int x = 0; x < 7; x++) {
                    IsoChunk chunk = this.chunkMap.getChunk(IsoChunkMap.chunkGridWidth / 2 - 3 + x, IsoChunkMap.chunkGridWidth / 2 - 3 + y);
                    if (chunk != null && chunk.loaded) {
                        loaded |= 1L << x + y * 7;
                    }
                }
            }

            boolean changed = wx != this.wx || wy != this.wy || loaded != this.loaded;
            if (changed) {
                this.wx = wx;
                this.wy = wy;
                this.loaded = loaded;
            }

            return changed;
        }

        void updateLoaded() {
            if (this.setLoaded()) {
                INetworkPacket.send(PacketTypes.PacketType.VehiclePassengerRequest, this.playerIndex, this.wx, this.wy, this.loaded);
            }
        }
    }

    private static final class PassengerRemote {
        int wx = -1;
        int wy = -1;
        long loaded;

        void setLoaded(int wx, int wy, long loaded) {
            this.wx = wx;
            this.wy = wy;
            this.loaded = loaded;
        }
    }
}
