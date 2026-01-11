// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.TreeMap;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.entity.GameEntityNetwork;
import zombie.network.anticheats.AntiCheat;
import zombie.network.packets.AddBrokenGlassPacket;
import zombie.network.packets.AddExplosiveTrapPacket;
import zombie.network.packets.AddInventoryItemToContainerPacket;
import zombie.network.packets.AddItemInInventoryPacket;
import zombie.network.packets.AddItemToMapPacket;
import zombie.network.packets.AddTicketPacket;
import zombie.network.packets.AddTrackPacket;
import zombie.network.packets.AddUserlogPacket;
import zombie.network.packets.AddWarningPointPacket;
import zombie.network.packets.AddXPMultiplierPacket;
import zombie.network.packets.AddXpPacket;
import zombie.network.packets.BanUnbanUserActionPacket;
import zombie.network.packets.BodyDamageUpdatePacket;
import zombie.network.packets.BodyPartSyncPacket;
import zombie.network.packets.BuildActionPacket;
import zombie.network.packets.CustomColorPacket;
import zombie.network.packets.EquipPacket;
import zombie.network.packets.ExtraInfoPacket;
import zombie.network.packets.FishingActionPacket;
import zombie.network.packets.GameCharacterAttachedItemPacket;
import zombie.network.packets.GeneralActionPacket;
import zombie.network.packets.GetModDataPacket;
import zombie.network.packets.GlobalObjectsPacket;
import zombie.network.packets.HumanVisualPacket;
import zombie.network.packets.INetworkPacket;
import zombie.network.packets.ItemStatsPacket;
import zombie.network.packets.ItemTransactionPacket;
import zombie.network.packets.KickedPacket;
import zombie.network.packets.MessageForAdminPacket;
import zombie.network.packets.MetaGridPacket;
import zombie.network.packets.NetTimedActionPacket;
import zombie.network.packets.NetworkUserActionPacket;
import zombie.network.packets.NetworkUsersPacket;
import zombie.network.packets.NotRequiredInZipPacket;
import zombie.network.packets.ObjectChangePacket;
import zombie.network.packets.ObjectModDataPacket;
import zombie.network.packets.PVPEventsPacket;
import zombie.network.packets.PlayerXpPacket;
import zombie.network.packets.ReadAnnotedMapPacket;
import zombie.network.packets.RegisterZonePacket;
import zombie.network.packets.ReloadOptionsPacket;
import zombie.network.packets.RemoveContestedItemsFromInventoryPacket;
import zombie.network.packets.RemoveInventoryItemFromContainerPacket;
import zombie.network.packets.RemoveItemFromSquarePacket;
import zombie.network.packets.RemoveTicketPacket;
import zombie.network.packets.RemoveUserlogPacket;
import zombie.network.packets.ReplaceInventoryItemInContainerPacket;
import zombie.network.packets.RequestDataPacket;
import zombie.network.packets.RequestItemsForContainerPacket;
import zombie.network.packets.RequestLargeAreaZipPacket;
import zombie.network.packets.RequestNetworkUsersPacket;
import zombie.network.packets.RequestRolesPacket;
import zombie.network.packets.RequestTradingPacket;
import zombie.network.packets.RequestZipListPacket;
import zombie.network.packets.RolesEditPacket;
import zombie.network.packets.RolesPacket;
import zombie.network.packets.SafetyPacket;
import zombie.network.packets.SentChunkPacket;
import zombie.network.packets.ServerMapPacket;
import zombie.network.packets.SledgehammerDestroyPacket;
import zombie.network.packets.SlowFactorPacket;
import zombie.network.packets.StartFirePacket;
import zombie.network.packets.StopFirePacket;
import zombie.network.packets.SyncClothingPacket;
import zombie.network.packets.SyncCustomLightSettingsPacket;
import zombie.network.packets.SyncExtendedPlacementPacket;
import zombie.network.packets.SyncHandWeaponFieldsPacket;
import zombie.network.packets.SyncItemActivatedPacket;
import zombie.network.packets.SyncItemDeletePacket;
import zombie.network.packets.SyncItemFieldsPacket;
import zombie.network.packets.SyncItemModDataPacket;
import zombie.network.packets.SyncNonPvpZonePacket;
import zombie.network.packets.SyncPlayerAlarmClockPacket;
import zombie.network.packets.SyncPlayerFieldsPacket;
import zombie.network.packets.SyncPlayerStatsPacket;
import zombie.network.packets.SyncThumpablePacket;
import zombie.network.packets.SyncVisualsPacket;
import zombie.network.packets.SyncWorldAlarmClockPacket;
import zombie.network.packets.SyncZonePacket;
import zombie.network.packets.TeleportPacket;
import zombie.network.packets.TeleportToHimUserActionPacket;
import zombie.network.packets.TeleportUserActionPacket;
import zombie.network.packets.TradingUIAddItemPacket;
import zombie.network.packets.TradingUIRemoveItemPacket;
import zombie.network.packets.TradingUIUpdateStatePacket;
import zombie.network.packets.UpdateOverlaySpritePacket;
import zombie.network.packets.VariableSyncPacket;
import zombie.network.packets.ViewTicketsPacket;
import zombie.network.packets.ViewedTicketPacket;
import zombie.network.packets.WarStateSyncPacket;
import zombie.network.packets.WarSyncPacket;
import zombie.network.packets.WaveSignalPacket;
import zombie.network.packets.WeatherPacket;
import zombie.network.packets.WorldMessagePacket;
import zombie.network.packets.ZombieHelmetFallingPacket;
import zombie.network.packets.actions.AddBloodPacket;
import zombie.network.packets.actions.AddCorpseToMapPacket;
import zombie.network.packets.actions.AnimalEventPacket;
import zombie.network.packets.actions.BurnCorpsePacket;
import zombie.network.packets.actions.EatFoodPacket;
import zombie.network.packets.actions.HelicopterPacket;
import zombie.network.packets.actions.RemoveBloodPacket;
import zombie.network.packets.actions.SmashWindowPacket;
import zombie.network.packets.actions.SneezeCoughPacket;
import zombie.network.packets.actions.StatePacket;
import zombie.network.packets.actions.WakeUpPlayerPacket;
import zombie.network.packets.character.AnimalCommandPacket;
import zombie.network.packets.character.AnimalOwnershipPacket;
import zombie.network.packets.character.AnimalPacket;
import zombie.network.packets.character.AnimalTracksPacket;
import zombie.network.packets.character.AnimalUpdateReliablePacket;
import zombie.network.packets.character.AnimalUpdateUnreliablePacket;
import zombie.network.packets.character.CreatePlayerPacket;
import zombie.network.packets.character.DeadAnimalPacket;
import zombie.network.packets.character.DeadPlayerPacket;
import zombie.network.packets.character.DeadZombiePacket;
import zombie.network.packets.character.ForageItemFoundPacket;
import zombie.network.packets.character.PlayerDamagePacket;
import zombie.network.packets.character.PlayerDataRequestPacket;
import zombie.network.packets.character.PlayerDropHeldItemsPacket;
import zombie.network.packets.character.PlayerEffectsPacket;
import zombie.network.packets.character.PlayerHealthPacket;
import zombie.network.packets.character.PlayerInjuriesPacket;
import zombie.network.packets.character.PlayerPacketReliable;
import zombie.network.packets.character.PlayerPacketUnreliable;
import zombie.network.packets.character.PlayerStatsPacket;
import zombie.network.packets.character.RemoveCorpseFromMapPacket;
import zombie.network.packets.character.ThumpPacket;
import zombie.network.packets.character.ZombieControlPacket;
import zombie.network.packets.character.ZombieDeleteOnClientPacket;
import zombie.network.packets.character.ZombieDeletePacket;
import zombie.network.packets.character.ZombieListPacket;
import zombie.network.packets.character.ZombieRequestPacket;
import zombie.network.packets.character.ZombieSimulationReliablePacket;
import zombie.network.packets.character.ZombieSimulationUnreliablePacket;
import zombie.network.packets.character.ZombieSynchronizationReliablePacket;
import zombie.network.packets.character.ZombieSynchronizationUnreliablePacket;
import zombie.network.packets.connection.ConnectCoopPacket;
import zombie.network.packets.connection.ConnectPacket;
import zombie.network.packets.connection.ConnectedCoopPacket;
import zombie.network.packets.connection.ConnectedPacket;
import zombie.network.packets.connection.GoogleAuthKeyPacket;
import zombie.network.packets.connection.GoogleAuthPacket;
import zombie.network.packets.connection.GoogleAuthRequestPacket;
import zombie.network.packets.connection.LoadPlayerProfilePacket;
import zombie.network.packets.connection.LoginPacket;
import zombie.network.packets.connection.LoginQueueDonePacket;
import zombie.network.packets.connection.MetaDataPacket;
import zombie.network.packets.connection.QueuePacket;
import zombie.network.packets.connection.ServerCustomizationPacket;
import zombie.network.packets.hit.AnimalHitAnimalPacket;
import zombie.network.packets.hit.AnimalHitPlayerPacket;
import zombie.network.packets.hit.AnimalHitThumpablePacket;
import zombie.network.packets.hit.PlayerHitAnimalPacket;
import zombie.network.packets.hit.PlayerHitObjectPacket;
import zombie.network.packets.hit.PlayerHitPlayerPacket;
import zombie.network.packets.hit.PlayerHitSquarePacket;
import zombie.network.packets.hit.PlayerHitVehiclePacket;
import zombie.network.packets.hit.PlayerHitZombiePacket;
import zombie.network.packets.hit.VehicleHitAnimalPacket;
import zombie.network.packets.hit.VehicleHitPlayerPacket;
import zombie.network.packets.hit.VehicleHitZombiePacket;
import zombie.network.packets.hit.ZombieHitPlayerPacket;
import zombie.network.packets.hit.ZombieHitThumpablePacket;
import zombie.network.packets.safehouse.SafehouseAcceptPacket;
import zombie.network.packets.safehouse.SafehouseChangeMemberPacket;
import zombie.network.packets.safehouse.SafehouseChangeOwnerPacket;
import zombie.network.packets.safehouse.SafehouseChangeRespawnPacket;
import zombie.network.packets.safehouse.SafehouseChangeTitlePacket;
import zombie.network.packets.safehouse.SafehouseClaimPacket;
import zombie.network.packets.safehouse.SafehouseInvitePacket;
import zombie.network.packets.safehouse.SafehouseReleasePacket;
import zombie.network.packets.safehouse.SafehouseSyncPacket;
import zombie.network.packets.safehouse.SafezoneClaimPacket;
import zombie.network.packets.service.AccessDeniedPacket;
import zombie.network.packets.service.ChecksumPacket;
import zombie.network.packets.service.GlobalModDataPacket;
import zombie.network.packets.service.GlobalModDataRequestPacket;
import zombie.network.packets.service.PlayerInventoryPacket;
import zombie.network.packets.service.PlayerTimeoutPacket;
import zombie.network.packets.service.PopmanDebugCommandPacket;
import zombie.network.packets.service.ReceiveModDataPacket;
import zombie.network.packets.service.RecipePacket;
import zombie.network.packets.service.RequestUserLogPacket;
import zombie.network.packets.service.ScoreboardUpdatePacket;
import zombie.network.packets.service.ServerDebugInfo;
import zombie.network.packets.service.ServerLOSPacket;
import zombie.network.packets.service.ServerQuitPacket;
import zombie.network.packets.service.SetMultiplierPacket;
import zombie.network.packets.service.StartPausePacket;
import zombie.network.packets.service.StatisticsPacket;
import zombie.network.packets.service.StopPausePacket;
import zombie.network.packets.service.SyncClockPacket;
import zombie.network.packets.service.TimeSyncPacket;
import zombie.network.packets.sound.PlaySoundPacket;
import zombie.network.packets.sound.PlayWorldSoundPacket;
import zombie.network.packets.sound.StopSoundPacket;
import zombie.network.packets.sound.WorldSoundPacket;
import zombie.network.packets.vehicle.VehicleCollidePacket;
import zombie.network.packets.vehicle.VehicleEnterPacket;
import zombie.network.packets.vehicle.VehicleExitPacket;
import zombie.network.packets.vehicle.VehicleFullUpdatePacket;
import zombie.network.packets.vehicle.VehiclePassengerPositionPacket;
import zombie.network.packets.vehicle.VehiclePassengerRequestPacket;
import zombie.network.packets.vehicle.VehiclePassengerResponsePacket;
import zombie.network.packets.vehicle.VehiclePhysicsReliablePacket;
import zombie.network.packets.vehicle.VehiclePhysicsUnreliablePacket;
import zombie.network.packets.vehicle.VehicleRemovePacket;
import zombie.network.packets.vehicle.VehicleRequestPacket;
import zombie.network.packets.vehicle.VehicleSwitchSeatPacket;
import zombie.network.packets.vehicle.VehicleTowingAttachPacket;
import zombie.network.packets.vehicle.VehicleTowingDetachPacket;
import zombie.network.packets.vehicle.VehicleTowingStatePacket;
import zombie.network.packets.vehicle.VehicleUpdatePacket;
import zombie.network.packets.world.DebugStoryPacket;
import zombie.network.server.EventManager;
import zombie.network.statistics.data.NetworkStatistic;

