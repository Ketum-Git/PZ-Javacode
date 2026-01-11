// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting;

import zombie.scripting.objects.Item;
import zombie.scripting.objects.Recipe;

public interface IScriptObjectStore {
    Item getItem(String name);

    Recipe getRecipe(String name);
}
