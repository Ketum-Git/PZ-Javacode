// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.interfaces;

public interface IDestroyable {
    /**
     * destory the object
     */
    void destroy();

    /**
     * returns if the object is destryed or not
     */
    boolean isDestroyed();
}
