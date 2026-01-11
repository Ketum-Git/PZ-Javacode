// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import zombie.UsedFromLua;
import zombie.core.random.Rand;

@UsedFromLua
public enum IsoDirections {
    N(0, 0, -1),
    NW(1, -1, -1),
    W(2, -1, 0),
    SW(3, -1, 1),
    S(4, 0, 1),
    SE(5, 1, 1),
    E(6, 1, 0),
    NE(7, 1, -1),
    Max(8, 0, 0);

    private final int index;
    private final int dx;
    private final int dy;
    private IsoDirections opposite;
    private IsoDirections right;
    private IsoDirections left;
    private float angle;
    private final Vector2 vector = new Vector2(0.0F, 0.0F);
    private static final IsoDirections[] VALUES = values();
    private static final Vector2 temp = new Vector2();
    private static final IsoDirections[][] directionLookup = generateTables();

    private IsoDirections(final int index, final int dx, final int dy) {
        this.index = index;
        this.dx = dx;
        this.dy = dy;
    }

    public static IsoDirections fromString(String str) {
        str = str.trim();
        if ("N".equalsIgnoreCase(str)) {
            return N;
        } else if ("NW".equalsIgnoreCase(str)) {
            return NW;
        } else if ("W".equalsIgnoreCase(str)) {
            return W;
        } else if ("SW".equalsIgnoreCase(str)) {
            return SW;
        } else if ("S".equalsIgnoreCase(str)) {
            return S;
        } else if ("SE".equalsIgnoreCase(str)) {
            return SE;
        } else if ("E".equalsIgnoreCase(str)) {
            return E;
        } else {
            return "NE".equalsIgnoreCase(str) ? NE : null;
        }
    }

    public static IsoDirections fromIndex(int index) {
        while (index < 0) {
            index += 8;
        }

        index %= 8;
        return VALUES[index];
    }

    public IsoDirections RotLeft(int time) {
        IsoDirections newDir = RotLeft(this);

        for (int i = 0; i < time - 1; i++) {
            newDir = RotLeft(newDir);
        }

        return newDir;
    }

    public IsoDirections RotRight(int time) {
        IsoDirections newDir = RotRight(this);

        for (int i = 0; i < time - 1; i++) {
            newDir = RotRight(newDir);
        }

        return newDir;
    }

    public IsoDirections RotLeft() {
        return this.left;
    }

    public IsoDirections RotRight() {
        return this.right;
    }

    public IsoDirections Rot180() {
        return this.opposite;
    }

    public static IsoDirections RotLeft(IsoDirections dir) {
        return dir.RotLeft();
    }

    public static IsoDirections RotRight(IsoDirections dir) {
        return dir.RotRight();
    }

    public static IsoDirections reverse(IsoDirections dir) {
        return dir.Rot180();
    }

    public static IsoDirections[][] generateTables() {
        IsoDirections[][] directionLookup = new IsoDirections[200][200];

        for (int x = 0; x < 200; x++) {
            for (int y = 0; y < 200; y++) {
                int ux = x - 100;
                int uy = y - 100;
                float fx = ux / 100.0F;
                float fy = uy / 100.0F;
                Vector2 vec = new Vector2(fx, fy);
                vec.normalize();
                directionLookup[x][y] = fromAngleActual(vec);
            }
        }

        return directionLookup;
    }

    public static IsoDirections fromAngleActual(Vector2 angle) {
        temp.x = angle.x;
        temp.y = angle.y;
        temp.normalize();
        float dir = temp.getDirectionNeg();
        float totalRad = (float) (Math.PI / 4);
        float radCheck = (float) (Math.PI * 2);
        radCheck = (float)(radCheck + Math.toRadians(112.5));

        for (int n = 0; n < 8; n++) {
            radCheck += (float) (Math.PI / 4);
            if (dir >= radCheck && dir <= radCheck + (float) (Math.PI / 4)
                || dir + (float) (Math.PI * 2) >= radCheck && dir + (float) (Math.PI * 2) <= radCheck + (float) (Math.PI / 4)
                || dir - (float) (Math.PI * 2) >= radCheck && dir - (float) (Math.PI * 2) <= radCheck + (float) (Math.PI / 4)) {
                return fromIndex(n);
            }

            if (radCheck > Math.PI * 2) {
                radCheck = (float)(radCheck - (Math.PI * 2));
            }
        }

        if (temp.x > 0.5F) {
            if (temp.y < -0.5F) {
                return NE;
            } else {
                return temp.y > 0.5F ? SE : E;
            }
        } else if (temp.x < -0.5F) {
            if (temp.y < -0.5F) {
                return NW;
            } else {
                return temp.y > 0.5F ? SW : W;
            }
        } else if (temp.y < -0.5F) {
            return N;
        } else {
            return temp.y > 0.5F ? S : N;
        }
    }

    public static IsoDirections fromAngle(float angleRadians) {
        float x = (float)Math.cos(angleRadians);
        float y = (float)Math.sin(angleRadians);
        return fromAngle(x, y);
    }

