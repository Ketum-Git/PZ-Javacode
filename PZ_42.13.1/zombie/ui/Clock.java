// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ui;

import java.util.ArrayList;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.textures.Texture;
import zombie.debug.DebugOptions;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.AlarmClock;
import zombie.inventory.types.AlarmClockClothing;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameClient;
import zombie.scripting.objects.ItemTag;

@UsedFromLua
public final class Clock extends UIElement {
    private boolean largeTextures;
    private Texture background;
    private Texture[] digitsLarge;
    private Texture[] digitsSmall;
    private Texture colon;
    private Texture slash;
    private Texture minus;
    private Texture dot;
    private Texture tempC;
    private Texture tempF;
    private Texture tempE;
    private Texture texAm;
    private Texture texPm;
    private Texture alarmOn;
    private Texture alarmRinging;
    private final Color displayColour = new Color(100, 200, 210, 255);
    private final Color ghostColour = new Color(40, 40, 40, 128);
    private int uxOriginal;
    private int uyOriginal;
    private int largeDigitSpacing;
    private int smallDigitSpacing;
    private int colonSpacing;
    private int ampmSpacing;
    private int alarmBellSpacing;
    private int decimalSpacing;
    private int degreeSpacing;
    private int slashSpacing;
    private int tempDateSpacing;
    private int dateOffset;
    private int minusOffset;
    private int amVerticalSpacing;
    private int pmVerticalSpacing;
    private int alarmBellVerticalSpacing;
    private int displayVerticalSpacing;
    private int decimalVerticalSpacing;
    private boolean digital;
    private boolean isAlarmSet;
    private boolean isAlarmRinging;
    private IsoPlayer clockPlayer;
    public static Clock instance;

    public Clock(int x, int y) {
        this.x = x;
        this.y = y;
        instance = this;
    }

    @Override
    public void render() {
        if (this.visible) {
            this.assignTextures(Core.getInstance().getOptionClockSize() == 2);
            this.DrawTexture(this.background, 0.0, 0.0, 0.75);
            this.renderDisplay(true, this.ghostColour);
            this.renderDisplay(false, this.displayColour);
            super.render();
        }
    }