public class PacketTypes {
    public static final byte PacketOrdering_General = 0;
    public static final byte PacketOrdering_Items = 1;
    public static final byte PacketOrdering_ServerCustomization = 2;
    public static final byte PacketOrdering_Object = 3;
    public static final byte PacketOrdering_Map = 4;
    public static final byte PacketOrdering_Player = 5;
    public static final byte PacketOrdering_Animal = 7;
    public static final byte PacketOrdering_Vehicle = 8;
    public static final Map<Short, PacketTypes.PacketType> packetTypes = new TreeMap<>();

    public static void doPingPacket(ByteBufferWriter bb) {
        bb.putInt(28);
    }

    static {
        for (PacketTypes.PacketType packetType : PacketTypes.PacketType.values()) {
            PacketTypes.PacketType previous = packetTypes.put(packetType.getId(), packetType);
            if (previous != null) {
                DebugLog.Multiplayer.error(String.format("PacketType: duplicate \"%s\" \"%s\" id=%d", previous.name(), packetType.name(), packetType.getId()));
            }
        }
    }

    public interface CallbackClientProcess {
        void call(ByteBuffer bb, short packetType) throws IOException;
    }

    public interface CallbackServerProcess {
        void call(ByteBuffer bb, UdpConnection connection, short packetType) throws Exception;
    }

