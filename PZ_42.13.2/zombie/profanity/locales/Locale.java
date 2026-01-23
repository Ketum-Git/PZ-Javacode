// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.profanity.locales;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import zombie.ZomboidFileSystem;
import zombie.profanity.Phonizer;
import zombie.profanity.ProfanityFilter;

public abstract class Locale {
    protected String id;
    protected int storeVowelsAmount = 3;
    protected String phoneticRules = "";
    protected Map<String, Phonizer> phonizers = new HashMap<>();
    protected Map<String, String> filterWords = new HashMap<>();
    protected List<String> filterWordsRaw = new ArrayList<>();
    protected List<String> filterContains = new ArrayList<>();
    protected ArrayList<String> whitelistWords = new ArrayList<>();
    protected Pattern pattern;
    private final Pattern preProcessLeet = Pattern.compile("(?<leet>[\\$@34701])\\k<leet>*|(?<nonWord>[^A-Z\\s\\$@34701]+)");
    private final Pattern preProcessDoubles = Pattern.compile("(?<doublechar>[A-Z])\\k<doublechar>+");
    private final Pattern preProcessVowels = Pattern.compile("(?<vowel>[AOUIE])");

    protected Locale(String id) {
        this.id = id;
        this.Init();
        this.finalizeData();
        this.loadFilterWords();
        this.loadFilterContains();
        this.loadWhiteListWords();
        ProfanityFilter.printDebug("Done init locale: " + this.id);
    }

    public String getID() {
        return this.id;
    }

    public String getPhoneticRules() {
        return this.phoneticRules;
    }

    public int getFilterWordsCount() {
        return this.filterWords.size();
    }

    protected abstract void Init();

    public void addWhiteListWord(String word) {
        word = word.toUpperCase().trim();
        if (!this.whitelistWords.contains(word)) {
            this.whitelistWords.add(word);
        }
    }

    public void removeWhiteListWord(String word) {
        word = word.toUpperCase().trim();
        if (this.whitelistWords.contains(word)) {
            this.whitelistWords.remove(word);
        }
    }

    public boolean isWhiteListedWord(String str) {
        return this.whitelistWords.contains(str.toUpperCase().trim());
    }

    public void addFilterWord(String word) {
        String phon = this.phonizeWord(word);
        if (phon.length() > 2) {
            String set = "";
            if (this.filterWords.containsKey(phon)) {
                set = set + this.filterWords.get(phon) + ",";
            }

            ProfanityFilter.printDebug("Adding word: " + word + ", Phonized: " + phon);
            this.filterWords.put(phon, set + word.toLowerCase());
        } else {
            ProfanityFilter.printDebug("Refusing word: " + word + ", Phonized: " + phon + ", null or phonized < 2 characters");
        }
    }

    public void removeFilterWord(String word) {
        String phon = this.phonizeWord(word);
        if (this.filterWords.containsKey(phon)) {
            this.filterWords.remove(phon);
        }
    }

    public void addFilterContains(String str) {
        if (str != null && !str.isEmpty() && !this.filterContains.contains(str.toUpperCase())) {
            this.filterContains.add(str.toUpperCase());
        }
    }

    public void removeFilterContains(String str) {
        this.filterContains.remove(str.toUpperCase());
    }

    public void addFilterRawWord(String word) {
        if (word != null && !word.isEmpty() && !this.filterWordsRaw.contains(word.toUpperCase())) {
            this.filterWordsRaw.add(word.toUpperCase());
        }
    }

    public void removeFilterWordRaw(String word) {
        this.filterWordsRaw.remove(word.toUpperCase());
    }

    protected String repeatString(int n, char c) {
        char[] chars = new char[n];
        Arrays.fill(chars, c);
        return new String(chars);
    }

    protected boolean containsIgnoreCase(String str, String searchStr) {
        if (str != null && searchStr != null) {
            int length = searchStr.length();
            if (length == 0) {
                return true;
            } else {
                for (int i = str.length() - length; i >= 0; i--) {
                    if (str.regionMatches(true, i, searchStr, 0, length)) {
                        return true;
                    }
                }

                return false;
            }
        } else {
            return false;
        }
    }

    public String filterWord(String str) {
        return this.filterWord(str, false);
    }

    public String filterWord(String str, boolean includeContaining) {
        if (this.isWhiteListedWord(str)) {
            return str;
        } else {
            String test = this.phonizeWord(str);
            if (this.filterWords.containsKey(test)) {
                return new String(new char[str.length()]).replace('\u0000', '*');
            } else {
                if (!this.filterWordsRaw.isEmpty()) {
                    for (int i = 0; i < this.filterWordsRaw.size(); i++) {
                        if (str.equalsIgnoreCase(this.filterWordsRaw.get(i))) {
                            return new String(new char[str.length()]).replace('\u0000', '*');
                        }
                    }
                }

                if (includeContaining) {
                    for (int ix = 0; ix < this.filterContains.size(); ix++) {
                        String filterWord = this.filterContains.get(ix);
                        if (this.containsIgnoreCase(str, filterWord)) {
                            str = str.replaceAll("(?i)" + Pattern.quote(filterWord), this.repeatString(filterWord.length(), '*'));
                        }
                    }
                }

                return str;
            }
        }
    }

