// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;

public class WordsFilter {
    private final int universalCharactersMax = 3;
    private static WordsFilter instance;
    private static final HashSet<Character> SKIP_SYMBOL = new HashSet<>();
    private static final HashSet<Character> UNIVERSAL_SYMBOL = new HashSet<>();
    private static final Map<Character, List<Character>> SYMBOL_REPLACEMENTS = new HashMap<>();
    private WordsFilter.TreeNode root = new WordsFilter.TreeNode(' ');

    public static WordsFilter getInstance() {
        if (instance == null) {
            instance = new WordsFilter();
        }

        return instance;
    }

    public void loadWords(String badWordsFilename, String goodWordsFilename) {
        if (goodWordsFilename != null && !goodWordsFilename.isEmpty()) {
            String line;
            try (BufferedReader reader = new BufferedReader(new FileReader(goodWordsFilename))) {
                while ((line = reader.readLine()) != null) {
                    this.insertWord(line.trim().toLowerCase(), WordsFilter.WordType.Good);
                }
            } catch (FileNotFoundException var12) {
                DebugLog.General.error("Can't open file with good words (" + goodWordsFilename + ")");
            } catch (IOException var13) {
                DebugLog.General.printException(var13, "Can't load file with good words", LogSeverity.Error);
            }
        }

        if (badWordsFilename != null && !badWordsFilename.isEmpty()) {
            String line;
            try (BufferedReader reader = new BufferedReader(new FileReader(badWordsFilename))) {
                while ((line = reader.readLine()) != null) {
                    this.insertWord(line.trim().toLowerCase(), WordsFilter.WordType.Bad);
                }
            } catch (FileNotFoundException var9) {
                DebugLog.General.error("Can't open file with bad words (" + badWordsFilename + ")");
            } catch (IOException var10) {
                DebugLog.General.printException(var10, "Can't load file with bad words", LogSeverity.Error);
            }
        }
    }

    public void buildTree(List<String> words, WordsFilter.WordType type) {
        for (String word : words) {
            this.insertWord(word, type);
        }
    }

    private void insertWord(String word, WordsFilter.WordType type) {
        WordsFilter.TreeNode current = this.root;

        for (char c : word.toCharArray()) {
            if (current.children.get(c) != null) {
                current = current.children.get(c);
                if (current.type == WordsFilter.WordType.Bad) {
                    break;
                }
            } else {
                current.children.put(c, new WordsFilter.TreeNode(c));
                current = current.children.get(c);
            }
        }

        current.type = type;
    }

