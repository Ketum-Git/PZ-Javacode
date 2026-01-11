// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.config.BooleanConfigOption;
import zombie.config.ConfigFile;
import zombie.config.ConfigOption;
import zombie.config.DoubleConfigOption;
import zombie.config.EnumConfigOption;
import zombie.config.IntegerConfigOption;
import zombie.config.StringConfigOption;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.logger.LoggerManager;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;

@UsedFromLua
public class ServerOptions {
    public static final ServerOptions instance = new ServerOptions();
    private final ArrayList<String> publicOptions = new ArrayList<>();
    public static HashMap<String, String> clientOptionsList;
    public static final int MAX_PORT = 65535;
    private final ArrayList<ServerOptions.ServerOption> options = new ArrayList<>();
    private final HashMap<String, ServerOptions.ServerOption> optionByName = new HashMap<>();
    public ServerOptions.BooleanServerOption pvp = new ServerOptions.BooleanServerOption(this, "PVP", true);
    public ServerOptions.BooleanServerOption pvpLogToolChat = new ServerOptions.BooleanServerOption(this, "PVPLogToolChat", true);
    public ServerOptions.BooleanServerOption pvpLogToolFile = new ServerOptions.BooleanServerOption(this, "PVPLogToolFile", true);
    public ServerOptions.BooleanServerOption pauseEmpty = new ServerOptions.BooleanServerOption(this, "PauseEmpty", true);
    public ServerOptions.BooleanServerOption globalChat = new ServerOptions.BooleanServerOption(this, "GlobalChat", true);
    public ServerOptions.StringServerOption chatStreams = new ServerOptions.StringServerOption(this, "ChatStreams", "s,r,a,w,y,sh,f,all", -1);
    public ServerOptions.BooleanServerOption open = new ServerOptions.BooleanServerOption(this, "Open", true);
    public ServerOptions.TextServerOption serverWelcomeMessage = new ServerOptions.TextServerOption(
        this,
        "ServerWelcomeMessage",
        "Welcome to Project Zomboid Multiplayer! <LINE> <LINE> To interact with the Chat panel: press Tab, T, or Enter. <LINE> <LINE> The Tab key will change the target stream of the message. <LINE> <LINE> Global Streams: /all <LINE> Local Streams: /say, /yell <LINE> Special Steams: /whisper, /safehouse, /faction. <LINE> <LINE> Press the Up arrow to cycle through your message history. Click the Gear icon to customize chat. <LINE> <LINE> Happy surviving!",
        -1
    );
    public ServerOptions.StringServerOption serverImageLoginScreen = new ServerOptions.StringServerOption(this, "ServerImageLoginScreen", "", -1);
    public ServerOptions.StringServerOption serverImageLoadingScreen = new ServerOptions.StringServerOption(this, "ServerImageLoadingScreen", "", -1);
    public ServerOptions.StringServerOption serverImageIcon = new ServerOptions.StringServerOption(this, "ServerImageIcon", "", -1);
    public ServerOptions.BooleanServerOption autoCreateUserInWhiteList = new ServerOptions.BooleanServerOption(this, "AutoCreateUserInWhiteList", false);
    public ServerOptions.BooleanServerOption displayUserName = new ServerOptions.BooleanServerOption(this, "DisplayUserName", true);
    public ServerOptions.BooleanServerOption showFirstAndLastName = new ServerOptions.BooleanServerOption(this, "ShowFirstAndLastName", false);
    public ServerOptions.BooleanServerOption usernameDisguises = new ServerOptions.BooleanServerOption(this, "UsernameDisguises", false);
    public ServerOptions.BooleanServerOption hideDisguisedUserName = new ServerOptions.BooleanServerOption(this, "HideDisguisedUserName", false);
    public ServerOptions.BooleanServerOption switchZombiesOwnershipEachUpdate = new ServerOptions.BooleanServerOption(
        this, "SwitchZombiesOwnershipEachUpdate", false
    );
    public ServerOptions.StringServerOption spawnPoint = new ServerOptions.StringServerOption(this, "SpawnPoint", "0,0,0", -1);
    public ServerOptions.BooleanServerOption safetySystem = new ServerOptions.BooleanServerOption(this, "SafetySystem", true);
    public ServerOptions.BooleanServerOption showSafety = new ServerOptions.BooleanServerOption(this, "ShowSafety", true);
    public ServerOptions.IntegerServerOption safetyToggleTimer = new ServerOptions.IntegerServerOption(this, "SafetyToggleTimer", 0, 1000, 2);
    public ServerOptions.IntegerServerOption safetyCooldownTimer = new ServerOptions.IntegerServerOption(this, "SafetyCooldownTimer", 0, 1000, 3);
    public ServerOptions.IntegerServerOption safetyDisconnectDelay = new ServerOptions.IntegerServerOption(this, "SafetyDisconnectDelay", 0, 60, 60);
    public ServerOptions.StringServerOption spawnItems = new ServerOptions.StringServerOption(this, "SpawnItems", "", -1);
    public ServerOptions.IntegerServerOption defaultPort = new ServerOptions.IntegerServerOption(this, "DefaultPort", 0, 65535, 16261);
    public ServerOptions.IntegerServerOption udpPort = new ServerOptions.IntegerServerOption(this, "UDPPort", 0, 65535, 16262);
    public ServerOptions.IntegerServerOption resetId = new ServerOptions.IntegerServerOption(this, "ResetID", 0, Integer.MAX_VALUE, Rand.Next(1000000000));
    public ServerOptions.StringServerOption mods = new ServerOptions.StringServerOption(this, "Mods", "", -1);
    public ServerOptions.StringServerOption map = new ServerOptions.StringServerOption(this, "Map", "Muldraugh, KY", -1);
    public ServerOptions.BooleanServerOption doLuaChecksum = new ServerOptions.BooleanServerOption(this, "DoLuaChecksum", true);
    public ServerOptions.BooleanServerOption denyLoginOnOverloadedServer = new ServerOptions.BooleanServerOption(this, "DenyLoginOnOverloadedServer", true);
    public ServerOptions.BooleanServerOption isPublic = new ServerOptions.BooleanServerOption(this, "Public", false);
    public ServerOptions.StringServerOption publicName = new ServerOptions.StringServerOption(this, "PublicName", "My PZ Server", 64);
    public ServerOptions.TextServerOption publicDescription = new ServerOptions.TextServerOption(this, "PublicDescription", "", 256);
    public ServerOptions.IntegerServerOption maxPlayers = new ServerOptions.IntegerServerOption(this, "MaxPlayers", 1, 100, 32);
    public ServerOptions.IntegerServerOption pingLimit = new ServerOptions.IntegerServerOption(this, "PingLimit", 0, Integer.MAX_VALUE, 0);
    public ServerOptions.BooleanServerOption safehousePreventsLootRespawn = new ServerOptions.BooleanServerOption(this, "SafehousePreventsLootRespawn", true);
    public ServerOptions.BooleanServerOption dropOffWhiteListAfterDeath = new ServerOptions.BooleanServerOption(this, "DropOffWhiteListAfterDeath", false);
    public ServerOptions.BooleanServerOption noFire = new ServerOptions.BooleanServerOption(this, "NoFire", false);
    public ServerOptions.BooleanServerOption announceDeath = new ServerOptions.BooleanServerOption(this, "AnnounceDeath", false);
    public ServerOptions.IntegerServerOption saveWorldEveryMinutes = new ServerOptions.IntegerServerOption(
        this, "SaveWorldEveryMinutes", 0, Integer.MAX_VALUE, 0
    );
    public ServerOptions.BooleanServerOption playerSafehouse = new ServerOptions.BooleanServerOption(this, "PlayerSafehouse", false);
    public ServerOptions.BooleanServerOption adminSafehouse = new ServerOptions.BooleanServerOption(this, "AdminSafehouse", false);
    public ServerOptions.BooleanServerOption safehouseAllowTrepass = new ServerOptions.BooleanServerOption(this, "SafehouseAllowTrepass", true);
    public ServerOptions.BooleanServerOption safehouseAllowFire = new ServerOptions.BooleanServerOption(this, "SafehouseAllowFire", true);
    public ServerOptions.BooleanServerOption safehouseAllowLoot = new ServerOptions.BooleanServerOption(this, "SafehouseAllowLoot", true);
    public ServerOptions.BooleanServerOption safehouseAllowRespawn = new ServerOptions.BooleanServerOption(this, "SafehouseAllowRespawn", false);
    public ServerOptions.IntegerServerOption safehouseDaySurvivedToClaim = new ServerOptions.IntegerServerOption(
        this, "SafehouseDaySurvivedToClaim", 0, Integer.MAX_VALUE, 0
    );
    public ServerOptions.IntegerServerOption safeHouseRemovalTime = new ServerOptions.IntegerServerOption(
        this, "SafeHouseRemovalTime", 0, Integer.MAX_VALUE, 144
    );
    public ServerOptions.BooleanServerOption safehouseAllowNonResidential = new ServerOptions.BooleanServerOption(this, "SafehouseAllowNonResidential", false);
    public ServerOptions.BooleanServerOption safehouseDisableDisguises = new ServerOptions.BooleanServerOption(this, "SafehouseDisableDisguises", true);
    public ServerOptions.IntegerServerOption maxSafezoneSize = new ServerOptions.IntegerServerOption(this, "MaxSafezoneSize", 0, Integer.MAX_VALUE, 20000);
    public ServerOptions.BooleanServerOption allowDestructionBySledgehammer = new ServerOptions.BooleanServerOption(
        this, "AllowDestructionBySledgehammer", true
    );
    public ServerOptions.BooleanServerOption sledgehammerOnlyInSafehouse = new ServerOptions.BooleanServerOption(this, "SledgehammerOnlyInSafehouse", false);
    public ServerOptions.IntegerServerOption warStartDelay = new ServerOptions.IntegerServerOption(this, "WarStartDelay", 60, Integer.MAX_VALUE, 600);
    public ServerOptions.IntegerServerOption warDuration = new ServerOptions.IntegerServerOption(this, "WarDuration", 60, Integer.MAX_VALUE, 3600);
    public ServerOptions.IntegerServerOption warSafehouseHitPoints = new ServerOptions.IntegerServerOption(
        this, "WarSafehouseHitPoints", 0, Integer.MAX_VALUE, 3
    );
    public ServerOptions.StringServerOption serverPlayerId = new ServerOptions.StringServerOption(
        this, "ServerPlayerID", Integer.toString(Rand.Next(Integer.MAX_VALUE)), -1
    );
    public ServerOptions.IntegerServerOption rconPort = new ServerOptions.IntegerServerOption(this, "RCONPort", 0, 65535, 27015);
    public ServerOptions.StringServerOption rconPassword = new ServerOptions.StringServerOption(this, "RCONPassword", "", -1);
    public ServerOptions.BooleanServerOption discordEnable = new ServerOptions.BooleanServerOption(this, "DiscordEnable", false);
    public ServerOptions.StringServerOption discordToken = new ServerOptions.StringServerOption(this, "DiscordToken", "", -1);
    public ServerOptions.StringServerOption discordChannel = new ServerOptions.StringServerOption(this, "DiscordChannel", "", -1);
    public ServerOptions.StringServerOption discordChannelId = new ServerOptions.StringServerOption(this, "DiscordChannelID", "", -1);
    public ServerOptions.StringServerOption webhookAddress = new ServerOptions.StringServerOption(this, "WebhookAddress", "", -1);
    public ServerOptions.StringServerOption password = new ServerOptions.StringServerOption(this, "Password", "", -1);
    public ServerOptions.IntegerServerOption maxAccountsPerUser = new ServerOptions.IntegerServerOption(this, "MaxAccountsPerUser", 0, Integer.MAX_VALUE, 0);
    public ServerOptions.BooleanServerOption allowCoop = new ServerOptions.BooleanServerOption(this, "AllowCoop", true);
    public ServerOptions.BooleanServerOption sleepAllowed = new ServerOptions.BooleanServerOption(this, "SleepAllowed", false);
    public ServerOptions.BooleanServerOption sleepNeeded = new ServerOptions.BooleanServerOption(this, "SleepNeeded", false);
    public ServerOptions.BooleanServerOption knockedDownAllowed = new ServerOptions.BooleanServerOption(this, "KnockedDownAllowed", false);
    public ServerOptions.BooleanServerOption sneakModeHideFromOtherPlayers = new ServerOptions.BooleanServerOption(this, "SneakModeHideFromOtherPlayers", true);
    public ServerOptions.BooleanServerOption ultraSpeedDoesnotAffectToAnimals = new ServerOptions.BooleanServerOption(
        this, "UltraSpeedDoesnotAffectToAnimals", false
    );
    public ServerOptions.StringServerOption workshopItems = new ServerOptions.StringServerOption(this, "WorkshopItems", "", -1);
    public ServerOptions.BooleanServerOption steamScoreboard = new ServerOptions.BooleanServerOption(this, "SteamScoreboard", false);
    public ServerOptions.BooleanServerOption steamVac = new ServerOptions.BooleanServerOption(this, "SteamVAC", true);
    public ServerOptions.BooleanServerOption uPnp = new ServerOptions.BooleanServerOption(this, "UPnP", true);
    public ServerOptions.BooleanServerOption voiceEnable = new ServerOptions.BooleanServerOption(this, "VoiceEnable", true);
    public ServerOptions.DoubleServerOption voiceMinDistance = new ServerOptions.DoubleServerOption(this, "VoiceMinDistance", 0.0, 100000.0, 10.0);
    public ServerOptions.DoubleServerOption voiceMaxDistance = new ServerOptions.DoubleServerOption(this, "VoiceMaxDistance", 0.0, 100000.0, 100.0);
    public ServerOptions.BooleanServerOption voice3d = new ServerOptions.BooleanServerOption(this, "Voice3D", true);
    public ServerOptions.DoubleServerOption speedLimit = new ServerOptions.DoubleServerOption(this, "SpeedLimit", 10.0, 150.0, 70.0);
    public ServerOptions.BooleanServerOption loginQueueEnabled = new ServerOptions.BooleanServerOption(this, "LoginQueueEnabled", false);
    public ServerOptions.IntegerServerOption loginQueueConnectTimeout = new ServerOptions.IntegerServerOption(this, "LoginQueueConnectTimeout", 20, 1200, 60);
    public ServerOptions.StringServerOption serverBrowserAnnouncedIp = new ServerOptions.StringServerOption(this, "server_browser_announced_ip", "", -1);
    public ServerOptions.BooleanServerOption playerRespawnWithSelf = new ServerOptions.BooleanServerOption(this, "PlayerRespawnWithSelf", false);
    public ServerOptions.BooleanServerOption playerRespawnWithOther = new ServerOptions.BooleanServerOption(this, "PlayerRespawnWithOther", false);
    public ServerOptions.DoubleServerOption fastForwardMultiplier = new ServerOptions.DoubleServerOption(this, "FastForwardMultiplier", 1.0, 100.0, 40.0);
    public ServerOptions.BooleanServerOption disableSafehouseWhenPlayerConnected = new ServerOptions.BooleanServerOption(
        this, "DisableSafehouseWhenPlayerConnected", false
    );
    public ServerOptions.BooleanServerOption faction = new ServerOptions.BooleanServerOption(this, "Faction", true);
    public ServerOptions.IntegerServerOption factionDaySurvivedToCreate = new ServerOptions.IntegerServerOption(
        this, "FactionDaySurvivedToCreate", 0, Integer.MAX_VALUE, 0
    );
    public ServerOptions.IntegerServerOption factionPlayersRequiredForTag = new ServerOptions.IntegerServerOption(
        this, "FactionPlayersRequiredForTag", 1, Integer.MAX_VALUE, 1
    );
    public ServerOptions.BooleanServerOption disableRadioStaff = new ServerOptions.BooleanServerOption(this, "DisableRadioStaff", false);
    public ServerOptions.BooleanServerOption disableRadioAdmin = new ServerOptions.BooleanServerOption(this, "DisableRadioAdmin", true);
    public ServerOptions.BooleanServerOption disableRadioGm = new ServerOptions.BooleanServerOption(this, "DisableRadioGM", true);
    public ServerOptions.BooleanServerOption disableRadioOverseer = new ServerOptions.BooleanServerOption(this, "DisableRadioOverseer", false);
    public ServerOptions.BooleanServerOption disableRadioModerator = new ServerOptions.BooleanServerOption(this, "DisableRadioModerator", false);
    public ServerOptions.BooleanServerOption disableRadioInvisible = new ServerOptions.BooleanServerOption(this, "DisableRadioInvisible", true);
    public ServerOptions.StringServerOption clientCommandFilter = new ServerOptions.StringServerOption(
        this, "ClientCommandFilter", "-vehicle.*;+vehicle.damageWindow;+vehicle.fixPart;+vehicle.installPart;+vehicle.uninstallPart", -1
    );
    public ServerOptions.StringServerOption clientActionLogs = new ServerOptions.StringServerOption(
        this, "ClientActionLogs", "ISEnterVehicle;ISExitVehicle;ISTakeEngineParts;", -1
    );
    public ServerOptions.BooleanServerOption perkLogs = new ServerOptions.BooleanServerOption(this, "PerkLogs", true);
    public ServerOptions.IntegerServerOption itemNumbersLimitPerContainer = new ServerOptions.IntegerServerOption(
        this, "ItemNumbersLimitPerContainer", 0, 9000, 0
    );
    public ServerOptions.IntegerServerOption bloodSplatLifespanDays = new ServerOptions.IntegerServerOption(this, "BloodSplatLifespanDays", 0, 365, 0);
    public ServerOptions.BooleanServerOption allowNonAsciiUsername = new ServerOptions.BooleanServerOption(this, "AllowNonAsciiUsername", false);
    public ServerOptions.BooleanServerOption banKickGlobalSound = new ServerOptions.BooleanServerOption(this, "BanKickGlobalSound", true);
    public ServerOptions.BooleanServerOption removePlayerCorpsesOnCorpseRemoval = new ServerOptions.BooleanServerOption(
        this, "RemovePlayerCorpsesOnCorpseRemoval", false
    );
    public ServerOptions.BooleanServerOption trashDeleteAll = new ServerOptions.BooleanServerOption(this, "TrashDeleteAll", false);
    public ServerOptions.BooleanServerOption pvpMeleeWhileHitReaction = new ServerOptions.BooleanServerOption(this, "PVPMeleeWhileHitReaction", false);
    public ServerOptions.BooleanServerOption mouseOverToSeeDisplayName = new ServerOptions.BooleanServerOption(this, "MouseOverToSeeDisplayName", true);
    public ServerOptions.BooleanServerOption hidePlayersBehindYou = new ServerOptions.BooleanServerOption(this, "HidePlayersBehindYou", true);
    public ServerOptions.DoubleServerOption pvpMeleeDamageModifier = new ServerOptions.DoubleServerOption(this, "PVPMeleeDamageModifier", 0.0, 500.0, 30.0);
    public ServerOptions.DoubleServerOption pvpFirearmDamageModifier = new ServerOptions.DoubleServerOption(this, "PVPFirearmDamageModifier", 0.0, 500.0, 50.0);
    public ServerOptions.DoubleServerOption carEngineAttractionModifier = new ServerOptions.DoubleServerOption(
        this, "CarEngineAttractionModifier", 0.0, 10.0, 0.5
    );
    public ServerOptions.BooleanServerOption playerBumpPlayer = new ServerOptions.BooleanServerOption(this, "PlayerBumpPlayer", false);
    public ServerOptions.IntegerServerOption mapRemotePlayerVisibility = new ServerOptions.IntegerServerOption(this, "MapRemotePlayerVisibility", 1, 3, 1);
    public ServerOptions.IntegerServerOption backupsCount = new ServerOptions.IntegerServerOption(this, "BackupsCount", 1, 300, 5);
    public ServerOptions.BooleanServerOption backupsOnStart = new ServerOptions.BooleanServerOption(this, "BackupsOnStart", true);
    public ServerOptions.BooleanServerOption backupsOnVersionChange = new ServerOptions.BooleanServerOption(this, "BackupsOnVersionChange", true);
    public ServerOptions.IntegerServerOption backupsPeriod = new ServerOptions.IntegerServerOption(this, "BackupsPeriod", 0, 1500, 0);
    public ServerOptions.BooleanServerOption disableVehicleTowing = new ServerOptions.BooleanServerOption(this, "DisableVehicleTowing", false);
    public ServerOptions.BooleanServerOption disableTrailerTowing = new ServerOptions.BooleanServerOption(this, "DisableTrailerTowing", false);
    public ServerOptions.BooleanServerOption disableBurntTowing = new ServerOptions.BooleanServerOption(this, "DisableBurntTowing", false);
    public ServerOptions.StringServerOption badWordListFile = new ServerOptions.StringServerOption(this, "BadWordListFile", "", -1);
    public ServerOptions.StringServerOption goodWordListFile = new ServerOptions.StringServerOption(this, "GoodWordListFile", "", -1);
    public ServerOptions.EnumServerOption badWordPolicy = new ServerOptions.EnumServerOption(this, "BadWordPolicy", 3, 3);
    public ServerOptions.StringServerOption badWordReplacement = new ServerOptions.StringServerOption(this, "BadWordReplacement", "[HIDDEN]", 16);
    public ServerOptions.EnumServerOption antiCheatSafety = new ServerOptions.EnumServerOption(this, "AntiCheatSafety", 4, 4);
    public ServerOptions.EnumServerOption antiCheatMovement = new ServerOptions.EnumServerOption(this, "AntiCheatMovement", 4, 4);
    public ServerOptions.EnumServerOption antiCheatHit = new ServerOptions.EnumServerOption(this, "AntiCheatHit", 4, 4);
    public ServerOptions.EnumServerOption antiCheatPacket = new ServerOptions.EnumServerOption(this, "AntiCheatPacket", 4, 4);
    public ServerOptions.EnumServerOption antiCheatPermission = new ServerOptions.EnumServerOption(this, "AntiCheatPermission", 4, 4);
    public ServerOptions.EnumServerOption antiCheatXp = new ServerOptions.EnumServerOption(this, "AntiCheatXP", 4, 4);
    public ServerOptions.EnumServerOption antiCheatFire = new ServerOptions.EnumServerOption(this, "AntiCheatFire", 4, 4);
    public ServerOptions.EnumServerOption antiCheatSafeHouse = new ServerOptions.EnumServerOption(this, "AntiCheatSafeHouse", 4, 4);
    public ServerOptions.EnumServerOption antiCheatRecipe = new ServerOptions.EnumServerOption(this, "AntiCheatRecipe", 4, 4);
    public ServerOptions.EnumServerOption antiCheatPlayer = new ServerOptions.EnumServerOption(this, "AntiCheatPlayer", 4, 4);
    public ServerOptions.EnumServerOption antiCheatChecksum = new ServerOptions.EnumServerOption(this, "AntiCheatChecksum", 4, 4);
    public ServerOptions.EnumServerOption antiCheatItem = new ServerOptions.EnumServerOption(this, "AntiCheatItem", 4, 4);
    public ServerOptions.EnumServerOption antiCheatServerCustomization = new ServerOptions.EnumServerOption(this, "AntiCheatServerCustomization", 4, 4);
    public ServerOptions.IntegerServerOption multiplayerStatisticsPeriod = new ServerOptions.IntegerServerOption(this, "MultiplayerStatisticsPeriod", 0, 10, 1);
    public ServerOptions.BooleanServerOption disableScoreboard = new ServerOptions.BooleanServerOption(this, "DisableScoreboard", false);
    public ServerOptions.BooleanServerOption hideAdminsInPlayerList = new ServerOptions.BooleanServerOption(this, "HideAdminsInPlayerList", false);
    public ServerOptions.StringServerOption seed = new ServerOptions.StringServerOption(this, "Seed", GameServer.seed, -1);
    public ServerOptions.BooleanServerOption usePhysicsHitReaction = new ServerOptions.BooleanServerOption(this, "UsePhysicsHitReaction", false);
    public ServerOptions.IntegerServerOption chatMessageCharacterLimit = new ServerOptions.IntegerServerOption(this, "ChatMessageCharacterLimit", 64, 1024, 200);
    public ServerOptions.IntegerServerOption chatMessageSlowModeTime = new ServerOptions.IntegerServerOption(this, "ChatMessageSlowModeTime", 1, 30, 3);
    public static ArrayList<String> cardList;