    public String validateWord(String str, boolean includeContaining) {
        if (this.isWhiteListedWord(str)) {
            return null;
        } else {
            String test = this.phonizeWord(str);
            if (this.filterWords.containsKey(test)) {
                return str;
            } else {
                if (!this.filterWordsRaw.isEmpty()) {
                    for (int i = 0; i < this.filterWordsRaw.size(); i++) {
                        if (str.equalsIgnoreCase(this.filterWordsRaw.get(i))) {
                            return str;
                        }
                    }
                }

                if (includeContaining) {
                    for (int ix = 0; ix < this.filterContains.size(); ix++) {
                        String filter = this.filterContains.get(ix);
                        if (this.containsIgnoreCase(str, filter)) {
                            return filter.toLowerCase();
                        }
                    }
                }

                return null;
            }
        }
    }

    public String returnMatchSetForWord(String str) {
        String test = this.phonizeWord(str);
        return this.filterWords.containsKey(test) ? this.filterWords.get(test) : null;
    }

    public String returnPhonizedWord(String str) {
        return this.phonizeWord(str);
    }

    protected String phonizeWord(String word) {
        word = word.toUpperCase().trim();
        if (this.whitelistWords.contains(word)) {
            return word;
        } else {
            word = this.preProcessWord(word);
            if (this.phonizers.size() <= 0) {
                return word;
            } else {
                Matcher m = this.pattern.matcher(word);
                StringBuffer s = new StringBuffer();

                while (m.find()) {
                    for (Entry<String, Phonizer> entry : this.phonizers.entrySet()) {
                        if (m.group(entry.getKey()) != null) {
                            entry.getValue().execute(m, s);
                            break;
                        }
                    }
                }

                m.appendTail(s);
                return s.toString();
            }
        }
    }

    private String preProcessWord(String word) {
        Matcher m = this.preProcessLeet.matcher(word);
        StringBuffer s = new StringBuffer();

        while (m.find()) {
            if (m.group("leet") != null) {
                String doneVowels = m.group("leet");
                switch (doneVowels) {
                    case "$":
                        m.appendReplacement(s, "S");
                        break;
                    case "4":
                    case "@":
                        m.appendReplacement(s, "A");
                        break;
                    case "3":
                        m.appendReplacement(s, "E");
                        break;
                    case "7":
                        m.appendReplacement(s, "T");
                        break;
                    case "0":
                        m.appendReplacement(s, "O");
                        break;
                    case "1":
                        m.appendReplacement(s, "I");
                }
            } else if (m.group("nonWord") != null) {
                m.appendReplacement(s, "");
            }
        }

        m.appendTail(s);
        m = this.preProcessDoubles.matcher(s.toString());
        s.delete(0, s.capacity());

        while (m.find()) {
            if (m.group("doublechar") != null) {
                m.appendReplacement(s, "${doublechar}");
            }
        }

        m.appendTail(s);
        m = this.preProcessVowels.matcher(s.toString());
        s.delete(0, s.capacity());
        int doneVowels = 0;

        while (m.find()) {
            if (m.group("vowel") != null) {
                if (doneVowels < this.storeVowelsAmount) {
                    m.appendReplacement(s, "${vowel}");
                    doneVowels++;
                } else {
                    m.appendReplacement(s, "");
                }
            }
        }

        m.appendTail(s);
        return s.toString();
    }

    protected void addPhonizer(Phonizer p) {
        if (p != null && !this.phonizers.containsKey(p.getName())) {
            this.phonizers.put(p.getName(), p);
        }
    }

    protected void finalizeData() {
        this.phoneticRules = "";
        int max = this.phonizers.size();
        int i = 0;

        for (Phonizer p : this.phonizers.values()) {
            this.phoneticRules = this.phoneticRules + p.getRegex();
            if (++i < max) {
                this.phoneticRules = this.phoneticRules + "|";
            }
        }

        ProfanityFilter.printDebug("PhoneticRules: " + this.phoneticRules);
        this.pattern = Pattern.compile(this.phoneticRules);
    }

    protected void loadFilterWords() {
        try {
            String fileName = ZomboidFileSystem.instance.getString(ProfanityFilter.LOCALES_DIR + "blacklist_" + this.id + ".txt");
            File file = new File(fileName);
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            String line;
            int words;
            for (words = 0; (line = bufferedReader.readLine()) != null; words++) {
                this.addFilterWord(line);
            }

            fileReader.close();
            ProfanityFilter.printDebug("BlackList, " + words + " added.");
        } catch (IOException var7) {
            var7.printStackTrace();
        }
    }

    protected void loadFilterContains() {
        try {
            String fileName = ZomboidFileSystem.instance.getString(ProfanityFilter.LOCALES_DIR + "blacklist_contains_" + this.id + ".txt");
            File file = new File(fileName);
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            int words = 0;

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (!line.startsWith("//")) {
                    this.addFilterContains(line);
                    words++;
                }
            }

            fileReader.close();
            ProfanityFilter.printDebug("BlackList contains, " + words + " added.");
        } catch (IOException var7) {
            var7.printStackTrace();
        }
    }

    protected void loadWhiteListWords() {
        try {
            String fileName = ZomboidFileSystem.instance.getString(ProfanityFilter.LOCALES_DIR + "whitelist_" + this.id + ".txt");
            File file = new File(fileName);
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            String line;
            int words;
            for (words = 0; (line = bufferedReader.readLine()) != null; words++) {
                this.addWhiteListWord(line);
            }

            fileReader.close();
            ProfanityFilter.printDebug("WhiteList, " + words + " added.");
        } catch (IOException var7) {
            var7.printStackTrace();
        }
    }
}
