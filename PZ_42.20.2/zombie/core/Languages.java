// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core;

import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.DirectoryStream.Filter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import zombie.ZomboidFileSystem;
import zombie.core.logger.ExceptionLogger;
import zombie.gameStates.ChooseGameInfo;
import zombie.util.Lambda;
import zombie.util.list.PZArrayUtil;

public final class Languages {
    public static final Languages instance = new Languages();
    private final ArrayList<Language> languages = new ArrayList<>();
    private Language defaultLanguage = new Language("EN", "English", null, false);

    public Languages() {
        this.languages.add(this.defaultLanguage);
    }

    public void init() {
        this.languages.clear();
        this.defaultLanguage = new Language("EN", "English", null, false);
        this.languages.add(this.defaultLanguage);
        this.loadTranslateDirectory(ZomboidFileSystem.instance.getMediaPath("lua/shared/Translate"));

        for (String modId : ZomboidFileSystem.instance.getModIDs()) {
            ChooseGameInfo.Mod mod = ChooseGameInfo.getAvailableModDetails(modId);
            if (mod != null) {
                File file = new File(mod.getCommonDir(), "media/lua/shared/Translate");
                if (file.isDirectory()) {
                    this.loadTranslateDirectory(file.getAbsolutePath());
                }

                file = new File(mod.getVersionDir(), "media/lua/shared/Translate");
                if (file.isDirectory()) {
                    this.loadTranslateDirectory(file.getAbsolutePath());
                }
            }
        }
    }

    public Language getDefaultLanguage() {
        return this.defaultLanguage;
    }

    public List<Language> getLanguages() {
        return Collections.unmodifiableList(this.languages);
    }

    public Language getByIndex(int index) {
        return index >= 0 && index < this.languages.size() ? this.languages.get(index) : null;
    }

    public Language getByName(String name) {
        return PZArrayUtil.find(this.languages, Lambda.predicate(name, (language, lName) -> language.name().equalsIgnoreCase(lName)));
    }

    public int getIndexByName(String name) {
        return PZArrayUtil.indexOf(this.languages, Lambda.predicate(name, (language, lName) -> language.name().equalsIgnoreCase(lName)));
    }

    private void loadTranslateDirectory(String dirName) {
        Filter<Path> filter = entry -> Files.isDirectory(entry) && Files.exists(entry.resolve("language.json"));
        Path dir = FileSystems.getDefault().getPath(dirName);
        if (Files.exists(dir)) {
            try (DirectoryStream<Path> dstrm = Files.newDirectoryStream(dir, filter)) {
                for (Path path : dstrm) {
                    LanguageFile data = this.loadLanguageDirectory(path.toAbsolutePath());
                    if (data != null) {
                        Language lang = new Language(data.name, data.language_name, data.base, data.azerty);
                        int index = this.getIndexByName(data.name);
                        if (index == -1) {
                            this.languages.add(lang);
                        } else {
                            this.languages.set(index, lang);
                            if (data.name.equals(this.defaultLanguage.name())) {
                                this.defaultLanguage = lang;
                            }
                        }
                    }
                }
            } catch (Exception var12) {
                ExceptionLogger.logException(var12);
            }
        }
    }

    private LanguageFile loadLanguageDirectory(Path dir) {
        String name = dir.getFileName().toString();

        try {
            return new LanguageFile(name, dir.resolve("language.json"));
        } catch (Exception var4) {
            ExceptionLogger.logException(var4);
            return null;
        }
    }
}