    public ServerOptions() {
        this.publicOptions.clear();
        this.publicOptions.addAll(this.optionByName.keySet());
        this.publicOptions.remove("Password");
        this.publicOptions.remove("RCONPort");
        this.publicOptions.remove("RCONPassword");
        this.publicOptions.remove(this.discordToken.getName());
        this.publicOptions.remove(this.discordChannel.getName());
        this.publicOptions.remove(this.discordChannelId.getName());
        Collections.sort(this.publicOptions);
    }

    private void initOptions() {
        initClientCommandsHelp();

        for (ServerOptions.ServerOption option : this.options) {
            option.asConfigOption().resetToDefault();
        }
    }

    public ArrayList<String> getPublicOptions() {
        return this.publicOptions;
    }

    public ArrayList<ServerOptions.ServerOption> getOptions() {
        return this.options;
    }

    public static void initClientCommandsHelp() {
        clientOptionsList = new HashMap<>();
        clientOptionsList.put("help", Translator.getText("UI_ServerOptionDesc_Help"));
        clientOptionsList.put("changepwd", Translator.getText("UI_ServerOptionDesc_ChangePwd"));
        clientOptionsList.put("roll", Translator.getText("UI_ServerOptionDesc_Roll"));
        clientOptionsList.put("card", Translator.getText("UI_ServerOptionDesc_Card"));
        clientOptionsList.put("safehouse", Translator.getText("UI_ServerOptionDesc_SafeHouse"));
    }

