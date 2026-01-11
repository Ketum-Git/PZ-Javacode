// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.radio.StorySounds;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.random.Rand;
import zombie.input.GameKeyboard;
import zombie.iso.Vector2;
import zombie.ui.TextManager;
import zombie.ui.UIFont;

/**
 * Turbo
 * 
 *  Story line sound manager
 */
@UsedFromLua
public final class SLSoundManager {
    public static boolean enabled;
    public static boolean debug;
    public static boolean luaDebug;
    public static StoryEmitter emitter = new StoryEmitter();
    private static SLSoundManager instance;
    private final HashMap<Integer, Boolean> state = new HashMap<>();
    private final ArrayList<StorySound> storySounds = new ArrayList<>();
    private int nextTick;
    private final float borderCenterX = 10500.0F;
    private final float borderCenterY = 9000.0F;
    private final float borderRadiusMin = 12000.0F;
    private final float borderRadiusMax = 16000.0F;
    private final float borderScale = 1.0F;

    public static SLSoundManager getInstance() {
        if (instance == null) {
            instance = new SLSoundManager();
        }

        return instance;
    }

    private SLSoundManager() {
        this.state.put(12, false);
        this.state.put(13, false);
    }

    public boolean getDebug() {
        return debug;
    }

    public boolean getLuaDebug() {
        return luaDebug;
    }

    public ArrayList<StorySound> getStorySounds() {
        return this.storySounds;
    }

    public void print(String line) {
        if (debug) {
            System.out.println(line);
        }
    }

    public void init() {
        this.loadSounds();
    }

    public void loadSounds() {
        this.storySounds.clear();

        try {
            File f = ZomboidFileSystem.instance.getMediaFile("sound" + File.separator);
            if (f.exists() && f.isDirectory()) {
                File[] fileList = f.listFiles();

                for (int i = 0; i < fileList.length; i++) {
                    if (fileList[i].isFile()) {
                        String fileName = fileList[i].getName();
                        if (fileName.lastIndexOf(".") != -1
                            && fileName.lastIndexOf(".") != 0
                            && fileName.substring(fileName.lastIndexOf(".") + 1).equals("ogg")) {
                            String sound = fileName.substring(0, fileName.lastIndexOf("."));
                            this.print("Adding sound: " + sound);
                            this.addStorySound(new StorySound(sound, 1.0F));
                        }
                    }
                }
            }
        } catch (Exception var6) {
            System.out.print(var6.getMessage());
        }
    }

    private void addStorySound(StorySound storySound) {
        this.storySounds.add(storySound);
    }

    public void updateKeys() {
        for (Entry<Integer, Boolean> entry : this.state.entrySet()) {
            boolean isdown = GameKeyboard.isKeyDown(entry.getKey());
            if (isdown && entry.getValue() != isdown) {
                switch (entry.getKey()) {
                    case 12:
                    case 26:
                    case 53:
                    default:
                        break;
                    case 13:
                        emitter.coordinate3d = !emitter.coordinate3d;
                }
            }

            entry.setValue(isdown);
        }
    }

    public void update(int storylineDay, int hour, int min) {
        this.updateKeys();
        emitter.tick();
    }

    public void thunderTest() {
        this.nextTick--;
        if (this.nextTick <= 0) {
            this.nextTick = Rand.Next(10, 180);
            float radius = Rand.Next(0.0F, 8000.0F);
            double angle = Math.random() * Math.PI * 2.0;
            float x = 10500.0F + (float)(Math.cos(angle) * radius);
            float y = 9000.0F + (float)(Math.sin(angle) * radius);
            if (Rand.Next(0, 100) < 60) {
                emitter.playSound("thunder", 1.0F, x, y, 0.0F, 100.0F, 8500.0F);
            } else {
                emitter.playSound("thundereffect", 1.0F, x, y, 0.0F, 100.0F, 8500.0F);
            }
        }
    }

    public void render() {
        this.renderDebug();
    }

    public void renderDebug() {
        if (debug) {
            String str = emitter.coordinate3d ? "3D coordinates, X-Z-Y" : "2D coordinates X-Y-Z";
            int w = TextManager.instance.MeasureStringX(UIFont.Large, str) / 2;
            int h = TextManager.instance.MeasureStringY(UIFont.Large, str);
            int midx = Core.getInstance().getScreenWidth() / 2;
            int midy = Core.getInstance().getScreenHeight() / 2;
            this.renderLine(UIFont.Large, str, midx - w, midy);
        }
    }

    private void renderLine(UIFont font, String line, int x, int y) {
        TextManager.instance.DrawString(font, x + 1, y + 1, line, 0.0, 0.0, 0.0, 1.0);
        TextManager.instance.DrawString(font, x - 1, y - 1, line, 0.0, 0.0, 0.0, 1.0);
        TextManager.instance.DrawString(font, x + 1, y - 1, line, 0.0, 0.0, 0.0, 1.0);
        TextManager.instance.DrawString(font, x - 1, y + 1, line, 0.0, 0.0, 0.0, 1.0);
        TextManager.instance.DrawString(font, x, y, line, 1.0, 1.0, 1.0, 1.0);
    }

    public Vector2 getRandomBorderPosition() {
        float radius = Rand.Next(12000.0F, 16000.0F);
        double angle = Math.random() * Math.PI * 2.0;
        float x = 10500.0F + (float)(Math.cos(angle) * radius);
        float y = 9000.0F + (float)(Math.sin(angle) * radius);
        return new Vector2(x, y);
    }

    public float getRandomBorderRange() {
        return Rand.Next(18000.0F, 24000.0F);
    }
}