    public void loadReplacementsFromFile(String filename) {
        String line;
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    char symbol = parts[0].charAt(0);
                    String replacementsString = parts[1];
                    String[] replacementArray = replacementsString.split(",");
                    List<Character> replacements = new ArrayList<>();

                    for (String replacement : replacementArray) {
                        if (!replacement.isEmpty()) {
                            replacements.add(replacement.charAt(0));
                        }
                    }

                    SYMBOL_REPLACEMENTS.put(symbol, replacements);
                }
            }
        } catch (IOException var15) {
            var15.printStackTrace();
        }
    }

    public List<WordsFilter.SearchResult> searchText(String text) {
        List<WordsFilter.SearchResult> goodResults = new ArrayList<>();
        List<WordsFilter.SearchResult> results = new ArrayList<>();
        if (text == null) {
            return results;
        } else {
            for (int i = 0; i < text.length(); i++) {
                if (!SKIP_SYMBOL.contains(text.charAt(i))) {
                    this.searchRecursive(this.root, text.toLowerCase(), i, i, results, goodResults, 0);
                    if (!results.isEmpty()) {
                        WordsFilter.SearchResult result = results.get(results.size() - 1);
                        if (result.endPosition > i) {
                            i = result.endPosition;
                        }
                    }
                }
            }

            for (WordsFilter.SearchResult r : goodResults) {
                results.removeIf(searchResult -> searchResult.startPosition >= r.startPosition && searchResult.endPosition <= r.endPosition);
            }

            return results;
        }
    }

    public boolean detectBadWords(String text) {
        List<WordsFilter.SearchResult> goodResults = new ArrayList<>();
        List<WordsFilter.SearchResult> results = new ArrayList<>();

        for (int i = 0; i < text.length(); i++) {
            if (!SKIP_SYMBOL.contains(text.charAt(i))) {
                this.searchRecursive(this.root, text.toLowerCase(), i, i, results, goodResults, 0);

                for (WordsFilter.SearchResult r : goodResults) {
                    results.removeIf(searchResult -> searchResult.startPosition >= r.startPosition && searchResult.endPosition <= r.endPosition);
                }

                if (!results.isEmpty()) {
                    return true;
                }
            }
        }

        return false;
    }

    public String hideBadWords(String text, List<WordsFilter.SearchResult> badWordsList, String hideChar) {
        if (text != null && !text.isEmpty() && badWordsList != null && !badWordsList.isEmpty()) {
            StringBuilder result = new StringBuilder(text);

            for (int i = badWordsList.size() - 1; i >= 0; i--) {
                WordsFilter.SearchResult resultItem = badWordsList.get(i);
                int start = resultItem.startPosition;
                int end = resultItem.endPosition;
                if (start >= 0 && end <= result.length() && start <= end) {
                    int wordStart = start;

                    while (wordStart > 0 && !Character.isWhitespace(result.charAt(wordStart - 1))) {
                        wordStart--;
                    }

                    int wordEnd = end;

                    while (wordEnd < result.length() && !Character.isWhitespace(result.charAt(wordEnd))) {
                        wordEnd++;
                    }

                    if (hideChar.length() == 1) {
                        for (int k = wordStart; k < wordEnd; k++) {
                            result.setCharAt(k, hideChar.charAt(0));
                        }
                    } else {
                        result.delete(wordStart, wordEnd);
                        result.insert(wordStart, hideChar);
                    }
                }
            }

            return result.toString();
        } else {
            return text;
        }
    }

    private void searchRecursive(
        WordsFilter.TreeNode node,
        String text,
        int startPos,
        int currentPos,
        List<WordsFilter.SearchResult> results,
        List<WordsFilter.SearchResult> goodResults,
        int universalCharacters
    ) {
        if (currentPos < text.length() && universalCharacters <= 3) {
            char currentChar = text.charAt(currentPos);
            WordsFilter.TreeNode nextNode = node.children.get(currentChar);
            if (nextNode != null) {
                if (nextNode.type == WordsFilter.WordType.Bad) {
                    WordsFilter.SearchResult result = new WordsFilter.SearchResult(startPos, currentPos);
                    results.add(result);
                    return;
                }

                if (nextNode.type == WordsFilter.WordType.Good) {
                    currentPos = text.indexOf(" ", currentPos);
                    if (currentPos == -1) {
                        currentPos = text.length() - 1;
                    }

                    results.removeIf(searchResult -> searchResult.startPosition >= startPos);
                    WordsFilter.SearchResult result = new WordsFilter.SearchResult(startPos, currentPos);
                    goodResults.add(result);
                }

                this.searchRecursive(nextNode, text, startPos, currentPos + 1, results, goodResults, universalCharacters);
            } else if (startPos != currentPos && UNIVERSAL_SYMBOL.contains(currentChar)) {
                for (WordsFilter.TreeNode node2 : node.children.values()) {
                    if (node2.type == WordsFilter.WordType.Bad && currentPos - startPos > universalCharacters) {
                        WordsFilter.SearchResult result = new WordsFilter.SearchResult(startPos, currentPos);
                        results.add(result);
                    }

                    this.searchRecursive(node2, text, startPos, currentPos + 1, results, goodResults, universalCharacters + 1);
                }
            } else if (SYMBOL_REPLACEMENTS.containsKey(currentChar)) {
                for (WordsFilter.TreeNode node2 : node.children.values()) {
                    if (SYMBOL_REPLACEMENTS.get(currentChar).contains(node2.value)) {
                        this.searchRecursive(node2, text, startPos, currentPos + 1, results, goodResults, universalCharacters);
                    }
                }
            }
        }
    }

    public void saveTreeToFile(String filename) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            this.saveNode(writer, this.root);
        }
    }

    private void saveNode(BufferedWriter writer, WordsFilter.TreeNode node) throws IOException {
        writer.write(node.value);
        writer.write(node.type.ordinal());
        writer.write(node.children.size());

        for (WordsFilter.TreeNode child : node.children.values()) {
            this.saveNode(writer, child);
        }
    }

    public void loadTreeFromFile(String filename) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            this.root = this.loadNode(reader);
        }
    }

    private WordsFilter.TreeNode loadNode(BufferedReader reader) throws IOException {
        int value = reader.read();
        WordsFilter.WordType type = WordsFilter.WordType.values()[reader.read()];
        WordsFilter.TreeNode node = new WordsFilter.TreeNode((char)value);
        node.type = type;
        int childCount = reader.read();

        for (int i = 0; i < childCount; i++) {
            WordsFilter.TreeNode child = this.loadNode(reader);
            node.children.put(child.value, child);
        }

        return node;
    }

    static {
        SKIP_SYMBOL.add('*');
        SKIP_SYMBOL.add(' ');
        SKIP_SYMBOL.add('.');
        SKIP_SYMBOL.add(',');
        SKIP_SYMBOL.add('!');
        SKIP_SYMBOL.add('@');
        SKIP_SYMBOL.add('$');
        SKIP_SYMBOL.add('#');
        SKIP_SYMBOL.add('%');
        SKIP_SYMBOL.add('&');
        SKIP_SYMBOL.add('~');
        UNIVERSAL_SYMBOL.add('*');
        UNIVERSAL_SYMBOL.add('@');
        UNIVERSAL_SYMBOL.add('#');
        UNIVERSAL_SYMBOL.add('$');
        UNIVERSAL_SYMBOL.add('%');
        UNIVERSAL_SYMBOL.add('&');
        UNIVERSAL_SYMBOL.add('!');
    }

    public static class Policy {
        public static final int Ban = 1;
        public static final int Kick = 2;
        public static final int Log = 3;
        public static final int Mute = 4;
        public static final int Nothing = 5;

        public static String name(int value) {
            return switch (value) {
                case 1 -> "Ban";
                case 2 -> "Kick";
                case 3 -> "Log";
                case 4 -> "Mute";
                case 5 -> "Nothing";
                default -> "Unknown";
            };
        }
    }

    public static class SearchResult {
        public final int startPosition;
        public final int endPosition;

        public SearchResult(int startPosition, int endPosition) {
            this.startPosition = startPosition;
            this.endPosition = endPosition;
        }

        @Override
        public String toString() {
            return String.format("Match[%d-%d]'", this.startPosition, this.endPosition);
        }
    }

    class TreeNode {
        char value;
        WordsFilter.WordType type;
        Map<Character, WordsFilter.TreeNode> children;

        public TreeNode(final char value) {
            Objects.requireNonNull(WordsFilter.this);
            super();
            this.value = value;
            this.type = WordsFilter.WordType.None;
            this.children = new HashMap<>();
        }
    }

    public static enum WordType {
        None,
        Good,
        Bad;
    }
}
