// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.skills;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import se.krka.kahlua.vm.KahluaTable;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaManager;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.gameStates.ChooseGameInfo;
import zombie.scripting.ScriptParser;
import zombie.util.StringUtils;

public final class CustomPerks {
    private static final int VERSION1 = 1;
    private static final int VERSION = 1;
    public static final CustomPerks instance = new CustomPerks();
    private final ArrayList<CustomPerk> perks = new ArrayList<>();

    public void init() {
        ArrayList<String> modIDs = ZomboidFileSystem.instance.getModIDs();

        for (int i = 0; i < modIDs.size(); i++) {
            String modID = modIDs.get(i);
            ChooseGameInfo.Mod mod = ChooseGameInfo.getAvailableModDetails(modID);
            if (mod != null) {
                File file = new File(mod.getVersionDir() + File.separator + "media" + File.separator + "perks.txt");
                if (file.exists()) {
                    this.readFile(file.getAbsolutePath());
                } else {
                    file = new File(mod.getCommonDir() + File.separator + "media" + File.separator + "perks.txt");
                    if (file.exists()) {
                        this.readFile(file.getAbsolutePath());
                    }
                }
            }
        }

        for (CustomPerk customPerk : this.perks) {
            PerkFactory.Perk perk = PerkFactory.Perks.FromString(customPerk.id);
            if (perk == null || perk == PerkFactory.Perks.None || perk == PerkFactory.Perks.MAX) {
                perk = new PerkFactory.Perk(customPerk.id);
                perk.setCustom();
            }
        }

        for (CustomPerk customPerkx : this.perks) {
            PerkFactory.Perk perk = PerkFactory.Perks.FromString(customPerkx.id);
            PerkFactory.Perk parent = PerkFactory.Perks.FromString(customPerkx.parent);
            if (parent == null || parent == PerkFactory.Perks.None || parent == PerkFactory.Perks.MAX) {
                parent = PerkFactory.Perks.None;
            }

            int[] xp = customPerkx.xp;
            PerkFactory.AddPerk(
                perk, customPerkx.translation, parent, xp[0], xp[1], xp[2], xp[3], xp[4], xp[5], xp[6], xp[7], xp[8], xp[9], customPerkx.passive
            );
        }
    }

    public void initLua() {
        KahluaTable Perks = (KahluaTable)LuaManager.env.rawget("Perks");

        for (CustomPerk customPerk : this.perks) {
            PerkFactory.Perk perk = PerkFactory.Perks.FromString(customPerk.id);
            Perks.rawset(perk.getId(), perk);
        }
    }

    public static void Reset() {
        instance.perks.clear();
    }

    private boolean readFile(String path) {
        try {
            boolean var6;
            try (
                FileReader fr = new FileReader(path);
                BufferedReader br = new BufferedReader(fr);
            ) {
                StringBuilder stringBuilder = new StringBuilder();

                for (String str = br.readLine(); str != null; str = br.readLine()) {
                    stringBuilder.append(str);
                }

                this.parse(stringBuilder.toString());
                var6 = true;
            }

            return var6;
        } catch (FileNotFoundException var11) {
            return false;
        } catch (Exception var12) {
            ExceptionLogger.logException(var12);
            return false;
        }
    }

    private void parse(String contents) {
        contents = ScriptParser.stripComments(contents);
        ScriptParser.Block block = ScriptParser.parse(contents);
        int version = -1;
        ScriptParser.Value value = block.getValue("VERSION");
        if (value != null) {
            version = PZMath.tryParseInt(value.getValue(), -1);
        }

        if (version >= 1 && version <= 1) {
            for (ScriptParser.Block block1 : block.children) {
                if (!block1.type.equalsIgnoreCase("perk")) {
                    throw new RuntimeException("unknown block type \"" + block1.type + "\"");
                }

                CustomPerk option = this.parsePerk(block1);
                if (option == null) {
                    DebugLog.General.warn("failed to parse custom perk \"%s\"", block1.id);
                } else {
                    this.perks.add(option);
                }
            }
        } else {
            throw new RuntimeException("invalid or missing VERSION");
        }
    }

    private CustomPerk parsePerk(ScriptParser.Block block) {
        if (StringUtils.isNullOrWhitespace(block.id)) {
            DebugLog.General.warn("missing or empty perk id");
            return null;
        } else {
            CustomPerk customPerk = new CustomPerk(block.id);
            ScriptParser.Value vParent = block.getValue("parent");
            if (vParent != null && !StringUtils.isNullOrWhitespace(vParent.getValue())) {
                customPerk.parent = vParent.getValue().trim();
            }

            ScriptParser.Value vTranslation = block.getValue("translation");
            if (vTranslation != null) {
                customPerk.translation = StringUtils.discardNullOrWhitespace(vTranslation.getValue().trim());
            }

            if (StringUtils.isNullOrWhitespace(customPerk.translation)) {
                customPerk.translation = customPerk.id;
            }

            ScriptParser.Value vPassive = block.getValue("passive");
            if (vPassive != null) {
                customPerk.passive = StringUtils.tryParseBoolean(vPassive.getValue().trim());
            }

            for (int i = 1; i <= 10; i++) {
                ScriptParser.Value vXP = block.getValue("xp" + i);
                if (vXP != null) {
                    int xp = PZMath.tryParseInt(vXP.getValue().trim(), -1);
                    if (xp > 0) {
                        customPerk.xp[i - 1] = xp;
                    }
                }
            }

            return customPerk;
        }
    }
}
