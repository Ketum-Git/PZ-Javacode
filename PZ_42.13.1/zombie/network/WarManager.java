// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.characters.Capability;
import zombie.characters.Faction;
import zombie.characters.IsoPlayer;
import zombie.debug.DebugLog;
import zombie.iso.areas.SafeHouse;
import zombie.network.packets.INetworkPacket;
import zombie.util.StringUtils;

@UsedFromLua
public final class WarManager {
    private static final ArrayList<WarManager.War> wars = new ArrayList<>();
    private static final ArrayList<WarManager.War> temp = new ArrayList<>();

    private WarManager() {
    }

    public static ArrayList<WarManager.War> getWarRelevent(IsoPlayer player) {
        temp.clear();
        if (player.role != null && player.role.hasCapability(Capability.CanGoInsideSafehouses)) {
            temp.addAll(wars);
        } else {
            for (WarManager.War war : wars) {
                if (war.isRelevant(player.getUsername())) {
                    temp.add(war);
                }
            }
        }

        return temp;
    }

    public static WarManager.War getWarNearest(IsoPlayer player) {
        WarManager.War nearest = null;

        for (WarManager.War war : getWarRelevent(player)) {
            if (nearest == null || war.timestamp < nearest.timestamp) {
                nearest = war;
            }
        }

        return nearest;
    }

    public static WarManager.War getWar(int onlineID, String attacker) {
        for (WarManager.War war : wars) {
            if (war.onlineId == onlineID && war.attacker.equals(attacker)) {
                return war;
            }
        }

        return null;
    }

    public static boolean isWarClaimed(int onlineID) {
        for (WarManager.War war : wars) {
            if (war.onlineId == onlineID) {
                return false;
            }
        }

        return true;
    }