    private void renderDisplay(boolean ghostDisplay, Color color) {
        int ux = this.uxOriginal;
        int uy = this.uyOriginal;

        for (int i = 0; i < 4; i++) {
            int[] timeDigits = this.timeDigits();
            if (ghostDisplay) {
                this.DrawTextureCol(this.digitsLarge[8], ux, uy, color);
            } else {
                this.DrawTextureCol(this.digitsLarge[timeDigits[i]], ux, uy, color);
            }

            ux += this.digitsLarge[0].getWidth();
            if (i == 1) {
                ux += this.colonSpacing;
                this.DrawTextureCol(this.colon, ux, uy, color);
                ux += this.colon.getWidth() + this.colonSpacing;
            } else if (i < 3) {
                ux += this.largeDigitSpacing;
            }
        }

        ux += this.ampmSpacing;
        if (!Core.getInstance().getOptionClock24Hour() || ghostDisplay) {
            if (ghostDisplay) {
                this.DrawTextureCol(this.texAm, ux, uy + this.amVerticalSpacing, color);
                this.DrawTextureCol(this.texPm, ux, uy + this.pmVerticalSpacing, color);
            } else if (GameTime.getInstance().getTimeOfDay() < 12.0F) {
                this.DrawTextureCol(this.texAm, ux, uy + this.amVerticalSpacing, color);
            } else {
                this.DrawTextureCol(this.texPm, ux, uy + this.pmVerticalSpacing, color);
            }
        }

        if (this.isAlarmRinging || ghostDisplay) {
            this.DrawTextureCol(this.alarmRinging, ux + this.texAm.getWidth() + this.alarmBellSpacing, uy + this.alarmBellVerticalSpacing, color);
        } else if (this.isAlarmSet) {
            this.DrawTextureCol(this.alarmOn, ux + this.texAm.getWidth() + this.alarmBellSpacing, uy + this.alarmBellVerticalSpacing, color);
        }

        if (this.digital || ghostDisplay) {
            ux = this.uxOriginal;
            uy += this.digitsLarge[0].getHeight() + this.displayVerticalSpacing;
            if (this.clockPlayer == null) {
                ux += this.dateOffset;
            } else {
                int[] tempDigits = this.tempDigits();
                if (tempDigits[0] == 1 || ghostDisplay) {
                    this.DrawTextureCol(this.minus, ux, uy, color);
                }

                ux += this.minusOffset;
                if (tempDigits[1] == 1 || ghostDisplay) {
                    this.DrawTextureCol(this.digitsSmall[1], ux, uy, color);
                }

                ux += this.digitsSmall[0].getWidth() + this.smallDigitSpacing;

                for (int i = 2; i < 5; i++) {
                    if (ghostDisplay) {
                        this.DrawTextureCol(this.digitsSmall[8], ux, uy, color);
                    } else {
                        this.DrawTextureCol(this.digitsSmall[tempDigits[i]], ux, uy, color);
                    }

                    ux += this.digitsSmall[0].getWidth();
                    if (i == 3) {
                        ux += this.decimalSpacing;
                        this.DrawTextureCol(this.dot, ux, uy + this.decimalVerticalSpacing, color);
                        ux += this.dot.getWidth() + this.decimalSpacing;
                    } else if (i < 4) {
                        ux += this.smallDigitSpacing;
                    }
                }

                ux += this.degreeSpacing;
                this.DrawTextureCol(this.dot, ux, uy, color);
                ux += this.dot.getWidth() + this.degreeSpacing;
                if (ghostDisplay) {
                    this.DrawTextureCol(this.tempE, ux, uy, color);
                } else if (tempDigits[5] == 0) {
                    this.DrawTextureCol(this.tempC, ux, uy, color);
                } else {
                    this.DrawTextureCol(this.tempF, ux, uy, color);
                }

                ux += this.digitsSmall[0].getWidth() + this.tempDateSpacing;
            }

            int[] dateDigits = this.dateDigits();

            for (int i = 0; i < 4; i++) {
                if (ghostDisplay) {
                    this.DrawTextureCol(this.digitsSmall[8], ux, uy, color);
                } else {
                    this.DrawTextureCol(this.digitsSmall[dateDigits[i]], ux, uy, color);
                }

                ux += this.digitsSmall[0].getWidth();
                if (i == 1) {
                    ux += this.slashSpacing;
                    this.DrawTextureCol(this.slash, ux, uy, color);
                    ux += this.slash.getWidth() + this.slashSpacing;
                } else if (i < 3) {
                    ux += this.smallDigitSpacing;
                }
            }
        }
    }

    private void assignTextures(boolean largeTextures) {
        if (this.digitsLarge == null || this.largeTextures != largeTextures) {
            this.largeTextures = largeTextures;
            if (largeTextures) {
                this.background = Texture.getSharedTexture("media/ui/ClockAssets/ClockLargeBackground.png");
            } else {
                this.background = Texture.getSharedTexture("media/ui/ClockAssets/ClockSmallBackground.png");
            }

            String LargeTex = "Medium";
            String SmallTex = "Small";
            if (largeTextures) {
                LargeTex = "Large";
                SmallTex = "Medium";
                this.assignLargeOffsets();
            } else {
                this.assignSmallOffsets();
            }

            if (this.digitsLarge == null) {
                this.digitsLarge = new Texture[10];
                this.digitsSmall = new Texture[10];
            }

            for (int n = 0; n < 10; n++) {
                this.digitsLarge[n] = Texture.getSharedTexture("media/ui/ClockAssets/ClockDigits" + LargeTex + n + ".png");
                this.digitsSmall[n] = Texture.getSharedTexture("media/ui/ClockAssets/ClockDigits" + SmallTex + n + ".png");
            }

            this.colon = Texture.getSharedTexture("media/ui/ClockAssets/ClockDivide" + LargeTex + ".png");
            this.slash = Texture.getSharedTexture("media/ui/ClockAssets/DateDivide" + SmallTex + ".png");
            this.minus = Texture.getSharedTexture("media/ui/ClockAssets/ClockDigits" + SmallTex + "Minus.png");
            this.dot = Texture.getSharedTexture("media/ui/ClockAssets/ClockDigits" + SmallTex + "Dot.png");
            this.tempC = Texture.getSharedTexture("media/ui/ClockAssets/ClockDigits" + SmallTex + "C.png");
            this.tempF = Texture.getSharedTexture("media/ui/ClockAssets/ClockDigits" + SmallTex + "F.png");
            this.tempE = Texture.getSharedTexture("media/ui/ClockAssets/ClockDigits" + SmallTex + "E.png");
            this.texAm = Texture.getSharedTexture("media/ui/ClockAssets/ClockAm" + LargeTex + ".png");
            this.texPm = Texture.getSharedTexture("media/ui/ClockAssets/ClockPm" + LargeTex + ".png");
            this.alarmOn = Texture.getSharedTexture("media/ui/ClockAssets/ClockAlarm" + LargeTex + "Set.png");
            this.alarmRinging = Texture.getSharedTexture("media/ui/ClockAssets/ClockAlarm" + LargeTex + "Sound.png");
        }
    }

