// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ui;

import java.util.HashMap;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.iso.IsoUtils;
import zombie.world.Wind;

public class LoadingQueueUI extends UIElement {
    private final String strLoadingQueue;
    private final String strQueuePlace;
    private static int placeInQueue = -1;
    private static final HashMap<String, Object> serverInformation = new HashMap<>();
    private double timerServerInformationAnim;
    private Texture arrowBg;
    private Texture arrowFg;
    private final Texture[] moons = new Texture[8];
    private final Texture[] windsock = new Texture[6];
    private double timerMultiplierAnim;
    private int animOffset = -1;

    public LoadingQueueUI() {
        this.strLoadingQueue = Translator.getText("UI_GameLoad_LoadingQueue");
        this.strQueuePlace = Translator.getText("UI_GameLoad_PlaceInQueue");
        this.arrowBg = Texture.getSharedTexture("media/ui/ArrowRight_Disabled.png");
        this.arrowFg = Texture.getSharedTexture("media/ui/ArrowRight.png");

        for (int i = 0; i < 8; i++) {
            this.moons[i] = Texture.getSharedTexture("media/ui/queue/moonN" + (i + 1) + ".png");
        }

        for (int i = 0; i < 6; i++) {
            this.windsock[i] = Texture.getSharedTexture("media/ui/queue/windsock" + (i + 1) + ".png");
        }

        placeInQueue = -1;
        this.onresize();
    }

    @Override
    public void update() {
    }

    @Override
    public void onresize() {
        this.x = 288.0;
        this.y = 101.0;
        this.width = (float)(Core.getInstance().getScreenWidth() - 2.0 * this.x);
        this.height = (float)(Core.getInstance().getScreenHeight() - 2.0 * this.y);
    }

