// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core;

import com.google.common.collect.Maps;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IllegalFormatException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.MissingFormatArgumentException;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.json.JSONObject;
import org.json.JSONParserConfiguration;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.characters.skills.PerkFactory;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.gameStates.ChooseGameInfo;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;
import zombie.util.StringUtils;

@UsedFromLua
public final class Translator {
    public static final char[] LOOKALIKE_CHARS = new char[]{
        '\u2019',
        '\u2018',
        '\u02bc',
        '\u02b9',
        '\u2032',
        '\u201c',
        '\u201d',
        '\u201f',
        '\u2033',
        '\u2013',
        '\u2014',
        '\u2212',
        '\u2010',
        '\u2011',
        '\u0430',
        '\u0435',
        '\u043e',
        '\u0440',
        '\u0441',
        '\u0443',
        '\u0445',
        '\u0456',
        '\u0458',
        '\u0455',
        '\u051d',
        '\u03f3',
        '\u2170',
        '\u2174',
        '\u2179',
        '\u217c',
        '\u217d',
        '\u217e',
        '\u217f',
        '\u200b',
        '\u200c',
        '\u200d',
        '\ufeff',
        '\u200e',
        '\u200f',
        '\u202a',
        '\u202b',
        '\u202c',
        '\u202d',
        '\u202e'
    };
    private static List<Language> availableLanguage;
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
    private static final Map<String, String> credits = new HashMap<>();
    private static final Map<String, String> tempMap = new HashMap<>();
    private static final Pattern MAYBE_PLACEHOLDER = Pattern.compile("%(.?)", 32);
    public static final Map<String, Map<String, String>> BY_NAME = new LinkedHashMap<String, Map<String, String>>() {
        {
            this.put("Tooltip", Translator.tooltip);
            this.put("IG_UI", Translator.igui);
            this.put("Recipes", Translator.recipe);
            this.put("RecipeGroups", Translator.recipeGroups);
            this.put("Farming", Translator.farming);
            this.put("ContextMenu", Translator.contextMenu);
            this.put("SurvivalGuide", Translator.survivalGuide);
            this.put("UI", Translator.ui);
            this.put("Items", Translator.items);
            this.put("ItemName", Translator.itemName);
            this.put("Moodles", Translator.moodles);
            this.put("Sandbox", Translator.sandbox);
            this.put("Challenge", Translator.challenge);
            this.put("Stash", Translator.stash);
            this.put("Moveables", Translator.moveables);
            this.put("MakeUp", Translator.makeup);
            this.put("GameSound", Translator.gameSound);
            this.put("DynamicRadio", Translator.dynamicRadio);
            this.put("EvolvedRecipeName", Translator.itemEvolvedRecipeName);
            this.put("Recorded_Media", Translator.recordedMedia);
            this.put("SurvivorNames", Translator.survivorNames);
            this.put("Attributes", Translator.attributes);
            this.put("Fluids", Translator.fluids);
            this.put("Print_Media", Translator.printMedia);
            this.put("Print_Text", Translator.printText);
            this.put("Entity", Translator.entity);
            this.put("RadioData", Translator.radioData);
            this.put("BodyParts", Translator.bodyParts);
            this.put("MapLabel", Translator.mapLabel);
            this.put("Credits", Translator.credits);
        }
    };
    public static Language language;
    private static final Pattern FORMAT_TOKEN = Pattern.compile("%%|%([1-9])");

