// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.popman.animal;

import zombie.network.GameServer;

public class AnimalController {
    private static final AnimalController instance = new AnimalController();

    public static AnimalController getInstance() {
        return instance;
    }

    private AnimalController() {
    }

    public void update() {
        if (GameServer.server) {
            AnimalSynchronizationManager.getInstance().update();
        }
    }
}
