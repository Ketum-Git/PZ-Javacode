// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

public class TimedInputHandler {
    public static final int MAX_CLICK_TIME = 400;
    private boolean keyDown;
    private boolean keyPressed;
    private boolean keyReleased;
    private boolean keyClicked;
    private boolean keyIsMouse;
    private boolean keyWasMouse;
    private boolean muted;
    private long keyDownMs;
    private final TimedInputHandler.IsKeyDownCallback isKeyDownCallback;
    private final TimedInputHandler.IsMouseKeyCallback isMouseKeyCallback;
    private final TimedInputHandler.IsKeyMutedCallback isMutedCallback;

    public TimedInputHandler(
        TimedInputHandler.IsKeyDownCallback isKeyDownCallback,
        TimedInputHandler.IsMouseKeyCallback isMouseKeyCallback,
        TimedInputHandler.IsKeyMutedCallback isMuted
    ) {
        this.isKeyDownCallback = isKeyDownCallback;
        this.isMouseKeyCallback = isMouseKeyCallback;
        this.isMutedCallback = isMuted;
    }

    public void mute() {
        this.muted = true;
    }

    public boolean queryKeyDown() {
        return this.isKeyDownCallback.isKeyDown();
    }

    public boolean queryKeyIsMouse() {
        return this.isMouseKeyCallback != null && this.isMouseKeyCallback.isMouseKey();
    }

    public boolean queryIsMuted() {
        return this.isMutedCallback != null && this.isMutedCallback.isMuted();
    }

    public void updateState() {
        long now = System.currentTimeMillis();
        this.keyDown = this.queryKeyDown();
        this.keyIsMouse = this.queryKeyIsMouse();
        this.keyPressed = this.keyDown && this.keyDownMs == 0L;
        this.keyReleased = !this.keyDown && this.keyDownMs != 0L;
        this.keyClicked = this.keyReleased && now - this.keyDownMs < 400L;
        if (this.keyPressed) {
            this.keyWasMouse = this.keyIsMouse;
        }

        if (this.keyPressed) {
            this.keyDownMs = now;
        } else if (this.keyReleased) {
            this.keyDownMs = 0L;
        }

        boolean shouldMute = this.queryIsMuted();
        if (this.muted != shouldMute) {
            this.muted = this.keyDown || this.keyReleased || shouldMute;
        }
    }

    public boolean isKeyDown() {
        return !this.muted && this.keyDown;
    }

    public boolean isKeyPressed() {
        return !this.muted && this.keyPressed;
    }

    public boolean isKeyReleased() {
        return !this.muted && this.keyReleased;
    }

    public boolean isMouseKey() {
        return this.keyIsMouse;
    }

    public boolean wasMouseKey() {
        return this.keyWasMouse;
    }

    public boolean isKeyClicked() {
        return !this.muted && this.keyClicked;
    }

    public boolean isMuted() {
        return this.muted;
    }

    public interface IsKeyDownCallback {
        boolean isKeyDown();
    }

    public interface IsKeyMutedCallback {
        boolean isMuted();
    }

    public interface IsMouseKeyCallback {
        boolean isMouseKey();
    }
}