    public static class PacketAuthorization {
        private static boolean isAuthorized(UdpConnection connection, PacketTypes.PacketType type) {
            boolean isAuthorized = type.requiredCapability == Capability.None
                || connection.role != null && connection.role.hasCapability(type.requiredCapability);
            if (!isAuthorized || type.serverHandler == null && type.handler == null) {
                onUnauthorized(connection, type);
            }

            return isAuthorized;
        }

        public static void onUnauthorized(UdpConnection connection, PacketTypes.PacketType type) {
            AntiCheat.Capability.act(connection, type.name());
        }
    }

    public static enum PacketType {
        Checksum(ChecksumPacket.class),
        Validate(RecipePacket.class),
        Login(LoginPacket.class),
        LoginQueueRequest(QueuePacket.class),
        LoginQueueDone(LoginQueueDonePacket.class),
        LoadPlayerProfile(LoadPlayerProfilePacket.class),
        CreatePlayer(CreatePlayerPacket.class),
        PlayerConnect(ConnectPacket.class),
        ConnectedPlayer(ConnectedPacket.class),
        ConnectCoop(ConnectCoopPacket.class),
        ConnectedCoop(ConnectedCoopPacket.class),
        GoogleAuthKey(GoogleAuthKeyPacket.class),
        GoogleAuth(GoogleAuthPacket.class),
        GoogleAuthRequest(GoogleAuthRequestPacket.class),
        ServerCustomization(ServerCustomizationPacket.class),
        PlayerUpdateReliable(PlayerPacketReliable.class),
        PlayerUpdateUnreliable(PlayerPacketUnreliable.class),
        PlayerDataRequest(PlayerDataRequestPacket.class),
        HumanVisual(HumanVisualPacket.class),
        SyncClothing(SyncClothingPacket.class),
        SyncVisuals(SyncVisualsPacket.class),
        Equip(EquipPacket.class),
        AddXP(AddXpPacket.class),
        AddXPMultiplier(AddXPMultiplierPacket.class),
        PlayerDeath(DeadPlayerPacket.class),
        ChangeSafety(SafetyPacket.class),
        BodyDamageUpdate(BodyDamageUpdatePacket.class),
        GameCharacterAttachedItem(GameCharacterAttachedItemPacket.class),
        VariableSync(VariableSyncPacket.class),
        SyncPlayerStats(SyncPlayerStatsPacket.class),
        SyncPlayerFields(SyncPlayerFieldsPacket.class),
        ForageItemFound(ForageItemFoundPacket.class),
        PlayerDropHeldItems(PlayerDropHeldItemsPacket.class),
        ServerMap(ServerMapPacket.class),
        RequestLargeAreaZip(RequestLargeAreaZipPacket.class),
        SentChunk(SentChunkPacket.class),
        RequestZipList(RequestZipListPacket.class),
        NotRequiredInZip(NotRequiredInZipPacket.class),
        RequestData(RequestDataPacket.class),
        ZombieList(ZombieListPacket.class),
        ZombieSimulationUnreliable(ZombieSimulationUnreliablePacket.class),
        ZombieSimulationReliable(ZombieSimulationReliablePacket.class),
        ZombieSynchronizationUnreliable(ZombieSynchronizationUnreliablePacket.class),
        ZombieSynchronizationReliable(ZombieSynchronizationReliablePacket.class),
        ZombieDelete(ZombieDeletePacket.class),
        ZombieDeleteOnClient(ZombieDeleteOnClientPacket.class),
        ZombieRequest(ZombieRequestPacket.class),
        ZombieDeath(DeadZombiePacket.class),
        SlowFactor(SlowFactorPacket.class),
        ZombieControl(ZombieControlPacket.class),
        Thump(ThumpPacket.class),
        AnimalCommand(AnimalCommandPacket.class),
        AnimalOwnership(AnimalOwnershipPacket.class),
        AnimalPacket(AnimalPacket.class),
        AnimalUpdateReliable(AnimalUpdateReliablePacket.class),
        AnimalUpdateUnreliable(AnimalUpdateUnreliablePacket.class),
        AnimalEvent(AnimalEventPacket.class),
        AnimalDeath(DeadAnimalPacket.class),
        AnimalTracks(AnimalTracksPacket.class),
        AddTrack(AddTrackPacket.class),
        VehicleRequest(VehicleRequestPacket.class),
        VehicleFullUpdate(VehicleFullUpdatePacket.class),
        VehicleUpdate(VehicleUpdatePacket.class),
        VehiclePhysicsReliable(VehiclePhysicsReliablePacket.class),
        VehiclePhysicsUnreliable(VehiclePhysicsUnreliablePacket.class),
        VehicleTowingAttach(VehicleTowingAttachPacket.class),
        VehicleTowingDetach(VehicleTowingDetachPacket.class),
        VehicleTowingState(VehicleTowingStatePacket.class),
        VehicleRemove(VehicleRemovePacket.class),
        VehicleCollide(VehicleCollidePacket.class),
        VehiclePassengerPosition(VehiclePassengerPositionPacket.class),
        VehiclePassengerRequest(VehiclePassengerRequestPacket.class),
        VehiclePassengerResponse(VehiclePassengerResponsePacket.class),
        VehicleEnter(VehicleEnterPacket.class),
        VehicleExit(VehicleExitPacket.class),
        VehicleSwitchSeat(VehicleSwitchSeatPacket.class),
        State(StatePacket.class),
        Helicopter(HelicopterPacket.class),
        SmashWindow(SmashWindowPacket.class),
        EatFood(EatFoodPacket.class),
        Drink(1, 2, Capability.LoginOnServer, GameServer::receiveDrink, null, null),
        BurnCorpse(BurnCorpsePacket.class),
        SneezeCough(SneezeCoughPacket.class),
        AddBlood(AddBloodPacket.class),
        RemoveBlood(RemoveBloodPacket.class),
        WakeUpPlayer(WakeUpPlayerPacket.class),
        EatBody(1, 2, Capability.LoginOnServer, GameServer::receiveEatBody, GameClient::receiveEatBody, null),
        ReadAnnotedMap(ReadAnnotedMapPacket.class),
        ZombieHelmetFalling(ZombieHelmetFallingPacket.class),
        TimeSync(TimeSyncPacket.class),
        SetMultiplier(SetMultiplierPacket.class),
        AccessDenied(AccessDeniedPacket.class),
        StartPause(StartPausePacket.class),
        StopPause(StopPausePacket.class),
        PlayerTimeout(PlayerTimeoutPacket.class),
        SyncClock(SyncClockPacket.class),
        SandboxOptions(1, 2, Capability.SandboxOptions, GameServer::receiveSandboxOptions, GameClient::receiveSandboxOptions, null),
        ServerQuit(ServerQuitPacket.class),
        MessageForAdmin(MessageForAdminPacket.class),
        MetaData(MetaDataPacket.class),
        MetaGrid(MetaGridPacket.class),
        WorldMessage(WorldMessagePacket.class),
        ReceiveCommand(2, 3, Capability.LoginOnServer, GameServer::receiveReceiveCommand, null, null),
        ReloadOptions(ReloadOptionsPacket.class),
        Kicked(KickedPacket.class),
        Ping(0, 0, Capability.None, GameServer::receivePing, GameClient::receivePing, GameClient::receivePing),
        SpawnRegion(1, 2, Capability.LoginOnServer, null, GameClient::receiveSpawnRegion, GameClient::receiveSpawnRegion),
        WorldMapPlayerPosition(3, 1, Capability.LoginOnServer, GameServer::receiveWorldMapPlayerPosition, GameClient::receiveWorldMapPlayerPosition, null),
        WorldMap(1, 2, Capability.LoginOnServer, GameServer::receiveWorldMap, GameClient::receiveWorldMap, GameClient::receiveWorldMap),
        ItemTransaction(ItemTransactionPacket.class),
        GeneralAction(GeneralActionPacket.class),
        NetTimedAction(NetTimedActionPacket.class),
        BuildAction(BuildActionPacket.class),
        FishingAction(FishingActionPacket.class),
        AddInventoryItemToContainer(AddInventoryItemToContainerPacket.class),
        RemoveInventoryItemFromContainer(RemoveInventoryItemFromContainerPacket.class),
        ReplaceInventoryItemInContainer(ReplaceInventoryItemInContainerPacket.class),
        SyncItemDelete(SyncItemDeletePacket.class),
        AddItemToMap(AddItemToMapPacket.class),
        AddCorpseToMap(AddCorpseToMapPacket.class),
        RemoveItemFromSquare(RemoveItemFromSquarePacket.class),
        RemoveCorpseFromMap(RemoveCorpseFromMapPacket.class),
        RequestItemsForContainer(RequestItemsForContainerPacket.class),
        RemoveContestedItemsFromInventory(RemoveContestedItemsFromInventoryPacket.class),
        ItemStats(ItemStatsPacket.class),
        AddBrokenGlass(AddBrokenGlassPacket.class),
        AddExplosiveTrap(AddExplosiveTrapPacket.class),
        SledgehammerDestroy(SledgehammerDestroyPacket.class),
        SyncPlayerAlarmClock(SyncPlayerAlarmClockPacket.class),
        SyncWorldAlarmClock(SyncWorldAlarmClockPacket.class),
        SyncExtendedPlacement(SyncExtendedPlacementPacket.class),
        SyncItemActivated(SyncItemActivatedPacket.class),
        GlobalObjects(GlobalObjectsPacket.class),
        ReceiveModData(ReceiveModDataPacket.class),
        GlobalModData(GlobalModDataPacket.class),
        GlobalModDataRequest(GlobalModDataRequestPacket.class),
        SyncItemFields(SyncItemFieldsPacket.class),
        SyncItemModData(SyncItemModDataPacket.class),
        SyncHandWeaponFields(SyncHandWeaponFieldsPacket.class),
        PlayerHitSquare(PlayerHitSquarePacket.class),
        PlayerHitObject(PlayerHitObjectPacket.class),
        PlayerHitVehicle(PlayerHitVehiclePacket.class),
        PlayerHitZombie(PlayerHitZombiePacket.class),
        PlayerHitPlayer(PlayerHitPlayerPacket.class),
        PlayerHitAnimal(PlayerHitAnimalPacket.class),
        ZombieHitPlayer(ZombieHitPlayerPacket.class),
        AnimalHitPlayer(AnimalHitPlayerPacket.class),
        AnimalHitAnimal(AnimalHitAnimalPacket.class),
        AnimalHitThumpable(AnimalHitThumpablePacket.class),
        ZombieHitThumpable(ZombieHitThumpablePacket.class),
        VehicleHitZombie(VehicleHitZombiePacket.class),
        VehicleHitPlayer(VehicleHitPlayerPacket.class),
        VehicleHitAnimal(VehicleHitAnimalPacket.class),
        PlayerDamage(PlayerDamagePacket.class),
        PlayerStats(PlayerStatsPacket.class),
        PlayerHealth(PlayerHealthPacket.class),
        PlayerInjuries(PlayerInjuriesPacket.class),
        PlayerEffects(PlayerEffectsPacket.class),
        PlayerXp(PlayerXpPacket.class),
        PlaySound(PlaySoundPacket.class),
        WorldSoundPacket(WorldSoundPacket.class),
        AddAmbient(1, 2, Capability.LoginOnServer, null, GameClient::receiveAddAmbient, null),
        ZombieSound(1, 2, Capability.LoginOnServer, null, GameClient::receiveZombieSound, null),
        PlayWorldSound(PlayWorldSoundPacket.class),
        StopSound(StopSoundPacket.class),
        PlaySoundEveryPlayer(1, 2, Capability.LoginOnServer, null, GameClient::receivePlaySoundEveryPlayer, null),
        Statistics(StatisticsPacket.class),
        ScoreboardUpdate(ScoreboardUpdatePacket.class),
        Weather(WeatherPacket.class),
        StartRain(1, 2, Capability.LoginOnServer, null, GameClient::receiveStartRain, null),
        StopRain(1, 2, Capability.LoginOnServer, null, GameClient::receiveStopRain, null),
        ClimateManagerPacket(1, 2, Capability.ClimateManager, GameServer::receiveClimateManagerPacket, GameClient::receiveClimateManagerPacket, null),
        IsoRegionServerPacket(1, 2, Capability.LoginOnServer, null, GameClient::receiveIsoRegionServerPacket, null),
        IsoRegionClientRequestFullUpdate(1, 2, Capability.LoginOnServer, GameServer::receiveIsoRegionClientRequestFullUpdate, null, null),
        SyncNonPvpZone(SyncNonPvpZonePacket.class),
        SyncThumpable(SyncThumpablePacket.class),
        SyncDoorKey(1, 2, Capability.LoginOnServer, GameServer::receiveSyncDoorKey, GameClient::receiveSyncDoorKey, null),
        SyncIsoObject(1, 2, 3, Capability.LoginOnServer, GameServer::receiveSyncIsoObject, GameClient::receiveSyncIsoObject, null),
        ClientCommand(1, 2, Capability.LoginOnServer, GameServer::receiveClientCommand, GameClient::receiveClientCommand, null),
        ObjectModData(ObjectModDataPacket.class),
        ObjectChange(ObjectChangePacket.class),
        BloodSplatter(1, 2, Capability.LoginOnServer, null, GameClient::receiveBloodSplatter, null),
        ZombieDescriptors(1, 2, Capability.LoginOnServer, null, GameClient::receiveZombieDescriptors, null),
        StartFire(StartFirePacket.class),
        StopFire(StopFirePacket.class),
        UpdateItemSprite(1, 2, Capability.LoginOnServer, GameServer::receiveUpdateItemSprite, GameClient::receiveUpdateItemSprite, null),
        SendCustomColor(CustomColorPacket.class),
        SyncCompost(1, 2, Capability.LoginOnServer, GameServer::receiveSyncCompost, GameClient::receiveSyncCompost, null),
        GetModData(GetModDataPacket.class),
        SyncPerks(1, 2, Capability.LoginOnServer, GameServer::receiveSyncPerks, GameClient::receiveSyncPerks, null),
        SyncWeight(1, 2, Capability.LoginOnServer, GameServer::receiveSyncWeight, GameClient::receiveSyncWeight, null),
        SyncEquippedRadioFreq(1, 2, Capability.LoginOnServer, GameServer::receiveSyncEquippedRadioFreq, GameClient::receiveSyncEquippedRadioFreq, null),
        UpdateOverlaySprite(UpdateOverlaySpritePacket.class),
        AddAlarm(1, 2, Capability.LoginOnServer, null, GameClient::receiveAddAlarm, null),
        ChangeTextColor(1, 2, Capability.LoginOnServer, GameServer::receiveChangeTextColor, GameClient::receiveChangeTextColor, null),
        SyncCustomLightSettings(SyncCustomLightSettingsPacket.class),
        ChunkObjectState(1, 2, 3, Capability.LoginOnServer, GameServer::receiveChunkObjectState, GameClient::receiveChunkObjectState, null),
        StartFishSplash(1, 2, Capability.LoginOnServer, GameServer::receiveBigWaterSplash, GameClient::receiveBigWaterSplash, null),
        FishingData(1, 2, Capability.LoginOnServer, GameServer::receiveFishingDataRequest, GameClient::receiveFishingData, null),
        ToxicBuilding(1, 2, Capability.LoginOnServer, null, GameClient::receiveToxicBuilding, null),
        AddItemInInventory(AddItemInInventoryPacket.class),
        PlayerInventory(PlayerInventoryPacket.class),
        ExtraInfo(ExtraInfoPacket.class),
        Teleport(TeleportPacket.class),
        InvMngReqItem(1, 2, Capability.InspectPlayerInventory, GameServer::receiveInvMngReqItem, GameClient::receiveInvMngReqItem, null),
        InvMngGetItem(1, 2, Capability.LoginOnServer, GameServer::receiveInvMngGetItem, GameClient::receiveInvMngGetItem, null),
        InvMngRemoveItem(1, 2, Capability.InspectPlayerInventory, GameServer::receiveInvMngRemoveItem, GameClient::receiveInvMngRemoveItem, null),
        InvMngUpdateItem(1, 2, Capability.InspectPlayerInventory, GameServer::receiveInvMngUpdateItem, null, null),
        ChangePlayerStats(1, 2, Capability.LoginOnServer, GameServer::receiveChangePlayerStats, GameClient::receiveChangePlayerStats, null),
        NetworkUsers(NetworkUsersPacket.class),
        PVPEvents(PVPEventsPacket.class),
        RequestNetworkUsers(RequestNetworkUsersPacket.class),
        NetworkUserAction(NetworkUserActionPacket.class),
        BanUnbanUserAction(BanUnbanUserActionPacket.class),
        TeleportUserAction(TeleportUserActionPacket.class),
        TeleportToHimUserAction(TeleportToHimUserActionPacket.class),
        Roles(RolesPacket.class),
        RequestRoles(RequestRolesPacket.class),
        RolesEdit(RolesEditPacket.class),
        RequestUserLog(RequestUserLogPacket.class),
        AddUserlog(AddUserlogPacket.class),
        RemoveUserlog(RemoveUserlogPacket.class),
        AddWarningPoint(AddWarningPointPacket.class),
        ConstructedZone(1, 2, Capability.LoginOnServer, GameServer::receiveConstructedZone, GameClient::receiveConstructedZone, null),
        RegisterZone(RegisterZonePacket.class),
        SyncZone(SyncZonePacket.class),
        SyncFaction(1, 2, Capability.LoginOnServer, GameServer::receiveSyncFaction, GameClient::receiveSyncFaction, null),
        SendFactionInvite(1, 2, Capability.LoginOnServer, GameServer::receiveSendFactionInvite, GameClient::receiveSendFactionInvite, null),
        AcceptedFactionInvite(1, 2, Capability.LoginOnServer, GameServer::receiveAcceptedFactionInvite, GameClient::receiveAcceptedFactionInvite, null),
        AddTicket(AddTicketPacket.class),
        ViewedTicket(ViewedTicketPacket.class),
        ViewTickets(ViewTicketsPacket.class),
        RemoveTicket(RemoveTicketPacket.class),
        ViewBannedIPs(1, 2, Capability.BanUnbanUser, GameServer::receiveViewBannedIPs, GameClient::receiveViewBannedIPs, null),
        ViewBannedSteamIDs(1, 2, Capability.BanUnbanUser, GameServer::receiveViewBannedSteamIDs, GameClient::receiveViewBannedSteamIDs, null),
        RequestTrading(RequestTradingPacket.class),
        TradingUIAddItem(TradingUIAddItemPacket.class),
        TradingUIRemoveItem(TradingUIRemoveItemPacket.class),
        TradingUIUpdateState(TradingUIUpdateStatePacket.class),
        WarStateSync(WarStateSyncPacket.class),
        WarSync(WarSyncPacket.class),
        InitPlayerChat(1, 2, Capability.LoginOnServer, null, GameClient::receiveInitPlayerChat, null),
        PlayerJoinChat(1, 2, Capability.LoginOnServer, null, GameClient::receivePlayerJoinChat, null),
        PlayerLeaveChat(1, 2, Capability.LoginOnServer, null, GameClient::receivePlayerLeaveChat, null),
        ChatMessageFromPlayer(1, 2, Capability.LoginOnServer, GameServer::receiveChatMessageFromPlayer, null, null),
        ChatMessageToPlayer(1, 2, Capability.LoginOnServer, null, GameClient::receiveChatMessageToPlayer, null),
        PlayerStartPMChat(1, 2, Capability.LoginOnServer, GameServer::receivePlayerStartPMChat, null, null),
        AddChatTab(1, 2, Capability.LoginOnServer, null, GameClient::receiveAddChatTab, null),
        RemoveChatTab(1, 2, Capability.LoginOnServer, null, GameClient::receiveRemoveChatTab, null),
        PlayerConnectedToChat(1, 2, Capability.LoginOnServer, null, GameClient::receivePlayerConnectedToChat, null),
        PlayerNotFound(1, 2, Capability.LoginOnServer, null, GameClient::receivePlayerNotFound, null),
        SafehouseInvite(SafehouseInvitePacket.class),
        SafehouseAccept(SafehouseAcceptPacket.class),
        SafehouseChangeMember(SafehouseChangeMemberPacket.class),
        SafehouseChangeOwner(SafehouseChangeOwnerPacket.class),
        SafehouseChangeRespawn(SafehouseChangeRespawnPacket.class),
        SafehouseChangeTitle(SafehouseChangeTitlePacket.class),
        SafehouseRelease(SafehouseReleasePacket.class),
        SafehouseClaim(SafehouseClaimPacket.class),
        SafezoneClaim(SafezoneClaimPacket.class),
        SafehouseSync(SafehouseSyncPacket.class),
        PopmanDebugCommand(PopmanDebugCommandPacket.class),
        ServerDebugInfo(ServerDebugInfo.class),
        ServerLOS(ServerLOSPacket.class),
        WaveSignal(WaveSignalPacket.class),
        PlayerListensChannel(1, 2, Capability.LoginOnServer, GameServer::receivePlayerListensChannel, null, null),
        RadioServerData(1, 2, Capability.LoginOnServer, GameServer::receiveRadioServerData, GameClient::receiveRadioServerData, null),
        RadioDeviceDataState(1, 2, Capability.LoginOnServer, GameServer::receiveRadioDeviceDataState, GameClient::receiveRadioDeviceDataState, null),
        RadioPostSilenceEvent(0, 2, Capability.LoginOnServer, null, GameClient::receiveRadioPostSilence, null),
        SyncRadioData(0, 3, Capability.LoginOnServer, GameServer::receiveSyncRadioData, GameClient::receiveSyncRadioData, null),
        BodyPartSync(BodyPartSyncPacket.class),
        DebugStory(DebugStoryPacket.class),
        SendItemListNet(1, 2, Capability.LoginOnServer, GameServer::receiveSendItemListNet, GameClient::receiveSendItemListNet, null),
        GameEntity(GameEntityNetwork.class);

