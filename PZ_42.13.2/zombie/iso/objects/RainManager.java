// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects;

import fmod.fmod.Audio;
import java.util.ArrayList;
import java.util.Stack;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.Lua.LuaEventManager;
import zombie.characters.IsoPlayer;
import zombie.core.PerformanceSettings;
import zombie.core.random.Rand;
import zombie.core.textures.ColorInfo;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameServer;

@UsedFromLua
public class RainManager {
    private static boolean isRaining;
    public static int numActiveRainSplashes;
    public static int numActiveRaindrops;
    public static int maxRainSplashObjects = 500;
    public static int maxRaindropObjects = 500;
    public static float rainSplashAnimDelay = 0.2F;
    public static int addNewSplashesDelay = 30;
    public static int addNewSplashesTimer = addNewSplashesDelay;
    public static float raindropGravity = 0.065F;
    public static float gravModMin = 0.28F;
    public static float gravModMax = 0.5F;
    public static float raindropStartDistance = 850.0F;
    public static IsoGridSquare[] playerLocation = new IsoGridSquare[4];
    public static IsoGridSquare[] playerOldLocation = new IsoGridSquare[4];
    public static boolean playerMoved = true;
    public static int rainRadius = 18;
    public static Audio rainAmbient;
    public static Audio thunderAmbient;
    public static ColorInfo rainSplashTintMod = new ColorInfo(0.8F, 0.9F, 1.0F, 0.3F);
    public static ColorInfo raindropTintMod = new ColorInfo(0.8F, 0.9F, 1.0F, 0.3F);
    public static ColorInfo darkRaindropTintMod = new ColorInfo(0.8F, 0.9F, 1.0F, 0.3F);
    public static ArrayList<IsoRainSplash> rainSplashStack = new ArrayList<>(1600);
    public static ArrayList<IsoRaindrop> raindropStack = new ArrayList<>(1600);
    public static Stack<IsoRainSplash> rainSplashReuseStack = new Stack<>();
    public static Stack<IsoRaindrop> raindropReuseStack = new Stack<>();
    private static float rainChangeTimer = 1.0F;
    private static float rainChangeRate = 0.01F;
    private static final float RainChangeRateMin = 0.006F;
    private static final float RainChangeRateMax = 0.01F;
    public static float rainIntensity = 1.0F;
    public static float rainDesiredIntensity = 1.0F;
    private static int randRain;
    public static int randRainMin;
    public static int randRainMax;
    private static boolean stopRain;
    static Audio outsideAmbient;
    static Audio outsideNightAmbient;
    static ColorInfo adjustedRainSplashTintMod = new ColorInfo();

    public static void reset() {
        rainSplashStack.clear();
        raindropStack.clear();
        raindropReuseStack.clear();
        rainSplashReuseStack.clear();
        numActiveRainSplashes = 0;
        numActiveRaindrops = 0;

        for (int i = 0; i < 4; i++) {
            playerLocation[i] = null;
            playerOldLocation[i] = null;
        }

        rainAmbient = null;
        thunderAmbient = null;
        isRaining = false;
        stopRain = false;
    }

    public static void AddRaindrop(IsoRaindrop NewRaindrop) {
        if (numActiveRaindrops < maxRaindropObjects) {
            raindropStack.add(NewRaindrop);
            numActiveRaindrops++;
        } else {
            IsoRaindrop OldestRaindropObject = null;
            int OldestAge = -1;

            for (int i = 0; i < raindropStack.size(); i++) {
                if (raindropStack.get(i).life > OldestAge) {
                    OldestAge = raindropStack.get(i).life;
                    OldestRaindropObject = raindropStack.get(i);
                }
            }

            if (OldestRaindropObject != null) {
                RemoveRaindrop(OldestRaindropObject);
                raindropStack.add(NewRaindrop);
                numActiveRaindrops++;
            }
        }
    }

    public static void AddRainSplash(IsoRainSplash NewRainSplash) {
        if (numActiveRainSplashes < maxRainSplashObjects) {
            rainSplashStack.add(NewRainSplash);
            numActiveRainSplashes++;
        } else {
            IsoRainSplash OldestRainSplashObject = null;
            int OldestAge = -1;

            for (int i = 0; i < rainSplashStack.size(); i++) {
                if (rainSplashStack.get(i).age > OldestAge) {
                    OldestAge = rainSplashStack.get(i).age;
                    OldestRainSplashObject = rainSplashStack.get(i);
                }
            }

            RemoveRainSplash(OldestRainSplashObject);
            rainSplashStack.add(NewRainSplash);
            numActiveRainSplashes++;
        }
    }

