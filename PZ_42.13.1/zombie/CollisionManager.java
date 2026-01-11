// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Stack;
import zombie.characters.AnimalFootstepManager;
import zombie.characters.AnimalVocalsManager;
import zombie.characters.IsoSurvivor;
import zombie.characters.ZombieFootstepManager;
import zombie.characters.ZombieThumpManager;
import zombie.characters.ZombieVocalsManager;
import zombie.characters.animals.AnimalPopulationManager;
import zombie.core.collision.Polygon;
import zombie.core.profiling.AbstractPerformanceProfileProbe;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.iso.FishSplashSoundManager;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoPushableObject;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;

public final class CollisionManager {
    static Vector2 temp = new Vector2();
    static Vector2 axis = new Vector2();
    static Polygon polygonA = new Polygon();
    static Polygon polygonB = new Polygon();
    float minA;
    float minB;
    float maxA;
    float maxB;
    CollisionManager.PolygonCollisionResult result = new CollisionManager.PolygonCollisionResult();
    public ArrayList<CollisionManager.Contact> contactMap = new ArrayList<>();
    Long[] longArray = new Long[1000];
    Stack<CollisionManager.Contact> contacts = new Stack<>();
    public static final CollisionManager instance = new CollisionManager();

    private void ProjectPolygonA(Vector2 axis, Polygon polygon) {
        float d = axis.dot(polygon.points.get(0));
        this.minA = d;
        this.maxA = d;

        for (int i = 0; i < polygon.points.size(); i++) {
            d = polygon.points.get(i).dot(axis);
            if (d < this.minA) {
                this.minA = d;
            } else if (d > this.maxA) {
                this.maxA = d;
            }
        }
    }

    private void ProjectPolygonB(Vector2 axis, Polygon polygon) {
        float d = axis.dot(polygon.points.get(0));
        this.minB = d;
        this.maxB = d;

        for (int i = 0; i < polygon.points.size(); i++) {
            d = polygon.points.get(i).dot(axis);
            if (d < this.minB) {
                this.minB = d;
            } else if (d > this.maxB) {
                this.maxB = d;
            }
        }
    }

    public CollisionManager.PolygonCollisionResult PolygonCollision(Vector2 velocity) {
        this.result.intersect = true;
        this.result.willIntersect = true;
        this.result.minimumTranslationVector.x = 0.0F;
        this.result.minimumTranslationVector.y = 0.0F;
        int edgeCountA = polygonA.edges.size();
        int edgeCountB = polygonB.edges.size();
        float minIntervalDistance = Float.POSITIVE_INFINITY;
        Vector2 translationAxis = new Vector2();

        for (int edgeIndex = 0; edgeIndex < edgeCountA + edgeCountB; edgeIndex++) {
            Vector2 edge;
            if (edgeIndex < edgeCountA) {
                edge = polygonA.edges.get(edgeIndex);
            } else {
                edge = polygonB.edges.get(edgeIndex - edgeCountA);
            }

            axis.x = -edge.y;
            axis.y = edge.x;
            axis.normalize();
            this.minA = 0.0F;
            this.minB = 0.0F;
            this.maxA = 0.0F;
            this.maxB = 0.0F;
            this.ProjectPolygonA(axis, polygonA);
            this.ProjectPolygonB(axis, polygonB);
            if (this.IntervalDistance(this.minA, this.maxA, this.minB, this.maxB) > 0.0F) {
                this.result.intersect = false;
            }

            float velocityProjection = axis.dot(velocity);
            if (velocityProjection < 0.0F) {
                this.minA += velocityProjection;
            } else {
                this.maxA += velocityProjection;
            }

            float intervalDistance = this.IntervalDistance(this.minA, this.maxA, this.minB, this.maxB);
            if (intervalDistance > 0.0F) {
                this.result.willIntersect = false;
            }

            if (!this.result.intersect && !this.result.willIntersect) {
                break;
            }

            intervalDistance = Math.abs(intervalDistance);
            if (intervalDistance < minIntervalDistance) {
                minIntervalDistance = intervalDistance;
                translationAxis.x = axis.x;
                translationAxis.y = axis.y;
                temp.x = polygonA.Center().x - polygonB.Center().x;
                temp.y = polygonA.Center().y - polygonB.Center().y;
                if (temp.dot(translationAxis) < 0.0F) {
                    translationAxis.x = -translationAxis.x;
                    translationAxis.y = -translationAxis.y;
                }
            }
        }

        if (this.result.willIntersect) {
            this.result.minimumTranslationVector.x = translationAxis.x * minIntervalDistance;
            this.result.minimumTranslationVector.y = translationAxis.y * minIntervalDistance;
        }

        return this.result;
    }

