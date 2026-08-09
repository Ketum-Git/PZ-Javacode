// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.skills;

public final class CustomPerk {
    public String id;
    public String parent = "None";
    public String translation;
    public boolean passive;
    public final int[] xp = new int[10];

    public CustomPerk(String id) {
        this.id = id;
    }
}