    public static void AddSplashes() {
        if (addNewSplashesTimer > 0) {
            addNewSplashesTimer--;
        } else {
            addNewSplashesTimer = (int)(addNewSplashesDelay * (PerformanceSettings.getLockFPS() / 30.0F));
            IsoGridSquare NewSquare = null;
            if (!stopRain) {
                if (playerMoved) {
                    for (int i = rainSplashStack.size() - 1; i >= 0; i--) {
                        IsoRainSplash rainsplash = rainSplashStack.get(i);
                        if (!inBounds(rainsplash.square)) {
                            RemoveRainSplash(rainsplash);
                        }
                    }

                    for (int ix = raindropStack.size() - 1; ix >= 0; ix--) {
                        IsoRaindrop raindrop = raindropStack.get(ix);
                        if (!inBounds(raindrop.square)) {
                            RemoveRaindrop(raindrop);
                        }
                    }
                }

                int numPlayers = 0;

                for (int ixx = 0; ixx < IsoPlayer.numPlayers; ixx++) {
                    if (IsoPlayer.players[ixx] != null) {
                        numPlayers++;
                    }
                }

                int area = rainRadius * 2 * rainRadius * 2;
                int count = area / (randRain + 1);
                count = Math.min(maxRainSplashObjects, count);

                while (numActiveRainSplashes > count * numPlayers) {
                    RemoveRainSplash(rainSplashStack.get(0));
                }

                while (numActiveRaindrops > count * numPlayers) {
                    RemoveRaindrop(raindropStack.get(0));
                }

                IsoCell cell = IsoWorld.instance.currentCell;

                for (int p = 0; p < IsoPlayer.numPlayers; p++) {
                    if (IsoPlayer.players[p] != null && playerLocation[p] != null) {
                        for (int ixxx = 0; ixxx < count; ixxx++) {
                            int x = Rand.Next(-rainRadius, rainRadius);
                            int y = Rand.Next(-rainRadius, rainRadius);
                            NewSquare = cell.getGridSquare(playerLocation[p].getX() + x, playerLocation[p].getY() + y, 0);
                            if (NewSquare != null
                                && NewSquare.isSeen(p)
                                && !NewSquare.getProperties().has(IsoFlagType.vegitation)
                                && NewSquare.getProperties().has(IsoFlagType.exterior)) {
                                StartRainSplash(cell, NewSquare, true);
                            }
                        }
                    }
                }
            }

            playerMoved = false;
            if (!stopRain) {
                randRain--;
                if (randRain < randRainMin) {
                    randRain = randRainMin;
                }
            } else {
                randRain = (int)(randRain - 1.0F * GameTime.instance.getMultiplier());
                if (randRain < randRainMin) {
                    removeAll();
                    randRain = randRainMin;
                } else {
                    for (int ixxxx = rainSplashStack.size() - 1; ixxxx >= 0; ixxxx--) {
                        if (Rand.Next(randRain) == 0) {
                            IsoRainSplash rainsplash = rainSplashStack.get(ixxxx);
                            RemoveRainSplash(rainsplash);
                        }
                    }

                    for (int ixxxxx = raindropStack.size() - 1; ixxxxx >= 0; ixxxxx--) {
                        if (Rand.Next(randRain) == 0) {
                            IsoRaindrop raindrop = raindropStack.get(ixxxxx);
                            RemoveRaindrop(raindrop);
                        }
                    }
                }
            }
        }
    }

    public static void RemoveRaindrop(IsoRaindrop DyingRaindrop) {
        if (DyingRaindrop.square != null) {
            DyingRaindrop.square.getProperties().unset(IsoFlagType.HasRaindrop);
            DyingRaindrop.square.setRainDrop(null);
            DyingRaindrop.square = null;
        }

        raindropStack.remove(DyingRaindrop);
        numActiveRaindrops--;
        raindropReuseStack.push(DyingRaindrop);
    }

    public static void RemoveRainSplash(IsoRainSplash DyingRainSplash) {
        if (DyingRainSplash.square != null) {
            DyingRainSplash.square.getProperties().unset(IsoFlagType.HasRainSplashes);
            DyingRainSplash.square.setRainSplash(null);
            DyingRainSplash.square = null;
        }

        rainSplashStack.remove(DyingRainSplash);
        numActiveRainSplashes--;
        rainSplashReuseStack.push(DyingRainSplash);
    }

