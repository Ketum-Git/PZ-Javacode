// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.popman;

import java.util.ArrayList;
import zombie.SandboxOptions;
import zombie.iso.BuildingDef;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;

final class PlayerSpawns {
    private final ArrayList<PlayerSpawns.PlayerSpawn> playerSpawns = new ArrayList<>();

    public void addSpawn(int x, int y, int z) {
        PlayerSpawns.PlayerSpawn ps = new PlayerSpawns.PlayerSpawn(x, y, z);
        if (ps.building != null) {
            this.playerSpawns.add(ps);
        }
    }

    public void update() {
        long ms = System.currentTimeMillis();

        for (int i = 0; i < this.playerSpawns.size(); i++) {
            PlayerSpawns.PlayerSpawn ps = this.playerSpawns.get(i);
            if (ps.counter == -1L) {
                ps.counter = ms;
            }

            if (ps.counter + 10000L <= ms) {
                this.playerSpawns.remove(i--);
            }
        }
    }

    public boolean allowZombie(IsoGridSquare sq) {
        for (int i = 0; i < this.playerSpawns.size(); i++) {
            PlayerSpawns.PlayerSpawn ps = this.playerSpawns.get(i);
            if (!ps.allowZombie(sq)) {
                return false;
            }
        }

        return true;
    }

    private static class PlayerSpawn {
        public int x;
        public int y;
        public long counter;
        public BuildingDef building;
        public RoomDef room;

        public PlayerSpawn(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.counter = -1L;
            RoomDef roomDef = IsoWorld.instance.getMetaGrid().getRoomAt(x, y, z);
            if (roomDef != null) {
                this.building = roomDef.getBuilding();
                this.room = roomDef;
            }
        }

        public boolean allowZombie(IsoGridSquare sq) {
            switch (SandboxOptions.instance.lore.playerSpawnZombieRemoval.getValue()) {
                case 1:
                    if (this.building == null) {
                        return true;
                    }

                    if (sq.getBuilding() != null && this.building == sq.getBuilding().getDef()) {
                        return false;
                    }

                    if (sq.getX() >= this.building.getX() - 15
                        && sq.getX() < this.building.getX2() + 15
                        && sq.getY() >= this.building.getY() - 15
                        && sq.getY() < this.building.getY2() + 15) {
                        return false;
                    }
                    break;
                case 2:
                    if (this.building == null) {
                        return true;
                    }

                    if (sq.getBuilding() != null && this.building == sq.getBuilding().getDef()) {
                        return false;
                    }
                    break;
                case 3:
                    if (this.room == null) {
                        return true;
                    }

                    if (sq.getRoom() != null && this.room == sq.getRoom().getRoomDef()) {
                        return false;
                    }
                    break;
                case 4:
                    return true;
            }

            return true;
        }
    }
}