    private void assignSmallOffsets() {
        this.uxOriginal = 3;
        this.uyOriginal = 3;
        this.largeDigitSpacing = 1;
        this.smallDigitSpacing = 1;
        this.colonSpacing = 1;
        this.ampmSpacing = 1;
        this.alarmBellSpacing = 1;
        this.decimalSpacing = 1;
        this.degreeSpacing = 1;
        this.slashSpacing = 1;
        this.tempDateSpacing = 5;
        this.dateOffset = 33;
        this.minusOffset = 0;
        this.amVerticalSpacing = 7;
        this.pmVerticalSpacing = 12;
        this.alarmBellVerticalSpacing = 1;
        this.displayVerticalSpacing = 2;
        this.decimalVerticalSpacing = 6;
    }

    private void assignLargeOffsets() {
        this.uxOriginal = 3;
        this.uyOriginal = 3;
        this.largeDigitSpacing = 2;
        this.smallDigitSpacing = 1;
        this.colonSpacing = 3;
        this.ampmSpacing = 3;
        this.alarmBellSpacing = 5;
        this.decimalSpacing = 2;
        this.degreeSpacing = 2;
        this.slashSpacing = 2;
        this.tempDateSpacing = 8;
        this.dateOffset = 65;
        this.minusOffset = -2;
        this.amVerticalSpacing = 15;
        this.pmVerticalSpacing = 25;
        this.alarmBellVerticalSpacing = 1;
        this.displayVerticalSpacing = 5;
        this.decimalVerticalSpacing = 15;
    }

    private int[] timeDigits() {
        float time = GameTime.getInstance().getTimeOfDay();
        if (GameClient.client && GameClient.fastForward) {
            time = GameTime.getInstance().serverTimeOfDay;
        }

        if (!Core.getInstance().getOptionClock24Hour()) {
            if (time >= 13.0F) {
                time -= 12.0F;
            }

            if (time < 1.0F) {
                time += 12.0F;
            }
        }

        int hours = (int)time;
        float minutes = (time - (int)time) * 60.0F;
        int hourTens = hours / 10;
        int hourUnit = hours % 10;
        int minTens = (int)(minutes / 10.0F);
        return new int[]{hourTens, hourUnit, minTens, 0};
    }

    private int[] dateDigits() {
        int dayTens = (GameTime.getInstance().getDay() + 1) / 10;
        int dayUnit = (GameTime.getInstance().getDay() + 1) % 10;
        int monthTens = (GameTime.getInstance().getMonth() + 1) / 10;
        int monthUnit = (GameTime.getInstance().getMonth() + 1) % 10;
        return Core.getInstance().getOptionClockFormat() == 1
            ? new int[]{monthTens, monthUnit, dayTens, dayUnit}
            : new int[]{dayTens, dayUnit, monthTens, monthUnit};
    }