    public static void loadFiles() {
        Translator.language = null;
        availableLanguage = null;
        File file = new File(ZomboidFileSystem.instance.getCacheDir() + File.separator + "translationProblems.txt");
        if (debug) {
            try {
                if (debugFile != null) {
                    debugFile.close();
                }

                debugFile = new FileWriter(file);
            } catch (IOException var7) {
                DebugType.General.printException(var7, LogSeverity.Error);
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
        credits.clear();
        DebugType.Translation.println("translator: language is " + getLanguage());
        debugErrors = false;
        BY_NAME.forEach((name, mapx) -> forLanguageStack(l -> {
            tryFillMapFromFile(ZomboidFileSystem.instance.base.canonicalFile.getPath(), name, mapx, l, Translator::formatFixer);
            tryFillMapFromMods(name, mapx, l);
        }));
        tryFillMapFromFile(
            ZomboidFileSystem.instance.base.canonicalFile.getPath(), "Recorded_Media", recordedMedia_EN, getDefaultLanguage(), Translator::formatFixer
        );
        tryFillMapFromMods("Recorded_Media", recordedMedia_EN, getDefaultLanguage());
        if (debug) {
            if (debugErrors) {
                DebugType.Translation.trace("translator: errors detected, please see " + file.getAbsolutePath());
            }

            debugItemEvolvedRecipeName.clear();
            debugItem.clear();
            debugMultiStageBuild.clear();
            debugRecipe.clear();
            debugRecipeGroups.clear();
        }

        PerkFactory.initTranslations();
        if (Core.IS_DEV) {
            int[] problemCounter = new int[]{0};
            Language sourceLanguage = Languages.instance.getDefaultLanguage();
            Map<String, Map<String, String>> source = Maps.newHashMap();

            for (String type : BY_NAME.keySet()) {
                Map<String, String> map = source.computeIfAbsent(type, k -> new HashMap<>());
                tryFillMapFromFile(ZomboidFileSystem.instance.base.canonicalFile.getPath(), type, map, sourceLanguage, Function.identity());
                map.forEach(
                    (key, value) -> {
                        for (String translatedPlaceholder : extractPlaceholders(value)) {
                            if (!isValidPlaceholder(translatedPlaceholder)) {
                                System.out
                                    .println(
                                        "workdir/media/lua/Translate/%s/%s.json key \"%s\" has malformed \"%s\" in \"%s\""
                                            .formatted(language.name(), type, key, translatedPlaceholder, value)
                                    );
                                problemCounter[0]++;
                            }
                        }
                    }
                );
            }

            if (problemCounter[0] > 0) {
                System.out.println("Found %d problems with our english translation source files".formatted(problemCounter[0]));
                System.exit(1);
            }

            for (Language language : Languages.instance.getLanguages()) {
                if (language != sourceLanguage) {
                    source.forEach(
                        (type, mapForType) -> {
                            Map<String, String> mapx = new HashMap<>();
                            tryFillMapFromFile(ZomboidFileSystem.instance.base.canonicalFile.getPath(), type, mapx, language, Function.identity());
                            mapForType.forEach(
                                (key, englishValue) -> {
                                    String translatedValue = mapx.get(key);
                                    if (!StringUtils.isNullOrWhitespace(translatedValue)) {
                                        Set<String> translatedPlaceholders = extractPlaceholders(translatedValue);

                                        for (String translatedPlaceholder : translatedPlaceholders) {
                                            if (!isValidPlaceholder(translatedPlaceholder)) {
                                                System.out
                                                    .println(
                                                        "workdir/media/lua/Translate/%s/%s.json key \"%s\" has malformed \"%s\" in \"%s\""
                                                            .formatted(language.name(), type, key, translatedPlaceholder, translatedValue)
                                                    );
                                                problemCounter[0]++;
                                            }
                                        }

                                        Set<String> expected = extractPlaceholders(englishValue);
                                        translatedPlaceholders.removeIf(expected::contains);
                                        translatedPlaceholders.removeIf(Predicate.not(Translator::isValidPlaceholder));
                                        translatedPlaceholders.removeIf("%%"::equals);
                                        if (!translatedPlaceholders.isEmpty()) {
                                            System.out
                                                .println(
                                                    "workdir/media/lua/Translate/%s/%s.json key \"%s\" has unknown %s in \"%s\" not found in source \"%s\" "
                                                        .formatted(language.name(), type, key, translatedPlaceholders, translatedValue, englishValue)
                                                );
                                            problemCounter[0]++;
                                        }
                                    }
                                }
                            );
                        }
                    );
                }
            }

            if (problemCounter[0] > 0) {
                System.out.println("Found %d problems with translations".formatted(problemCounter[0]));
                System.exit(1);
            }
        }
    }

    private static boolean isValidPlaceholder(String s) {
        if (s != null && s.length() == 2 && s.charAt(0) == '%') {
            char c = s.charAt(1);
            return c == '%' || c >= '1' && c <= '9';
        } else {
            return false;
        }
    }

    private static Set<String> extractPlaceholders(String text) {
        Set<String> result = new HashSet<>();
        Matcher matcher = MAYBE_PLACEHOLDER.matcher(text);

        while (matcher.find()) {
            result.add(matcher.group());
        }

        return result;
    }

    public static void forLanguageStack(Consumer<Language> consumer) {
        Set<Language> bases = new LinkedHashSet<>();
        Language language = getLanguage();

        while (language != null && bases.add(language)) {
            language = Languages.instance.getByName(language.base());
        }

        bases.add(getDefaultLanguage());
        Language[] languages = bases.toArray(new Language[0]);

        for (int i = languages.length - 1; i >= 0; i--) {
            consumer.accept(languages[i]);
        }
    }

    private static void tryFillMapFromFile(String rootDir, String fileName, Map<String, String> map, Language language, Function<String, String> formatFixer) {
        File file = new File("%s/media/lua/shared/Translate/%s/%s.json".formatted(rootDir, language, fileName));
        if (file.exists()) {
            try {
                String content = Files.readString(file.toPath());
                if (Core.IS_DEV && "EN".equals(language.name()) && !"Mod".equals(fileName)) {
                    cryAboutUnicodeConfusables(content, file);
                }

                new JSONObject(content, new JSONParserConfiguration().withStrictMode(Core.IS_DEV)).toMap().forEach((k, v) -> {
                    if (!map.containsKey(k) || !StringUtils.isNullOrEmpty(v.toString())) {
                        map.put(k, formatFixer.apply((String)v));
                    }
                });
            } catch (Exception var7) {
                throw new RuntimeException("JSON Error in: %s".formatted(file), var7);
            }
        }
    }

    private static void tryFillMapFromMods(String fileName, Map<String, String> map, Language language) {
        ArrayList<String> modIDs = ZomboidFileSystem.instance.getModIDs();

        for (int n = 0; n < modIDs.size(); n++) {
            ChooseGameInfo.Mod mod = ChooseGameInfo.getAvailableModDetails(modIDs.get(n));
            if (mod != null) {
                String modDir = mod.getCommonDir();
                if (modDir != null) {
                    tryFillMapFromFile(modDir, fileName, map, language, Translator::formatFixer);
                }

                modDir = mod.getVersionDir();
                if (modDir != null) {
                    tryFillMapFromFile(modDir, fileName, map, language, Translator::formatFixer);
                }
            }
        }
    }

    public static void readMapTranslation(ChooseGameInfo.Map map, String dir) {
        tempMap.clear();
        forLanguageStack(lang -> {
            tryFillMapFromFile(ZomboidFileSystem.instance.base.canonicalFile.getPath(), dir, tempMap, lang, Translator::formatFixer);
            tryFillMapFromMods(dir, tempMap, lang);
        });
        map.setTitle(tempMap.getOrDefault("title", map.getTitle()));
        map.setDescription(tempMap.getOrDefault("description", map.getDescription()));
    }

    public static void readModTranslation(ChooseGameInfo.Mod mod) {
        tempMap.clear();
        forLanguageStack(lang -> {
            String modDir = mod.getCommonDir();
            if (modDir != null) {
                tryFillMapFromFile(modDir, "Mod", tempMap, lang, Translator::formatFixer);
            }

            modDir = mod.getVersionDir();
            if (modDir != null) {
                tryFillMapFromFile(modDir, "Mod", tempMap, lang, Translator::formatFixer);
            }
        });
        mod.setName(tempMap.getOrDefault("name", mod.getName()));
        mod.setDescription(tempMap.getOrDefault("description", mod.getDescription()));
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
        } else if (desc.startsWith("credits_")) {
            result = credits.get(desc);
        } else if (desc.startsWith("AEBS_")) {
            result = dynamicRadio.get(desc);
        }

        String dbg = Core.debug && DebugOptions.instance.translationPrefix.getValue() ? "*" : null;
        if (result == null) {
            if (nullOK) {
                return null;
            }

            if (!missing.contains(desc)) {
                if (Core.debug) {
                    DebugType.Translation.error("ERROR: Missing translation \"" + desc + "\"");
                }

                if (debug) {
                    debugwrite("ERROR: Missing translation \"" + desc + "\"\r\n");
                }

                missing.add(desc);
            }

            result = desc;
            dbg = Core.debug && DebugOptions.instance.translationPrefix.getValue() ? "!" : null;
        }

        if (!result.contains("<br>") && !result.contains("<BR>")) {
            return dbg == null ? result : dbg + result;
        } else {
            return brReplacements.computeIfAbsent(result, s -> s.replace("<br>", "\n").replace("<BR>", "\n"));
        }
    }

    public static String getText(String desc, Object... args) {
        return reportMissingArgumentsFromPastAbuse(desc, getTextInternal(desc, false), args);
    }

    private static String reportMissingArgumentsFromPastAbuse(String desc, String text, Object[] args) {
        try {
            return text.formatted(fixupArgs(args));
        } catch (MissingFormatArgumentException var6) {
            DebugType.General.printException(var6, LogSeverity.Warning, "ERROR: Missing arguments for \"" + desc + "\"");
            if (Core.IS_DEV) {
                DebugType.General.printStackTrace(LogSeverity.Warning, -1, null);
            }

            return text;
        } catch (IllegalFormatException var7) {
            DebugType.General.printException(var7, LogSeverity.Warning, "ERROR: Formatting \"" + desc + "\"");
            if (Core.IS_DEV) {
                DebugType.General.printStackTrace(LogSeverity.Warning, -1, null);
                throw var7;
            } else {
                try {
                    return text.replaceAll("%(?=[^ds.]|$)", "%%").replaceAll("%%(\\d+)", "%$1\\$s").formatted(args);
                } catch (IllegalFormatException var5) {
                    return text;
                }
            }
        }
    }

    public static String getTextOrNull(String desc, Object... args) {
        String text = getTextInternal(desc, true);
        return text == null ? null : reportMissingArgumentsFromPastAbuse(desc, text, args);
    }

    private static Object[] fixupArgs(Object[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                args[i] = "";
            } else if (args[i] instanceof Double boxedDouble) {
                double d = boxedDouble;
                args[i] = d == (long)d ? Long.toString((long)d) : args[i].toString();
            }
        }

        return args;
    }

