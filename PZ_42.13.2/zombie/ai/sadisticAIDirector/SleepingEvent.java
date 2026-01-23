// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.sadisticAIDirector;

import zombie.GameTime;
import zombie.GameWindow;
import zombie.SandboxOptions;
import zombie.SoundManager;
import zombie.UsedFromLua;
import zombie.VirtualZombieManager;
import zombie.WorldSoundManager;
import zombie.ZombieSpawnRecorder;
import zombie.characters.CharacterStat;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.BodyDamage.BodyPart;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoRadio;
import zombie.iso.objects.IsoStove;
import zombie.iso.objects.IsoTelevision;
import zombie.iso.objects.IsoWindow;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameClient;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.MoodleType;
import zombie.ui.UIManager;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public final class SleepingEvent {
    public static final SleepingEvent instance = new SleepingEvent();
    public static boolean zombiesInvasion;

    public void setPlayerFallAsleep(IsoPlayer chr, int sleepingTime) {
        this.setPlayerFallAsleep(chr, sleepingTime, false, false);
    }

    public void setPlayerFallAsleep(IsoPlayer chr, int sleepingTime, boolean forceZombieEvent, boolean forceNightmareEvent) {
        SleepingEventData data = chr.getOrCreateSleepingEventData();
        data.reset();
        if (ClimateManager.getInstance().isRaining() && this.isExposedToPrecipitation(chr)) {
            data.raining = true;
            data.wasRainingAtStart = true;
            data.rainTimeStartHours = GameTime.getInstance().getWorldAgeHours();
        }

        data.sleepingTime = sleepingTime;
        chr.setTimeOfSleep(GameTime.instance.getTimeOfDay());
        this.doDelayToSleep(chr);
        this.checkNightmare(chr, sleepingTime);
        if (forceNightmareEvent) {
            Rand.Next(3, sleepingTime - 2);
        }

        if (data.nightmareWakeUp <= -1) {
            if (SandboxOptions.instance.sleepingEvent.getValue() != 1 && zombiesInvasion || forceZombieEvent) {
                if (chr.getCurrentSquare() == null || chr.getCurrentSquare().getZone() == null || !chr.getCurrentSquare().getZone().haveConstruction) {
                    boolean sleepDuringNight = false;
                    if ((GameTime.instance.getHour() >= 0 && GameTime.instance.getHour() < 5 || GameTime.instance.getHour() > 18) && sleepingTime >= 4) {
                        sleepDuringNight = true;
                    }

                    int baseChance = 20;
                    if (SandboxOptions.instance.sleepingEvent.getValue() == 3) {
                        baseChance = 45;
                    }

                    if (forceZombieEvent || Rand.Next(100) <= baseChance && !chr.getCell().getZombieList().isEmpty() && sleepingTime >= 4) {
                        int zombieIntrudersChance = 0;
                        if (chr.getCurrentBuilding() != null) {
                            if (!forceZombieEvent) {
                                IsoGridSquare sq = null;
                                IsoWindow window = null;

                                for (int z = 0; z < 3; z++) {
                                    for (int x = chr.getCurrentBuilding().getDef().getX() - 2; x < chr.getCurrentBuilding().getDef().getX2() + 2; x++) {
                                        for (int y = chr.getCurrentBuilding().getDef().getY() - 2; y < chr.getCurrentBuilding().getDef().getY2() + 2; y++) {
                                            sq = IsoWorld.instance.getCell().getGridSquare(x, y, z);
                                            if (sq != null) {
                                                boolean electricity = sq.haveElectricity() || sq.hasGridPower();
                                                if (electricity) {
                                                    for (int i = 0; i < sq.getObjects().size(); i++) {
                                                        IsoObject tileObject = sq.getObjects().get(i);
                                                        if (tileObject.getContainer() != null
                                                            && (
                                                                tileObject.getContainer().getType().equals("fridge")
                                                                    || tileObject.getContainer().getType().equals("freezer")
                                                            )) {
                                                            zombieIntrudersChance += 3;
                                                        }

                                                        if (tileObject instanceof IsoStove isoStove && isoStove.Activated()) {
                                                            zombieIntrudersChance += 5;
                                                        }

                                                        if (tileObject instanceof IsoTelevision isoTelevision && isoTelevision.getDeviceData().getIsTurnedOn()) {
                                                            zombieIntrudersChance += 30;
                                                        }

                                                        if (tileObject instanceof IsoRadio isoRadio && isoRadio.getDeviceData().getIsTurnedOn()) {
                                                            zombieIntrudersChance += 30;
                                                        }
                                                    }
                                                }

                                                window = sq.getWindow();
                                                if (window != null) {
                                                    zombieIntrudersChance += this.checkWindowStatus(window);
                                                }

                                                IsoDoor door = sq.getIsoDoor();
                                                if (door != null && door.isExterior() && door.IsOpen()) {
                                                    zombieIntrudersChance += 25;
                                                    data.openDoor = door;
                                                }
                                            }
                                        }
                                    }
                                }

                                if (SandboxOptions.instance.sleepingEvent.getValue() == 3) {
                                    zombieIntrudersChance = (int)(zombieIntrudersChance * 1.5);
                                }

                                if (zombieIntrudersChance > 70) {
                                    zombieIntrudersChance = 70;
                                }

                                if (!sleepDuringNight) {
                                    zombieIntrudersChance /= 2;
                                }
                            }

                            if (forceZombieEvent || Rand.Next(100) <= zombieIntrudersChance) {
                                data.forceWakeUpTime = Rand.Next(sleepingTime - 4, sleepingTime - 1);
                                data.zombiesIntruders = true;
                            }
                        }
                    }
                }
            }
        }
    }

    private void doDelayToSleep(IsoPlayer chr) {
        float delay = 0.3F;
        float maxDelay = 2.0F;
        if (chr.hasTrait(CharacterTrait.INSOMNIAC)) {
            delay = 1.0F;
        }

        if (chr.getMoodles().getMoodleLevel(MoodleType.PAIN) > 0) {
            delay += 1.0F + chr.getMoodles().getMoodleLevel(MoodleType.PAIN) * 0.2F;
        }

        if (chr.getMoodles().getMoodleLevel(MoodleType.STRESS) > 0) {
            delay *= 1.2F;
        }

        if (chr.getBedType().contains("averageBedPillow")) {
            delay *= 1.0F;
        }

        if ("badBed".equals(chr.getBedType())) {
            delay *= 1.3F;
        } else if (chr.getBedType().contains("badBedPillow")) {
            delay *= 1.25F;
        } else if ("goodBed".equals(chr.getBedType())) {
            delay *= 0.8F;
        } else if (chr.getBedType().contains("goodBedPillow")) {
            delay *= 0.6F;
        } else if ("floor".equals(chr.getBedType())) {
            delay *= 1.6F;
        } else if ("floorPillow".equals(chr.getBedType())) {
            delay *= 1.45F;
        }

        if (chr.hasTrait(CharacterTrait.NIGHT_OWL)) {
            delay *= 0.5F;
        }

        if (chr.getSleepingTabletEffect() > 1000.0F) {
            delay = 0.1F;
        }

        if (delay > 2.0F) {
            delay = 2.0F;
        }

        float finalDelay = Rand.Next(0.0F, delay);
        chr.setDelayToSleep(GameTime.instance.getTimeOfDay() + finalDelay);
    }

    private void checkNightmare(IsoPlayer chr, int sleepingTime) {
        if (!GameClient.client) {
            SleepingEventData data = chr.getOrCreateSleepingEventData();
            if (sleepingTime >= 3) {
                int baseChance = 5;
                if (chr.hasTrait(CharacterTrait.DESENSITIZED)) {
                    baseChance += 5;
                }

                baseChance += chr.getMoodles().getMoodleLevel(MoodleType.STRESS) * 10;
                if (Rand.Next(100) < baseChance) {
                    data.nightmareWakeUp = Rand.Next(3, sleepingTime - 2);
                }
            }
        }
    }

    private int checkWindowStatus(IsoWindow window) {
        IsoGridSquare sq = window.getSquare();
        if (window.getSquare().getRoom() == null) {
            if (!window.isNorth()) {
                sq = window.getSquare().getCell().getGridSquare(window.getSquare().getX() - 1, window.getSquare().getY(), window.getSquare().getZ());
            } else {
                sq = window.getSquare().getCell().getGridSquare(window.getSquare().getX(), window.getSquare().getY() - 1, window.getSquare().getZ());
            }
        }

        int chance = 0;
        boolean lightsOn = false;

        for (int i = 0; i < sq.getRoom().lightSwitches.size(); i++) {
            if (sq.getRoom().lightSwitches.get(i).isActivated()) {
                lightsOn = true;
                break;
            }
        }

        if (lightsOn) {
            int var7 = 20;
            if (window.HasCurtains() != null && !window.HasCurtains().open) {
                var7 -= 17;
            }

            IsoBarricade barricade = window.getBarricadeOnOppositeSquare();
            if (barricade == null) {
                barricade = window.getBarricadeOnSameSquare();
            }

            if (barricade != null && (barricade.getNumPlanks() > 4 || barricade.isMetal())) {
                var7 -= 20;
            }

            if (var7 < 0) {
                var7 = 0;
            }

            if (sq.getZ() > 0) {
                var7 /= 2;
            }

            return var7;
        } else {
            int var6 = 5;
            if (window.HasCurtains() != null && !window.HasCurtains().open) {
                var6 -= 5;
            }

            IsoBarricade barricadex = window.getBarricadeOnOppositeSquare();
            if (barricadex == null) {
                barricadex = window.getBarricadeOnSameSquare();
            }

            if (barricadex != null && (barricadex.getNumPlanks() > 3 || barricadex.isMetal())) {
                var6 -= 5;
            }

            if (var6 < 0) {
                var6 = 0;
            }

            if (sq.getZ() > 0) {
                var6 /= 2;
            }

            return var6;
        }
    }

    public void update(IsoPlayer chr) {
        if (chr != null) {
            SleepingEventData data = chr.getOrCreateSleepingEventData();
            if (chr.getStats().getNumVeryCloseZombies() > 0) {
                chr.getStats().add(CharacterStat.PANIC, 70.0F);
                chr.getStats().add(CharacterStat.STRESS, 0.5F);
                WorldSoundManager.instance.addSound(chr, PZMath.fastfloor(chr.getX()), PZMath.fastfloor(chr.getY()), PZMath.fastfloor(chr.getZ()), 6, 1);
                SoundManager.instance.setMusicWakeState(chr, "WakeZombies");
                data.fastWakeup = true;
                this.wakeUp(chr);
            }

            if (data.nightmareWakeUp == (int)chr.getAsleepTime()) {
                chr.getStats().add(CharacterStat.PANIC, 70.0F);
                chr.getStats().add(CharacterStat.STRESS, 0.5F);
                WorldSoundManager.instance.addSound(chr, PZMath.fastfloor(chr.getX()), PZMath.fastfloor(chr.getY()), PZMath.fastfloor(chr.getZ()), 6, 1);
                SoundManager.instance.setMusicWakeState(chr, "WakeNightmare");
                data.fastWakeup = true;
                this.wakeUp(chr);
            }

            if (data.forceWakeUpTime == (int)chr.getAsleepTime() && data.zombiesIntruders) {
                this.spawnZombieIntruders(chr);
                WorldSoundManager.instance.addSound(chr, PZMath.fastfloor(chr.getX()), PZMath.fastfloor(chr.getY()), PZMath.fastfloor(chr.getZ()), 6, 1);
                SoundManager.instance.setMusicWakeState(chr, "WakeZombies");
                data.fastWakeup = true;
                this.wakeUp(chr);
            }

            this.updateRain(chr);
            this.updateSnow(chr);
            this.updateTemperature(chr);
            this.updateWetness(chr);
        }
    }

    private void updateRain(IsoPlayer chr) {
        SleepingEventData data = chr.getOrCreateSleepingEventData();
        if (!ClimateManager.getInstance().isRaining()) {
            data.raining = false;
            data.wasRainingAtStart = false;
            data.rainTimeStartHours = -1.0;
        } else if (this.isExposedToPrecipitation(chr)) {
            double worldAgeHours = GameTime.getInstance().getWorldAgeHours();
            if (!data.wasRainingAtStart) {
                if (!data.raining) {
                    data.rainTimeStartHours = worldAgeHours;
                }

                if (data.getHoursSinceRainStarted() >= 0.16666666666666666) {
                }
            }

            data.raining = true;
        }
    }

    private void updateSnow(IsoPlayer chr) {
        if (ClimateManager.getInstance().isSnowing()) {
            if (this.isExposedToPrecipitation(chr)) {
                ;
            }
        }
    }

    private void updateTemperature(IsoPlayer chr) {
    }

    private void updateWetness(IsoPlayer chr) {
    }

    private boolean isExposedToPrecipitation(IsoGameCharacter chr) {
        if (chr.getCurrentSquare() == null) {
            return false;
        } else if (!chr.getCurrentSquare().isInARoom() && !chr.getCurrentSquare().haveRoof) {
            if (chr.getBed() == null || !chr.getBed().isTent() && !"Tent".equals(chr.getBed().getName())) {
                BaseVehicle vehicle = chr.getVehicle();
                return vehicle == null || !vehicle.hasRoof(vehicle.getSeat(chr));
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private void spawnZombieIntruders(IsoPlayer chr) {
        SleepingEventData data = chr.getOrCreateSleepingEventData();
        IsoGridSquare sq = null;
        if (data.openDoor != null) {
            sq = data.openDoor.getSquare();
        } else {
            data.weakestWindow = this.getWeakestWindow(chr);
            if (data.weakestWindow != null && data.weakestWindow.getZ() == 0.0F) {
                if (!data.weakestWindow.isNorth()) {
                    if (data.weakestWindow.getSquare().getRoom() == null) {
                        sq = data.weakestWindow.getSquare();
                    } else {
                        sq = data.weakestWindow
                            .getSquare()
                            .getCell()
                            .getGridSquare(
                                data.weakestWindow.getSquare().getX() - 1, data.weakestWindow.getSquare().getY(), data.weakestWindow.getSquare().getZ()
                            );
                    }
                } else if (data.weakestWindow.getSquare().getRoom() == null) {
                    sq = data.weakestWindow.getSquare();
                } else {
                    sq = data.weakestWindow
                        .getSquare()
                        .getCell()
                        .getGridSquare(data.weakestWindow.getSquare().getX(), data.weakestWindow.getSquare().getY() + 1, data.weakestWindow.getSquare().getZ());
                }

                IsoBarricade barricade = data.weakestWindow.getBarricadeOnOppositeSquare();
                if (barricade == null) {
                    barricade = data.weakestWindow.getBarricadeOnSameSquare();
                }

                if (barricade != null) {
                    barricade.Damage(Rand.Next(500, 900));
                } else {
                    data.weakestWindow.Damage(200.0F);
                    data.weakestWindow.smashWindow();
                    if (data.weakestWindow.HasCurtains() != null) {
                        data.weakestWindow.removeSheet(null);
                    }

                    if (sq != null) {
                        sq.addBrokenGlass();
                    }
                }
            }
        }

        chr.getStats().add(CharacterStat.PANIC, Rand.Next(30, 60));
        if (sq != null) {
            if (IsoWorld.getZombiesEnabled()) {
                int numZombies = Rand.Next(3) + 1;

                for (int i = 0; i < numZombies; i++) {
                    VirtualZombieManager.instance.choices.clear();
                    VirtualZombieManager.instance.choices.add(sq);
                    IsoZombie zed = VirtualZombieManager.instance.createRealZombieAlways(IsoDirections.fromIndex(Rand.Next(8)).index(), false);
                    if (zed != null) {
                        zed.setTarget(chr);
                        zed.pathToCharacter(chr);
                        zed.spotted(chr, true);
                        ZombieSpawnRecorder.instance.record(zed, this.getClass().getSimpleName());
                    }
                }
            }
        }
    }

    private IsoWindow getWeakestWindow(IsoPlayer chr) {
        IsoGridSquare sq = null;
        IsoWindow window = null;
        IsoWindow weakestWindow = null;
        int currentWindowPower = 0;

        for (int x = chr.getCurrentBuilding().getDef().getX() - 2; x < chr.getCurrentBuilding().getDef().getX2() + 2; x++) {
            for (int y = chr.getCurrentBuilding().getDef().getY() - 2; y < chr.getCurrentBuilding().getDef().getY2() + 2; y++) {
                sq = IsoWorld.instance.getCell().getGridSquare(x, y, 0);
                if (sq != null) {
                    window = sq.getWindow();
                    if (window != null) {
                        int windowPower = this.checkWindowStatus(window);
                        if (windowPower > currentWindowPower) {
                            currentWindowPower = windowPower;
                            weakestWindow = window;
                        }
                    }
                }
            }
        }

        return weakestWindow;
    }

    public void wakeUp(IsoGameCharacter chr) {
        if (chr != null) {
            this.wakeUp(chr, false);
        }
    }

    public void wakeUp(IsoGameCharacter chr, boolean remote) {
        SleepingEventData data = chr.getOrCreateSleepingEventData();
        if (GameClient.client && !remote) {
            INetworkPacket.send(PacketTypes.PacketType.WakeUpPlayer, chr);
        }

        boolean doSave = false;
        IsoPlayer player = Type.tryCastTo(chr, IsoPlayer.class);
        if (player != null && player.isLocalPlayer()) {
            UIManager.setFadeBeforeUI(player.getPlayerNum(), true);
            UIManager.FadeIn(player.getPlayerNum(), data.fastWakeup ? 0.5 : 2.0);
            if (!GameClient.client && IsoPlayer.allPlayersAsleep()) {
                UIManager.getSpeedControls().SetCurrentGameSpeed(1);
                doSave = true;
            }

            chr.setLastHourSleeped((int)player.getHoursSurvived());
        }

        chr.setForceWakeUpTime(-1.0F);
        chr.setAsleep(false);
        if (doSave) {
            try {
                GameWindow.save(true);
            } catch (Throwable var8) {
                ExceptionLogger.logException(var8);
            }
        }

        BodyPart neck = chr.getBodyDamage().getBodyPart(BodyPartType.Neck);
        float sleepingTimeDelta = data.sleepingTime / 8.0F;
        if ("goodBed".equals(chr.getBedType()) || "goodBedPillow".equals(chr.getBedType())) {
            chr.getStats().remove(CharacterStat.FATIGUE, Rand.Next(0.05F, 0.12F) * sleepingTimeDelta);
        } else if ("badBed".equals(chr.getBedType())) {
            chr.getStats().add(CharacterStat.FATIGUE, Rand.Next(0.1F, 0.2F) * sleepingTimeDelta);
            if (Rand.Next(5) == 0) {
                neck.AddDamage(Rand.Next(5.0F, 15.0F));
                neck.setAdditionalPain(neck.getAdditionalPain() + Rand.Next(30.0F, 50.0F));
            }
        } else if ("badBedPillow".equals(chr.getBedType())) {
            chr.getStats().add(CharacterStat.FATIGUE, Rand.Next(0.1F, 0.2F) * sleepingTimeDelta);
            if (Rand.Next(10) == 0) {
                neck.AddDamage(Rand.Next(2.5F, 7.5F));
                neck.setAdditionalPain(neck.getAdditionalPain() + Rand.Next(15.0F, 25.0F));
            }
        } else if ("floor".equals(chr.getBedType())) {
            chr.getStats().add(CharacterStat.FATIGUE, Rand.Next(0.15F, 0.25F) * sleepingTimeDelta);
            if (Rand.Next(5) == 0) {
                neck.AddDamage(Rand.Next(10.0F, 20.0F));
                neck.setAdditionalPain(neck.getAdditionalPain() + Rand.Next(30.0F, 50.0F));
            }
        } else if ("floorPillow".equals(chr.getBedType())) {
            chr.getStats().add(CharacterStat.FATIGUE, Rand.Next(0.15F, 0.25F) * sleepingTimeDelta);
            if (Rand.Next(10) == 0) {
                neck.AddDamage(Rand.Next(5.0F, 10.0F));
                neck.setAdditionalPain(neck.getAdditionalPain() + Rand.Next(15.0F, 25.0F));
            }
        } else if ("averageBedPillow".equals(chr.getBedType())) {
            if (Rand.Next(20) == 0) {
                neck.AddDamage(Rand.Next(1.5F, 6.0F));
                neck.setAdditionalPain(neck.getAdditionalPain() + Rand.Next(5.0F, 15.0F));
            }
        } else if (Rand.Next(10) == 0) {
            neck.AddDamage(Rand.Next(3.0F, 12.0F));
            neck.setAdditionalPain(neck.getAdditionalPain() + Rand.Next(10.0F, 30.0F));
        }

        data.reset();
    }
}