    public static boolean isWarClaimed(String username) {
        for (WarManager.War war : wars) {
            if (war.attacker.equals(username)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isWarStarted(int onlineID, String username) {
        for (WarManager.War war : wars) {
            if (war.onlineId == onlineID && WarManager.State.Started == war.state) {
                return war.isRelevant(username);
            }
        }

        return false;
    }

    public static void removeWar(int onlineID, String attacker) {
        wars.removeIf(war -> war.onlineId == onlineID && (StringUtils.isNullOrEmpty(attacker) || war.attacker.equals(attacker)));
    }

    public static void clear() {
        wars.clear();
    }

    public static void sendWarToPlayer(IsoPlayer player) {
        for (WarManager.War war : wars) {
            INetworkPacket.send(player, PacketTypes.PacketType.WarSync, war);
        }
    }

    public static void updateWar(int onlineID, String attacker, WarManager.State state, long timestamp) {
        for (WarManager.War war : wars) {
            if (war.onlineId == onlineID && war.attacker.equals(attacker)) {
                war.setState(state);
                if (GameClient.client || GameServer.server && WarManager.State.Ended == state) {
                    war.setTimestamp(timestamp);
                }

                return;
            }
        }

        WarManager.War warx = new WarManager.War(onlineID, attacker, state, timestamp);
        wars.add(warx);
    }

    public static void update() {
        long timestamp = GameTime.getServerTimeMills();
        Iterator<WarManager.War> iterator = wars.iterator();

        while (iterator.hasNext()) {
            WarManager.War war = iterator.next();
            if (timestamp >= war.timestamp && war.state != null) {
                switch (war.state) {
                    case Ended:
                        iterator.remove();
                    case Started:
                    case Blocked:
                    default:
                        break;
                    case Refused:
                    case Claimed:
                        SafeHouse.hitPoint(war.onlineId);
                        break;
                    case Accepted:
                    case Canceled:
                        war.setTimestamp(timestamp + getWarDuration());
                }

                if (war.state.next != null) {
                    war.setState(war.state.next);
                }
            }
        }
    }

    public static long getWarDuration() {
        return ServerOptions.instance.warDuration.getValue() * 1000L;
    }

    public static long getStartDelay() {
        return ServerOptions.instance.warStartDelay.getValue() * 1000L;
    }

    @UsedFromLua
    public static enum State {
        Ended(null),
        Started(Ended),
        Blocked(Ended),
        Refused(Ended),
        Claimed(Ended),
        Accepted(Started),
        Canceled(Blocked);

        private final WarManager.State next;

        private State(final WarManager.State next) {
            this.next = next;
        }

        public static WarManager.State valueOf(int ordinal) {
            for (WarManager.State state : values()) {
                if (state.ordinal() == ordinal) {
                    return state;
                }
            }

            return Ended;
        }
    }

    @UsedFromLua
    public static class War {
        private static final HashMap<WarManager.State, WarManager.State> transitions = new HashMap<WarManager.State, WarManager.State>() {
            {
                this.put(WarManager.State.Accepted, WarManager.State.Claimed);
                this.put(WarManager.State.Canceled, WarManager.State.Claimed);
                this.put(WarManager.State.Refused, WarManager.State.Claimed);
                this.put(WarManager.State.Started, WarManager.State.Accepted);
                this.put(WarManager.State.Blocked, WarManager.State.Canceled);
            }
        };
        private final int onlineId;
        private final String attacker;
        private WarManager.State state = WarManager.State.Ended;
        private long timestamp;

        public War(int onlineId, String attacker, WarManager.State state, long timestamp) {
            this.onlineId = onlineId;
            this.attacker = attacker;
            this.setTimestamp(timestamp);
            this.setState(state);
        }

        public int getOnlineID() {
            return this.onlineId;
        }

        public String getAttacker() {
            return this.attacker;
        }

        public String getDefender() {
            SafeHouse safehouse = SafeHouse.getSafeHouse(this.onlineId);
            return safehouse != null ? safehouse.getOwner() : "";
        }

        public WarManager.State getState() {
            return this.state;
        }

        public boolean isValidState(WarManager.State state) {
            return transitions.get(state) == this.state;
        }

        public void setState(WarManager.State state) {
            DebugLog.Multiplayer.debugln("War id=%d state=%s->%s", this.onlineId, this.state, state);
            this.state = state;
            INetworkPacket.sendToAll(PacketTypes.PacketType.WarSync, this);
        }

        public long getTimestamp() {
            return this.timestamp;
        }

        public void setTimestamp(long timestamp) {
            DebugLog.Multiplayer.debugln("War id=%d time=%d", this.onlineId, timestamp - GameTime.getServerTimeMills());
            this.timestamp = timestamp;
        }

        public String getTime() {
            String result = "00:00:00";
            long serverTime = GameTime.getServerTimeMills();
            if (serverTime > 0L) {
                long time = this.timestamp - serverTime;
                if (time > 0L) {
                    long seconds = time / 1000L % 60L;
                    long minutes = time / 1000L / 60L % 60L;
                    long hours = time / 3600000L;
                    result = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                }
            }

            return result;
        }

        private boolean isRelevant(String username) {
            if (username != null) {
                if (this.attacker.equals(username)) {
                    return true;
                }

                Faction attackerFaction = Faction.getPlayerFaction(this.attacker);
                if (attackerFaction != null && (attackerFaction.isOwner(username) || attackerFaction.isMember(username))) {
                    return true;
                }

                SafeHouse safehouse = SafeHouse.getSafeHouse(this.onlineId);
                if (safehouse != null) {
                    String defender = safehouse.getOwner();
                    if (defender.equals(username)) {
                        return true;
                    }

                    Faction defenderFaction = Faction.getPlayerFaction(defender);
                    if (defenderFaction == null || !defenderFaction.isOwner(username) && !defenderFaction.isMember(username)) {
                        if (safehouse.playerAllowed(username)) {
                            return true;
                        }

                        return false;
                    }

                    return true;
                }
            }

            return false;
        }
    }
}
