// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.GameWindow;
import zombie.characters.CharacterStat;
import zombie.characters.IsoPlayer;
import zombie.characters.BodyDamage.BodyDamage;
import zombie.characters.BodyDamage.BodyPart;
import zombie.core.Core;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugLog;
import zombie.network.packets.BodyDamageUpdatePacket;
import zombie.scripting.objects.MoodleType;

public class BodyDamageSync {
    public static final byte BD_Health = 1;
    public static final byte BD_bandaged = 2;
    public static final byte BD_bitten = 3;
    public static final byte BD_bleeding = 4;
    public static final byte BD_IsBleedingStemmed = 5;
    public static final byte BD_IsCauterized = 6;
    public static final byte BD_scratched = 7;
    public static final byte BD_stitched = 8;
    public static final byte BD_deepWounded = 9;
    public static final byte BD_IsInfected = 10;
    public static final byte BD_IsFakeInfected = 11;
    public static final byte BD_bandageLife = 12;
    public static final byte BD_scratchTime = 13;
    public static final byte BD_biteTime = 14;
    public static final byte BD_alcoholicBandage = 15;
    public static final byte BD_woundInfectionLevel = 16;
    public static final byte BD_infectedWound = 17;
    public static final byte BD_bleedingTime = 18;
    public static final byte BD_deepWoundTime = 19;
    public static final byte BD_haveGlass = 20;
    public static final byte BD_stitchTime = 21;
    public static final byte BD_alcoholLevel = 22;
    public static final byte BD_additionalPain = 23;
    public static final byte BD_bandageType = 24;
    public static final byte BD_getBandageXp = 25;
    public static final byte BD_getStitchXp = 26;
    public static final byte BD_getSplintXp = 27;
    public static final byte BD_fractureTime = 28;
    public static final byte BD_splint = 29;
    public static final byte BD_splintFactor = 30;
    public static final byte BD_haveBullet = 31;
    public static final byte BD_burnTime = 32;
    public static final byte BD_needBurnWash = 33;
    public static final byte BD_lastTimeBurnWash = 34;
    public static final byte BD_splintItem = 35;
    public static final byte BD_plantainFactor = 36;
    public static final byte BD_comfreyFactor = 37;
    public static final byte BD_garlicFactor = 38;
    public static final byte BD_cut = 39;
    public static final byte BD_cutTime = 40;
    public static final byte BD_stiffness = 41;
    public static final byte BD_MaxParam = 42;
    public static final byte BD_BodyDamage = 50;
    public static final byte BD_START = 64;
    public static final byte BD_END = 65;
    public static BodyDamageSync instance = new BodyDamageSync();
    private final ArrayList<BodyDamageSync.Updater> updaters = new ArrayList<>();

    private static void noise(String s) {
        if (Core.debug || GameServer.server && GameServer.debug) {
            DebugLog.log("BodyDamage: " + s);
        }
    }

    public void startSendingUpdates(short localIndex, short remoteID) {
        if (GameClient.client) {
            noise("start sending updates to " + remoteID);

            for (int i = 0; i < this.updaters.size(); i++) {
                BodyDamageSync.Updater updater = this.updaters.get(i);
                if (updater.localIndex == localIndex && updater.remoteId == remoteID) {
                    return;
                }
            }

            IsoPlayer player = IsoPlayer.players[localIndex];
            BodyDamageSync.Updater updater = new BodyDamageSync.Updater();
            updater.localIndex = localIndex;
            updater.remoteId = remoteID;
            updater.bdLocal = player.getBodyDamage();
            updater.bdSent = new BodyDamage(player);
            this.updaters.add(updater);
        }
    }

    public void stopSendingUpdates(short localIndex, short remoteID) {
        if (GameClient.client) {
            noise("stop sending updates to " + remoteID);

            for (int i = 0; i < this.updaters.size(); i++) {
                BodyDamageSync.Updater updater = this.updaters.get(i);
                if (updater.localIndex == localIndex && updater.remoteId == remoteID) {
                    this.updaters.remove(i);
                    return;
                }
            }
        }
    }

    public void startReceivingUpdates(IsoPlayer remotePlayer) {
        if (GameClient.client) {
            BodyDamageUpdatePacket packet = new BodyDamageUpdatePacket();
            packet.setStart(remotePlayer);
            ByteBufferWriter b = GameClient.connection.startPacket();
            PacketTypes.PacketType.BodyDamageUpdate.doPacket(b);
            packet.write(b);
            PacketTypes.PacketType.BodyDamageUpdate.send(GameClient.connection);
        }
    }