    public static IsoDirections fromAngle(Vector2 angle) {
        return fromAngle(angle.x, angle.y);
    }

    public static IsoDirections fromAngle(float angleX, float angleY) {
        temp.x = angleX;
        temp.y = angleY;
        if (temp.getLengthSquared() != 1.0F) {
            temp.normalize();
        }

        int x = (int)((temp.x + 1.0F) * 100.0F);
        int y = (int)((temp.y + 1.0F) * 100.0F);
        if (x >= 200) {
            x = 199;
        }

        if (y >= 200) {
            y = 199;
        }

        if (x < 0) {
            x = 0;
        }

        if (y < 0) {
            y = 0;
        }

        return directionLookup[x][y];
    }

    public static IsoDirections cardinalFromAngle(Vector2 angle) {
        boolean div1 = angle.getX() >= angle.getY();
        boolean div2 = angle.getX() > -angle.getY();
        if (div1) {
            return div2 ? E : N;
        } else {
            return div2 ? S : W;
        }
    }

    public int index() {
        return this.index % 8;
    }

    public int indexUnmodified() {
        return this.index;
    }

    public int dx() {
        return this.dx;
    }

    public int dy() {
        return this.dy;
    }

    public String toCompassString() {
        switch (this.index) {
            case 0:
                return "9";
            case 1:
                return "8";
            case 2:
                return "7";
            case 3:
                return "4";
            case 4:
                return "1";
            case 5:
                return "2";
            case 6:
                return "3";
            case 7:
                return "6";
            default:
                return "";
        }
    }

    public Vector2 ToVector() {
        return this.ToVector(temp);
    }

    public Vector2 ToVector(Vector2 out_result) {
        out_result.set(this.vector);
        return out_result;
    }

    public float toAngle() {
        return this.angle;
    }

    public float toAngleDegrees() {
        return this.angle * (180.0F / (float)Math.PI);
    }

    public static IsoDirections getRandom() {
        return fromIndex(Rand.Next(0, Max.index));
    }

    private static void initAllValues() {
        for (IsoDirections dir : values()) {
            dir.init();
        }
    }

    private void init() {
        this.opposite = getReverse(this);
        this.left = getLeft(this);
        this.right = getRight(this);
        this.angle = getAngle(this);
        getVector(this, this.vector);
    }

    private static IsoDirections getLeft(IsoDirections dir) {
        switch (dir) {
            case N:
                return NW;
            case NW:
                return W;
            case W:
                return SW;
            case SW:
                return S;
            case S:
                return SE;
            case SE:
                return E;
            case E:
                return NE;
            case NE:
                return N;
            default:
                return Max;
        }
    }

    private static IsoDirections getRight(IsoDirections dir) {
        switch (dir) {
            case N:
                return NE;
            case NW:
                return N;
            case W:
                return NW;
            case SW:
                return W;
            case S:
                return SW;
            case SE:
                return S;
            case E:
                return SE;
            case NE:
                return E;
            default:
                return Max;
        }
    }

    private static IsoDirections getReverse(IsoDirections dir) {
        switch (dir) {
            case N:
                return S;
            case NW:
                return SE;
            case W:
                return E;
            case SW:
                return NE;
            case S:
                return N;
            case SE:
                return NW;
            case E:
                return W;
            case NE:
                return SW;
            default:
                return Max;
        }
    }

    private static float getAngle(IsoDirections dir) {
        float div = (float) (Math.PI / 4);
        switch (dir) {
            case N:
                return 0.0F;
            case NW:
                return (float) (Math.PI / 4);
            case W:
                return (float) (Math.PI / 2);
            case SW:
                return (float) (Math.PI * 3.0 / 4.0);
            case S:
                return (float) Math.PI;
            case SE:
                return (float) Math.PI * 5.0F / 4.0F;
            case E:
                return (float) (Math.PI * 3.0 / 2.0);
            case NE:
                return (float) Math.PI * 7.0F / 4.0F;
            default:
                return 0.0F;
        }
    }

    private static void getVector(IsoDirections dir, Vector2 out_result) {
        switch (dir) {
            case N:
                out_result.x = 0.0F;
                out_result.y = -1.0F;
                break;
            case NW:
                out_result.x = -1.0F;
                out_result.y = -1.0F;
                break;
            case W:
                out_result.x = -1.0F;
                out_result.y = 0.0F;
                break;
            case SW:
                out_result.x = -1.0F;
                out_result.y = 1.0F;
                break;
            case S:
                out_result.x = 0.0F;
                out_result.y = 1.0F;
                break;
            case SE:
                out_result.x = 1.0F;
                out_result.y = 1.0F;
                break;
            case E:
                out_result.x = 1.0F;
                out_result.y = 0.0F;
                break;
            case NE:
                out_result.x = 1.0F;
                out_result.y = -1.0F;
        }

        out_result.normalize();
    }

    static {
        initAllValues();
    }
}