    @Override
    public void render() {
        this.onresize();
        double r = 0.4F;
        double g = 0.4F;
        double b = 0.4F;
        double a = 1.0;
        this.DrawTextureScaledColor(null, 0.0, 0.0, 1.0, (double)this.height, 0.4F, 0.4F, 0.4F, 1.0);
        this.DrawTextureScaledColor(null, 1.0, 0.0, this.width - 2.0, 1.0, 0.4F, 0.4F, 0.4F, 1.0);
        this.DrawTextureScaledColor(null, this.width - 1.0, 0.0, 1.0, (double)this.height, 0.4F, 0.4F, 0.4F, 1.0);
        this.DrawTextureScaledColor(null, 1.0, this.height - 1.0, this.width - 2.0, 1.0, 0.4F, 0.4F, 0.4F, 1.0);
        this.DrawTextureScaledColor(null, 1.0, 1.0, this.width - 2.0, (double)(this.height - 2.0F), 0.0, 0.0, 0.0, 0.5);
        TextManager.instance.DrawStringCentre(UIFont.Large, this.x + this.width / 2.0F, this.y + 60.0, this.strLoadingQueue, 1.0, 1.0, 1.0, 1.0);
        this.DrawTextureColor(this.arrowBg, (this.width - this.arrowBg.getWidth()) / 2.0F - 15.0F, 120.0, 1.0, 1.0, 1.0, 1.0);
        this.DrawTextureColor(this.arrowBg, (this.width - this.arrowBg.getWidth()) / 2.0F, 120.0, 1.0, 1.0, 1.0, 1.0);
        this.DrawTextureColor(this.arrowBg, (this.width - this.arrowBg.getWidth()) / 2.0F + 15.0F, 120.0, 1.0, 1.0, 1.0, 1.0);
        this.timerMultiplierAnim = this.timerMultiplierAnim + UIManager.getMillisSinceLastRender();
        if (this.timerMultiplierAnim <= 500.0) {
            this.animOffset = Integer.MIN_VALUE;
        } else if (this.timerMultiplierAnim <= 1000.0) {
            this.animOffset = -15;
        } else if (this.timerMultiplierAnim <= 1500.0) {
            this.animOffset = 0;
        } else if (this.timerMultiplierAnim <= 2000.0) {
            this.animOffset = 15;
        } else {
            this.timerMultiplierAnim = 0.0;
        }

        if (this.animOffset != Integer.MIN_VALUE) {
            this.DrawTextureColor(this.arrowFg, (this.width - this.arrowBg.getWidth()) / 2.0F + this.animOffset, 120.0, 1.0, 1.0, 1.0, 1.0);
        }

        if (placeInQueue >= 0) {
            TextManager.instance
                .DrawStringCentre(
                    UIFont.Medium, this.x + this.width / 2.0F, this.y + 180.0, String.format(this.strQueuePlace, placeInQueue), 1.0, 1.0, 1.0, 1.0
                );
        }

        if (serverInformation != null) {
            try {
                this.timerServerInformationAnim = this.timerServerInformationAnim + UIManager.getMillisSinceLastRender();
                if (this.timerServerInformationAnim / 40000.0 > 1.0) {
                    this.timerServerInformationAnim -= 40000.0;
                }

                float state1 = IsoUtils.smoothstep(0.0F, 2000.0F, (float)this.timerServerInformationAnim)
                    * IsoUtils.smoothstep(10000.0F, 8000.0F, (float)this.timerServerInformationAnim);
                float state2 = IsoUtils.smoothstep(10000.0F, 12000.0F, (float)this.timerServerInformationAnim)
                    * IsoUtils.smoothstep(20000.0F, 18000.0F, (float)this.timerServerInformationAnim);
                float state3 = IsoUtils.smoothstep(20000.0F, 22000.0F, (float)this.timerServerInformationAnim)
                    * IsoUtils.smoothstep(30000.0F, 28000.0F, (float)this.timerServerInformationAnim);
                float state4 = IsoUtils.smoothstep(30000.0F, 32000.0F, (float)this.timerServerInformationAnim)
                    * IsoUtils.smoothstep(40000.0F, 38000.0F, (float)this.timerServerInformationAnim);
                if (state1 > 0.0F) {
                    int y2 = 240;
                    float stageStepDelta = 0.2F;
                    float stageStep1 = 0.5F;
                    TextManager.instance
                        .DrawStringCentre(
                            UIFont.Medium,
                            this.x + this.width / 2.0F,
                            this.y + y2,
                            String.format(
                                Translator.getText("UI_GameLoad_PlayerPopulation"), serverInformation.get("countPlayers"), serverInformation.get("maxPlayers")
                            ),
                            1.0,
                            1.0,
                            1.0,
                            IsoUtils.smoothstep(stageStep1 - 0.2F, stageStep1, state1)
                        );
                    y2 += 30;
                    stageStep1 += 0.1F;
                    Integer zombieKilledToday = (Integer)serverInformation.get("ZombiesKilledToday");
                    if (zombieKilledToday == 0) {
                        TextManager.instance
                            .DrawStringCentre(
                                UIFont.Medium,
                                this.x + this.width / 2.0F,
                                this.y + y2,
                                Translator.getText("UI_GameLoad_zombieKilledToday0"),
                                1.0,
                                1.0,
                                1.0,
                                IsoUtils.smoothstep(stageStep1 - 0.2F, stageStep1, state1)
                            );
                    } else if (zombieKilledToday == 1) {
                        TextManager.instance
                            .DrawStringCentre(
                                UIFont.Medium,
                                this.x + this.width / 2.0F,
                                this.y + y2,
                                Translator.getText("UI_GameLoad_zombieKilledToday1"),
                                1.0,
                                1.0,
                                1.0,
                                IsoUtils.smoothstep(stageStep1 - 0.2F, stageStep1, state1)
                            );
                    } else {
                        TextManager.instance
                            .DrawStringCentre(
                                UIFont.Medium,
                                this.x + this.width / 2.0F,
                                this.y + y2,
                                String.format(Translator.getText("UI_GameLoad_zombieKilledTodayN"), zombieKilledToday),
                                1.0,
                                1.0,
                                1.0,
                                IsoUtils.smoothstep(stageStep1 - 0.2F, stageStep1, state1)
                            );
                    }

                    y2 += 30;
                    stageStep1 += 0.1F;
                    Integer zombifiedPlayersToday = (Integer)serverInformation.get("ZombifiedPlayersToday");
                    if (zombifiedPlayersToday == 0) {
                        TextManager.instance
                            .DrawStringCentre(
                                UIFont.Medium,
                                this.x + this.width / 2.0F,
                                this.y + y2,
                                Translator.getText("UI_GameLoad_zombifiedPlayersToday0"),
                                1.0,
                                1.0,
                                1.0,
                                IsoUtils.smoothstep(stageStep1 - 0.2F, stageStep1, state1)
                            );
                    } else if (zombifiedPlayersToday == 1) {
                        TextManager.instance
                            .DrawStringCentre(
                                UIFont.Medium,
                                this.x + this.width / 2.0F,
                                this.y + y2,
                                Translator.getText("UI_GameLoad_zombifiedPlayersToday1"),
                                1.0,
                                1.0,
                                1.0,
                                IsoUtils.smoothstep(stageStep1 - 0.2F, stageStep1, state1)
                            );
                    } else {
                        TextManager.instance
                            .DrawStringCentre(
                                UIFont.Medium,
                                this.x + this.width / 2.0F,
                                this.y + y2,
                                String.format(Translator.getText("UI_GameLoad_zombifiedPlayersTodayN"), zombifiedPlayersToday),
                                1.0,
                                1.0,
                                1.0,
                                IsoUtils.smoothstep(stageStep1 - 0.2F, stageStep1, state1)
                            );
                    }

                    y2 += 30;
                    stageStep1 += 0.1F;
                    Integer burnedZombiesToday = (Integer)serverInformation.get("BurnedCorpsesToday");
                    if (burnedZombiesToday == 0) {
                        TextManager.instance
                            .DrawStringCentre(
                                UIFont.Medium,
                                this.x + this.width / 2.0F,
                                this.y + y2,
                                Translator.getText("UI_GameLoad_burnedZombiesToday0"),
                                1.0,
                                1.0,
                                1.0,
                                IsoUtils.smoothstep(stageStep1 - 0.2F, stageStep1, state1)
                            );
                    } else if (burnedZombiesToday == 1) {
                        TextManager.instance
                            .DrawStringCentre(
                                UIFont.Medium,
                                this.x + this.width / 2.0F,
                                this.y + y2,
                                Translator.getText("UI_GameLoad_burnedZombiesToday1"),
                                1.0,
                                1.0,
                                1.0,
                                IsoUtils.smoothstep(stageStep1 - 0.2F, stageStep1, state1)
                            );
                    } else {
                        TextManager.instance
                            .DrawStringCentre(
                                UIFont.Medium,
                                this.x + this.width / 2.0F,
                                this.y + y2,
                                String.format(Translator.getText("UI_GameLoad_burnedZombiesTodayN"), burnedZombiesToday),
                                1.0,
                                1.0,
                                1.0,
                                IsoUtils.smoothstep(stageStep1 - 0.2F, stageStep1, state1)
                            );
                    }
                }

                if (state2 > 0.0F) {
                    float stageStepDeltax = 0.2F;
                    float stageStep1x = 0.5F;
                    int y2x = 240;
                    Byte timeHour = (Byte)serverInformation.get("Hour");
                    Byte timeMinute = (Byte)serverInformation.get("Minutes");
                    TextManager.instance
                        .DrawStringCentre(
                            UIFont.Medium,
                            this.x + this.width / 2.0F,
                            this.y + y2x,
                            String.format(Translator.getText("UI_GameLoad_time"), timeHour, timeMinute),
                            1.0,
                            1.0,
                            1.0,
                            IsoUtils.smoothstep(stageStep1x - 0.2F, stageStep1x, state2)
                        );
                    y2x += 30;
                    stageStep1x += 0.1F;
                    TextManager.instance
                        .DrawStringCentre(
                            UIFont.Medium,
                            this.x + this.width / 2.0F,
                            this.y + y2x,
                            String.format("%d/%d/%d", serverInformation.get("Month"), serverInformation.get("Day"), serverInformation.get("Year")),
                            1.0,
                            1.0,
                            1.0,
                            IsoUtils.smoothstep(stageStep1x - 0.2F, stageStep1x, state2)
                        );
                    y2x += 30;
                    stageStep1x += 0.1F;
                    TextManager.instance
                        .DrawStringCentre(
                            UIFont.Medium,
                            this.x + this.width / 2.0F,
                            this.y + y2x,
                            String.format(Translator.getText("UI_GameLoad_temperature"), serverInformation.get("Temperature")),
                            1.0,
                            1.0,
                            1.0,
                            IsoUtils.smoothstep(stageStep1x - 0.2F, stageStep1x, state2)
                        );
                    y2x += 30;
                    stageStep1x += 0.1F;
                    TextManager.instance
                        .DrawStringCentre(
                            UIFont.Medium,
                            this.x + this.width / 2.0F,
                            this.y + y2x,
                            String.format(Translator.getText("UI_GameLoad_humidity"), (Float)serverInformation.get("Humidity") * 100.0F),
                            1.0,
                            1.0,
                            1.0,
                            IsoUtils.smoothstep(stageStep1x - 0.2F, stageStep1x, state2)
                        );
                }

                if (state3 > 0.0F) {
                    float stageStepDeltax = 0.2F;
                    float stageStep1x = 0.5F;
                    int y2x = 240;
                    float windKph = (Float)serverInformation.get("WindspeedKph");
                    int windBeaufortNumber = Wind.getBeaufortNumber(windKph);
                    String windName = Wind.getName(windBeaufortNumber);
                    String windDescription = Wind.getDescription(windBeaufortNumber);
                    this.DrawTextureScaled(
                        this.windsock[Wind.getWindsockSegments(windKph)],
                        (this.width - 100.0F) / 2.0F,
                        y2x,
                        100.0,
                        100.0,
                        IsoUtils.smoothstep(stageStep1x - 0.2F, stageStep1x, state3)
                    );
                    y2x += 130;
                    stageStep1x += 0.1F;
                    TextManager.instance
                        .DrawStringCentre(
                            UIFont.Medium,
                            this.x + this.width / 2.0F,
                            this.y + y2x,
                            String.format(Translator.getText("UI_GameLoad_windSpeed"), windName, (int)Wind.getWindKnots(windKph), (int)windKph),
                            1.0,
                            1.0,
                            1.0,
                            IsoUtils.smoothstep(stageStep1x - 0.2F, stageStep1x, state3)
                        );
                    y2x += 30;
                    stageStep1x += 0.1F;
                    TextManager.instance
                        .DrawStringCentre(
                            UIFont.Medium,
                            this.x + this.width / 2.0F,
                            this.y + y2x,
                            windDescription,
                            1.0,
                            1.0,
                            1.0,
                            IsoUtils.smoothstep(stageStep1x - 0.2F, stageStep1x, state3)
                        );
                    y2x += 30;
                    stageStep1x += 0.1F;
                    float fog = (Float)serverInformation.get("Fog");
                    if (fog < 0.2) {
                        TextManager.instance
                            .DrawStringCentre(
                                UIFont.Medium,
                                this.x + this.width / 2.0F,
                                this.y + y2x,
                                Translator.getText("UI_GameLoad_fogNo"),
                                1.0,
                                1.0,
                                1.0,
                                IsoUtils.smoothstep(stageStep1x - 0.2F, stageStep1x, state3)
                            );
                    } else if (fog < 0.8) {
                        TextManager.instance
                            .DrawStringCentre(
                                UIFont.Medium,
                                this.x + this.width / 2.0F,
                                this.y + y2x,
                                Translator.getText("UI_GameLoad_fogMedium"),
                                1.0,
                                1.0,
                                1.0,
                                IsoUtils.smoothstep(stageStep1x - 0.2F, stageStep1x, state3)
                            );
                    } else {
                        TextManager.instance
                            .DrawStringCentre(
                                UIFont.Medium,
                                this.x + this.width / 2.0F,
                                this.y + y2x,
                                Translator.getText("UI_GameLoad_fogHeavy"),
                                1.0,
                                1.0,
                                1.0,
                                IsoUtils.smoothstep(stageStep1x - 0.2F, stageStep1x, state3)
                            );
                    }

                    y2x += 30;
                    stageStep1x += 0.1F;
                    TextManager.instance
                        .DrawStringCentre(
                            UIFont.Medium,
                            this.x + this.width / 2.0F,
                            this.y + y2x,
                            Translator.getText("UI_GameLoad_season" + serverInformation.get("SeasonId")),
                            1.0,
                            1.0,
                            1.0,
                            IsoUtils.smoothstep(stageStep1x - 0.2F, stageStep1x, state3)
                        );
                }

                if (state4 > 0.0F) {
                    float stageStepDeltax = 0.2F;
                    float stageStep1x = 0.5F;
                    this.DrawTextureScaled(
                        this.moons[Math.max(0, Math.min(7, (Byte)serverInformation.get("Moon")))],
                        (this.width - 100.0F) / 2.0F,
                        240.0,
                        100.0,
                        100.0,
                        IsoUtils.smoothstep(stageStep1x - 0.2F, stageStep1x, state4)
                    );
                    stageStep1x += 0.1F;
                    TextManager.instance
                        .DrawStringCentre(
                            UIFont.Medium,
                            this.x + this.width / 2.0F,
                            this.y + 240.0 + 30.0 + 100.0,
                            Translator.getText("UI_GameLoad_moon" + serverInformation.get("Moon")),
                            1.0,
                            1.0,
                            1.0,
                            IsoUtils.smoothstep(stageStep1x - 0.2F, stageStep1x, state4)
                        );
                }
            } catch (Exception var21) {
                DebugLog.General.printException(var21, "LoadingQueueUI render failed", LogSeverity.Error);
            }
        }
    }

    public void setPlaceInQueue(int placeInQueue) {
        LoadingQueueUI.placeInQueue = placeInQueue;
    }

    public void setServerInformation(HashMap<String, Object> serverInformation) {
        LoadingQueueUI.serverInformation.clear();
        LoadingQueueUI.serverInformation.putAll(serverInformation);
    }
}
