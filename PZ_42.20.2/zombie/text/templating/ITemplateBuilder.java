// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.text.templating;

import se.krka.kahlua.j2se.KahluaTableImpl;

/**
 * TurboTuTone.
 */
public interface ITemplateBuilder {
    String Build(String input);

    String Build(String input, IReplaceProvider replaceProvider);

    String Build(String input, KahluaTableImpl table);

    void RegisterKey(String key, KahluaTableImpl table);

    void RegisterKey(String key, IReplace replace);

    void Reset();

    void CopyFrom(Object arg0);
}