    public void init() {
        this.initOptions();
        File serverFolder = new File(ZomboidFileSystem.instance.getCacheDir() + File.separator + "Server");
        if (!serverFolder.exists()) {
            serverFolder.mkdirs();
        }

        File serverOptsFile = new File(ZomboidFileSystem.instance.getCacheDir() + File.separator + "Server" + File.separator + GameServer.serverName + ".ini");
        if (serverOptsFile.exists()) {
            try {
                Core.getInstance().loadOptions();
            } catch (IOException var4) {
                DebugLog.General.printException(var4, "Can't load server options", LogSeverity.Error);
            }

            if (this.loadServerTextFile(GameServer.serverName)) {
                this.saveServerTextFile(GameServer.serverName);
            }
        } else {
            this.saveServerTextFile(GameServer.serverName);
        }

        tryInitSpawnRegionsFile();
        LoggerManager.init();
    }

    public void resetRegionFile() {
        File file = new File(
            ZomboidFileSystem.instance.getCacheDir() + File.separator + "Server" + File.separator + GameServer.serverName + "_spawnregions.lua"
        );
        file.delete();
        tryInitSpawnRegionsFile();
    }

    private static void tryInitSpawnRegionsFile() {
        try {
            File spawnRegionsFile = new File(
                ZomboidFileSystem.instance.getCacheDir() + File.separator + "Server" + File.separator + GameServer.serverName + "_spawnregions.lua"
            );
            if (!spawnRegionsFile.exists()) {
                DebugLog.DetailedInfo.trace("creating server spawnregions file \"" + spawnRegionsFile.getPath() + "\"");
                spawnRegionsFile.createNewFile();
                FileWriter fw = new FileWriter(spawnRegionsFile);
                fw.write("function SpawnRegions()" + System.lineSeparator());
                fw.write("\treturn {" + System.lineSeparator());
                fw.write("\t\t{ name = \"Muldraugh, KY\", file = \"media/maps/Muldraugh, KY/spawnpoints.lua\" }," + System.lineSeparator());
                fw.write("\t\t{ name = \"West Point, KY\", file = \"media/maps/West Point, KY/spawnpoints.lua\" }," + System.lineSeparator());
                fw.write("\t\t{ name = \"Rosewood, KY\", file = \"media/maps/Rosewood, KY/spawnpoints.lua\" }," + System.lineSeparator());
                fw.write("\t\t{ name = \"Riverside, KY\", file = \"media/maps/Riverside, KY/spawnpoints.lua\" }," + System.lineSeparator());
                fw.write("\t\t-- Uncomment the line below to add a custom spawnpoint for this server." + System.lineSeparator());
                fw.write("--\t\t{ name = \"Twiggy's Bar\", serverfile = \"" + GameServer.serverName + "_spawnpoints.lua\" }," + System.lineSeparator());
                fw.write("\t}" + System.lineSeparator());
                fw.write("end" + System.lineSeparator());
                fw.close();
            }

            File spawnPointsFile = new File(spawnRegionsFile.getParent() + File.separator + GameServer.serverName + "_spawnpoints.lua");
            if (!spawnPointsFile.exists()) {
                DebugLog.DetailedInfo.trace("creating server spawnpoints file \"" + spawnRegionsFile.getPath() + "\"");
                spawnPointsFile.createNewFile();
                FileWriter fw = new FileWriter(spawnPointsFile);
                fw.write("function SpawnPoints()" + System.lineSeparator());
                fw.write("\treturn {" + System.lineSeparator());
                fw.write("\t\tunemployed = {" + System.lineSeparator());
                fw.write("\t\t\t{ worldX = 40, worldY = 22, posX = 67, posY = 201 }" + System.lineSeparator());
                fw.write("\t\t}" + System.lineSeparator());
                fw.write("\t}" + System.lineSeparator());
                fw.write("end" + System.lineSeparator());
                fw.close();
            }
        } catch (Exception var3) {
            DebugLog.General.printException(var3, "Can't initialize spawn regions or spawn points", LogSeverity.Error);
        }
    }

