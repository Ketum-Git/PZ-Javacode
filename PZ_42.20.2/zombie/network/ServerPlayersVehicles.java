// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import zombie.core.logger.ExceptionLogger;
import zombie.savefile.ServerPlayerDB;
import zombie.vehicles.VehiclesDB2;

public class ServerPlayersVehicles {
    public static final ServerPlayersVehicles instance = new ServerPlayersVehicles();
    private ServerPlayersVehicles.SPVThread thread;

    public void init() {
        this.thread = new ServerPlayersVehicles.SPVThread();
        this.thread.setName("ServerPlayersVehicles");
        this.thread.setDaemon(true);
        this.thread.start();
    }

    public void stop() {
        if (this.thread != null) {
            this.thread.stop = true;

            while (this.thread.isAlive()) {
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException var2) {
                }
            }

            this.thread = null;
        }
    }

    private static final class SPVThread extends Thread {
        boolean stop;

        @Override
        public void run() {
            while (!this.stop) {
                try {
                    this.runInner();
                } catch (Throwable var2) {
                    ExceptionLogger.logException(var2);
                }
            }
        }

        void runInner() {
            ServerPlayerDB.getInstance().process();
            VehiclesDB2.instance.updateWorldStreamer();

            try {
                Thread.sleep(500L);
            } catch (InterruptedException var2) {
            }
        }
    }
}
