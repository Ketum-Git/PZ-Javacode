// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import se.krka.kahlua.vm.KahluaTable;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaManager;
import zombie.characters.skills.PerkFactory;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.gameStates.ChooseGameInfo;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;
import zombie.util.StringUtils;

@UsedFromLua
public final class Translator {
    private static ArrayList<Language> availableLanguage;
    public static boolean debug;
    private static FileWriter debugFile;
    private static boolean debugErrors;
    private static final Set<String> debugItemEvolvedRecipeName = new HashSet<>();
    private static final Set<String> debugItem = new HashSet<>();
    private static final Set<String> debugMultiStageBuild = new HashSet<>();
    private static final Set<String> debugRecipe = new HashSet<>();
    private static final Set<String> debugRecipeGroups = new HashSet<>();
    private static final Map<String, String> moodles = new HashMap<>();
    private static final Map<String, String> ui = new HashMap<>();
    private static final Map<String, String> survivalGuide = new HashMap<>();
    private static final Map<String, String> contextMenu = new HashMap<>();
    private static final Map<String, String> farming = new HashMap<>();
    private static final Map<String, String> recipe = new LinkedHashMap<>();
    private static final Map<String, String> recipeGroups = new HashMap<>();
    private static final Map<String, String> igui = new HashMap<>();
    private static final Map<String, String> sandbox = new HashMap<>();
    private static final Map<String, String> tooltip = new HashMap<>();
    private static final Map<String, String> challenge = new HashMap<>();
    private static final Set<String> missing = new HashSet<>();
    private static ArrayList<String> azertyLanguages;
    private static final Map<String, String> stash = new HashMap<>();
    private static final Map<String, String> multiStageBuild = new HashMap<>();
    private static final Map<String, String> moveables = new HashMap<>();
    private static final Map<String, String> makeup = new HashMap<>();
    private static final Map<String, String> gameSound = new HashMap<>();
    private static final Map<String, String> dynamicRadio = new HashMap<>();
    private static final Map<String, String> items = new HashMap<>();
    private static final Map<String, String> itemName = new HashMap<>();
    private static final Map<String, String> itemEvolvedRecipeName = new HashMap<>();
    private static final Map<String, String> recordedMedia = new HashMap<>();
    private static final Map<String, String> recordedMedia_EN = new HashMap<>();
    private static final Map<String, String> survivorNames = new HashMap<>();
    private static final Map<String, String> attributes = new HashMap<>();
    private static final Map<String, String> fluids = new HashMap<>();
    private static final Map<String, String> entity = new HashMap<>();
    private static final Map<String, String> mapLabel = new HashMap<>();
    private static final Map<String, String> printMedia = new HashMap<>();
    private static final Map<String, String> printText = new HashMap<>();
    private static final Map<String, String> radioData = new HashMap<>();
    private static final Map<String, String> bodyParts = new HashMap<>();
    private static final Map<String, String> brReplacements = new HashMap<>();
    public static Language language;

