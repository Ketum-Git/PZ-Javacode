// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import zombie.GameTime;
import zombie.core.Core;
import zombie.iso.IsoCamera;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.ui.TextManager;
import zombie.ui.UIFont;

public final class ThumpDamageRender {
    public static final ThumpDamageRender instance = new ThumpDamageRender();
    private static final float DISPLAY_TIME = 2.0F;
    private final Map<IsoMovingObject, ThumpDamageRender.Thumper> damageMap = new HashMap<>();
    private final List<IsoMovingObject> removeList = new ArrayList<>();

    public void thump(IsoMovingObject chr, float damage, float health) {
        ThumpDamageRender.Thumper damage1 = this.damageMap.get(chr);
        if (damage1 == null) {
            damage1 = new ThumpDamageRender.Thumper(chr);
            this.damageMap.put(chr, damage1);
        }

        int maxQsize = 8;

        while (damage1.damageQueue.size() >= 8) {
            damage1.damageQueue.removeFirst();
        }

        ThumpDamageRender.Damage damage2 = new ThumpDamageRender.Damage(damage, health);
        damage1.damageQueue.add(damage2);
    }

    public void render() {
        float dt = GameTime.getInstance().getRealworldSecondsSinceLastUpdate();
        this.removeList.clear();

        for (Entry<IsoMovingObject, ThumpDamageRender.Thumper> entry : this.damageMap.entrySet()) {
            IsoMovingObject chr = entry.getKey();
            if (!IsoWorld.instance.currentCell.getObjectList().contains(chr)) {
                this.removeList.add(chr);
            } else {
                ThumpDamageRender.Thumper thumper = entry.getValue();
                thumper.render(dt);
                if (thumper.damageQueue.isEmpty()) {
                    this.removeList.add(chr);
                }
            }
        }

        for (IsoMovingObject chr : this.removeList) {
            this.damageMap.remove(chr);
        }
    }

    public void Reset() {
        this.damageMap.clear();
    }

    private static final class Damage {
        float damage;
        float health;
        float time;

        Damage(float damage, float health) {
            this.damage = damage;
            this.health = health;
        }
    }

    private static final class Thumper {
        IsoMovingObject chr;
        final List<ThumpDamageRender.Damage> damageQueue = new ArrayList<>();

        Thumper(IsoMovingObject chr) {
            this.chr = chr;
        }

        void render(float dt) {
            float renderX = this.chr.getX();
            float renderY = this.chr.getY();
            float sx = IsoUtils.XToScreenExact(renderX, renderY, this.chr.getZ(), 0);
            float sy = IsoUtils.YToScreenExact(renderX, renderY, this.chr.getZ(), 0);
            float zoom = Core.getInstance().getZoom(IsoCamera.frameState.playerIndex);
            sx /= zoom;
            sy /= zoom;
            sx -= this.chr.offsetX;
            sy -= this.chr.offsetY;
            float characterHgt = 128.0F;
            int fontHgt = TextManager.instance.getFontHeight(UIFont.Medium);
            sy -= 128.0F / (2.0F / Core.tileScale) / zoom + fontHgt;
            this.removeOldDamage();

            for (int i = 0; i < this.damageQueue.size(); i++) {
                ThumpDamageRender.Damage damage = this.damageQueue.get(i);
                if (!GameTime.isGamePaused()) {
                    damage.time += dt;
                }

                float sy1 = sy - i * fontHgt;
                TextManager.instance
                    .DrawStringCentre(UIFont.Medium, (double)sx, (double)sy1, String.format("%.1f / %.1f", damage.damage, damage.health), 1.0, 0.0, 0.0, 1.0);
            }
        }

        void removeOldDamage() {
            for (int i = 0; i < this.damageQueue.size(); i++) {
                ThumpDamageRender.Damage damage = this.damageQueue.get(i);
                if (damage.time >= 2.0F) {
                    this.damageQueue.remove(i--);
                }
            }
        }
    }
}
