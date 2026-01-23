// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.text.templating;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import se.krka.kahlua.j2se.KahluaTableImpl;
import zombie.debug.DebugLog;

/**
 * TurboTuTone.
 *  A generic non-pooled ReplaceProvider that can be used for example in scripting,
 *  where the provider could provide forced overrides for certain template keys.
 */
public class ReplaceProvider implements IReplaceProvider {
    protected final Map<String, IReplace> keys = new HashMap<>();

    public void addKey(String key, final String value) {
        this.addReplacer(key, new IReplace() {
            {
                Objects.requireNonNull(ReplaceProvider.this);
            }

            @Override
            public String getString() {
                return value;
            }
        });
    }

    public void addKey(String key, KahluaTableImpl table) {
        try {
            ArrayList<String> list = new ArrayList<>();

            for (int i = 1; i < table.len() + 1; i++) {
                list.add((String)table.rawget(i));
            }

            if (!list.isEmpty()) {
                this.addReplacer(key, new ReplaceList(list));
            } else {
                DebugLog.log("ReplaceProvider -> key '" + key + "' contains no entries, ignoring.");
            }
        } catch (Exception var5) {
            var5.printStackTrace();
        }
    }

    public void addReplacer(String key, IReplace replace) {
        if (this.keys.containsKey(key.toLowerCase())) {
            DebugLog.log("ReplaceProvider -> Warning: key '" + key + "' replaces an existing key.");
        }

        this.keys.put(key.toLowerCase(), replace);
    }

    @Override
    public boolean hasReplacer(String key) {
        return this.keys.containsKey(key);
    }

    @Override
    public IReplace getReplacer(String key) {
        return this.keys.get(key);
    }
}