        private Capability requiredCapability;
        public int packetPriority;
        public int packetReliability;
        public byte orderingChannel;
        public int handlingType;
        public AntiCheat[] anticheats;
        private final PacketTypes.CallbackServerProcess serverHandler;
        private final PacketTypes.CallbackClientProcess clientHandler;
        private final PacketTypes.CallbackClientProcess clientLoadingHandler;
        public final Class<? extends INetworkPacket> handler;
        private boolean logEnabled;

        private PacketType(
            final int priority,
            final int reliability,
            final Capability requiredCapability,
            final PacketTypes.CallbackServerProcess server,
            final PacketTypes.CallbackClientProcess client,
            final PacketTypes.CallbackClientProcess clientLoading
        ) {
            this(
                priority,
                reliability,
                (byte)0,
                requiredCapability,
                PacketSetting.HandlingType.getType(server != null, client != null, clientLoading != null),
                server,
                client,
                clientLoading,
                null
            );
        }

        private PacketType(
            final int priority,
            final int reliability,
            final int ordering,
            final Capability requiredCapability,
            final PacketTypes.CallbackServerProcess server,
            final PacketTypes.CallbackClientProcess client,
            final PacketTypes.CallbackClientProcess clientLoading
        ) {
            this(
                priority,
                reliability,
                (byte)ordering,
                requiredCapability,
                PacketSetting.HandlingType.getType(server != null, client != null, clientLoading != null),
                server,
                client,
                clientLoading,
                null
            );
        }