    public String getOption(String key) {
        ServerOptions.ServerOption option = this.getOptionByName(key);
        return option == null ? null : option.asConfigOption().getValueAsString();
    }

    public Boolean getBoolean(String key) {
        return this.getOptionByName(key) instanceof ServerOptions.BooleanServerOption booleanServerOption
            ? (Boolean)booleanServerOption.getValueAsObject()
            : null;
    }

    public Float getFloat(String key) {
        return this.getOptionByName(key) instanceof ServerOptions.DoubleServerOption doubleServerOption ? (float)doubleServerOption.getValue() : null;
    }

    public Double getDouble(String key) {
        return this.getOptionByName(key) instanceof ServerOptions.DoubleServerOption doubleServerOption ? doubleServerOption.getValue() : null;
    }

    public Integer getInteger(String key) {
        return this.getOptionByName(key) instanceof ServerOptions.IntegerServerOption integerServerOption ? integerServerOption.getValue() : null;
    }

    public void putOption(String key, String value) {
        ServerOptions.ServerOption option = this.getOptionByName(key);
        if (option != null) {
            option.asConfigOption().parse(value);
        }
    }

    public void putSaveOption(String key, String value) {
        this.putOption(key, value);
        this.saveServerTextFile(GameServer.serverName);
    }

    public String changeOption(String key, String value) {
        ServerOptions.ServerOption option = this.getOptionByName(key);
        if (option == null) {
            return "Option " + key + " doesn't exist.";
        } else {
            option.asConfigOption().parse(value);
            return !this.saveServerTextFile(GameServer.serverName)
                ? "An error as occured."
                : "Option : " + key + " is now : " + option.asConfigOption().getValueAsString();
        }
    }