    private int[] tempDigits() {
        float temperature = ClimateManager.getInstance().getAirTemperatureForCharacter(this.clockPlayer, false);
        int negative = 0;
        int fahrenheit = 0;
        if (!Core.getInstance().getOptionTemperatureDisplayCelsius()) {
            temperature = temperature * 1.8F + 32.0F;
            fahrenheit = 1;
        }

        if (temperature < 0.0F) {
            negative = 1;
            temperature *= -1.0F;
        }

        int tempHund = (int)temperature / 100;
        int tempTens = (int)(temperature % 100.0F) / 10;
        int tempUnit = (int)temperature % 10;
        int tempDeci = (int)(temperature * 10.0F) % 10;
        return new int[]{negative, tempHund, tempTens, tempUnit, tempDeci, fahrenheit};
    }

    public void resize() {
        this.visible = false;
        this.digital = false;
        this.clockPlayer = null;
        this.isAlarmSet = false;
        this.isAlarmRinging = false;
        if (IsoPlayer.getInstance() != null) {
            for (int i = 0; i < IsoPlayer.numPlayers; i++) {
                IsoPlayer player = IsoPlayer.players[i];
                if (player != null && !player.isDead()) {
                    for (int j = 0; j < player.getWornItems().size(); j++) {
                        InventoryItem item = player.getWornItems().getItemByIndex(j);
                        if (item instanceof AlarmClock || item instanceof AlarmClockClothing) {
                            this.visible = UIManager.visibleAllUi;
                            this.digital = this.digital | item.hasTag(ItemTag.DIGITAL);
                            if (item instanceof AlarmClock alarmClock) {
                                if (alarmClock.isAlarmSet()) {
                                    this.isAlarmSet = true;
                                }

                                if (alarmClock.isRinging()) {
                                    this.isAlarmRinging = true;
                                }
                            } else {
                                if (((AlarmClockClothing)item).isAlarmSet()) {
                                    this.isAlarmSet = true;
                                }

                                if (((AlarmClockClothing)item).isRinging()) {
                                    this.isAlarmRinging = true;
                                }
                            }

                            this.clockPlayer = player;
                        }
                    }

                    if (this.clockPlayer != null) {
                        break;
                    }

                    ArrayList<InventoryItem> items = player.getInventory().getItems();

                    for (int jx = 0; jx < items.size(); jx++) {
                        InventoryItem item = items.get(jx);
                        if ((item instanceof AlarmClock || item instanceof AlarmClockClothing) && (item.isWorn() || item.isEquipped())) {
                            this.visible = UIManager.visibleAllUi;
                            this.digital = this.digital | item.hasTag(ItemTag.DIGITAL);
                            if (item instanceof AlarmClock alarmClock) {
                                if (alarmClock.isAlarmSet()) {
                                    this.isAlarmSet = true;
                                }

                                if (alarmClock.isRinging()) {
                                    this.isAlarmRinging = true;
                                }
                            } else {
                                if (((AlarmClockClothing)item).isAlarmSet()) {
                                    this.isAlarmSet = true;
                                }

                                if (((AlarmClockClothing)item).isRinging()) {
                                    this.isAlarmRinging = true;
                                }
                            }

                            this.clockPlayer = player;
                        }
                    }
                }
            }
        }

        if (DebugOptions.instance.cheat.clock.visible.getValue()) {
            this.digital = true;
            this.visible = UIManager.visibleAllUi;
        }

        if (this.background == null) {
            if (Core.getInstance().getOptionClockSize() == 2) {
                this.background = Texture.getSharedTexture("media/ui/ClockAssets/ClockLargeBackground.png");
            } else {
                this.background = Texture.getSharedTexture("media/ui/ClockAssets/ClockSmallBackground.png");
            }
        }

        this.setHeight(this.background.getHeight());
        this.setWidth(this.background.getWidth());
    }

    public boolean isDateVisible() {
        return this.visible && this.digital;
    }