    public float IntervalDistance(float minA, float maxA, float minB, float maxB) {
        return minA < minB ? minB - maxA : minA - maxB;
    }

    public void initUpdate() {
        if (this.longArray[0] == null) {
            for (int n = 0; n < this.longArray.length; n++) {
                this.longArray[n] = 0L;
            }
        }

        for (int n = 0; n < this.contactMap.size(); n++) {
            this.contactMap.get(n).a = null;
            this.contactMap.get(n).b = null;
            this.contacts.push(this.contactMap.get(n));
        }

        this.contactMap.clear();
    }

    public void AddContact(IsoMovingObject a, IsoMovingObject b) {
        if (!(a instanceof IsoSurvivor) && !(b instanceof IsoSurvivor) || !(a instanceof IsoPushableObject) && !(b instanceof IsoPushableObject)) {
            if (a.getID() < b.getID()) {
                this.contactMap.add(this.contact(a, b));
            }
        }
    }

    CollisionManager.Contact contact(IsoMovingObject a, IsoMovingObject b) {
        if (this.contacts.isEmpty()) {
            for (int n = 0; n < 50; n++) {
                this.contacts.push(new CollisionManager.Contact(null, null));
            }
        }

        CollisionManager.Contact c = this.contacts.pop();
        c.a = a;
        c.b = b;
        return c;
    }

    public void ResolveContacts() {
        try (AbstractPerformanceProfileProbe ignored = CollisionManager.s_performance.profile_ResolveContacts.profile()) {
            this.resolveContactsInternal();
        }
    }