    public static ServerOptions getInstance() {
        return instance;
    }

    public static ArrayList<String> getClientCommandList(boolean doLine) {
        String carriageReturn = " <LINE> ";
        if (!doLine) {
            carriageReturn = "\n";
        }

        if (clientOptionsList == null) {
            initClientCommandsHelp();
        }

        ArrayList<String> result = new ArrayList<>();
        Iterator<String> it = clientOptionsList.keySet().iterator();
        String key = null;
        result.add("List of commands : " + carriageReturn);

        while (it.hasNext()) {
            key = it.next();
            result.add("* " + key + " : " + clientOptionsList.get(key) + (it.hasNext() ? carriageReturn : ""));
        }

        return result;
    }

    public static String getRandomCard() {
        if (cardList == null) {
            cardList = new ArrayList<>();
            cardList.add("the Ace of Clubs");
            cardList.add("a Two of Clubs");
            cardList.add("a Three of Clubs");
            cardList.add("a Four of Clubs");
            cardList.add("a Five of Clubs");
            cardList.add("a Six of Clubs");
            cardList.add("a Seven of Clubs");
            cardList.add("an Eight of Clubs");
            cardList.add("a Nine of Clubs");
            cardList.add("a Ten of Clubs");
            cardList.add("the Jack of Clubs");
            cardList.add("the Queen of Clubs");
            cardList.add("the King of Clubs");
            cardList.add("the Ace of Diamonds");
            cardList.add("a Two of Diamonds");
            cardList.add("a Three of Diamonds");
            cardList.add("a Four of Diamonds");
            cardList.add("a Five of Diamonds");
            cardList.add("a Six of Diamonds");
            cardList.add("a Seven of Diamonds");
            cardList.add("an Eight of Diamonds");
            cardList.add("a Nine of Diamonds");
            cardList.add("a Ten of Diamonds");
            cardList.add("the Jack of Diamonds");
            cardList.add("the Queen of Diamonds");
            cardList.add("the King of Diamonds");
            cardList.add("the Ace of Hearts");
            cardList.add("a Two of Hearts");
            cardList.add("a Three of Hearts");
            cardList.add("a Four of Hearts");
            cardList.add("a Five of Hearts");
            cardList.add("a Six of Hearts");
            cardList.add("a Seven of Hearts");
            cardList.add("an Eight of Hearts");
            cardList.add("a Nine of Hearts");
            cardList.add("a Ten of Hearts");
            cardList.add("the Jack of Hearts");
            cardList.add("the Queen of Hearts");
            cardList.add("the King of Hearts");
            cardList.add("the Ace of Spades");
            cardList.add("a Two of Spades");
            cardList.add("a Three of Spades");
            cardList.add("a Four of Spades");
            cardList.add("a Five of Spades");
            cardList.add("a Six of Spades");
            cardList.add("a Seven of Spades");
            cardList.add("an Eight of Spades");
            cardList.add("a Nine of Spades");
            cardList.add("a Ten of Spades");
            cardList.add("the Jack of Spades");
            cardList.add("the Queen of Spades");
            cardList.add("the King of Spades");
        }

        return cardList.get(Rand.Next(cardList.size()));
    }