        private PacketType(final Class<? extends INetworkPacket> handler) {
            this(2, 0, (byte)0, Capability.LoginOnServer, 7, null, null, null, handler);
        }

        private PacketType(
            final int priority,
            final int reliability,
            final byte ordering,
            final Capability requiredCapability,
            final int handlingType,
            final PacketTypes.CallbackServerProcess server,
            final PacketTypes.CallbackClientProcess client,
            final PacketTypes.CallbackClientProcess clientLoading,
            final Class<? extends INetworkPacket> handler
        ) {
            this.requiredCapability = requiredCapability;
            this.packetPriority = priority;
            this.packetReliability = reliability;
            this.orderingChannel = ordering;
            this.handlingType = handlingType;
            this.serverHandler = server;
            this.clientHandler = client;
            this.clientLoadingHandler = clientLoading;
            this.handler = handler;
            if (handler != null) {
                PacketSetting setting = getPacketSetting(handler);
                if (setting == null) {
                    DebugLog.Multiplayer.error("The %s class doesn't have PacketSetting attributes", handler.getSimpleName());
                } else {
                    this.packetPriority = setting.priority();
                    this.packetReliability = setting.reliability();
                    this.orderingChannel = setting.ordering();
                    this.requiredCapability = setting.requiredCapability();
                    this.handlingType = setting.handlingType();
                    this.anticheats = setting.anticheats();
                }
            }
        }

