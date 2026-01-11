// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

public class NetworkVariables {
    public static enum ThumpType {
        TTNone(""),
        TTDoor("Door"),
        TTClaw("DoorClaw"),
        TTBang("DoorBang");

        private final String thumpType;

        private ThumpType(final String thumpType) {
            this.thumpType = thumpType;
        }

        @Override
        public String toString() {
            return this.thumpType;
        }

        public static NetworkVariables.ThumpType fromString(String thumpType) {
            for (NetworkVariables.ThumpType type : values()) {
                if (type.thumpType.equalsIgnoreCase(thumpType)) {
                    return type;
                }
            }

            return TTNone;
        }

        public static NetworkVariables.ThumpType fromByte(Byte thumpType) {
            for (NetworkVariables.ThumpType type : values()) {
                if (type.ordinal() == thumpType) {
                    return type;
                }
            }

            return TTNone;
        }
    }

    public static enum WalkType {
        WT1("1"),
        WT2("2"),
        WT3("3"),
        WT4("4"),
        WT5("5"),
        WTSprint1("sprint1"),
        WTSprint2("sprint2"),
        WTSprint3("sprint3"),
        WTSprint4("sprint4"),
        WTSprint5("sprint5"),
        WTSlow1("slow1"),
        WTSlow2("slow2"),
        WTSlow3("slow3");

        private final String walkType;

        private WalkType(final String walkType) {
            this.walkType = walkType;
        }

        @Override
        public String toString() {
            return this.walkType;
        }

        public static NetworkVariables.WalkType fromString(String walkType) {
            for (NetworkVariables.WalkType type : values()) {
                if (type.walkType.equalsIgnoreCase(walkType)) {
                    return type;
                }
            }

            return WT1;
        }

        public static NetworkVariables.WalkType fromByte(byte walkType) {
            for (NetworkVariables.WalkType type : values()) {
                if (type.ordinal() == walkType) {
                    return type;
                }
            }

            return WT1;
        }
    }

    public static enum ZombieState {
        Idle("idle"),
        WalkToward("walktoward"),
        TurnAlerted("turnalerted"),
        PathFind("pathfind"),
        Sitting("sitting"),
        HitReaction("hitreaction"),
        HitReactionHit("hitreaction-hit"),
        Thump("thump"),
        ClimbFence("climbfence"),
        Lunge("lunge"),
        Attack("attack"),
        AttackNetwork("attack-network"),
        AttackVehicle("attackvehicle"),
        AttackVehicleNetwork("attackvehicle-network"),
        Bumped("bumped"),
        ClimbWindow("climbwindow"),
        EatBody("eatbody"),
        FaceTarget("face-target"),
        FakeDead("fakedead"),
        FakeDeadAttack("fakedead-attack"),
        FakeDeadAttackNetwork("fakedead-attack-network"),
        FallDown("falldown"),
        Falling("falling"),
        GetDown("getdown"),
        Getup("getup"),
        Grappled("grappled"),
        HitWhileStaggered("hitwhilestaggered"),
        LungeNetwork("lunge-network"),
        OnGround("onground"),
        StaggerBack("staggerback"),
        WalkTowardNetwork("walktoward-network"),
        AnimalAlerted("alerted"),
        AnimalDeath("death"),
        AnimalEating("eating"),
        AnimalHutch("hutch"),
        AnimalTrailer("trailer"),
        AnimalWalk("walk"),
        AnimalZone("zone"),
        FakeZombieStay("fakezombie-stay"),
        FakeZombieNormal("fakezombie-normal"),
        FakeZombieAttack("fakezombie-attack");

        private final String zombieState;

        private ZombieState(final String zombieState) {
            this.zombieState = zombieState.toLowerCase();
        }

        @Override
        public String toString() {
            return this.zombieState;
        }

        public static NetworkVariables.ZombieState fromString(String zombieState) {
            if (zombieState == null) {
                return Idle;
            } else {
                String zombieStateLC = zombieState.toLowerCase();

                for (NetworkVariables.ZombieState type : values()) {
                    if (type.zombieState.equals(zombieStateLC)) {
                        return type;
                    }
                }

                return Idle;
            }
        }

        public static NetworkVariables.ZombieState fromByte(Byte zombieState) {
            return zombieState >= 0 && zombieState <= values().length ? values()[zombieState] : Idle;
        }

        public byte toByte() {
            return (byte)this.ordinal();
        }
    }
}
