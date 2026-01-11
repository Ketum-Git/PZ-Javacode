// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.itemConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VariableBuilder {
    private static final String fieldStart = "\\$\\[";
    private static final String fieldEnd = "\\]";
    private static final String regex = "\\$\\[([^]]+)\\]";
    private static final Pattern pattern = Pattern.compile("\\$\\[([^]]+)\\]");
    private static final Map<String, String> m_keys = new HashMap<>();

    public static void clear() {
        m_keys.clear();
    }

    public static void addKey(String key, String value) {
        m_keys.put(key, value);
    }

    public static void setKeys(Map<String, String> map) {
        m_keys.clear();

        for (Entry<String, String> entry : map.entrySet()) {
            m_keys.put(entry.getKey(), entry.getValue());
        }
    }

    public static String Build(String input) throws ItemConfig.ItemConfigException {
        return format(input);
    }

    private static String format(String format) throws ItemConfig.ItemConfigException {
        Matcher m = pattern.matcher(format);
        String result = format;

        while (m.find()) {
            String found = m.group(1).toLowerCase().trim();
            String replacement = m_keys.get(found);
            if (replacement == null) {
                throw new ItemConfig.ItemConfigException("Variable not found: " + found);
            }

            result = result.replaceFirst("\\$\\[([^]]+)\\]", replacement);
        }

        return result;
    }
}