    public static void loadFiles() {
        language = null;
        availableLanguage = null;
        File file = new File(ZomboidFileSystem.instance.getCacheDir() + File.separator + "translationProblems.txt");
        if (debug) {
            try {
                if (debugFile != null) {
                    debugFile.close();
                }

                debugFile = new FileWriter(file);
            } catch (IOException var2) {
                var2.printStackTrace();
            }
        }

        moodles.clear();
        ui.clear();
        survivalGuide.clear();
        items.clear();
        itemName.clear();
        contextMenu.clear();
        farming.clear();
        recipe.clear();
        recipeGroups.clear();
        igui.clear();
        sandbox.clear();
        tooltip.clear();
        challenge.clear();
        missing.clear();
        stash.clear();
        multiStageBuild.clear();
        moveables.clear();
        makeup.clear();
        gameSound.clear();
        dynamicRadio.clear();
        itemEvolvedRecipeName.clear();
        recordedMedia.clear();
        survivorNames.clear();
        attributes.clear();
        fluids.clear();
        printMedia.clear();
        printText.clear();
        radioData.clear();
        bodyParts.clear();
        mapLabel.clear();
        DebugLog.Translation.println("translator: language is " + getLanguage());
        debugErrors = false;
        fillMapFromFile("Tooltip", tooltip);
        fillMapFromFile("IG_UI", igui);
        fillMapFromFile("Recipes", recipe);
        fillMapFromFile("RecipeGroups", recipeGroups);
        fillMapFromFile("Farming", farming);
        fillMapFromFile("ContextMenu", contextMenu);
        fillMapFromFile("SurvivalGuide", survivalGuide);
        fillMapFromFile("UI", ui);
        fillMapFromFile("Items", items);
        fillMapFromFile("ItemName", itemName);
        fillMapFromFile("Moodles", moodles);
        fillMapFromFile("Sandbox", sandbox);
        fillMapFromFile("Challenge", challenge);
        fillMapFromFile("Stash", stash);
        fillMapFromFile("MultiStageBuild", multiStageBuild);
        fillMapFromFile("Moveables", moveables);
        fillMapFromFile("MakeUp", makeup);
        fillMapFromFile("GameSound", gameSound);
        fillMapFromFile("DynamicRadio", dynamicRadio);
        fillMapFromFile("EvolvedRecipeName", itemEvolvedRecipeName);
        fillMapFromFile("Recorded_Media", recordedMedia);
        fillMapFromFile("SurvivorNames", survivorNames);
        fillMapFromFile("Attributes", attributes);
        fillMapFromFile("Fluids", fluids);
        fillMapFromFile("Print_Media", printMedia);
        fillMapFromFile("Print_Text", printText);
        fillMapFromFile("Entity", entity);
        fillMapFromFile("RadioData", radioData);
        fillMapFromFile("BodyParts", bodyParts);
        fillMapFromFile("MapLabel", mapLabel);
        if (debug) {
            if (debugErrors) {
                DebugLog.Translation.trace("translator: errors detected, please see " + file.getAbsolutePath());
            }

            debugItemEvolvedRecipeName.clear();
            debugItem.clear();
            debugMultiStageBuild.clear();
            debugRecipe.clear();
            debugRecipeGroups.clear();
        }

        PerkFactory.initTranslations();
    }

    private static void tryFillMapFromFile(String rootDir, String fileName, Map<String, String> map, Language language) {
        File file = new File(
            rootDir
                + File.separator
                + "media"
                + File.separator
                + "lua"
                + File.separator
                + "shared"
                + File.separator
                + "Translate"
                + File.separator
                + language
                + File.separator
                + fileName
                + "_"
                + language
                + ".txt"
        );
        if (file.exists()) {
            parseFile(file, map, language);
        }
    }

    private static void tryFillMapFromMods(String fileName, Map<String, String> map, Language language) {
        ArrayList<String> modIDs = ZomboidFileSystem.instance.getModIDs();

        for (int n = modIDs.size() - 1; n >= 0; n--) {
            ChooseGameInfo.Mod mod = ChooseGameInfo.getAvailableModDetails(modIDs.get(n));
            if (mod != null) {
                String modDir = mod.getCommonDir();
                if (modDir != null) {
                    tryFillMapFromFile(modDir, fileName, map, language);
                }

                modDir = mod.getVersionDir();
                if (modDir != null) {
                    tryFillMapFromFile(modDir, fileName, map, language);
                }
            }
        }
    }

    public static void addLanguageToList(Language language, ArrayList<Language> languages) {
        if (language != null) {
            if (!languages.contains(language)) {
                languages.add(language);
                if (language.base() != null) {
                    language = Languages.instance.getByName(language.base());
                    addLanguageToList(language, languages);
                }
            }
        }
    }

    private static void fillMapFromFile(String fileName, Map<String, String> map) {
        ArrayList<Language> languages = new ArrayList<>();
        addLanguageToList(getLanguage(), languages);
        addLanguageToList(getDefaultLanguage(), languages);

        for (int i = 0; i < languages.size(); i++) {
            Language language = languages.get(i);
            tryFillMapFromMods(fileName, map, language);
            tryFillMapFromFile(ZomboidFileSystem.instance.base.canonicalFile.getPath(), fileName, map, language);
        }

        languages.clear();
    }