    public void stopReceivingUpdates(IsoPlayer remotePlayer) {
        if (GameClient.client) {
            BodyDamageUpdatePacket packet = new BodyDamageUpdatePacket();
            packet.setStop(remotePlayer);
            ByteBufferWriter b = GameClient.connection.startPacket();
            PacketTypes.PacketType.BodyDamageUpdate.doPacket(b);
            packet.write(b);
            PacketTypes.PacketType.BodyDamageUpdate.send(GameClient.connection);
        }
    }

    public void update() {
        if (GameClient.client) {
            for (int i = 0; i < this.updaters.size(); i++) {
                BodyDamageSync.Updater updater = this.updaters.get(i);
                updater.update();
            }
        }
    }

    public static final class Updater {
        private static final ByteBuffer bb = ByteBuffer.allocate(1024);
        private short localIndex;
        private short remoteId;
        private BodyDamage bdLocal;
        private BodyDamage bdSent;
        private boolean partStarted;
        private byte partIndex;
        private long sendTime;

        private void update() {
            long currentTime = System.currentTimeMillis();
            if (currentTime - this.sendTime >= 500L) {
                this.sendTime = currentTime;
                bb.clear();
                int PainLevelLocal = this.bdLocal.getParentChar().getMoodles().getMoodleLevel(MoodleType.PAIN);
                float localZombieInfection = this.bdLocal.getParentChar().getStats().get(CharacterStat.ZOMBIE_INFECTION);
                float remoteZombieInfection = this.bdSent.getParentChar().getStats().get(CharacterStat.ZOMBIE_INFECTION);
                if (this.compareFloats(this.bdLocal.getOverallBodyHealth(), (int)this.bdSent.getOverallBodyHealth())
                    || PainLevelLocal != this.bdSent.getRemotePainLevel()
                    || this.bdLocal.isFakeInfected != this.bdSent.isFakeInfected
                    || this.compareFloats(localZombieInfection, remoteZombieInfection)) {
                    bb.put((byte)50);
                    bb.putFloat(this.bdLocal.getOverallBodyHealth());
                    bb.put((byte)PainLevelLocal);
                    bb.put((byte)(this.bdLocal.isFakeInfected ? 1 : 0));
                    bb.putFloat(localZombieInfection);
                    this.bdSent.setOverallBodyHealth(this.bdLocal.getOverallBodyHealth());
                    this.bdSent.setRemotePainLevel(PainLevelLocal);
                    this.bdSent.isFakeInfected = this.bdLocal.isFakeInfected;
                    this.bdSent.getParentChar().getStats().set(CharacterStat.ZOMBIE_INFECTION, localZombieInfection);
                }

                for (int i = 0; i < this.bdLocal.getBodyParts().size(); i++) {
                    this.updatePart(i);
                }

                if (bb.position() > 0) {
                    bb.put((byte)65);
                    if (IsoPlayer.players[this.localIndex] != null && GameClient.IDToPlayerMap.get(this.remoteId) != null) {
                        BodyDamageUpdatePacket packet = new BodyDamageUpdatePacket();
                        packet.setUpdate(IsoPlayer.players[this.localIndex], GameClient.IDToPlayerMap.get(this.remoteId), bb);
                        ByteBufferWriter b = GameClient.connection.startPacket();
                        PacketTypes.PacketType.BodyDamageUpdate.doPacket(b);
                        packet.write(b);
                        PacketTypes.PacketType.BodyDamageUpdate.send(GameClient.connection);
                    }
                }
            }
        }

        private void updatePart(int index) {
            BodyPart partLocal = this.bdLocal.getBodyParts().get(index);
            BodyPart partSent = this.bdSent.getBodyParts().get(index);
            this.partStarted = false;
            this.partIndex = (byte)index;
            partLocal.sync(partSent, this);
            if (this.partStarted) {
                bb.put((byte)65);
            }
        }

        public void updateField(byte id, boolean value) {
            if (!this.partStarted) {
                bb.put((byte)64);
                bb.put(this.partIndex);
                this.partStarted = true;
            }

            bb.put(id);
            bb.put((byte)(value ? 1 : 0));
        }

        private boolean compareFloats(float value1, float value2) {
            return Float.compare(value1, 0.0F) != Float.compare(value2, 0.0F) ? true : (int)value1 != (int)value2;
        }

        public boolean updateField(byte id, float value1, float value2) {
            if (!this.compareFloats(value1, value2)) {
                return false;
            } else {
                if (!this.partStarted) {
                    bb.put((byte)64);
                    bb.put(this.partIndex);
                    this.partStarted = true;
                }

                bb.put(id);
                bb.putFloat(value1);
                return true;
            }
        }

        public void updateField(byte id, String value) {
            if (!this.partStarted) {
                bb.put((byte)64);
                bb.put(this.partIndex);
                this.partStarted = true;
            }

            bb.put(id);
            GameWindow.WriteStringUTF(bb, value);
        }
    }
}
