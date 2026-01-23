// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import zombie.GameTime;
import zombie.core.math.PZMath;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.iso.areas.NonPvpZone;
import zombie.network.GameServer;
import zombie.network.PVPLogTool;
import zombie.network.ServerOptions;
import zombie.util.Type;

public class SafetySystemManager {
    private static final LinkedHashMap<String, Float> playerCooldown = new LinkedHashMap<>();
    private static final LinkedHashMap<String, Boolean> playerSafety = new LinkedHashMap<>();
    private static final LinkedHashMap<String, Long> playerDelay = new LinkedHashMap<>();
    private static final long safetyDelay = 1500L;

    private static void updateTimers(Safety safety) {
        float time = GameTime.instance.getRealworldSecondsSinceLastUpdate();
        if (safety.getToggle() > 0.0F) {
            safety.setToggle(safety.getToggle() - time);
            if (safety.getToggle() <= 0.0F) {
                safety.setToggle(0.0F);
                if (!safety.isLast()) {
                    safety.setEnabled(!safety.isEnabled());
                }
            }
        } else if (safety.getCooldown() > 0.0F) {
            safety.setCooldown(safety.getCooldown() - time);
        } else {
            safety.setCooldown(0.0F);
        }
    }

    private static void updateNonPvpZone(IsoPlayer player, boolean isNonPvpZone) {
        if (isNonPvpZone && !player.networkAi.wasNonPvpZone) {
            storeSafety(player);
            GameServer.sendChangeSafety(player.getSafety());
        } else if (!isNonPvpZone && player.networkAi.wasNonPvpZone) {
            restoreSafety(player);
            GameServer.sendChangeSafety(player.getSafety());
        }

        player.networkAi.wasNonPvpZone = isNonPvpZone;
    }

    static void update(IsoPlayer player) {
        boolean isNonPvpZone = NonPvpZone.getNonPvpZone(PZMath.fastfloor(player.getX()), PZMath.fastfloor(player.getY())) != null;
        if (!isNonPvpZone) {
            updateTimers(player.getSafety());
        }

        if (GameServer.server) {
            updateNonPvpZone(player, isNonPvpZone);
        }
    }

    public static void clear() {
        playerCooldown.clear();
        playerSafety.clear();
        playerDelay.clear();
    }

    public static void clearSafety(IsoPlayer player) {
        playerCooldown.remove(player.getUsername());
        playerSafety.remove(player.getUsername());
        playerDelay.remove(player.getUsername());
        player.getSafety().setCooldown(0.0F);
        player.getSafety().setToggle(0.0F);
        PVPLogTool.logSafety(player, "clear");
    }

    public static void storeSafety(IsoPlayer player) {
        try {
            if (player != null && player.isAlive()) {
                Safety safety = player.getSafety();
                playerSafety.put(player.getUsername(), safety.isEnabled());
                playerCooldown.put(player.getUsername(), safety.getCooldown());
                playerDelay.put(player.getUsername(), System.currentTimeMillis());
                if (playerCooldown.size() > ServerOptions.instance.maxPlayers.getValue() * 1000) {
                    Iterator<Entry<String, Float>> i = playerCooldown.entrySet().iterator();
                    if (i.hasNext()) {
                        i.next();
                        i.remove();
                    }
                }

                if (playerSafety.size() > ServerOptions.instance.maxPlayers.getValue() * 1000) {
                    Iterator<Entry<String, Boolean>> i = playerSafety.entrySet().iterator();
                    if (i.hasNext()) {
                        i.next();
                        i.remove();
                    }
                }

                if (playerDelay.size() > ServerOptions.instance.maxPlayers.getValue() * 1000) {
                    Iterator<Entry<String, Long>> i = playerDelay.entrySet().iterator();
                    if (i.hasNext()) {
                        i.next();
                        i.remove();
                    }
                }

                PVPLogTool.logSafety(player, "store");
            } else {
                DebugLog.Combat.debugln("StoreSafety: player not found");
            }
        } catch (Exception var3) {
            DebugLog.Multiplayer.printException(var3, "StoreSafety failed", LogSeverity.Error);
        }
    }

