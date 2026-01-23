// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.inventory.types;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.GameTime;
import zombie.SoundManager;
import zombie.UsedFromLua;
import zombie.WorldSoundManager;
import zombie.ai.sadisticAIDirector.SleepingEvent;
import zombie.audio.BaseSoundEmitter;
import zombie.characters.BaseCharacterSoundEmitter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.core.utils.OnceEvery;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemSoundManager;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemType;
import zombie.ui.ObjectTooltip;

@UsedFromLua
public final class AlarmClockClothing extends Clothing implements IAlarmClock {
    private int alarmHour = -1;
    private int alarmMinutes = -1;
    private boolean alarmSet;
    private long ringSound;
    private double ringSince = -1.0;
    private int forceDontRing = -1;
    private String alarmSound = "AlarmClockLoop";
    private int soundRadius = 40;
    private boolean isDigital = true;
    private IsoPlayer playerOwner;
    public static short packetPlayer = 1;
    public static short packetWorld = 2;
    private static final OnceEvery sendEvery = new OnceEvery(2.0F);

    public AlarmClockClothing(String module, String name, String itemType, String texName, String palette, String SpriteName) {
        super(module, name, itemType, texName, palette, SpriteName);
        this.itemType = ItemType.ALARM_CLOCK_CLOTHING;
        if (this.fullType.contains("Classic")) {
            this.isDigital = false;
        }

        this.randomizeAlarm();
    }

    public AlarmClockClothing(String module, String name, String itemType, Item item, String palette, String SpriteName) {
        super(module, name, itemType, item, palette, SpriteName);
        this.itemType = ItemType.ALARM_CLOCK_CLOTHING;
        if (this.fullType.contains("Classic")) {
            this.isDigital = false;
        }

        this.randomizeAlarm();
    }

    private void randomizeAlarm() {
        if (!Core.lastStand) {
            if (this.isDigital()) {
                this.alarmHour = Rand.Next(0, 23);
                this.alarmMinutes = (int)Math.floor(Rand.Next(0, 59) / 10) * 10;
                this.alarmSet = Rand.Next(15) == 1;
            }
        }
    }

    public IsoGridSquare getAlarmSquare() {
        IsoGridSquare sq = null;
        ItemContainer outermostContainer = this.getOutermostContainer();
        if (outermostContainer != null) {
            sq = outermostContainer.getSourceGrid();
            if (sq == null && outermostContainer.parent != null) {
                sq = outermostContainer.parent.square;
            }

            InventoryItem item = outermostContainer.containingItem;
            if (sq == null && item != null && item.getWorldItem() != null) {
                sq = item.getWorldItem().getSquare();
            }
        }

        if (sq == null && this.getWorldItem() != null && this.getWorldItem().getWorldObjectIndex() != -1) {
            sq = this.getWorldItem().square;
        }

        return sq;
    }

    @Override
    public boolean shouldUpdateInWorld() {
        return this.alarmSet;
    }

    @Override
    public void update() {
        if (this.alarmSet) {
            int minutes = GameTime.instance.getMinutes();
            if (GameServer.server || GameClient.client) {
                minutes = minutes / 10 * 10;
            }

            if (!this.isRinging() && this.forceDontRing != minutes && this.alarmHour == GameTime.instance.getHour() && this.alarmMinutes == minutes) {
                this.ringSince = GameTime.getInstance().getWorldAgeHours();
                if (GameServer.server) {
                    this.syncAlarmClock();
                }
            }

            if (this.isRinging()) {
                double worldAgeHours = GameTime.getInstance().getWorldAgeHours();
                if (this.ringSince > worldAgeHours) {
                    this.ringSince = worldAgeHours;
                }

                IsoGridSquare sq = this.getAlarmSquare();
                if (sq == null || this.ringSince + 0.5 < worldAgeHours) {
                    this.stopRinging();
                    if (GameServer.server) {
                        this.syncStopRinging();
                    }
                } else if (!GameClient.client && sq != null) {
                    WorldSoundManager.instance.addSoundRepeating(null, sq.getX(), sq.getY(), sq.getZ(), this.getSoundRadius(), 3, false);
                }

                if (!GameServer.server && this.isRinging()) {
                    ItemSoundManager.addItem(this);
                }
            }

            if (this.forceDontRing != minutes) {
                this.forceDontRing = -1;
            }
        }
    }

