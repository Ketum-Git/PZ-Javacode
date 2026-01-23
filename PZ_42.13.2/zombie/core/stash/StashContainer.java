// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.stash;

public final class StashContainer {
    public String room;
    public String containerSprite;
    public String containerType;
    public int contX = -1;
    public int contY = -1;
    public int contZ = -1;
    public String containerItem;

    public StashContainer(String room, String containerSprite, String containerType) {
        if (room == null) {
            this.room = "all";
        } else {
            this.room = room;
        }

        this.containerSprite = containerSprite;
        this.containerType = containerType;
    }
}