    private static void parseFile(File file, Map<String, String> map, Language language) {
        String line = null;

        try (
            FileInputStream fis = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fis, Charset.forName(language.charset()));
            BufferedReader br = new BufferedReader(isr);
        ) {
            br.readLine();
            boolean inLine = false;
            String key = "";
            String value = "";
            int lineNumber = 1;
            String debugHeader = file.getName().replace("_" + getDefaultLanguage(), "_" + getLanguage());

            while ((line = br.readLine()) != null) {
                lineNumber++;

                try {
                    if (line.contains("=") && line.contains("\"")) {
                        if (line.trim().startsWith("Recipe_")) {
                            key = line.split("=")[0].replaceAll("Recipe_", "").replaceAll("_", " ").trim();
                            value = line.split("=")[1];
                            value = value.substring(value.indexOf("\"") + 1, value.lastIndexOf("\""));
                        } else if (line.trim().startsWith("DisplayName")) {
                            String[] ss = line.split("=");
                            if (line.trim().startsWith("DisplayName_")) {
                                key = ss[0].replaceAll("DisplayName_", "").trim();
                            } else {
                                key = ss[0].replaceAll("DisplayName", "").trim();
                            }

                            if ("Anti_depressants".equals(key)) {
                                key = "Antidepressants";
                            }

                            value = ss[1];
                            value = value.substring(value.indexOf("\"") + 1, value.lastIndexOf("\""));
                        } else if (line.trim().startsWith("EvolvedRecipeName_")) {
                            String[] ssx = line.split("=");
                            key = ssx[0].replaceAll("EvolvedRecipeName_", "").trim();
                            value = ssx[1];
                            int i1 = value.indexOf("\"");
                            int i2 = value.lastIndexOf("\"");
                            value = value.substring(i1 + 1, i2);
                        } else if (line.trim().startsWith("ItemName_")) {
                            String[] ssx = line.split("=");
                            key = ssx[0].replaceAll("ItemName_", "").trim();
                            value = ssx[1];
                            int i1 = value.indexOf("\"");
                            int i2 = value.lastIndexOf("\"");
                            value = value.substring(i1 + 1, i2);
                        } else {
                            key = line.split("=")[0].trim();
                            value = line.substring(line.indexOf("=") + 1);
                            value = value.substring(value.indexOf("\"") + 1, value.lastIndexOf("\""));
                            if (line.contains("..")) {
                                inLine = true;
                            }
                        }
                    } else if (line.contains("--") || line.trim().isEmpty() || !line.trim().endsWith("..") && !inLine) {
                        inLine = false;
                    } else {
                        inLine = true;
                        value = value + line.substring(line.indexOf("\"") + 1, line.lastIndexOf("\""));
                    }

                    if (!inLine || !line.trim().endsWith("..")) {
                        if (!key.isEmpty()) {
                            if (!map.containsKey(key)) {
                                map.put(key, value);
                                if (map == recordedMedia && language == getDefaultLanguage()) {
                                    recordedMedia_EN.put(key, value);
                                }

                                if (debug && language == getDefaultLanguage() && getLanguage() != getDefaultLanguage()) {
                                    if (debugHeader != null) {
                                        debugwrite(debugHeader + "\r\n");
                                        debugHeader = null;
                                    }

                                    debugwrite("\t" + key + " = \"" + value + "\",\r\n");
                                    debugErrors = true;
                                }
                            } else if (debug && language == getDefaultLanguage() && getLanguage() != getDefaultLanguage()) {
                                String translation = map.get(key);
                                if (countSubstitutions(translation) != countSubstitutions(value)) {
                                    debugwrite(
                                        "wrong number of % substitutions in "
                                            + key
                                            + "    "
                                            + getDefaultLanguage()
                                            + "=\""
                                            + value
                                            + "\"    "
                                            + getLanguage()
                                            + "=\""
                                            + translation
                                            + "\"\r\n"
                                    );
                                    debugErrors = true;
                                }
                            }
                        }

                        inLine = false;
                        value = "";
                        key = "";
                    }
                } catch (Exception var18) {
                    if (debug) {
                        if (debugHeader != null) {
                            debugwrite(debugHeader + "\r\n");
                            debugHeader = null;
                        }

                        debugwrite("line " + lineNumber + ": " + key + " = " + value + "\r\n");
                        if (debugFile != null) {
                            var18.printStackTrace(new PrintWriter(debugFile));
                        }

                        debugwrite("\r\n");
                        debugErrors = true;
                    }
                }
            }
        } catch (Exception var22) {
            var22.printStackTrace();
        }
    }

    /**
     * Return the translated text for the selected language
     *  If we don't fnid any translation for the selected language, we return the default text (in English)
     */
    public static String getText(String desc) {
        return getTextInternal(desc, false);
    }

    public static String getTextOrNull(String desc) {
        return getTextInternal(desc, true);
    }

    private static String getTextInternal(String desc, boolean nullOK) {
        if (ui == null) {
            loadFiles();
        }

        String result = null;
        if (desc.startsWith("UI_")) {
            result = ui.get(desc);
        } else if (desc.startsWith("Moodles_")) {
            result = moodles.get(desc);
        } else if (desc.startsWith("SurvivalGuide_")) {
            result = survivalGuide.get(desc);
        } else if (desc.startsWith("Farming_")) {
            result = farming.get(desc);
        } else if (desc.startsWith("IGUI_")) {
            result = igui.get(desc);
        } else if (desc.startsWith("ContextMenu_")) {
            result = contextMenu.get(desc);
        } else if (desc.startsWith("GameSound_")) {
            result = gameSound.get(desc);
        } else if (desc.startsWith("Sandbox_")) {
            result = sandbox.get(desc);
        } else if (desc.startsWith("Tooltip_")) {
            result = tooltip.get(desc);
        } else if (desc.startsWith("Challenge_")) {
            result = challenge.get(desc);
        } else if (desc.startsWith("MakeUp")) {
            result = makeup.get(desc);
        } else if (desc.startsWith("Stash_")) {
            result = stash.get(desc);
        } else if (desc.startsWith("RM_")) {
            result = recordedMedia.get(desc);
        } else if (desc.startsWith("SurvivorName_")) {
            result = survivorNames.get(desc);
        } else if (desc.startsWith("SurvivorSurname_")) {
            result = survivorNames.get(desc);
        } else if (desc.startsWith("Attributes_")) {
            result = attributes.get(desc);
        } else if (desc.startsWith("Fluid_")) {
            result = fluids.get(desc);
        } else if (desc.startsWith("Print_Media_")) {
            result = printMedia.get(desc);
        } else if (desc.startsWith("Print_Text_")) {
            result = printText.get(desc);
        } else if (desc.startsWith("EC_")) {
            result = entity.get(desc);
        } else if (desc.startsWith("RD_")) {
            result = radioData.get(desc);
        } else if (desc.startsWith("BODYPART_")) {
            result = bodyParts.get(desc);
        } else if (desc.startsWith("MapLabel_")) {
            result = mapLabel.get(desc);
        }

        String DBG = Core.debug && DebugOptions.instance.translationPrefix.getValue() ? "*" : null;
        if (result == null) {
            if (nullOK) {
                return null;
            }

            if (!missing.contains(desc)) {
                if (Core.debug) {
                    DebugLog.Translation.error("ERROR: Missing translation \"" + desc + "\"");
                }

                if (debug) {
                    debugwrite("ERROR: Missing translation \"" + desc + "\"\r\n");
                }

                missing.add(desc);
            }

            result = desc;
            DBG = Core.debug && DebugOptions.instance.translationPrefix.getValue() ? "!" : null;
        }

        if (!result.contains("<br>") && !result.contains("<BR>")) {
            return DBG == null ? result : DBG + result;
        } else {
            return brReplacements.computeIfAbsent(result, s -> s.replaceAll("<br>", "\n").replaceAll("<BR>", "\n"));
        }
    }

    private static int countSubstitutions(String s) {
        int count = 0;
        if (s.contains("%1")) {
            count++;
        }

        if (s.contains("%2")) {
            count++;
        }

        if (s.contains("%3")) {
            count++;
        }

        if (s.contains("%4")) {
            count++;
        }

        return count;
    }

    private static String subst(String s, String f, Object arg) {
        if (arg != null) {
            if (arg instanceof Double boxedDouble) {
                double d = boxedDouble;
                s = s.replaceAll(f, d == (long)d ? Long.toString((long)d) : arg.toString());
            } else {
                s = s.replaceAll(f, Matcher.quoteReplacement(arg.toString()));
            }
        }

        return s;
    }

    public static String getText(String desc, Object arg1) {
        String s = getText(desc);
        return subst(s, "%1", arg1);
    }

    public static String getText(String desc, Object arg1, Object arg2) {
        String s = getText(desc);
        s = subst(s, "%1", arg1);
        return subst(s, "%2", arg2);
    }

    public static String getText(String desc, Object arg1, Object arg2, Object arg3) {
        String s = getText(desc);
        s = subst(s, "%1", arg1);
        s = subst(s, "%2", arg2);
        return subst(s, "%3", arg3);
    }

    public static String getText(String desc, Object arg1, Object arg2, Object arg3, Object arg4) {
        String s = getText(desc);
        s = subst(s, "%1", arg1);
        s = subst(s, "%2", arg2);
        s = subst(s, "%3", arg3);
        return subst(s, "%4", arg4);
    }

    public static String getTextOrNull(String desc, Object arg1) {
        String s = getTextOrNull(desc);
        return s == null ? null : subst(s, "%1", arg1);
    }

    public static String getTextOrNull(String desc, Object arg1, Object arg2) {
        String s = getTextOrNull(desc);
        if (s == null) {
            return null;
        } else {
            s = subst(s, "%1", arg1);
            return subst(s, "%2", arg2);
        }
    }

    public static String getTextOrNull(String desc, Object arg1, Object arg2, Object arg3) {
        String s = getTextOrNull(desc);
        if (s == null) {
            return null;
        } else {
            s = subst(s, "%1", arg1);
            s = subst(s, "%2", arg2);
            return subst(s, "%3", arg3);
        }
    }

    public static String getTextOrNull(String desc, Object arg1, Object arg2, Object arg3, Object arg4) {
        String s = getTextOrNull(desc);
        if (s == null) {
            return null;
        } else {
            s = subst(s, "%1", arg1);
            s = subst(s, "%2", arg2);
            s = subst(s, "%3", arg3);
            return subst(s, "%4", arg4);
        }
    }

    private static String getDefaultText(String desc) {
        return changeSomeStuff((String)((KahluaTable)LuaManager.env.rawget(desc.split("_")[0] + "_" + getDefaultLanguage().name())).rawget(desc));
    }

    private static String changeSomeStuff(String txt) {
        return txt;
    }

    public static void setLanguage(Language newlanguage) {
        if (newlanguage == null) {
            newlanguage = getDefaultLanguage();
        }

        language = newlanguage;
    }

    public static void setLanguage(int languageId) {
        Language language = Languages.instance.getByIndex(languageId);
        setLanguage(language);
    }

    public static Language getLanguage() {
        if (language == null) {
            String languageName = Core.getInstance().getOptionLanguageName();
            if (!StringUtils.isNullOrWhitespace(languageName)) {
                language = Languages.instance.getByName(languageName);
            }
        }

        if (language == null) {
            language = Languages.instance.getByName(System.getProperty("user.language").toUpperCase());
        }

        if (language == null) {
            language = getDefaultLanguage();
        }

        return language;
    }

    public static String getCharset() {
        return getLanguage().charset();
    }

    public static ArrayList<Language> getAvailableLanguage() {
        if (availableLanguage == null) {
            availableLanguage = new ArrayList<>();

            for (int i = 0; i < Languages.instance.getNumLanguages(); i++) {
                availableLanguage.add(Languages.instance.getByIndex(i));
            }
        }

        return availableLanguage;
    }

    public static String getDisplayItemName(String trim) {
        String result = null;
        result = items.get(trim.replaceAll(" ", "_").replaceAll("-", "_"));
        return result == null ? trim : result;
    }

    public static String getItemNameFromFullType(String fullType) {
        if (!fullType.contains(".")) {
            throw new IllegalArgumentException("fullType must contain \".\" i.e. module.type");
        } else {
            String name = itemName.get(fullType);
            if (name == null) {
                if (debug && getLanguage() != getDefaultLanguage() && !debugItem.contains(fullType)) {
                    debugItem.add(fullType);
                }

                Item scriptItem = ScriptManager.instance.getItem(fullType);
                if (scriptItem == null) {
                    name = fullType;
                } else {
                    name = scriptItem.getDisplayName();
                }

                itemName.put(fullType, name);
            }

            return name;
        }
    }

    public static void setDefaultItemEvolvedRecipeName(String fullType, String english) {
        if (getLanguage() == getDefaultLanguage()) {
            if (!fullType.contains(".")) {
                throw new IllegalArgumentException("fullType must contain \".\" i.e. module.type");
            } else if (!itemEvolvedRecipeName.containsKey(fullType)) {
                itemEvolvedRecipeName.put(fullType, english);
            }
        }
    }

    public static String getItemEvolvedRecipeName(String fullType) {
        if (!fullType.contains(".")) {
            throw new IllegalArgumentException("fullType must contain \".\" i.e. module.type");
        } else {
            String name = itemEvolvedRecipeName.get(fullType);
            if (name == null) {
                if (debug && getLanguage() != getDefaultLanguage() && !debugItemEvolvedRecipeName.contains(fullType)) {
                    debugItemEvolvedRecipeName.add(fullType);
                }

                Item scriptItem = ScriptManager.instance.getItem(fullType);
                if (scriptItem == null) {
                    name = fullType;
                } else {
                    name = scriptItem.getDisplayName();
                }

                itemEvolvedRecipeName.put(fullType, name);
            }

            return name;
        }
    }

    public static String getMoveableDisplayName(String name) {
        String replaced = name.replaceAll(" ", "_").replaceAll("-", "_").replaceAll("'", "").replaceAll("\\.", "");
        String result = moveables.get(replaced);
        if (result == null) {
            return Core.debug && DebugOptions.instance.translationPrefix.getValue() ? "!" + name : name;
        } else {
            return Core.debug && DebugOptions.instance.translationPrefix.getValue() ? "*" + result : result;
        }
    }

    public static String getMoveableDisplayNameOrNull(String name) {
        String replaced = name.replaceAll(" ", "_").replaceAll("-", "_").replaceAll("'", "").replaceAll("\\.", "");
        String result = moveables.get(replaced);
        if (result == null) {
            return null;
        } else {
            return Core.debug && DebugOptions.instance.translationPrefix.getValue() ? "*" + result : result;
        }
    }

    public static String getMultiStageBuild(String name) {
        String result = multiStageBuild.get("MultiStageBuild_" + name);
        if (result == null) {
            if (debug && getLanguage() != getDefaultLanguage() && !debugMultiStageBuild.contains(name)) {
                debugMultiStageBuild.add(name);
            }

            return name;
        } else {
            return result;
        }
    }

    public static String getRecipeName(String name) {
        String result = null;
        result = recipe.get(name);
        if (result != null && !result.isEmpty()) {
            return result;
        } else {
            if (debug && getLanguage() != getDefaultLanguage() && !debugRecipe.contains(name)) {
                debugRecipe.add(name);
            }

            return name;
        }
    }

    public static String getRecipeGroupName(String name) {
        String result = recipeGroups.get(name);
        if (result != null && !result.isEmpty()) {
            return result;
        } else {
            if (debug && getLanguage() != getDefaultLanguage()) {
                debugRecipeGroups.add(name);
            }

            return name;
        }
    }

    public static Language getDefaultLanguage() {
        return Languages.instance.getDefaultLanguage();
    }

    public static void debugItemEvolvedRecipeNames() {
        if (debug && !debugItemEvolvedRecipeName.isEmpty()) {
            debugwrite("EvolvedRecipeName_" + getLanguage() + ".txt\r\n");
            ArrayList<String> sorted = new ArrayList<>();
            sorted.addAll(debugItemEvolvedRecipeName);
            Collections.sort(sorted);

            for (String name : sorted) {
                debugwrite("\tEvolvedRecipeName_" + name + " = \"" + itemEvolvedRecipeName.get(name) + "\",\r\n");
            }

            debugItemEvolvedRecipeName.clear();
        }
    }

    public static void debugItemNames() {
        if (debug && !debugItem.isEmpty()) {
            debugwrite("ItemName_" + getLanguage() + ".txt\r\n");
            ArrayList<String> sorted = new ArrayList<>();
            sorted.addAll(debugItem);
            Collections.sort(sorted);

            for (String name : sorted) {
                debugwrite("\tItemName_" + name + " = \"" + itemName.get(name) + "\",\r\n");
            }

            debugItem.clear();
        }
    }

    public static void debugMultiStageBuildNames() {
        if (debug && !debugMultiStageBuild.isEmpty()) {
            debugwrite("MultiStageBuild_" + getLanguage() + ".txt\r\n");
            ArrayList<String> sorted = new ArrayList<>();
            sorted.addAll(debugMultiStageBuild);
            Collections.sort(sorted);

            for (String name : sorted) {
                debugwrite("\tMultiStageBuild_" + name + " = \"\",\r\n");
            }

            debugMultiStageBuild.clear();
        }
    }

    public static void debugRecipeNames() {
        if (debug && !debugRecipe.isEmpty()) {
            debugwrite("Recipes_" + getLanguage() + ".txt\r\n");
            ArrayList<String> sorted = new ArrayList<>();
            sorted.addAll(debugRecipe);
            Collections.sort(sorted);

            for (String name : sorted) {
                debugwrite("\tRecipe_" + name.replace(" ", "_") + " = \"\",\r\n");
            }

            debugRecipe.clear();
        }
    }

    public static void debugRecipeGroupNames() {
        if (debug && !debugRecipeGroups.isEmpty()) {
            debugwrite("RecipeGroups_" + getLanguage() + ".txt\r\n");
            ArrayList<String> sorted = new ArrayList<>(debugRecipeGroups);
            Collections.sort(sorted);

            for (String name : sorted) {
                debugwrite("\tRecipeGroup_" + name.replace(" ", "_") + " = \"\",\r\n");
            }

            debugRecipeGroups.clear();
        }
    }

    private static void debugwrite(String s) {
        if (debugFile != null) {
            try {
                debugFile.write(s);
                debugFile.flush();
            } catch (IOException var2) {
            }
        }
    }

    public static ArrayList<String> getAzertyMap() {
        if (azertyLanguages == null) {
            azertyLanguages = new ArrayList<>();
            azertyLanguages.add("FR");
        }

        return azertyLanguages;
    }

    public static String getRadioText(String s) {
        String result = dynamicRadio.get(s);
        return result == null ? s : result;
    }

    public static String getTextMediaEN(String desc) {
        if (ui == null) {
            loadFiles();
        }

        String result = null;
        if (desc.startsWith("RM_")) {
            result = recordedMedia_EN.get(desc);
        }

        String DBG = Core.debug && DebugOptions.instance.translationPrefix.getValue() ? "*" : null;
        if (result == null) {
            if (!missing.contains(desc) && Core.debug) {
                if (Core.debug) {
                    DebugLog.Translation.error("ERROR: Missing translation \"" + desc + "\"");
                }

                if (debug) {
                    debugwrite("ERROR: Missing translation \"" + desc + "\"\r\n");
                }

                missing.add(desc);
            }

            result = desc;
            DBG = Core.debug && DebugOptions.instance.translationPrefix.getValue() ? "!" : null;
        }

        if (result.contains("<br>")) {
            return result.replaceAll("<br>", "\n");
        } else {
            return DBG == null ? result : DBG + result;
        }
    }

    public static String getAttributeText(String s) {
        return getAttributeText(s, false);
    }

    public static String getAttributeTextOrNull(String s) {
        return getAttributeText(s, true);
    }

    private static String getAttributeText(String s, boolean nullOnFail) {
        String result = attributes.get(s);
        if (result == null) {
            if (!missing.contains(s)) {
                DebugLog.Translation.error("ERROR: Missing translation \"" + s + "\"");
                if (debug) {
                    debugwrite("ERROR: Missing translation \"" + s + "\"\r\n");
                }

                missing.add(s);
            }

            return nullOnFail ? null : s;
        } else {
            return result;
        }
    }

    public static String getFluidText(String s) {
        String result = fluids.get(s);
        return result == null ? s : result;
    }

    public static String getEntityText(String s) {
        String result = entity.get(s);
        return result == null ? s : result;
    }

    public static String getMapLabelText(String s) {
        String result = mapLabel.get(s);
        return result == null ? s : result;
    }

    public static Map<String, String> getUI() {
        return ui;
    }

    private static final class News {
        String version;
        final ArrayList<String> sectionNames = new ArrayList<>();
        final HashMap<String, ArrayList<String>> sectionLists = new HashMap<>();

        News(String version) {
            this.version = version;
        }

        ArrayList<String> getOrCreateSectionList(String sectionName) {
            if (this.sectionNames.contains(sectionName)) {
                return this.sectionLists.get(sectionName);
            } else {
                this.sectionNames.add(sectionName);
                ArrayList<String> list = new ArrayList<>();
                this.sectionLists.put(sectionName, list);
                return list;
            }
        }

        String toRichText() {
            StringBuilder txt = new StringBuilder();

            for (String sectionName : this.sectionNames) {
                ArrayList<String> lines = this.sectionLists.get(sectionName);
                if (!lines.isEmpty()) {
                    txt.append("<LINE> <LEFT> <SIZE:medium> %s <LINE> <LINE> ".formatted(sectionName));

                    for (String line : lines) {
                        txt.append(line);
                    }
                }
            }

            return txt.toString();
        }
    }
}