    public static void SetPlayerLocation(int playerIndex, IsoGridSquare PlayerCurrentSquare) {
        playerOldLocation[playerIndex] = playerLocation[playerIndex];
        playerLocation[playerIndex] = PlayerCurrentSquare;
        if (playerOldLocation[playerIndex] != playerLocation[playerIndex]) {
            playerMoved = true;
        }
    }

    public static Boolean isRaining() {
        return ClimateManager.getInstance().isRaining();
    }

    public static void stopRaining() {
        stopRain = true;
        randRain = randRainMax;
        rainDesiredIntensity = 0.0F;
        if (GameServer.server) {
            GameServer.stopRain();
        }

        LuaEventManager.triggerEvent("OnRainStop");
    }

    public static void startRaining() {
    }

    public static void StartRaindrop(IsoCell cell, IsoGridSquare gridSquare, boolean CanSee) {
        if (!gridSquare.getProperties().has(IsoFlagType.HasRaindrop)) {
            IsoRaindrop NewRaindrop = null;
            if (!raindropReuseStack.isEmpty()) {
                if (CanSee) {
                    if (gridSquare.getRainDrop() != null) {
                        return;
                    }

                    NewRaindrop = raindropReuseStack.pop();
                    NewRaindrop.Reset(gridSquare, CanSee);
                    gridSquare.setRainDrop(NewRaindrop);
                }
            } else if (CanSee) {
                if (gridSquare.getRainDrop() != null) {
                    return;
                }

                NewRaindrop = new IsoRaindrop(cell, gridSquare, CanSee);
                gridSquare.setRainDrop(NewRaindrop);
            }
        }
    }

    public static void StartRainSplash(IsoCell cell, IsoGridSquare gridSquare, boolean CanSee) {
    }

    public static void Update() {
        isRaining = ClimateManager.getInstance().isRaining();
        rainIntensity = isRaining ? ClimateManager.getInstance().getPrecipitationIntensity() : 0.0F;
    }

    public static void UpdateServer() {
    }

    public static void setRandRainMax(int pRandRainMax) {
        randRainMax = pRandRainMax;
        randRain = randRainMax;
    }

    public static void setRandRainMin(int pRandRainMin) {
        randRainMin = pRandRainMin;
    }

    public static boolean inBounds(IsoGridSquare sq) {
        if (sq == null) {
            return false;
        } else {
            for (int i = 0; i < IsoPlayer.numPlayers; i++) {
                IsoPlayer player = IsoPlayer.players[i];
                if (player != null && playerLocation[i] != null) {
                    if (sq.getX() < playerLocation[i].getX() - rainRadius || sq.getX() >= playerLocation[i].getX() + rainRadius) {
                        return true;
                    }

                    if (sq.getY() < playerLocation[i].getY() - rainRadius || sq.getY() >= playerLocation[i].getY() + rainRadius) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    public static void RemoveAllOn(IsoGridSquare sq) {
        if (sq.getRainDrop() != null) {
            RemoveRaindrop(sq.getRainDrop());
        }

        if (sq.getRainSplash() != null) {
            RemoveRainSplash(sq.getRainSplash());
        }
    }

    public static float getRainIntensity() {
        return ClimateManager.getInstance().getPrecipitationIntensity();
    }

    private static void removeAll() {
        for (int i = rainSplashStack.size() - 1; i >= 0; i--) {
            IsoRainSplash rainsplash = rainSplashStack.get(i);
            RemoveRainSplash(rainsplash);
        }

        for (int i = raindropStack.size() - 1; i >= 0; i--) {
            IsoRaindrop raindrop = raindropStack.get(i);
            RemoveRaindrop(raindrop);
        }

        raindropStack.clear();
        rainSplashStack.clear();
        numActiveRainSplashes = 0;
        numActiveRaindrops = 0;
    }

    private static boolean interruptSleep(IsoPlayer ply) {
        if (ply.isAsleep() && ply.isOutside() && ply.getBed() != null && !ply.getBed().isTent()) {
            IsoObject bed = ply.getBed();
            if (bed.getCell().getGridSquare((double)bed.getX(), (double)bed.getY(), (double)(bed.getZ() + 1.0F)) == null
                || bed.getCell().getGridSquare((double)bed.getX(), (double)bed.getY(), (double)(bed.getZ() + 1.0F)).getFloor() == null) {
                return true;
            }
        }

        return false;
    }
}