    private void resolveContactsInternal() {
        Vector2 vel = CollisionManager.l_ResolveContacts.vel;
        Vector2 vel2 = CollisionManager.l_ResolveContacts.vel2;
        List<IsoPushableObject> pushables = CollisionManager.l_ResolveContacts.pushables;
        List<IsoPushableObject> pol = IsoWorld.instance.currentCell.getPushableObjectList();
        int size = pol.size();

        for (int i = 0; i < size; i++) {
            IsoPushableObject o = pol.get(i);
            if (o.getImpulsex() != 0.0F || o.getImpulsey() != 0.0F) {
                if (o.connectList != null) {
                    pushables.add(o);
                } else {
                    o.setNextX(o.getNextX() + o.getImpulsex());
                    o.setNextY(o.getNextY() + o.getImpulsey());
                    o.setImpulsex(o.getNextX() - o.getX());
                    o.setImpulsey(o.getNextY() - o.getY());
                    o.setNextX(o.getX());
                    o.setNextY(o.getY());
                }
            }
        }

        int numPushables = pushables.size();

        for (int n = 0; n < numPushables; n++) {
            IsoPushableObject p = pushables.get(n);
            float impulseTotx = 0.0F;
            float impulseToty = 0.0F;

            for (int m = 0; m < p.connectList.size(); m++) {
                impulseTotx += p.connectList.get(m).getImpulsex();
                impulseToty += p.connectList.get(m).getImpulsey();
            }

            impulseTotx /= p.connectList.size();
            impulseToty /= p.connectList.size();

            for (int m = 0; m < p.connectList.size(); m++) {
                p.connectList.get(m).setImpulsex(impulseTotx);
                p.connectList.get(m).setImpulsey(impulseToty);
                int inof = pushables.indexOf(p.connectList.get(m));
                pushables.remove(p.connectList.get(m));
                if (inof <= n) {
                    n--;
                }
            }

            if (n < 0) {
                n = 0;
            }
        }

        pushables.clear();
        int numContacts = this.contactMap.size();

        for (int ix = 0; ix < numContacts; ix++) {
            CollisionManager.Contact c = this.contactMap.get(ix);
            if (!(Math.abs(c.a.getZ() - c.b.getZ()) > 0.3F)) {
                vel.x = c.a.getNextX() - c.a.getX();
                vel.y = c.a.getNextY() - c.a.getY();
                vel2.x = c.b.getNextX() - c.b.getX();
                vel2.y = c.b.getNextY() - c.b.getY();
                if (vel.x != 0.0F
                    || vel.y != 0.0F
                    || vel2.x != 0.0F
                    || vel2.y != 0.0F
                    || c.a.getImpulsex() != 0.0F
                    || c.a.getImpulsey() != 0.0F
                    || c.b.getImpulsex() != 0.0F
                    || c.b.getImpulsey() != 0.0F) {
                    float ax1 = c.a.getX() - c.a.getWidth();
                    float ax2 = c.a.getX() + c.a.getWidth();
                    float ay1 = c.a.getY() - c.a.getWidth();
                    float ay2 = c.a.getY() + c.a.getWidth();
                    float bx1 = c.b.getX() - c.b.getWidth();
                    float bx2 = c.b.getX() + c.b.getWidth();
                    float by1 = c.b.getY() - c.b.getWidth();
                    float by2 = c.b.getY() + c.b.getWidth();
                    polygonA.Set(ax1, ay1, ax2, ay2);
                    polygonB.Set(bx1, by1, bx2, by2);
                    CollisionManager.PolygonCollisionResult result = this.PolygonCollision(vel);
                    if (result.willIntersect) {
                        c.a.collideWith(c.b);
                        c.b.collideWith(c.a);
                        float weightdelta = 1.0F
                            - c.a.getWeight(result.minimumTranslationVector.x, result.minimumTranslationVector.y)
                                / (
                                    c.a.getWeight(result.minimumTranslationVector.x, result.minimumTranslationVector.y)
                                        + c.b.getWeight(result.minimumTranslationVector.x, result.minimumTranslationVector.y)
                                );
                        if (c.a instanceof IsoPushableObject object && c.b instanceof IsoSurvivor survivor) {
                            survivor.collidedWithPushable = true;
                            survivor.collidePushable = object;
                        } else if (c.b instanceof IsoPushableObject isoPushableObject && c.a instanceof IsoSurvivor isoSurvivor) {
                            isoSurvivor.collidedWithPushable = true;
                            isoSurvivor.collidePushable = isoPushableObject;
                        }

                        if (c.a instanceof IsoPushableObject pushableObject) {
                            ArrayList<IsoPushableObject> connectListA = pushableObject.connectList;
                            if (connectListA != null) {
                                int connectListSize = connectListA.size();

                                for (int j = 0; j < connectListSize; j++) {
                                    IsoPushableObject p = connectListA.get(j);
                                    p.setImpulsex(p.getImpulsex() + result.minimumTranslationVector.x * weightdelta);
                                    p.setImpulsey(p.getImpulsey() + result.minimumTranslationVector.y * weightdelta);
                                }
                            }
                        } else {
                            c.a.setImpulsex(c.a.getImpulsex() + result.minimumTranslationVector.x * weightdelta);
                            c.a.setImpulsey(c.a.getImpulsey() + result.minimumTranslationVector.y * weightdelta);
                        }

                        if (c.b instanceof IsoPushableObject isoPushableObject) {
                            ArrayList<IsoPushableObject> connectListB = isoPushableObject.connectList;
                            if (connectListB != null) {
                                int connectListSize = connectListB.size();

                                for (int j = 0; j < connectListSize; j++) {
                                    IsoPushableObject p = connectListB.get(j);
                                    p.setImpulsex(p.getImpulsex() - result.minimumTranslationVector.x * (1.0F - weightdelta));
                                    p.setImpulsey(p.getImpulsey() - result.minimumTranslationVector.y * (1.0F - weightdelta));
                                }
                            }
                        } else {
                            c.b.setImpulsex(c.b.getImpulsex() - result.minimumTranslationVector.x * (1.0F - weightdelta));
                            c.b.setImpulsey(c.b.getImpulsey() - result.minimumTranslationVector.y * (1.0F - weightdelta));
                        }
                    }
                }
            }
        }

        List<IsoMovingObject> objectList = IsoWorld.instance.currentCell.getObjectList();
        int objectsListCount = objectList.size();
        AnimalPopulationManager.getInstance().update();
        AnimalVocalsManager.instance.update();
        AnimalFootstepManager.instance.update();
        IsoMovingObject.treeSoundMgr.update();
        FishSplashSoundManager.instance.update();
        ZombieFootstepManager.instance.update();
        ZombieThumpManager.instance.update();
        ZombieVocalsManager.instance.update();
    }

    public class Contact {
        public IsoMovingObject a;
        public IsoMovingObject b;

        public Contact(final IsoMovingObject a, final IsoMovingObject b) {
            Objects.requireNonNull(CollisionManager.this);
            super();
            this.a = a;
            this.b = b;
        }
    }

    public class PolygonCollisionResult {
        public boolean willIntersect;
        public boolean intersect;
        public Vector2 minimumTranslationVector;

        public PolygonCollisionResult() {
            Objects.requireNonNull(CollisionManager.this);
            super();
            this.minimumTranslationVector = new Vector2();
        }
    }

    private static class l_ResolveContacts {
        static final Vector2 vel = new Vector2();
        static final Vector2 vel2 = new Vector2();
        static final List<IsoPushableObject> pushables = new ArrayList<>();
        static IsoMovingObject[] objectListInvoking = new IsoMovingObject[1024];
    }

    private static class s_performance {
        static final PerformanceProfileProbe profile_ResolveContacts = new PerformanceProfileProbe("CollisionManager.ResolveContacts");
        static final PerformanceProfileProbe profile_MovingObjectPostUpdate = new PerformanceProfileProbe("IsoMovingObject.postupdate");
    }
}
