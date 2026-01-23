// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import zombie.UsedFromLua;
import zombie.ai.states.ThumpState;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.Stance;
import zombie.characters.Stats;
import zombie.characters.SurvivorGroup;
import zombie.core.math.PZMath;
import zombie.iso.IsoMovingObject;
import zombie.iso.LosUtil;
import zombie.iso.Vector2;
import zombie.iso.Vector3;

@UsedFromLua
public final class GameCharacterAIBrain {
    private final IsoGameCharacter character;
    public final ArrayList<IsoGameCharacter> spottedCharacters = new ArrayList<>();
    public boolean stepBehaviors;
    public Stance stance;
    public boolean controlledByAdvancedPathfinder;
    public boolean isInMeta;
    public final HashMap<Vector3, ArrayList<Vector3>> blockedMemories = new HashMap<>();
    public final Vector2 aiFocusPoint = new Vector2();
    public final Vector3 nextPathTarget = new Vector3();
    public IsoMovingObject aiTarget;
    public boolean nextPathNodeInvalidated;
    public final AIBrainPlayerControlVars humanControlVars = new AIBrainPlayerControlVars();
    String order;
    public ArrayList<IsoZombie> teammateChasingZombies = new ArrayList<>();
    public ArrayList<IsoZombie> chasingZombies = new ArrayList<>();
    public boolean allowLongTermTick = true;
    public boolean isAi;
    static ArrayList<IsoZombie> tempZombies = new ArrayList<>();
    static IsoGameCharacter compare;
    private static final Stack<Vector3> Vectors = new Stack<>();

    public IsoGameCharacter getCharacter() {
        return this.character;
    }

    public GameCharacterAIBrain(IsoGameCharacter character) {
        this.character = character;
    }

    public void update() {
    }

    public void postUpdateHuman(IsoPlayer isoPlayer) {
    }

    public String getOrder() {
        return this.order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public SurvivorGroup getGroup() {
        return this.character.getDescriptor().getGroup();
    }

    public int getCloseZombieCount() {
        return Stats.numCloseZombies;
    }

    public IsoZombie getClosestChasingZombie(boolean recurse) {
        IsoZombie closest = null;
        float dist = 1.0E7F;

        for (int n = 0; n < this.chasingZombies.size(); n++) {
            IsoZombie z = this.chasingZombies.get(n);
            float ndist = z.DistTo(this.character);
            if (z.isOnFloor()) {
                ndist += 2.0F;
            }

            if (!LosUtil.lineClearCollide(
                    PZMath.fastfloor(z.getX()),
                    PZMath.fastfloor(z.getY()),
                    PZMath.fastfloor(z.getZ()),
                    PZMath.fastfloor(this.character.getX()),
                    PZMath.fastfloor(this.character.getY()),
                    PZMath.fastfloor(this.character.getZ()),
                    false
                )
                && z.getStateMachine().getCurrent() != ThumpState.instance()
                && ndist < dist
                && z.target == this.character) {
                dist = ndist;
                closest = this.chasingZombies.get(n);
            }
        }

        if (closest == null && recurse) {
            for (int n = 0; n < this.getGroup().members.size(); n++) {
                IsoGameCharacter instance1 = this.getGroup().members.get(n).getInstance();
                IsoZombie zx = instance1.getGameCharacterAIBrain().getClosestChasingZombie(false);
                if (zx != null) {
                    float ndistx = zx.DistTo(this.character);
                    if (ndistx < dist) {
                        dist = ndistx;
                        closest = zx;
                    }
                }
            }
        }

        if (closest == null && recurse) {
            for (int nx = 0; nx < this.spottedCharacters.size(); nx++) {
                IsoGameCharacter instance1 = this.spottedCharacters.get(nx);
                IsoZombie zx = instance1.getGameCharacterAIBrain().getClosestChasingZombie(false);
                if (zx != null) {
                    float ndistx = zx.DistTo(this.character);
                    if (ndistx < dist) {
                        dist = ndistx;
                        closest = zx;
                    }
                }
            }
        }

        return closest != null && closest.DistTo(this.character) > 30.0F ? null : closest;
    }

    public IsoZombie getClosestChasingZombie() {
        return this.getClosestChasingZombie(true);
    }

    public ArrayList<IsoZombie> getClosestChasingZombies(int num) {
        tempZombies.clear();
        IsoZombie closest = null;
        float dist = 1.0E7F;

        for (int n = 0; n < this.chasingZombies.size(); n++) {
            IsoZombie z = this.chasingZombies.get(n);
            float ndist = z.DistTo(this.character);
            if (!LosUtil.lineClearCollide(
                PZMath.fastfloor(z.getX()),
                PZMath.fastfloor(z.getY()),
                PZMath.fastfloor(z.getZ()),
                PZMath.fastfloor(this.character.getX()),
                PZMath.fastfloor(this.character.getY()),
                PZMath.fastfloor(this.character.getZ()),
                false
            )) {
                tempZombies.add(z);
            }
        }

        compare = this.character;
        tempZombies.sort((o1, o2) -> {
            float a = compare.DistTo(o1);
            float b = compare.DistTo(o2);
            if (a > b) {
                return 1;
            } else {
                return a < b ? -1 : 0;
            }
        });
        int toRemove = num - tempZombies.size();
        if (toRemove > tempZombies.size() - 2) {
            toRemove = tempZombies.size() - 2;
        }

        for (int nx = 0; nx < toRemove; nx++) {
            tempZombies.remove(tempZombies.size() - 1);
        }

        return tempZombies;
    }

    public void AddBlockedMemory(int ttx, int tty, int ttz) {
        synchronized (this.blockedMemories) {
            Vector3 v = new Vector3(PZMath.fastfloor(this.character.getX()), PZMath.fastfloor(this.character.getY()), PZMath.fastfloor(this.character.getZ()));
            if (!this.blockedMemories.containsKey(v)) {
                this.blockedMemories.put(v, new ArrayList<>());
            }

            ArrayList<Vector3> b = this.blockedMemories.get(v);
            Vector3 v2 = new Vector3(ttx, tty, ttz);
            if (!b.contains(v2)) {
                b.add(v2);
            }
        }
    }

    public boolean HasBlockedMemory(int lx, int ly, int lz, int x, int y, int z) {
        synchronized (this.blockedMemories) {
            synchronized (Vectors) {
                Vector3 tempo3a;
                if (Vectors.isEmpty()) {
                    tempo3a = new Vector3();
                } else {
                    tempo3a = Vectors.pop();
                }

                Vector3 tempo3b;
                if (Vectors.isEmpty()) {
                    tempo3b = new Vector3();
                } else {
                    tempo3b = Vectors.pop();
                }

                tempo3a.x = lx;
                tempo3a.y = ly;
                tempo3a.z = lz;
                tempo3b.x = x;
                tempo3b.y = y;
                tempo3b.z = z;
                if (!this.blockedMemories.containsKey(tempo3a)) {
                    Vectors.push(tempo3a);
                    Vectors.push(tempo3b);
                    return false;
                }

                if (this.blockedMemories.get(tempo3a).contains(tempo3b)) {
                    Vectors.push(tempo3a);
                    Vectors.push(tempo3b);
                    return true;
                }

                Vectors.push(tempo3a);
                Vectors.push(tempo3b);
            }

            return false;
        }
    }

    public void renderlast() {
    }
}
