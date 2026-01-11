// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ui;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.characters.CharacterStat;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.gameStates.IngameState;

@UsedFromLua
public final class ModalDialog extends NewWindow {
    public boolean yes;
    public String name;
    UIEventHandler handler;
    public boolean clicked;

    public ModalDialog(String name, String help, boolean bYesNo) {
        super(Core.getInstance().getOffscreenWidth(0) / 2, Core.getInstance().getOffscreenHeight(0) / 2, 470, 10, false);
        this.name = name;
        this.resizeToFitY = false;
        this.ignoreLossControl = true;
        TextBox text = new TextBox(UIFont.Medium, 0, 0, 450, help);
        text.centered = true;
        text.resizeParent = true;
        text.update();
        this.Nest(text, 20, 10, 20, 10);
        this.update();
        this.height *= 1.3F;
        if (bYesNo) {
            this.AddChild(new DialogButton(this, (float)(this.getWidth().intValue() / 2 - 40), (float)(this.getHeight().intValue() - 18), "Yes", "Yes"));
            this.AddChild(new DialogButton(this, (float)(this.getWidth().intValue() / 2 + 40), (float)(this.getHeight().intValue() - 18), "No", "No"));
        } else {
            this.AddChild(new DialogButton(this, (float)(this.getWidth().intValue() / 2), (float)(this.getHeight().intValue() - 18), "Ok", "Ok"));
        }

        this.x = this.x - this.width / 2.0F;
        this.y = this.y - this.height / 2.0F;
    }

    @Override
    public void ButtonClicked(String name) {
        if (this.handler != null) {
            this.handler.ModalClick(this.name, name);
            this.setVisible(false);
        } else {
            if (name.equals("Ok")) {
                UIManager.getSpeedControls().SetCurrentGameSpeed(4);
                this.Clicked(name);
                this.clicked = true;
                this.yes = true;
                this.setVisible(false);
                IngameState.instance.paused = false;
            }

            if (name.equals("Yes")) {
                UIManager.getSpeedControls().SetCurrentGameSpeed(4);
                this.Clicked(name);
                this.clicked = true;
                this.yes = true;
                this.setVisible(false);
                IngameState.instance.paused = false;
            }

            if (name.equals("No")) {
                UIManager.getSpeedControls().SetCurrentGameSpeed(4);
                this.Clicked(name);
                this.clicked = true;
                this.yes = false;
                this.setVisible(false);
                IngameState.instance.paused = false;
            }
        }
    }

    public void Clicked(String name) {
        if (this.name.equals("Sleep") && name.equals("Yes")) {
            float SleepHours = 12.0F * IsoPlayer.getInstance().getStats().get(CharacterStat.FATIGUE);
            if (SleepHours < 7.0F) {
                SleepHours = 7.0F;
            }

            SleepHours += GameTime.getInstance().getTimeOfDay();
            if (SleepHours >= 24.0F) {
                SleepHours -= 24.0F;
            }

            IsoPlayer.getInstance().setForceWakeUpTime((int)SleepHours);
            IsoPlayer.getInstance().setAsleepTime(0.0F);
            TutorialManager.instance.stealControl = true;
            IsoPlayer.getInstance().setAsleep(true);
            UIManager.setbFadeBeforeUI(true);
            UIManager.FadeOut(4.0);
            UIManager.getSpeedControls().SetCurrentGameSpeed(3);

            try {
                GameWindow.save(true);
            } catch (IOException var4) {
                Logger.getLogger(ModalDialog.class.getName()).log(Level.SEVERE, null, var4);
            }
        }

        UIManager.modal.setVisible(false);
    }
}
