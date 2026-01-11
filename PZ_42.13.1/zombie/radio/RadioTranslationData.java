// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.radio;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import zombie.core.Language;
import zombie.core.Languages;
import zombie.core.Translator;
import zombie.util.StringUtils;

/**
 * Turbo
 */
public final class RadioTranslationData {
    private final String filePath;
    private String guid;
    private String language;
    private Language languageEnum;
    private int version = -1;
    private final ArrayList<String> translators = new ArrayList<>();
    private final Map<String, String> translations = new HashMap<>();

    public RadioTranslationData(String file) {
        this.filePath = file;
    }

    public String getFilePath() {
        return this.filePath;
    }

    public String getGuid() {
        return this.guid;
    }

    public String getLanguage() {
        return this.language;
    }

    public Language getLanguageEnum() {
        return this.languageEnum;
    }

    public int getVersion() {
        return this.version;
    }

    public int getTranslationCount() {
        return this.translations.size();
    }

    public ArrayList<String> getTranslators() {
        return this.translators;
    }

    public boolean validate() {
        return this.guid != null && this.language != null && this.version >= 0;
    }

    public boolean loadTranslations() {
        boolean valid = false;
        if (Translator.getLanguage() != this.languageEnum) {
            System.out.println("Radio translations trying to load language that is not the current language...");
            return false;
        } else {
            try {
                File f = new File(this.filePath);
                if (f.exists() && !f.isDirectory()) {
                    BufferedReader br = new BufferedReader(
                        new InputStreamReader(new FileInputStream(this.filePath), Charset.forName(this.languageEnum.charset()))
                    );
                    String line = null;
                    boolean open = false;
                    ArrayList<String> memberKeys = new ArrayList<>();

                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (line.equals("[Translations]")) {
                            open = true;
                        } else if (open) {
                            if (!line.equals("[Collection]")) {
                                if (line.equals("[/Translations]")) {
                                    valid = true;
                                    break;
                                }

                                String[] parts = line.split("=", 2);
                                if (parts.length == 2) {
                                    String key = parts[0].trim();
                                    String val = parts[1].trim();
                                    this.translations.put(key, val);
                                }
                            } else {
                                String text = null;

                                while ((line = br.readLine()) != null) {
                                    line = line.trim();
                                    if (line.equals("[/Collection]")) {
                                        break;
                                    }

                                    String[] parts = line.split("=", 2);
                                    if (parts.length == 2) {
                                        String key = parts[0].trim();
                                        String val = parts[1].trim();
                                        if (key.equals("text")) {
                                            text = val;
                                        } else if (key.equals("member")) {
                                            memberKeys.add(val);
                                        }
                                    }
                                }

                                if (text != null && !memberKeys.isEmpty()) {
                                    for (String guid : memberKeys) {
                                        this.translations.put(guid, text);
                                    }
                                }

                                memberKeys.clear();
                            }
                        }
                    }
                }
            } catch (Exception var11) {
                var11.printStackTrace();
                valid = false;
            }

            return valid;
        }
    }

    public String getTranslation(String guid) {
        return this.translations.containsKey(guid) ? this.translations.get(guid) : null;
    }

    public static RadioTranslationData ReadFile(String file) {
        RadioTranslationData transData = new RadioTranslationData(file);
        File f = new File(file);
        if (f.exists() && !f.isDirectory()) {
            Language language = parseLanguageFromFilename(f.getName());
            String charsetName = language == null ? null : language.charset();

            try (
                FileInputStream fis = new FileInputStream(file);
                InputStreamReader isr = new InputStreamReader(fis, charsetName);
                BufferedReader br = new BufferedReader(isr);
            ) {
                String line = null;

                while ((line = br.readLine()) != null) {
                    String[] parts = line.split("=");
                    if (parts.length > 1) {
                        String id = parts[0].trim();
                        String txt = "";

                        for (int i = 1; i < parts.length; i++) {
                            txt = txt + parts[i];
                        }

                        txt = txt.trim();
                        if (id.equals("guid")) {
                            transData.guid = txt;
                        } else if (id.equals("language")) {
                            transData.language = txt;
                        } else if (id.equals("version")) {
                            transData.version = Integer.parseInt(txt);
                        } else if (id.equals("translator")) {
                            String[] nms = txt.split(",");
                            if (nms.length > 0) {
                                for (String name : nms) {
                                    transData.translators.add(name);
                                }
                            }
                        }
                    }

                    line = line.trim();
                    if (line.equals("[/Info]")) {
                        break;
                    }
                }
            } catch (Exception var23) {
                var23.printStackTrace();
            }
        }

        boolean found = false;
        if (transData.language != null) {
            for (Language lang : Translator.getAvailableLanguage()) {
                if (lang.toString().equals(transData.language)) {
                    transData.languageEnum = lang;
                    found = true;
                    break;
                }
            }
        }

        if (!found && transData.language != null) {
            System.out.println("Language " + transData.language + " not found");
            return null;
        } else {
            return transData.guid != null && transData.language != null && transData.version >= 0 ? transData : null;
        }
    }

    private static Language parseLanguageFromFilename(String fileName) {
        if (!StringUtils.startsWithIgnoreCase(fileName, "RadioData_")) {
            return null;
        } else {
            String language = fileName.replaceFirst("RadioData_", "").replace(".txt", "");
            return Languages.instance.getByName(language);
        }
    }
}