    public void addOption(ServerOptions.ServerOption option) {
        if (this.optionByName.containsKey(option.asConfigOption().getName())) {
            throw new IllegalArgumentException();
        } else {
            this.options.add(option);
            this.optionByName.put(option.asConfigOption().getName(), option);
        }
    }

    public int getNumOptions() {
        return this.options.size();
    }

    public ServerOptions.ServerOption getOptionByIndex(int index) {
        return this.options.get(index);
    }

    public ServerOptions.ServerOption getOptionByName(String name) {
        return this.optionByName.get(name);
    }

    public boolean loadServerTextFile(String serverName) {
        ConfigFile configFile = new ConfigFile();
        String fileName = ZomboidFileSystem.instance.getCacheDir() + File.separator + "Server" + File.separator + serverName + ".ini";
        if (configFile.read(fileName)) {
            for (ConfigOption configOption : configFile.getOptions()) {
                ServerOptions.ServerOption option = this.optionByName.get(configOption.getName());
                if (option != null) {
                    option.asConfigOption().parse(configOption.getValueAsString());
                }
            }

            return true;
        } else {
            return false;
        }
    }

    public boolean saveServerTextFile(String serverName) {
        ConfigFile configFile = new ConfigFile();
        String fileName = ZomboidFileSystem.instance.getCacheDir() + File.separator + "Server" + File.separator + serverName + ".ini";
        ArrayList<ConfigOption> configOptions = new ArrayList<>();

        for (ServerOptions.ServerOption option : this.options) {
            configOptions.add(option.asConfigOption());
        }

        return configFile.write(fileName, 0, configOptions);
    }

