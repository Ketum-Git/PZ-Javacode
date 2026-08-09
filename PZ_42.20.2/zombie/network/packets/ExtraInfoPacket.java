// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.Role;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.character.PlayerID;
import zombie.network.server.EventManager;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class ExtraInfoPacket implements INetworkPacket {
    @JSONField
    boolean isForced;
    @JSONField
    final PlayerID playerId = new PlayerID();
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
    boolean canUseBrushTool;
    @JSONField
    boolean canUseLootZed;
    @JSONField
    boolean canUseLootLog;
    @JSONField
    boolean animalCheat;
    @JSONField
    boolean animalExtraValues;
    @JSONField
    boolean canUseDebugContextMenu;
    @JSONField
    boolean isAlwaysDayCheat;

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
        this.canUseBrushTool = player.isCanUseBrushTool();
        this.canUseLootZed = player.canUseLootZed();
        this.canUseLootLog = player.canUseLootLog();
        this.animalCheat = player.isAnimalCheat();
        this.animalExtraValues = player.isAnimalExtraValuesCheat();
        this.isAlwaysDayCheat = player.isAlwaysDayCheat();
        this.canUseDebugContextMenu = player.canUseDebugContextMenu();
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.playerId.write(b);
        this.role.send(b);
        b.putBoolean(this.isForced);
        b.putBoolean(this.showAdminTag);
        b.putBoolean(this.zombiesDontAttack);
        b.putBoolean(this.godMod);
        b.putBoolean(this.invisible);
        b.putBoolean(this.unlimitedEndurance);
        b.putBoolean(this.unlimitedAmmo);
        b.putBoolean(this.knowAllRecipes);
        b.putBoolean(this.unlimitedCarry);
        b.putBoolean(this.buildCheat);
        b.putBoolean(this.farmingCheat);
        b.putBoolean(this.fishingCheat);
        b.putBoolean(this.healthCheat);
        b.putBoolean(this.mechanicsCheat);
        b.putBoolean(this.movablesCheat);
        b.putBoolean(this.fastMoveCheat);
        b.putBoolean(this.timedActionInstantCheat);
        b.putBoolean(this.noClip);
        b.putBoolean(this.canSeeAll);
        b.putBoolean(this.canHearAll);
        b.putBoolean(this.canUseBrushTool);
        b.putBoolean(this.canUseLootZed);
        b.putBoolean(this.canUseLootLog);
        b.putBoolean(this.animalCheat);
        b.putBoolean(this.animalExtraValues);
        b.putBoolean(this.isAlwaysDayCheat);
        b.putBoolean(this.canUseDebugContextMenu);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.playerId.parse(b, connection);
        this.role = new Role("");
        this.role.parse(b);
        this.isForced = b.getBoolean();
        this.showAdminTag = b.getBoolean();
        this.zombiesDontAttack = b.getBoolean();
        this.godMod = b.getBoolean();
        this.invisible = b.getBoolean();
        this.unlimitedEndurance = b.getBoolean();
        this.unlimitedAmmo = b.getBoolean();
        this.knowAllRecipes = b.getBoolean();
        this.unlimitedCarry = b.getBoolean();
        this.buildCheat = b.getBoolean();
        this.farmingCheat = b.getBoolean();
        this.fishingCheat = b.getBoolean();
        this.healthCheat = b.getBoolean();
        this.mechanicsCheat = b.getBoolean();
        this.movablesCheat = b.getBoolean();
        this.fastMoveCheat = b.getBoolean();
        this.timedActionInstantCheat = b.getBoolean();
        this.noClip = b.getBoolean();
        this.canSeeAll = b.getBoolean();
        this.canHearAll = b.getBoolean();
        this.canUseBrushTool = b.getBoolean();
        this.canUseLootZed = b.getBoolean();
        this.canUseLootLog = b.getBoolean();
        this.animalCheat = b.getBoolean();
        this.animalExtraValues = b.getBoolean();
        this.isAlwaysDayCheat = b.getBoolean();
        this.canUseDebugContextMenu = b.getBoolean();
    }

    @Override
    public void processClient(UdpConnection connection) {
        IsoPlayer player = this.playerId.getPlayer();
        if (!player.remote) {
            GameClient.connection.setRole(this.role);
        }

        player.setRole(this.role);
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
        player.setCanUseLootZed(this.canUseLootZed);
        player.setCanUseLootLog(this.canUseLootLog);
        player.setAnimalCheat(this.animalCheat);
        player.setAnimalExtraValuesCheat(this.animalExtraValues);
        player.setAlwaysDayCheat(this.isAlwaysDayCheat);
        player.setCanUseDebugContextMenu(this.canUseDebugContextMenu);
        LuaEventManager.triggerEvent("RefreshCheats");
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        IsoPlayer player = this.playerId.getPlayer();
        if (!connection.getRole().hasCapability(Capability.ToggleWriteRoleNameAbove) && this.showAdminTag) {
            PacketTypes.PacketAuthorization.onUnauthorized(connection, PacketTypes.PacketType.ExtraInfo);
        } else {
            player.setShowAdminTag(this.showAdminTag);
            if (!connection.getRole().hasCapability(Capability.UseZombieDontAttackCheat) && this.zombiesDontAttack) {
                PacketTypes.PacketAuthorization.onUnauthorized(connection, PacketTypes.PacketType.ExtraInfo);
            } else {
                player.setZombiesDontAttack(this.zombiesDontAttack);
                if (!connection.getRole().hasCapability(Capability.ToggleGodModHimself) && this.godMod) {
                    PacketTypes.PacketAuthorization.onUnauthorized(connection, PacketTypes.PacketType.ExtraInfo);
                } else {
                    if (this.godMod) {
                        EventManager.instance().report(String.format("[%s] player \"%s\" set god-mod", GameServer.serverName, player.getUsername()));
                    }

                    player.setGodMod(this.godMod);
                    if (!connection.getRole().hasCapability(Capability.ToggleInvisibleHimself) && this.invisible) {
                        PacketTypes.PacketAuthorization.onUnauthorized(connection, PacketTypes.PacketType.ExtraInfo);
                    } else {
                        player.setInvisible(this.invisible);
                        if (!connection.getRole().hasCapability(Capability.ToggleUnlimitedEndurance) && this.unlimitedEndurance) {
                            PacketTypes.PacketAuthorization.onUnauthorized(connection, PacketTypes.PacketType.ExtraInfo);
                        } else {
                            player.setUnlimitedEndurance(this.unlimitedEndurance);
                            if (!connection.getRole().hasCapability(Capability.ToggleUnlimitedAmmo) && this.unlimitedAmmo) {
                                PacketTypes.PacketAuthorization.onUnauthorized(connection, PacketTypes.PacketType.ExtraInfo);
                            } else {
                                player.setUnlimitedAmmo(this.unlimitedAmmo);
                                if (!connection.getRole().hasCapability(Capability.ToggleKnowAllRecipes) && this.knowAllRecipes) {
                                    PacketTypes.PacketAuthorization.onUnauthorized(connection, PacketTypes.PacketType.ExtraInfo);
                                } else {
                                    player.setKnowAllRecipes(this.knowAllRecipes);
                                    if (!connection.getRole().hasCapability(Capability.ToggleUnlimitedCarry) && this.unlimitedCarry) {
                                        PacketTypes.PacketAuthorization.onUnauthorized(connection, PacketTypes.PacketType.ExtraInfo);
                                    } else {
                                        player.setUnlimitedCarry(this.unlimitedCarry);
                                        if (!connection.getRole().hasCapability(Capability.UseMovablesCheat) && this.movablesCheat) {
                                            PacketTypes.PacketAuthorization.onUnauthorized(connection, PacketTypes.PacketType.ExtraInfo);
                                        } else {
                                            player.setMovablesCheat(this.movablesCheat);
                                            if (!connection.getRole().hasCapability(Capability.UseFastMoveCheat) && this.fastMoveCheat) {
                                                PacketTypes.PacketAuthorization.onUnauthorized(connection, PacketTypes.PacketType.ExtraInfo);
                                            } else {
                                                player.setFastMoveCheat(this.fastMoveCheat);
                                                if (!connection.getRole().hasCapability(Capability.UseBuildCheat) && this.buildCheat) {
                                                    PacketTypes.PacketAuthorization.onUnauthorized(connection, PacketTypes.PacketType.ExtraInfo);
                                                } else {
                                                    player.setBuildCheat(this.buildCheat);
                                                    if (!connection.getRole().hasCapability(Capability.UseFarmingCheat) && this.farmingCheat) {
                                                        PacketTypes.PacketAuthorization.onUnauthorized(connection, PacketTypes.PacketType.ExtraInfo);
                                                    } else {
                                                        player.setFarmingCheat(this.farmingCheat);
                                                        if (!connection.getRole().hasCapability(Capability.UseFishingCheat) && this.fishingCheat) {
                                                            PacketTypes.PacketAuthorization.onUnauthorized(connection, PacketTypes.PacketType.ExtraInfo);
                                                        } else {
                                                            player.setFishingCheat(this.fishingCheat);
                                                            if (!connection.getRole().hasCapability(Capability.UseHealthCheat) && this.healthCheat) {
                                                                PacketTypes.PacketAuthorization.onUnauthorized(connection, PacketTypes.PacketType.ExtraInfo);
                                                            } else {
                                                                player.setHealthCheat(this.healthCheat);
                                                                if (!connection.getRole().hasCapability(Capability.UseMechanicsCheat) && this.mechanicsCheat) {
                                                                    PacketTypes.PacketAuthorization.onUnauthorized(connection, PacketTypes.PacketType.ExtraInfo);
                                                                } else {
                                                                    player.setMechanicsCheat(this.mechanicsCheat);
                                                                    if (!connection.getRole().hasCapability(Capability.UseTimedActionInstantCheat)
                                                                        && this.timedActionInstantCheat) {
                                                                        PacketTypes.PacketAuthorization.onUnauthorized(
                                                                            connection, PacketTypes.PacketType.ExtraInfo
                                                                        );
                                                                    } else {
                                                                        player.setTimedActionInstantCheat(this.timedActionInstantCheat);
                                                                        if (!connection.getRole().hasCapability(Capability.ToggleNoclipHimself) && this.noClip) {
                                                                            PacketTypes.PacketAuthorization.onUnauthorized(
                                                                                connection, PacketTypes.PacketType.ExtraInfo
                                                                            );
                                                                        } else {
                                                                            player.setNoClip(this.noClip);
                                                                            if (!connection.getRole().hasCapability(Capability.CanSeeAll) && this.canSeeAll) {
                                                                                PacketTypes.PacketAuthorization.onUnauthorized(
                                                                                    connection, PacketTypes.PacketType.ExtraInfo
                                                                                );
                                                                            } else {
                                                                                player.setCanSeeAll(this.canSeeAll);
                                                                                if (!connection.getRole().hasCapability(Capability.CanHearAll)
                                                                                    && this.canHearAll) {
                                                                                    PacketTypes.PacketAuthorization.onUnauthorized(
                                                                                        connection, PacketTypes.PacketType.ExtraInfo
                                                                                    );
                                                                                } else {
                                                                                    player.setCanHearAll(this.canHearAll);
                                                                                    if (!connection.getRole().hasCapability(Capability.UseBrushToolManager)
                                                                                        && this.canUseBrushTool) {
                                                                                        PacketTypes.PacketAuthorization.onUnauthorized(
                                                                                            connection, PacketTypes.PacketType.ExtraInfo
                                                                                        );
                                                                                    } else {
                                                                                        player.setCanUseBrushTool(this.canUseBrushTool);
                                                                                        if (!connection.getRole().hasCapability(Capability.UseLootZed)
                                                                                            && this.canUseLootZed) {
                                                                                            PacketTypes.PacketAuthorization.onUnauthorized(
                                                                                                connection, PacketTypes.PacketType.ExtraInfo
                                                                                            );
                                                                                        } else {
                                                                                            player.setCanUseLootZed(this.canUseLootZed);
                                                                                            if (!connection.getRole().hasCapability(Capability.UseLootLog)
                                                                                                && this.canUseLootLog) {
                                                                                                PacketTypes.PacketAuthorization.onUnauthorized(
                                                                                                    connection, PacketTypes.PacketType.ExtraInfo
                                                                                                );
                                                                                            } else {
                                                                                                player.setCanUseLootLog(this.canUseLootLog);
                                                                                                if (!connection.getRole()
                                                                                                        .hasCapability(Capability.AnimalCheats)
                                                                                                    && this.animalCheat) {
                                                                                                    PacketTypes.PacketAuthorization.onUnauthorized(
                                                                                                        connection, PacketTypes.PacketType.ExtraInfo
                                                                                                    );
                                                                                                } else {
                                                                                                    player.setAnimalCheat(this.animalCheat);
                                                                                                    if (!connection.getRole()
                                                                                                            .hasCapability(Capability.AnimalCheats)
                                                                                                        && this.animalExtraValues) {
                                                                                                        PacketTypes.PacketAuthorization.onUnauthorized(
                                                                                                            connection, PacketTypes.PacketType.ExtraInfo
                                                                                                        );
                                                                                                    } else {
                                                                                                        player.setAnimalExtraValuesCheat(this.animalExtraValues);
                                                                                                        if (!connection.getRole()
                                                                                                                .hasCapability(Capability.ClimateManager)
                                                                                                            && this.isAlwaysDayCheat) {
                                                                                                            PacketTypes.PacketAuthorization.onUnauthorized(
                                                                                                                connection, PacketTypes.PacketType.ExtraInfo
                                                                                                            );
                                                                                                        } else {
                                                                                                            player.setAlwaysDayCheat(this.isAlwaysDayCheat);
                                                                                                            if (!connection.getRole()
                                                                                                                    .hasCapability(
                                                                                                                        Capability.UseDebugContextMenu
                                                                                                                    )
                                                                                                                && this.canUseDebugContextMenu) {
                                                                                                                PacketTypes.PacketAuthorization.onUnauthorized(
                                                                                                                    connection,
                                                                                                                    PacketTypes.PacketType.ExtraInfo
                                                                                                                );
                                                                                                            } else {
                                                                                                                player.setCanUseDebugContextMenu(
                                                                                                                    this.canUseDebugContextMenu
                                                                                                                );
                                                                                                                this.sendToClients(
                                                                                                                    PacketTypes.PacketType.ExtraInfo, null
                                                                                                                );
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
            }
        }
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return this.playerId.getPlayer() != null && (connection.getRole().hasAdminPower() || GameClient.client);
    }
}
