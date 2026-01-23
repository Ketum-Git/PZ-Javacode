// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.Role;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameClient;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.character.PlayerID;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class ExtraInfoPacket implements INetworkPacket {
    @JSONField
    boolean isForced = false;
    @JSONField
    PlayerID playerId = new PlayerID();
    @JSONField
    Role role;
    @JSONField
    boolean showAdminTag;
    @JSONField
    boolean zombiesDontAttack;
    @JSONField
    boolean godMod;
    @JSONField
    boolean invisible;
    @JSONField
    boolean unlimitedEndurance;
    @JSONField
    boolean unlimitedAmmo;
    @JSONField
    boolean knowAllRecipes;
    @JSONField
    boolean unlimitedCarry;
    @JSONField
    boolean buildCheat;
    @JSONField
    boolean farmingCheat;
    @JSONField
    boolean fishingCheat;
    @JSONField
    boolean healthCheat;
    @JSONField
    boolean mechanicsCheat;
    @JSONField
    boolean movablesCheat;
    @JSONField
    boolean fastMoveCheat;
    @JSONField
    boolean timedActionInstantCheat;
    @JSONField
    boolean noClip;
    @JSONField
    boolean canSeeAll;
    @JSONField
    boolean canHearAll;
    @JSONField
    boolean showMpInfos;
    @JSONField
    boolean canUseBrushTool;
    @JSONField
    boolean canUseLootTool;
    @JSONField
    boolean animalCheat;
    @JSONField
    boolean canUseDebugContextMenu;

    @Override
    public void setData(Object... values) {
        IsoPlayer player = (IsoPlayer)values[0];
        if (values.length > 1) {
            this.isForced = (Boolean)values[1];
        }

        this.playerId.set(player);
        this.role = player.getRole();
        this.showAdminTag = player.isShowAdminTag();
        this.zombiesDontAttack = player.isZombiesDontAttack();
        this.godMod = player.isGodMod();
        this.invisible = player.isInvisible();
        this.unlimitedEndurance = player.isUnlimitedEndurance();
        this.unlimitedAmmo = player.isUnlimitedAmmo();
        this.knowAllRecipes = player.isKnowAllRecipes();
        this.unlimitedCarry = player.isUnlimitedCarry();
        this.buildCheat = player.isBuildCheat();
        this.farmingCheat = player.isFarmingCheat();
        this.fishingCheat = player.isFishingCheat();
        this.healthCheat = player.isHealthCheat();
        this.mechanicsCheat = player.isMechanicsCheat();
        this.movablesCheat = player.isMovablesCheat();
        this.fastMoveCheat = player.isFastMoveCheat();
        this.timedActionInstantCheat = player.isTimedActionInstantCheat();
        this.noClip = player.isNoClip();
        this.canSeeAll = player.canSeeAll();
        this.canHearAll = player.canHearAll();
        this.showMpInfos = player.isShowMPInfos();
        this.canUseBrushTool = player.isCanUseBrushTool();
        this.canUseLootTool = player.canUseLootTool();
        this.animalCheat = player.isAnimalCheat();
        this.canUseDebugContextMenu = player.canUseDebugContextMenu();
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.playerId.write(b);
        this.role.send(b.bb);
        b.putByte((byte)(this.isForced ? 1 : 0));
        b.putByte((byte)(this.showAdminTag ? 1 : 0));
        b.putByte((byte)(this.zombiesDontAttack ? 1 : 0));
        b.putByte((byte)(this.godMod ? 1 : 0));
        b.putByte((byte)(this.invisible ? 1 : 0));
        b.putByte((byte)(this.unlimitedEndurance ? 1 : 0));
        b.putByte((byte)(this.unlimitedAmmo ? 1 : 0));
        b.putByte((byte)(this.knowAllRecipes ? 1 : 0));
        b.putByte((byte)(this.unlimitedCarry ? 1 : 0));
        b.putByte((byte)(this.buildCheat ? 1 : 0));
        b.putByte((byte)(this.farmingCheat ? 1 : 0));
        b.putByte((byte)(this.fishingCheat ? 1 : 0));
        b.putByte((byte)(this.healthCheat ? 1 : 0));
        b.putByte((byte)(this.mechanicsCheat ? 1 : 0));
        b.putByte((byte)(this.movablesCheat ? 1 : 0));
        b.putByte((byte)(this.fastMoveCheat ? 1 : 0));
        b.putByte((byte)(this.timedActionInstantCheat ? 1 : 0));
        b.putByte((byte)(this.noClip ? 1 : 0));
        b.putByte((byte)(this.canSeeAll ? 1 : 0));
        b.putByte((byte)(this.canHearAll ? 1 : 0));
        b.putByte((byte)(this.showMpInfos ? 1 : 0));
        b.putByte((byte)(this.canUseBrushTool ? 1 : 0));
        b.putByte((byte)(this.canUseLootTool ? 1 : 0));
        b.putByte((byte)(this.animalCheat ? 1 : 0));
        b.putByte((byte)(this.canUseDebugContextMenu ? 1 : 0));
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.playerId.parse(b, connection);
        this.role = new Role("");
        this.role.parse(b);
        this.isForced = b.get() == 1;
        this.showAdminTag = b.get() == 1;
        this.zombiesDontAttack = b.get() == 1;
        this.godMod = b.get() == 1;
        this.invisible = b.get() == 1;
        this.unlimitedEndurance = b.get() == 1;
        this.unlimitedAmmo = b.get() == 1;
        this.knowAllRecipes = b.get() == 1;
        this.unlimitedCarry = b.get() == 1;
        this.buildCheat = b.get() == 1;
        this.farmingCheat = b.get() == 1;
        this.fishingCheat = b.get() == 1;
        this.healthCheat = b.get() == 1;
        this.mechanicsCheat = b.get() == 1;
        this.movablesCheat = b.get() == 1;
        this.fastMoveCheat = b.get() == 1;
        this.timedActionInstantCheat = b.get() == 1;
        this.noClip = b.get() == 1;
        this.canSeeAll = b.get() == 1;
        this.canHearAll = b.get() == 1;
        this.showMpInfos = b.get() == 1;
        this.canUseBrushTool = b.get() == 1;
        this.canUseLootTool = b.get() == 1;
        this.animalCheat = b.get() == 1;
        this.canUseDebugContextMenu = b.get() == 1;
    }

    @Override
    public void processClient(UdpConnection connection) {
        IsoPlayer player = this.playerId.getPlayer();
        if (!player.remote) {
            GameClient.connection.role = this.role;
            player.setRole(this.role);
        }

        player.setShowAdminTag(this.showAdminTag);
        player.setZombiesDontAttack(this.zombiesDontAttack);
        player.setGodMod(this.godMod, this.isForced);
        player.setInvisible(this.invisible, this.isForced);
        player.setUnlimitedEndurance(this.unlimitedEndurance);
        player.setUnlimitedAmmo(this.unlimitedAmmo);
        player.setKnowAllRecipes(this.knowAllRecipes);
        player.setUnlimitedCarry(this.unlimitedCarry);
        player.setBuildCheat(this.buildCheat);
        player.setFarmingCheat(this.farmingCheat);
        player.setFishingCheat(this.fishingCheat);
        player.setHealthCheat(this.healthCheat);
        player.setMechanicsCheat(this.mechanicsCheat);
        player.setMovablesCheat(this.movablesCheat);
        player.setFastMoveCheat(this.fastMoveCheat);
        player.setTimedActionInstantCheat(this.timedActionInstantCheat);
        player.setNoClip(this.noClip, this.isForced);
        player.setCanSeeAll(this.canSeeAll);
        player.setCanHearAll(this.canHearAll);
        player.setCanUseBrushTool(this.canUseBrushTool);
        player.setShowMPInfos(this.showMpInfos);
        player.setCanUseLootTool(this.canUseLootTool);
        player.setAnimalCheat(this.animalCheat);
        player.setCanUseDebugContextMenu(this.canUseDebugContextMenu);
        LuaEventManager.triggerEvent("RefreshCheats");
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        IsoPlayer player = this.playerId.getPlayer();
        if (!connection.role.hasCapability(Capability.ToggleWriteRoleNameAbove) && this.showAdminTag) {
            PacketTypes.PacketAuthorization.onUnauthorized(connection, PacketTypes.PacketType.ExtraInfo);
        } else {
            player.setShowAdminTag(this.showAdminTag);
            if (!connection.role.hasCapability(Capability.UseZombieDontAttackCheat) && this.zombiesDontAttack) {
                PacketTypes.PacketAuthorization.onUnauthorized(connection, PacketTypes.PacketType.ExtraInfo);
            } else {
                player.setZombiesDontAttack(this.zombiesDontAttack);
                if (!connection.role.hasCapability(Capability.ToggleGodModHimself) && this.godMod) {
                    PacketTypes.PacketAuthorization.onUnauthorized(connection, PacketTypes.PacketType.ExtraInfo);
                } else {
                    player.setGodMod(this.godMod);
                    if (!connection.role.hasCapability(Capability.ToggleInvisibleHimself) && this.invisible) {
                        PacketTypes.PacketAuthorization.onUnauthorized(connection, PacketTypes.PacketType.ExtraInfo);
                    } else {
                        player.setInvisible(this.invisible);
                        if (!connection.role.hasCapability(Capability.ToggleUnlimitedEndurance) && this.unlimitedEndurance) {
                            PacketTypes.PacketAuthorization.onUnauthorized(connection, PacketTypes.PacketType.ExtraInfo);
                        } else {
                            player.setUnlimitedEndurance(this.unlimitedEndurance);
                            if (!connection.role.hasCapability(Capability.ToggleUnlimitedAmmo) && this.unlimitedAmmo) {
                                PacketTypes.PacketAuthorization.onUnauthorized(connection, PacketTypes.PacketType.ExtraInfo);
                            } else {
                                player.setUnlimitedAmmo(this.unlimitedAmmo);
                                if (!connection.role.hasCapability(Capability.ToggleKnowAllRecipes) && this.knowAllRecipes) {
                                    PacketTypes.PacketAuthorization.onUnauthorized(connection, PacketTypes.PacketType.ExtraInfo);
                                } else {
                                    player.setKnowAllRecipes(this.knowAllRecipes);
                                    if (!connection.role.hasCapability(Capability.ToggleUnlimitedCarry) && this.unlimitedCarry) {
                                        PacketTypes.PacketAuthorization.onUnauthorized(connection, PacketTypes.PacketType.ExtraInfo);
                                    } else {
                                        player.setUnlimitedCarry(this.unlimitedCarry);
                                        if (!connection.role.hasCapability(Capability.UseMovablesCheat) && this.movablesCheat) {
                                            PacketTypes.PacketAuthorization.onUnauthorized(connection, PacketTypes.PacketType.ExtraInfo);
                                        } else {
                                            player.setMovablesCheat(this.movablesCheat);
                                            if (!connection.role.hasCapability(Capability.UseFastMoveCheat) && this.fastMoveCheat) {
                                                PacketTypes.PacketAuthorization.onUnauthorized(connection, PacketTypes.PacketType.ExtraInfo);
                                            } else {
                                                player.setFastMoveCheat(this.fastMoveCheat);
                                                if (!connection.role.hasCapability(Capability.UseBuildCheat) && this.buildCheat) {
                                                    PacketTypes.PacketAuthorization.onUnauthorized(connection, PacketTypes.PacketType.ExtraInfo);
                                                } else {
                                                    player.setBuildCheat(this.buildCheat);
                                                    if (!connection.role.hasCapability(Capability.UseFarmingCheat) && this.farmingCheat) {
                                                        PacketTypes.PacketAuthorization.onUnauthorized(connection, PacketTypes.PacketType.ExtraInfo);
                                                    } else {
                                                        player.setFarmingCheat(this.farmingCheat);
                                                        if (!connection.role.hasCapability(Capability.UseFishingCheat) && this.fishingCheat) {
                                                            PacketTypes.PacketAuthorization.onUnauthorized(connection, PacketTypes.PacketType.ExtraInfo);
                                                        } else {
                                                            player.setFishingCheat(this.fishingCheat);
                                                            if (!connection.role.hasCapability(Capability.UseHealthCheat) && this.healthCheat) {
                                                                PacketTypes.PacketAuthorization.onUnauthorized(connection, PacketTypes.PacketType.ExtraInfo);
                                                            } else {
                                                                player.setHealthCheat(this.healthCheat);
                                                                if (!connection.role.hasCapability(Capability.UseMechanicsCheat) && this.mechanicsCheat) {
                                                                    PacketTypes.PacketAuthorization.onUnauthorized(connection, PacketTypes.PacketType.ExtraInfo);
                                                                } else {
                                                                    player.setMechanicsCheat(this.mechanicsCheat);
                                                                    if (!connection.role.hasCapability(Capability.UseTimedActionInstantCheat)
                                                                        && this.timedActionInstantCheat) {
                                                                        PacketTypes.PacketAuthorization.onUnauthorized(
                                                                            connection, PacketTypes.PacketType.ExtraInfo
                                                                        );
                                                                    } else {
                                                                        player.setTimedActionInstantCheat(this.timedActionInstantCheat);
                                                                        if (!connection.role.hasCapability(Capability.ToggleNoclipHimself) && this.noClip) {
                                                                            PacketTypes.PacketAuthorization.onUnauthorized(
                                                                                connection, PacketTypes.PacketType.ExtraInfo
                                                                            );
                                                                        } else {
                                                                            player.setNoClip(this.noClip);
                                                                            if (!connection.role.hasCapability(Capability.CanSeeAll) && this.canSeeAll) {
                                                                                PacketTypes.PacketAuthorization.onUnauthorized(
                                                                                    connection, PacketTypes.PacketType.ExtraInfo
                                                                                );
                                                                            } else {
                                                                                player.setCanSeeAll(this.canSeeAll);
                                                                                if (!connection.role.hasCapability(Capability.CanHearAll) && this.canHearAll) {
                                                                                    PacketTypes.PacketAuthorization.onUnauthorized(
                                                                                        connection, PacketTypes.PacketType.ExtraInfo
                                                                                    );
                                                                                } else {
                                                                                    player.setCanHearAll(this.canHearAll);
                                                                                    player.setShowMPInfos(this.showMpInfos);
                                                                                    if (!connection.role.hasCapability(Capability.UseBrushToolManager)
                                                                                        && this.canUseBrushTool) {
                                                                                        PacketTypes.PacketAuthorization.onUnauthorized(
                                                                                            connection, PacketTypes.PacketType.ExtraInfo
                                                                                        );
                                                                                    } else {
                                                                                        player.setCanUseBrushTool(this.canUseBrushTool);
                                                                                        if (!connection.role.hasCapability(Capability.UseLootTool)
                                                                                            && this.canUseLootTool) {
                                                                                            PacketTypes.PacketAuthorization.onUnauthorized(
                                                                                                connection, PacketTypes.PacketType.ExtraInfo
                                                                                            );
                                                                                        } else {
                                                                                            player.setCanUseLootTool(this.canUseLootTool);
                                                                                            if (!connection.role.hasCapability(Capability.AnimalCheats)
                                                                                                && this.animalCheat) {
                                                                                                PacketTypes.PacketAuthorization.onUnauthorized(
                                                                                                    connection, PacketTypes.PacketType.ExtraInfo
                                                                                                );
                                                                                            } else {
                                                                                                player.setAnimalCheat(this.animalCheat);
                                                                                                if (!connection.role
                                                                                                        .hasCapability(Capability.UseDebugContextMenu)
                                                                                                    && this.canUseDebugContextMenu) {
                                                                                                    PacketTypes.PacketAuthorization.onUnauthorized(
                                                                                                        connection, PacketTypes.PacketType.ExtraInfo
                                                                                                    );
                                                                                                } else {
                                                                                                    player.setCanUseDebugContextMenu(
                                                                                                        this.canUseDebugContextMenu
                                                                                                    );
                                                                                                    this.sendToClients(PacketTypes.PacketType.ExtraInfo, null);
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.playerId.getPlayer() != null && (connection.role.hasAdminPower() || GameClient.client);
    }
}
