// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap.styles;

public abstract class IWorldMapStyleListener {
    abstract void onAdd(WorldMapStyleLayer arg0);

    abstract void onBeforeRemove(WorldMapStyleLayer arg0);

    abstract void onAfterRemove(WorldMapStyleLayer arg0);

    abstract void onMoveLayer(int arg0, int arg1);

    abstract void onBeforeClear();

    abstract void onAfterClear();
}
