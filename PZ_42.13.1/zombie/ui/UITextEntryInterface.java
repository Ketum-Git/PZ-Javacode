// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ui;

import zombie.core.Color;

public interface UITextEntryInterface {
    boolean isDoingTextEntry();

    void setDoingTextEntry(boolean arg0);

    String getUIName();

    boolean isEditable();

    UINineGrid getFrame();

    boolean isIgnoreFirst();

    void setIgnoreFirst(boolean arg0);

    void setSelectingRange(boolean arg0);

    Color getStandardFrameColour();

    void onKeyEnter();

    void onKeyHome();

    void onKeyEnd();

    void onKeyUp();

    void onKeyDown();

    void onKeyLeft();

    void onKeyRight();

    void onKeyDelete();

    void onKeyBack();

    void pasteFromClipboard();

    void copyToClipboard();

    void cutToClipboard();

    void selectAll();

    boolean isTextLimit();

    boolean isOnlyNumbers();

    boolean isOnlyText();

    void onOtherKey(int arg0);

    void putCharacter(char arg0);
}