        private static PacketSetting getPacketSetting(Class<? extends INetworkPacket> cls) {
            PacketSetting[] settings = cls.getAnnotationsByType(PacketSetting.class);
            return settings.length == 0 ? null : settings[0];
        }

        public void send(UdpConnection connection) {
            connection.endPacket(this.packetPriority, this.packetReliability, this.orderingChannel);
            if (this.isLogEnabled()) {
                DebugLog.Packet.debugln("%s %s", connection.toString(), connection.getPacket(this).getDescription());
            }
        }

        public void doPacket(ByteBufferWriter bb) {
            bb.putByte((byte)-122);
            bb.putShort(this.getId());
        }

        public short getId() {
            return (short)this.ordinal();
        }

        public void onServerPacket(ByteBuffer bb, UdpConnection connection) throws Exception {
            NetworkStatistic.getInstance().addIncomePacket(this.getId(), bb.limit(), connection);
            if (PacketTypes.PacketAuthorization.isAuthorized(connection, this) && (this.handlingType & 1) != 0) {
                if (this.handler != null) {
                    INetworkPacket packet = connection.getPacket(this);
                    if (packet.isPostponed()) {
                        packet = this.handler.getDeclaredConstructor().newInstance();
                    }

                    packet.parseServer(bb, connection);
                    if (!packet.isConsistent(connection)) {
                        DebugLog.Multiplayer.warn("The packet %s is not consistent: %s", this.name(), packet.getDescription());
                        packet.sync(this, connection);
                        return;
                    }

                    if (this.isLogEnabled()) {
                        DebugLog.Packet.debugln("%s %s", connection.toString(), packet.getDescription());
                    }

                    if (this.anticheats != null) {
                        for (AntiCheat anticheat : this.anticheats) {
                            if (AntiCheat.None != anticheat && anticheat.isEnabled() && !anticheat.isValid(connection, packet)) {
                                DebugLog.Multiplayer.warn("The packet %s is not valid", this.name());
                                packet.sync(this, connection);
                                return;
                            }
                        }
                    }

                    packet.processServer(this, connection);
                } else {
                    this.serverHandler.call(bb, connection, this.getId());
                }
            }
        }

