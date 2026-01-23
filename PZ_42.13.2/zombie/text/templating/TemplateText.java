// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.text.templating;

import java.util.Random;
import se.krka.kahlua.j2se.KahluaTableImpl;
import zombie.UsedFromLua;
import zombie.Lua.LuaEventManager;
import zombie.characters.SurvivorFactory;

/**
 * TurboTuTone.
 */
@UsedFromLua
public class TemplateText {
    private static final ITemplateBuilder builder = new TemplateTextBuilder();
    private static final Random m_random = new Random(4397238L);

    public static ITemplateBuilder CreateBlanc() {
        return new TemplateTextBuilder();
    }

    public static ITemplateBuilder CreateCopy() {
        ITemplateBuilder builder = new TemplateTextBuilder();
        builder.CopyFrom(TemplateText.builder);
        return builder;
    }

    public static String Build(String input) {
        return builder.Build(input);
    }

    public static String Build(String input, IReplaceProvider replaceProvider) {
        return builder.Build(input, replaceProvider);
    }

    public static String Build(String input, KahluaTableImpl table) {
        try {
            return builder.Build(input, table);
        } catch (Exception var3) {
            var3.printStackTrace();
            return input;
        }
    }

    public static void RegisterKey(String key, KahluaTableImpl table) {
        builder.RegisterKey(key, table);
    }

    public static void RegisterKey(String key, IReplace replace) {
        builder.RegisterKey(key, replace);
    }

    public static void Initialize() {
        builder.RegisterKey("lastname", new IReplace() {
            @Override
            public String getString() {
                return SurvivorFactory.getRandomSurname();
            }
        });
        builder.RegisterKey("firstname", new IReplace() {
            @Override
            public String getString() {
                return TemplateText.RandNext(100) > 50 ? SurvivorFactory.getRandomForename(true) : SurvivorFactory.getRandomForename(false);
            }
        });
        builder.RegisterKey("maleName", new IReplace() {
            @Override
            public String getString() {
                return SurvivorFactory.getRandomForename(false);
            }
        });
        builder.RegisterKey("femaleName", new IReplace() {
            @Override
            public String getString() {
                return SurvivorFactory.getRandomForename(true);
            }
        });
        LuaEventManager.triggerEvent("OnTemplateTextInit");
    }

    public static void Reset() {
        builder.Reset();
    }

    public static float RandNext(float min, float max) {
        if (min == max) {
            return min;
        } else {
            if (min > max) {
                min = max;
                max = max;
            }

            return min + m_random.nextFloat() * (max - min);
        }
    }

    public static float RandNext(float bound) {
        return m_random.nextFloat() * bound;
    }

    public static int RandNext(int min, int max) {
        if (min == max) {
            return min;
        } else {
            if (min > max) {
                min = max;
                max = max;
            }

            return min + m_random.nextInt(max - min);
        }
    }

    public static int RandNext(int bound) {
        return m_random.nextInt(bound);
    }
}
