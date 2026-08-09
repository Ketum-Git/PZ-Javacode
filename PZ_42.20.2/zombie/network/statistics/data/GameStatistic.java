// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.statistics.data;

import zombie.MovingObjectUpdateScheduler;
import zombie.characters.animals.IsoAnimal;
import zombie.iso.IsoWorld;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.network.statistics.counters.Counter;
import zombie.popman.animal.AnimalInstanceManager;

public class GameStatistic extends Statistic implements IStatistic {
    private static final GameStatistic instance = new GameStatistic("game");
    public final Counter players = new Counter(this, "players", 0.0, this::getPlayersCount, "Player online", "number");
    public final Counter animalObjects = new Counter(
        this,
        "animals-objects",
        0.0,
        () -> IsoWorld.instance.currentCell.getObjectList().stream().filter(obj -> obj instanceof IsoAnimal).count(),
        "Animals in world",
        "number"
    );
    public final Counter animalInstances = new Counter(
        this, "animals-instances", 0.0, () -> AnimalInstanceManager.getInstance().getAnimals().size(), "Animals to sync", "number"
    );
    public final Counter zombiesUpdated = new Counter(this, "zombies-updated", 0.0, null, "Updated zombies", "number");
    public final Counter zombiesUpdates = new Counter(this, "zombies-updates", 0.0, null, "Zombies update cycles", "number");
    public final Counter clientZombiesTotal = new Counter(this, "zombies-total", 0.0, this::getZombiesCount, "Zombies in world", "number");
    public final Counter zombiesLoaded = new Counter(
        this, "zombies-loaded", 0.0, () -> IsoWorld.instance.getCell().getZombieList().size(), "Zombies loaded", "number"
    );
    public final Counter zombiesSimulated = new Counter(
        this,
        "zombies-simulated",
        0.0,
        () -> (long)(this.zombiesUpdated.get() / Math.max(MovingObjectUpdateScheduler.instance.getFrameCounter() - this.zombiesUpdates.get(), 1.0)),
        "Zombies simulated",
        "number"
    );
    public final Counter zombiesCulled = new Counter(this, "zombies-culled", 0.0, null, "Zombies culled", "number");
    public final Counter playersTeleports = new Counter(this, "players-teleports", 0.0, null, "Players teleports", "number");
    public final Counter zombiesTeleports = new Counter(this, "zombies-teleports", 0.0, null, "Zombies teleports", "number");
    public final Counter loadedCells = new Counter(this, "loaded-cells", 0.0, () -> ServerMap.instance.loadedCells.size(), "Loaded cells", "number");

    public GameStatistic(String application) {
        super(application);
    }

    public static GameStatistic getInstance() {
        return instance;
    }

    @Override
    public void update() {
        super.update();
        this.zombiesUpdates.set(MovingObjectUpdateScheduler.instance.getFrameCounter());
    }

    public int getPlayersCount() {
        if (GameServer.server) {
            return GameServer.IDToPlayerMap.size();
        } else {
            return GameClient.client ? GameClient.IDToPlayerMap.size() : 0;
        }
    }

    public int getZombiesCount() {
        if (GameServer.server) {
            return ServerMap.instance.zombieMap.size();
        } else {
            return GameClient.client ? GameClient.IDToZombieMap.size() : 0;
        }
    }
}
