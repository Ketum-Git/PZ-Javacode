// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug.debugWindows;

import zombie.SoundAssetManager;
import zombie.SoundManager;
import zombie.debug.BaseDebugWindow;

public class PZDebugWindow extends BaseDebugWindow {
    @Override
    public void doWindow() {
        super.doWindow();
    }

    @Override
    protected void onWindowDocked() {
        if (SoundAssetManager.useImGuiSounds && !SoundManager.instance.isPlayingUISound("UIDocked")) {
            SoundManager.instance.playUISound("UIDocked");
        }
    }

    @Override
    protected void onWindowUndocked() {
        if (SoundAssetManager.useImGuiSounds && !SoundManager.instance.isPlayingUISound("UIUndocked")) {
            SoundManager.instance.playUISound("UIUndocked");
        }
    }

    @Override
    protected void onOpenWindow() {
        if (SoundAssetManager.useImGuiSounds && !SoundManager.instance.isPlayingUISound("UIDocked")) {
            SoundManager.instance.playUISound("UIDocked");
        }
    }

    @Override
    protected void onCloseWindow() {
        if (SoundAssetManager.useImGuiSounds && !SoundManager.instance.isPlayingUISound("UIUndocked")) {
            SoundManager.instance.playUISound("UIUndocked");
        }
    }
}