    public static void setLanguage(Language newlanguage) {
        if (newlanguage == null) {
            newlanguage = getDefaultLanguage();
        }

        language = newlanguage;
    }

    public static void setLanguage(int languageId) {
        setLanguage(Languages.instance.getLanguages().get(languageId));
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

    public static List<Language> getAvailableLanguage() {
        if (availableLanguage == null) {
            availableLanguage = Languages.instance.getLanguages();
        }

        return availableLanguage;
    }

    public static String getDisplayItemName(String trim) {
        String result = items.get(trim.replace(" ", "_").replace("-", "_"));
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
        String replaced = name.replace(" ", "_").replace("-", "_").replace("'", "").replace("\\.", "");
        String result = moveables.get(replaced);
        if (result == null) {
            return Core.debug && DebugOptions.instance.translationPrefix.getValue() ? "!" + name : name;
        } else {
            return Core.debug && DebugOptions.instance.translationPrefix.getValue() ? "*" + result : result;
        }
    }

    public static String getMoveableDisplayNameOrNull(String name) {
        String replaced = name.replace(" ", "_").replace("-", "_").replace("'", "").replace("\\.", "");
        String result = moveables.get(replaced);
        if (result == null) {
            return null;
        } else {
            return Core.debug && DebugOptions.instance.translationPrefix.getValue() ? "*" + result : result;
        }
    }

    public static String getRecipeName(String name) {
        String result = recipe.get(name);
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
            ArrayList<String> sorted = new ArrayList<>(debugItemEvolvedRecipeName);
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
            ArrayList<String> sorted = new ArrayList<>(debugItem);
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
            ArrayList<String> sorted = new ArrayList<>(debugMultiStageBuild);
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
            ArrayList<String> sorted = new ArrayList<>(debugRecipe);
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

    public static String getTextMediaEN(String desc) {
        if (ui == null) {
            loadFiles();
        }

        String result = null;
        if (desc.startsWith("RM_")) {
            result = recordedMedia_EN.get(desc);
        }

        String dbg = Core.debug && DebugOptions.instance.translationPrefix.getValue() ? "*" : null;
        if (result == null) {
            if (!missing.contains(desc) && Core.debug) {
                if (Core.debug) {
                    DebugType.Translation.error("ERROR: Missing translation \"" + desc + "\"");
                }

                if (debug) {
                    debugwrite("ERROR: Missing translation \"" + desc + "\"\r\n");
                }

                missing.add(desc);
            }

            result = desc;
            dbg = Core.debug && DebugOptions.instance.translationPrefix.getValue() ? "!" : null;
        }

        if (result.contains("<br>")) {
            return result.replace("<br>", "\n");
        } else {
            return dbg == null ? result : dbg + result;
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
                DebugType.Translation.error("ERROR: Missing translation \"" + s + "\"");
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

    private static void cryAboutUnicodeConfusables(String content, File file) {
        Set<Character> found = new LinkedHashSet<>();

        for (char c : LOOKALIKE_CHARS) {
            if (content.contains(String.valueOf(c))) {
                found.add(c);
            }
        }

        if (!found.isEmpty()) {
            Object[] var10003 = new Object[2];
            Stream var10006 = found.stream().map(String::valueOf);
            String var7 = "'%s'";
            var10003[0] = var10006.<CharSequence>map(xva$0 -> "'%s'".formatted(xva$0)).collect(Collectors.joining(","));
            var10003[1] = file;
            throw new IllegalStateException("Found look-a-like unicode char: %s in EN translation: %s".formatted(var10003));
        }
    }

    private static String formatFixer(String input) {
        return FORMAT_TOKEN.matcher(input).replaceAll(match -> match.group(1) == null ? "%%" : "%" + match.group(1) + "\\$s");
    }
}
