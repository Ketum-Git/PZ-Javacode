// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ui;

public interface UIEventHandler {
    void DoubleClick(String name, int x, int y);

    void ModalClick(String name, String chosen);

    void Selected(String name, int Selected, int LastSelected);
}
