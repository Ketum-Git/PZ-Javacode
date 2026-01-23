// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.ai.states.ZombieIdleState;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.SliceY;
import zombie.iso.objects.IsoFireManager;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerMap;

public final class ReanimatedPlayers {
    public static ReanimatedPlayers instance = new ReanimatedPlayers();
    private final ArrayList<IsoZombie> zombies = new ArrayList<>();

    public void addReanimatedPlayersToChunk(IsoChunk chunk) {
        int minX = chunk.wx * 8;
        int minY = chunk.wy * 8;
        int maxX = minX + 8;
        int maxY = minY + 8;

        for (int i = 0; i < this.zombies.size(); i++) {
            IsoZombie zombie = this.zombies.get(i);
            if (zombie.getX() >= minX && zombie.getX() < maxX && zombie.getY() >= minY && zombie.getY() < maxY) {
                IsoGridSquare square = chunk.getGridSquare(
                    PZMath.fastfloor(zombie.getX()) - minX, PZMath.fastfloor(zombie.getY()) - minY, PZMath.fastfloor(zombie.getZ())
                );
                if (square != null) {
                    if (GameServer.server) {
                        if (zombie.onlineId != -1) {
                            DebugLog.Zombie.error("ERROR? OnlineID != -1 for reanimated player zombie");
                        }

                        zombie.onlineId = ServerMap.instance.getUniqueZombieId();
                        if (zombie.onlineId == -1) {
                            continue;
                        }

                        ServerMap.instance.zombieMap.put(zombie.onlineId, zombie);
                    }

                    zombie.setCurrent(square);

                    assert !IsoWorld.instance.currentCell.getObjectList().contains(zombie);

                    assert !IsoWorld.instance.currentCell.getZombieList().contains(zombie);

                    IsoWorld.instance.currentCell.getObjectList().add(zombie);
                    IsoWorld.instance.currentCell.getZombieList().add(zombie);
                    this.zombies.remove(i);
                    i--;
                    SharedDescriptors.createPlayerZombieDescriptor(zombie);
                    DebugLog.Zombie.debugln("Added to world " + zombie);
                }
            }
        }
    }

    public void removeReanimatedPlayerFromWorld(IsoZombie zombie) {
        if (zombie.isReanimatedPlayer()) {
            if (!GameServer.server) {
                zombie.setSceneCulled(true);
            }

            if (zombie.isOnFire()) {
                IsoFireManager.RemoveBurningCharacter(zombie);
                zombie.setOnFire(false);
            }

            if (zombie.attachedAnimSprite != null) {
                ArrayList<IsoSpriteInstance> attachedAnimSprite = zombie.attachedAnimSprite;

                for (int i = 0; i < attachedAnimSprite.size(); i++) {
                    IsoSpriteInstance isoSpriteInstance = attachedAnimSprite.get(i);
                    IsoSpriteInstance.add(isoSpriteInstance);
                }

                zombie.attachedAnimSprite.clear();
            }

            if (!GameServer.server) {
                for (int playerIndex = 0; playerIndex < IsoPlayer.numPlayers; playerIndex++) {
                    IsoPlayer player = IsoPlayer.players[playerIndex];
                    if (player != null && player.reanimatedCorpse == zombie) {
                        player.reanimatedCorpse = null;
                        player.reanimatedCorpseId = -1;
                    }
                }
            }

            if (GameServer.server && zombie.onlineId != -1) {
                ServerMap.instance.zombieMap.remove(zombie.onlineId);
                zombie.onlineId = -1;
            }

            SharedDescriptors.releasePlayerZombieDescriptor(zombie);

            assert !VirtualZombieManager.instance.isReused(zombie);

            if (!zombie.isDead()) {
                if (!GameClient.client) {
                    if (!this.zombies.contains(zombie)) {
                        this.zombies.add(zombie);
                        DebugLog.Zombie.debugln("Added to Zombies ", zombie);
                        zombie.setStateMachineLocked(false);
                        zombie.changeState(ZombieIdleState.instance());
                    }
                }
            }
        }
    }

    public void saveReanimatedPlayers() {
        if (!GameClient.client) {
            ArrayList<IsoZombie> zombies = new ArrayList<>();

            try {
                ByteBuffer out = SliceY.SliceBuffer;
                out.clear();
                out.putInt(241);
                zombies.addAll(this.zombies);

                for (IsoZombie zed : IsoWorld.instance.currentCell.getZombieList()) {
                    if (zed.isReanimatedPlayer() && !zed.isDead() && !zombies.contains(zed)) {
                        zombies.add(zed);
                    }
                }

                out.putInt(zombies.size());

                for (IsoZombie zedx : zombies) {
                    zedx.save(out);
                }

                File file = ZomboidFileSystem.instance.getFileInCurrentSave("reanimated.bin");
                FileOutputStream fos = new FileOutputStream(file);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                bos.write(out.array(), 0, out.position());
                bos.flush();
                bos.close();
            } catch (Exception var7) {
                ExceptionLogger.logException(var7);
                return;
            }

            DebugLog.Zombie.debugln("Saved %d zombies", zombies.size());
        }
    }

    public void loadReanimatedPlayers() {
        if (!GameClient.client) {
            this.zombies.clear();
            File file = ZomboidFileSystem.instance.getFileInCurrentSave("reanimated.bin");

            try (
                FileInputStream fis = new FileInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(fis);
            ) {
                synchronized (SliceY.SliceBufferLock) {
                    ByteBuffer in = SliceY.SliceBuffer;
                    in.clear();
                    int numBytes = bis.read(in.array());
                    in.limit(numBytes);
                    this.loadReanimatedPlayers(in);
                }
            } catch (FileNotFoundException var13) {
                return;
            } catch (Exception var14) {
                ExceptionLogger.logException(var14);
                return;
            }

            DebugLog.Zombie.debugln("Loaded %d zombies.", this.zombies.size());
        }
    }

    private void loadReanimatedPlayers(ByteBuffer in) throws IOException, RuntimeException {
        int worldVersion = in.getInt();
        int numZombies = in.getInt();

        for (int i = 0; i < numZombies; i++) {
            if (!(IsoObject.factoryFromFileInput(IsoWorld.instance.currentCell, in) instanceof IsoZombie zombie)) {
                throw new RuntimeException("expected IsoZombie here");
            }

            zombie.load(in, worldVersion);
            zombie.getDescriptor().setID(0);
            zombie.setReanimatedPlayer(true);
            IsoWorld.instance.currentCell.getAddList().remove(zombie);
            IsoWorld.instance.currentCell.getObjectList().remove(zombie);
            IsoWorld.instance.currentCell.getZombieList().remove(zombie);
            this.zombies.add(zombie);
        }
    }
}