    @Override
    public void updateSound(BaseSoundEmitter emitter) {
        assert !GameServer.server;

        IsoGridSquare sq = this.getAlarmSquare();
        if (sq != null) {
            emitter.setPos(sq.x + 0.5F, sq.y + 0.5F, sq.z);
            if (this.alarmSound == null || "".equals(this.alarmSound)) {
                this.alarmSound = "AlarmClockLoop";
            }

            if (this.isInLocalPlayerInventory()) {
                this.playerOwner = this.getOwnerPlayer(this.getContainer());
                BaseCharacterSoundEmitter currentEmitter = this.playerOwner.getEmitter();
                if (!currentEmitter.isPlaying(this.ringSound)) {
                    this.ringSound = currentEmitter.playSound(this.alarmSound, this.playerOwner);
                }
            } else if (!emitter.isPlaying(this.ringSound)) {
                this.ringSound = emitter.playSoundImpl(this.alarmSound, sq);
            }

            if (GameClient.client && sendEvery.Check() && this.isInLocalPlayerInventory()) {
                WorldSoundManager.instance.addSound(null, sq.x, sq.y, sq.z, this.getSoundRadius(), 3, false, 0.0F, 1.0F);
            }

            this.wakeUpPlayers(sq);
        }
    }

    private void wakeUpPlayers(IsoGridSquare sq) {
        if (!GameServer.server) {
            int radius = this.getSoundRadius();
            int minZ = Math.max(sq.getZ() - 3, -32);
            int maxZ = Math.min(sq.getZ() + 3, 31);

            for (int i = 0; i < IsoPlayer.numPlayers; i++) {
                IsoPlayer player = IsoPlayer.players[i];
                if (player != null && !player.isDead() && player.getCurrentSquare() != null && !player.hasTrait(CharacterTrait.DEAF)) {
                    IsoGridSquare playerSq = player.getCurrentSquare();
                    if (playerSq.z >= minZ && playerSq.z < maxZ) {
                        float distSq = IsoUtils.DistanceToSquared(sq.x, sq.y, playerSq.x, playerSq.y);
                        distSq *= PZMath.pow(player.getHearDistanceModifier(), 2.0F);
                        if (!(distSq > radius * radius)) {
                            this.wakeUp(player);
                        }
                    }
                }
            }
        }
    }

    private void wakeUp(IsoPlayer chr) {
        if (chr.asleep) {
            SoundManager.instance.setMusicWakeState(chr, "WakeNormal");
            SleepingEvent.instance.wakeUp(chr);
        }
    }

    public boolean isRinging() {
        return this.ringSince >= 0.0;
    }

    @Override
    public boolean finishupdate() {
        return !this.alarmSet;
    }

    @Override
    public void DoTooltip(ObjectTooltip tooltipUI, ObjectTooltip.Layout layout) {
        ObjectTooltip.LayoutItem item = layout.addItem();
        item.setLabel(Translator.getText("IGUI_CurrentTime"), 1.0F, 1.0F, 0.8F, 1.0F);
        int minutes = GameTime.instance.getMinutes() / 10 * 10;
        item.setValue(GameTime.getInstance().getHour() + ":" + (minutes == 0 ? "00" : minutes), 1.0F, 1.0F, 0.8F, 1.0F);
        if (this.alarmSet) {
            item = layout.addItem();
            item.setLabel(Translator.getText("IGUI_AlarmIsSetFor"), 1.0F, 1.0F, 0.8F, 1.0F);
            item.setValue(this.alarmHour + ":" + (this.alarmMinutes == 0 ? "00" : this.alarmMinutes), 1.0F, 1.0F, 0.8F, 1.0F);
        }
    }

    @Override
    public void save(ByteBuffer output, boolean net) throws IOException {
        super.save(output, net);
        output.putInt(this.alarmHour);
        output.putInt(this.alarmMinutes);
        output.put((byte)(this.alarmSet ? 1 : 0));
        output.putFloat((float)this.ringSince);
        output.putInt(this.forceDontRing);
    }

    @Override
    public void load(ByteBuffer input, int WorldVersion) throws IOException {
        super.load(input, WorldVersion);
        this.alarmHour = input.getInt();
        this.alarmMinutes = input.getInt();
        this.alarmSet = input.get() == 1;
        this.ringSince = input.getFloat();
        if (WorldVersion >= 205) {
            this.forceDontRing = input.getInt();
        }

        this.ringSound = -1L;
    }

    @Override
    public String getCategory() {
        return this.mainCategory != null ? this.mainCategory : "AlarmClock";
    }

    @Override
    public void setAlarmSet(boolean alarmSet) {
        this.stopRinging();
        this.alarmSet = alarmSet;
        this.ringSound = -1L;
        if (alarmSet) {
            IsoWorld.instance.currentCell.addToProcessItems(this);
            IsoWorldInventoryObject worldInvObj = this.getWorldItem();
            if (worldInvObj != null && worldInvObj.getSquare() != null) {
                IsoCell cell = IsoWorld.instance.getCell();
                if (!cell.getProcessWorldItems().contains(worldInvObj)) {
                    cell.getProcessWorldItems().add(worldInvObj);
                }
            }
        } else {
            IsoWorld.instance.currentCell.addToProcessItemsRemove(this);
        }
    }