        public void onClientPacket(ByteBuffer bb) throws Exception {
            NetworkStatistic.getInstance().addIncomePacket(this.getId(), bb.limit(), GameClient.connection);
            if ((this.handlingType & 2) != 0) {
                if (this.handler != null) {
                    INetworkPacket packet = GameClient.connection.getPacket(this);
                    if (packet.isPostponed()) {
                        packet = this.handler.getDeclaredConstructor().newInstance();
                    }

                    packet.parseClient(bb, GameClient.connection);
                    if (!packet.isConsistent(GameClient.connection)) {
                        EventManager.instance().report(String.format("The packet %s is not consistent: %s", this.name(), packet.getDescription()));
                        return;
                    }

                    if (this.isLogEnabled()) {
                        DebugLog.Packet.debugln("%s %s", GameClient.connection.toString(), packet.getDescription());
                    }

                    if (packet.isPostponed()) {
                        packet.postpone();
                    } else {
                        packet.processClient(GameClient.connection);
                    }
                } else {
                    this.clientHandler.call(bb, this.getId());
                }
            }
        }

        public boolean onClientLoadingPacket(ByteBuffer bb) throws Exception {
            if ((this.handlingType & 4) != 0) {
                if (this.handler != null) {
                    INetworkPacket packet = GameClient.connection.getPacket(this);
                    if (packet.isPostponed()) {
                        packet = this.handler.getDeclaredConstructor().newInstance();
                    }

                    packet.parseClientLoading(bb, GameClient.connection);
                    if (!packet.isConsistent(GameClient.connection)) {
                        return false;
                    }

                    if (this.isLogEnabled()) {
                        DebugLog.Packet.debugln("%s %s", GameClient.connection.toString(), packet.getDescription());
                    }

                    packet.processClientLoading(GameClient.connection);
                } else {
                    this.clientLoadingHandler.call(bb, this.getId());
                }

                return true;
            } else {
                return false;
            }
        }

        public String getFlags() {
            String separator = "-";
            String result = String.valueOf(this.orderingChannel);
            result = result + "-";
            switch (this.packetPriority) {
                case 0:
                    result = result + "I";
                    break;
                case 1:
                    result = result + "H";
                    break;
                case 2:
                    result = result + "M";
                    break;
                case 3:
                    result = result + "L";
            }

            result = result + "-";
            switch (this.packetReliability) {
                case 0:
                    result = result + "U";
                    break;
                case 1:
                    result = result + "US";
                    break;
                case 2:
                    result = result + "R";
                    break;
                case 3:
                    result = result + "RO";
                    break;
                case 4:
                    result = result + "RS";
            }

            return result;
        }

        public boolean isLogEnabled() {
            return this.logEnabled;
        }

        public void setLogEnabled(boolean logEnabled) {
            this.logEnabled = logEnabled;
        }
    }
}