    public static void restoreSafety(IsoPlayer player) {
        try {
            if (player != null) {
                Safety safety = player.getSafety();
                if (playerSafety.containsKey(player.getUsername())) {
                    safety.setEnabled(playerSafety.remove(player.getUsername()));
                }

                if (playerCooldown.containsKey(player.getUsername())) {
                    safety.setCooldown(playerCooldown.remove(player.getUsername()));
                }

                playerDelay.put(player.getUsername(), System.currentTimeMillis());
                PVPLogTool.logSafety(player, "restore");
            } else {
                DebugLog.Combat.debugln("RestoreSafety: player not found");
            }
        } catch (Exception var2) {
            DebugLog.Multiplayer.printException(var2, "RestoreSafety failed", LogSeverity.Error);
        }
    }

    public static void updateOptions() {
        boolean pvp = ServerOptions.instance.pvp.getValue();
        boolean safety = ServerOptions.instance.safetySystem.getValue();
        if (!pvp) {
            clear();

            for (IsoPlayer player : GameServer.IDToPlayerMap.values()) {
                if (player != null) {
                    player.getSafety().setEnabled(true);
                    player.getSafety().setLast(false);
                    player.getSafety().setCooldown(0.0F);
                    player.getSafety().setToggle(0.0F);
                    GameServer.sendChangeSafety(player.getSafety());
                }
            }
        } else if (!safety) {
            clear();

            for (IsoPlayer playerx : GameServer.IDToPlayerMap.values()) {
                if (playerx != null) {
                    playerx.getSafety().setEnabled(false);
                    playerx.getSafety().setLast(false);
                    playerx.getSafety().setCooldown(0.0F);
                    playerx.getSafety().setToggle(0.0F);
                    GameServer.sendChangeSafety(playerx.getSafety());
                }
            }
        }
    }

    public static boolean checkUpdateDelay(IsoGameCharacter wielder, IsoGameCharacter target) {
        boolean result = false;
        IsoPlayer wielderPlayer = Type.tryCastTo(wielder, IsoPlayer.class);
        IsoPlayer targetPlayer = Type.tryCastTo(target, IsoPlayer.class);
        if (wielderPlayer != null && targetPlayer != null) {
            long time = System.currentTimeMillis();
            if (playerDelay.containsKey(wielderPlayer.getUsername())) {
                long wielderDelay = time - playerDelay.getOrDefault(wielderPlayer.getUsername(), 0L);
                boolean isWielderDelayed = wielderDelay < 1500L;
                result = isWielderDelayed;
                if (!isWielderDelayed) {
                    playerDelay.remove(wielderPlayer.getUsername());
                }
            }

            if (playerDelay.containsKey(targetPlayer.getUsername())) {
                long targetDelay = time - playerDelay.getOrDefault(targetPlayer.getUsername(), 0L);
                boolean isTargetDelayed = targetDelay < 1500L;
                if (!result) {
                    result = isTargetDelayed;
                }

                if (!isTargetDelayed) {
                    playerDelay.remove(targetPlayer.getUsername());
                }
            }
        }

        return result;
    }

    public static long getSafetyTimestamp(String username) {
        return playerDelay.getOrDefault(username, System.currentTimeMillis());
    }

    public static long getSafetyDelay() {
        return 1500L;
    }

    public static float getCooldown(UdpConnection connection) {
        if (ServerOptions.getInstance().pvp.getValue()
            && ServerOptions.getInstance().safetySystem.getValue()
            && ServerOptions.getInstance().safetyDisconnectDelay.getValue() > 0) {
            float result = 0.0F;
            IsoPlayer[] players;
            if (GameServer.server) {
                players = connection.players;
            } else {
                players = IsoPlayer.players;
            }

            for (IsoPlayer player : players) {
                if (player != null && player.getSafety().getCooldown() + player.getSafety().getToggle() > result) {
                    result = player.getSafety().getCooldown() + player.getSafety().getToggle();
                }
            }

            if (GameServer.server) {
                if (result > 0.0F) {
                    result = ServerOptions.getInstance().safetyDisconnectDelay.getValue();
                }

                DebugLog.Multiplayer.debugln("Delay %f", result);
            }

            return result;
        } else {
            return 0.0F;
        }
    }
}