    @Override
    public Boolean onMouseDown(double x, double y) {
        if (!this.isVisible()) {
            return false;
        } else {
            if (this.isAlarmRinging) {
                if (IsoPlayer.getInstance() != null) {
                    for (int i = 0; i < IsoPlayer.numPlayers; i++) {
                        IsoPlayer player = IsoPlayer.players[i];
                        if (player != null && !player.isDead()) {
                            for (int j = 0; j < player.getWornItems().size(); j++) {
                                InventoryItem item = player.getWornItems().getItemByIndex(j);
                                if (item instanceof AlarmClock alarmClock) {
                                    alarmClock.stopRinging();
                                } else if (item instanceof AlarmClockClothing alarmClockClothing) {
                                    alarmClockClothing.stopRinging();
                                }
                            }

                            for (int jx = 0; jx < player.getInventory().getItems().size(); jx++) {
                                InventoryItem inventoryItem = player.getInventory().getItems().get(jx);
                                if (inventoryItem instanceof AlarmClock alarmClock) {
                                    alarmClock.stopRinging();
                                } else if (inventoryItem instanceof AlarmClockClothing alarmClockClothing) {
                                    alarmClockClothing.stopRinging();
                                }
                            }
                        }
                    }
                }
            } else if (this.isAlarmSet) {
                if (IsoPlayer.getInstance() != null) {
                    for (int ix = 0; ix < IsoPlayer.numPlayers; ix++) {
                        IsoPlayer player = IsoPlayer.players[ix];
                        if (player != null && !player.isDead()) {
                            for (int jxx = 0; jxx < player.getWornItems().size(); jxx++) {
                                InventoryItem item = player.getWornItems().getItemByIndex(jxx);
                                if (item instanceof AlarmClock alarmClock && alarmClock.isAlarmSet()) {
                                    alarmClock.setAlarmSet(false);
                                } else if (item instanceof AlarmClockClothing alarmClockClothing && alarmClockClothing.isAlarmSet()) {
                                    alarmClockClothing.setAlarmSet(false);
                                }
                            }

                            for (int jxxx = 0; jxxx < player.getInventory().getItems().size(); jxxx++) {
                                InventoryItem inventoryItem = player.getInventory().getItems().get(jxxx);
                                if (inventoryItem instanceof AlarmClockClothing alarmClockClothing && alarmClockClothing.isAlarmSet()) {
                                    alarmClockClothing.setAlarmSet(false);
                                }

                                if (inventoryItem instanceof AlarmClock alarmClock && alarmClock.isAlarmSet()) {
                                    alarmClock.setAlarmSet(false);
                                }
                            }
                        }
                    }
                }
            } else if (IsoPlayer.getInstance() != null) {
                for (int ixx = 0; ixx < IsoPlayer.numPlayers; ixx++) {
                    IsoPlayer player = IsoPlayer.players[ixx];
                    if (player != null && !player.isDead()) {
                        for (int jxxx = 0; jxxx < player.getWornItems().size(); jxxx++) {
                            InventoryItem item = player.getWornItems().getItemByIndex(jxxx);
                            if (item instanceof AlarmClock alarmClock && alarmClock.isDigital() && !alarmClock.isAlarmSet()) {
                                alarmClock.setAlarmSet(true);
                                if (this.isAlarmSet) {
                                    return true;
                                }
                            }

                            if (item instanceof AlarmClockClothing alarmClockClothing && alarmClockClothing.isDigital() && !alarmClockClothing.isAlarmSet()) {
                                alarmClockClothing.setAlarmSet(true);
                                if (this.isAlarmSet) {
                                    return true;
                                }
                            }
                        }

                        for (int jxxx = 0; jxxx < player.getInventory().getItems().size(); jxxx++) {
                            InventoryItem inventoryItemx = player.getInventory().getItems().get(jxxx);
                            if (inventoryItemx instanceof AlarmClock alarmClockx && alarmClockx.isDigital() && !alarmClockx.isAlarmSet()) {
                                alarmClockx.setAlarmSet(true);
                                if (this.isAlarmSet) {
                                    return true;
                                }
                            }

                            if (inventoryItemx instanceof AlarmClockClothing alarmClockClothingx
                                && alarmClockClothingx.isDigital()
                                && !alarmClockClothingx.isAlarmSet()) {
                                alarmClockClothingx.setAlarmSet(true);
                                if (this.isAlarmSet) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }

            return true;
        }
    }
}