    @Override
    public boolean isAlarmSet() {
        return this.alarmSet;
    }

    @Override
    public void setHour(int hour) {
        this.alarmHour = hour;
        this.forceDontRing = -1;
    }

    @Override
    public void setMinute(int min) {
        this.alarmMinutes = min;
        this.forceDontRing = -1;
    }

    @Override
    public void setForceDontRing(int min) {
        this.forceDontRing = min;
    }

    @Override
    public int getHour() {
        return this.alarmHour;
    }

    @Override
    public int getMinute() {
        return this.alarmMinutes;
    }

    public void syncAlarmClock() {
        IsoPlayer player = this.getOwnerPlayer(this.container);
        if (player != null) {
            this.syncAlarmClock_Player(player);
        }

        if (this.worldItem != null) {
            this.syncAlarmClock_World();
        }
    }

    public void syncAlarmClock_Player(IsoPlayer player) {
        if (GameClient.client) {
            INetworkPacket.send(
                PacketTypes.PacketType.SyncPlayerAlarmClock, player, this.id, false, this.alarmHour, this.alarmMinutes, this.alarmSet, this.forceDontRing
            );
        } else if (GameServer.server) {
            IsoPlayer ownerPlayer = this.getOwnerPlayer(this.container);
            if (ownerPlayer != null) {
                INetworkPacket.sendToRelative(
                    PacketTypes.PacketType.SyncPlayerAlarmClock,
                    ownerPlayer.getSquare().getX(),
                    ownerPlayer.getSquare().getY(),
                    player,
                    this.id,
                    false,
                    this.alarmHour,
                    this.alarmMinutes,
                    this.alarmSet,
                    this.forceDontRing
                );
            }
        }
    }

    public void syncAlarmClock_World() {
        if (GameClient.client) {
            INetworkPacket.send(
                PacketTypes.PacketType.SyncWorldAlarmClock,
                this.worldItem.square.getX(),
                this.worldItem.square.getY(),
                (byte)this.worldItem.square.getZ(),
                this.id,
                false,
                this.alarmHour,
                this.alarmMinutes,
                this.alarmSet,
                this.forceDontRing
            );
        } else if (GameServer.server) {
            INetworkPacket.sendToRelative(
                PacketTypes.PacketType.SyncWorldAlarmClock,
                this.worldItem.square.getX(),
                this.worldItem.square.getY(),
                this.worldItem.square.getX(),
                this.worldItem.square.getY(),
                (byte)this.worldItem.square.getZ(),
                this.id,
                false,
                this.alarmHour,
                this.alarmMinutes,
                this.alarmSet,
                this.forceDontRing
            );
        }
    }

    public void syncStopRinging() {
        IsoPlayer player = this.getOwnerPlayer(this.container);
        if (player != null) {
            INetworkPacket.sendToRelative(
                PacketTypes.PacketType.SyncPlayerAlarmClock,
                player.getX(),
                player.getY(),
                player,
                this.id,
                true,
                0,
                0,
                false,
                this.forceDontRing,
                this.ringSince
            );
        } else if (this.getWorldItem() != null) {
            INetworkPacket.sendToRelative(
                PacketTypes.PacketType.SyncWorldAlarmClock,
                this.worldItem.square.getX(),
                this.worldItem.square.getY(),
                this.worldItem.square.getX(),
                this.worldItem.square.getY(),
                (byte)this.worldItem.square.getZ(),
                this.id,
                true,
                0,
                0,
                false,
                this.forceDontRing
            );
        }
    }

    @Override
    public void stopRinging() {
        if (this.playerOwner != null) {
            BaseCharacterSoundEmitter currentEmitter = this.playerOwner.getEmitter();
            currentEmitter.stopSound(this.ringSound);
        }

        if (this.ringSound != -1L) {
            this.ringSound = -1L;
        }

        ItemSoundManager.removeItem(this);
        this.ringSince = -1.0;
        this.forceDontRing = GameTime.instance.getMinutes() / 10 * 10;
    }

    public String getAlarmSound() {
        return this.alarmSound;
    }

    public void setAlarmSound(String alarmSound) {
        this.alarmSound = alarmSound;
    }

    public int getSoundRadius() {
        return this.soundRadius;
    }

    public void setSoundRadius(int soundRadius) {
        this.soundRadius = soundRadius;
    }

    public boolean isDigital() {
        return this.isDigital;
    }
}