    public int getMaxPlayers() {
        return Math.min(100, getInstance().maxPlayers.getValue());
    }

    @UsedFromLua
    public static class BooleanServerOption extends BooleanConfigOption implements ServerOptions.ServerOption {
        public BooleanServerOption(ServerOptions owner, String name, boolean defaultValue) {
            super(name, defaultValue);
            owner.addOption(this);
        }

        @Override
        public ConfigOption asConfigOption() {
            return this;
        }

        @Override
        public String getTooltip() {
            return Translator.getTextOrNull("UI_ServerOption_" + this.name + "_tooltip");
        }
    }

    @UsedFromLua
    public static class DoubleServerOption extends DoubleConfigOption implements ServerOptions.ServerOption {
        public DoubleServerOption(ServerOptions owner, String name, double min, double max, double defaultValue) {
            super(name, min, max, defaultValue);
            owner.addOption(this);
        }

        @Override
        public ConfigOption asConfigOption() {
            return this;
        }

        @Override
        public String getTooltip() {
            String s1 = Translator.getTextOrNull("UI_ServerOption_" + this.name + "_tooltip");
            String s2 = Translator.getText(
                "Sandbox_MinMaxDefault", String.format("%.02f", this.min), String.format("%.02f", this.max), String.format("%.02f", this.defaultValue)
            );
            if (s1 == null) {
                return s2;
            } else {
                return s2 == null ? s1 : s1 + "\\n" + s2;
            }
        }
    }

