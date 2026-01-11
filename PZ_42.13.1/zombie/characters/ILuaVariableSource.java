// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

/**
 * ILuaVariableSource
 *   Provides the functions expected by LUA when dealing with objects of this type.
 */
public interface ILuaVariableSource {
    String GetVariable(String key);

    void SetVariable(String key, String value);

    void ClearVariable(String key);
}