    @UsedFromLua
    public static class EnumServerOption extends EnumConfigOption implements ServerOptions.ServerOption {
        public EnumServerOption(ServerOptions owner, String name, int numValues, int defaultValue) {
            super(name, numValues, defaultValue);
            owner.addOption(this);
        }

        @Override
        public ConfigOption asConfigOption() {
            return this;
        }

        @Override
        public String getTooltip() {
            return Translator.getTextOrNull("UI_ServerOption_" + this.name + "_tooltip");
        }

        public String getValueTranslationByIndex(int index) {
            if (index >= 1 && index <= this.getNumValues()) {
                return Translator.getTextOrNull("UI_ServerOption_AntiCheat_option" + index);
            } else {
                throw new ArrayIndexOutOfBoundsException();
            }
        }
    }

    @UsedFromLua
    public static class IntegerServerOption extends IntegerConfigOption implements ServerOptions.ServerOption {
        public IntegerServerOption(ServerOptions owner, String name, int min, int max, int defaultValue) {
            super(name, min, max, defaultValue);
            owner.addOption(this);
        }

        @Override
        public ConfigOption asConfigOption() {
            return this;
        }

        @Override
        public String getTooltip() {
            String s1 = Translator.getTextOrNull("UI_ServerOption_" + this.name + "_tooltip");
            String s2 = Translator.getText("Sandbox_MinMaxDefault", this.min, this.max, this.defaultValue);
            if (s1 == null) {
                return s2;
            } else {
                return s2 == null ? s1 : s1 + "\\n" + s2;
            }
        }
    }

    public interface ServerOption {
        ConfigOption asConfigOption();

        String getTooltip();
    }

    @UsedFromLua
    public static class StringServerOption extends StringConfigOption implements ServerOptions.ServerOption {
        public StringServerOption(ServerOptions owner, String name, String defaultValue, int maxLength) {
            super(name, defaultValue, maxLength);
            owner.addOption(this);
        }

        @Override
        public ConfigOption asConfigOption() {
            return this;
        }

        @Override
        public String getTooltip() {
            return Translator.getTextOrNull("UI_ServerOption_" + this.name + "_tooltip");
        }
    }

    @UsedFromLua
    public static class TextServerOption extends StringConfigOption implements ServerOptions.ServerOption {
        public TextServerOption(ServerOptions owner, String name, String defaultValue, int maxLength) {
            super(name, defaultValue, maxLength);
            owner.addOption(this);
        }

        @Override
        public String getType() {
            return "text";
        }

        @Override
        public ConfigOption asConfigOption() {
            return this;
        }

        @Override
        public String getTooltip() {
            return Translator.getTextOrNull("UI_ServerOption_" + this.name + "_tooltip");
        }
    }
}
