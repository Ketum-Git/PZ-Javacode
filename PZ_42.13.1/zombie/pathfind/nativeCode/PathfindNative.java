// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind.nativeCode;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.Lua.LuaManager;
import zombie.ai.astar.Mover;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.gameStates.DebugChunkState;
import zombie.gameStates.IngameState;
import zombie.input.GameKeyboard;
import zombie.input.Mouse;
import zombie.iso.IsoCamera;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.network.GameServer;
import zombie.pathfind.IPathfinder;
import zombie.pathfind.PolygonalMap2;
import zombie.pathfind.TestRequest;
import zombie.vehicles.BaseVehicle;

public class PathfindNative {
    public static final PathfindNative instance = new PathfindNative();
    public static boolean useNativeCode = true;
    private final HashMap<BaseVehicle, VehicleState> vehicleState = new HashMap<>();
    private int testZ;
    private final ByteBuffer requestBb = ByteBuffer.allocateDirect(50);
    private final PathFindRequest request = new PathFindRequest();
    private final TestRequest finder = new TestRequest();
    private boolean testRequestAdded;

    public static void init() {
        String libSuffix = "";
        if ("1".equals(System.getProperty("zomboid.debuglibs.pathfind"))) {
            DebugLog.log("***** Loading debug version of PZPathFind");
            libSuffix = "d";
        }

        if (System.getProperty("os.name").contains("OS X")) {
            System.loadLibrary("PZPathFind");
        } else {
            System.loadLibrary("PZPathFind64" + libSuffix);
        }
    }

    public static native void initWorld(int var0, int var1, int var2, int var3, boolean var4);

    public static native void destroyWorld();

    public static native void freeMemoryAtExit();

    public static native void update();

    public static native void updateChunk(int var0, int var1, int var2, ByteBuffer var3);

    public static native void removeChunk(int var0, int var1);

    public static native void updateSquare(int var0, int var1, int var2, int var3, int var4, short var5, int var6, float var7, float var8);

    public static native void addVehicle(ByteBuffer var0);

    public static native void removeVehicle(int var0);

    public static native void teleportVehicle(ByteBuffer var0);

    public static native int findPath(ByteBuffer var0, ByteBuffer var1);

    public void init(IsoMetaGrid metaGrid) {
        initWorld(metaGrid.getMinX(), metaGrid.getMinY(), metaGrid.getWidth(), metaGrid.getHeight(), GameServer.server);
        PathfindNativeThread.instance = new PathfindNativeThread();
        ByteBuffer bb = PathfindNativeThread.instance.pathBb;
        PathfindNativeThread.instance.pathBb.order(ByteOrder.BIG_ENDIAN);
        bb.clear();
        PathfindNativeThread.instance.setName("PathfindNativeThread");
        PathfindNativeThread.instance.setDaemon(true);
        PathfindNativeThread.instance.start();
    }

    public void stop() {
        PathfindNativeThread.instance.stopThread();
        PathfindNativeThread.instance.cleanup();
        PathfindNativeThread.instance = null;

        for (VehicleState state : this.vehicleState.values()) {
            state.release();
        }

        this.vehicleState.clear();
        this.testRequestAdded = false;
        destroyWorld();
    }

    public void checkUseNativeCode() {
        if (useNativeCode != DebugOptions.instance.pathfindUseNativeCode.getValue()) {
            if (useNativeCode) {
                this.stop();
            } else {
                PolygonalMap2.instance.stop();
            }

            useNativeCode = DebugOptions.instance.pathfindUseNativeCode.getValue();
            if (useNativeCode) {
                this.init(IsoWorld.instance.metaGrid);
            } else {
                PolygonalMap2.instance.init(IsoWorld.instance.metaGrid);
            }

            for (int playerIndex = 0; playerIndex < IsoPlayer.numPlayers; playerIndex++) {
                IsoChunkMap chunkMap = IsoWorld.instance.currentCell.getChunkMap(playerIndex);
                if (!chunkMap.ignore) {
                    for (int y = 0; y < IsoChunkMap.chunkGridWidth; y++) {
                        for (int x = 0; x < IsoChunkMap.chunkGridWidth; x++) {
                            IsoChunk chunk = chunkMap.getChunk(x, y);
                            if (chunk != null) {
                                if (useNativeCode) {
                                    this.addChunkToWorld(chunk);
                                } else {
                                    PolygonalMap2.instance.addChunkToWorld(chunk);
                                }
                            }
                        }
                    }
                }
            }

            ArrayList<BaseVehicle> vehicles = IsoWorld.instance.currentCell.getVehicles();

            for (int i = 0; i < vehicles.size(); i++) {
                BaseVehicle vehicle = vehicles.get(i);
                if (useNativeCode) {
                    this.addVehicle(vehicle);
                } else {
                    PolygonalMap2.instance.addVehicleToWorld(vehicle);
                }
            }
        }
    }

    public void addChunkToWorld(IsoChunk chunk) {
        ChunkUpdateTask task = ChunkUpdateTask.alloc().init(chunk);
        PathfindNativeThread.instance.chunkTaskQueue.add(task);
        PathfindNativeThread.instance.wake();
        chunk.loadedBits = (short)(chunk.loadedBits | 2);
    }

    public void removeChunkFromWorld(IsoChunk chunk) {
        if (PathfindNativeThread.instance != null) {
            ChunkRemoveTask task = ChunkRemoveTask.alloc().init(chunk);
            PathfindNativeThread.instance.chunkTaskQueue.add(task);
            PathfindNativeThread.instance.wake();
        }
    }

    public void squareChanged(IsoGridSquare square) {
        if ((square.chunk.loadedBits & 2) != 0) {
            for (int i = 0; i < 8; i++) {
                IsoDirections dir = IsoDirections.fromIndex(i);
                IsoGridSquare square2 = square.getAdjacentSquare(dir);
                if (square2 != null) {
                    SquareUpdateTask task = SquareUpdateTask.alloc().init(square2);
                    PathfindNativeThread.instance.squareTaskQueue.add(task);
                }
            }

            SquareUpdateTask task = SquareUpdateTask.alloc().init(square);
            PathfindNativeThread.instance.squareTaskQueue.add(task);
            PathfindNativeThread.instance.wake();
        }
    }

    public void addVehicle(BaseVehicle vehicle) {
        VehicleState state = this.vehicleState.get(vehicle);
        if (state == null) {
            state = VehicleState.alloc();
            this.vehicleState.put(vehicle, state);
        } else {
            boolean task = true;
        }

        state.init(vehicle);
        VehicleAddTask task = VehicleAddTask.alloc().init(vehicle);
        PathfindNativeThread.instance.vehicleTaskQueue.add(task);
        PathfindNativeThread.instance.wake();
    }

    public void removeVehicle(BaseVehicle vehicle) {
        VehicleState vehicleState1 = this.vehicleState.remove(vehicle);
        if (vehicleState1 != null) {
            vehicleState1.release();
        }

        if (PathfindNativeThread.instance != null) {
            VehicleRemoveTask task = VehicleRemoveTask.alloc().init(vehicle);
            PathfindNativeThread.instance.vehicleTaskQueue.add(task);
            PathfindNativeThread.instance.wake();
        }
    }

    public void updateVehicle(BaseVehicle vehicle) {
        VehicleUpdateTask task = VehicleUpdateTask.alloc().init(vehicle);
        PathfindNativeThread.instance.vehicleTaskQueue.add(task);
        PathfindNativeThread.instance.wake();
    }

    public PathFindRequest addRequest(
        IPathfinder pathfinder, Mover mover, float startX, float startY, float startZ, float targetX, float targetY, float targetZ
    ) {
        this.cancelRequest(mover);
        PathFindRequest request = PathFindRequest.alloc().init(pathfinder, mover, startX, startY, startZ, targetX, targetY, targetZ);
        PathfindNativeThread.instance.requestMap.put(mover, request);
        PathRequestTask task = PathRequestTask.alloc().init(request);
        PathfindNativeThread.instance.requestTaskQueue.add(task);
        PathfindNativeThread.instance.wake();
        return request;
    }

    public void cancelRequest(Mover mover) {
        if (PathfindNativeThread.instance != null) {
            PathFindRequest request = PathfindNativeThread.instance.requestMap.remove(mover);
            if (request != null) {
                request.cancel = true;
            }
        }
    }

    public void updateMain() {
        ConcurrentLinkedQueue<IPathfindTask> queue = PathfindNativeThread.instance.taskReturnQueue;

        for (IPathfindTask task = queue.poll(); task != null; task = queue.poll()) {
            task.release();
        }

        ArrayList<BaseVehicle> vehicles = IsoWorld.instance.currentCell.getVehicles();

        for (int i = 0; i < vehicles.size(); i++) {
            BaseVehicle vehicle = vehicles.get(i);
            VehicleState state = this.vehicleState.get(vehicle);
            if (state != null && state.check()) {
                this.updateVehicle(vehicle);
            }
        }

        ConcurrentLinkedQueue<PathFindRequest> requestToMain = PathfindNativeThread.instance.requestToMain;

        for (PathFindRequest request1 = requestToMain.poll(); request1 != null; request1 = requestToMain.poll()) {
            if (PathfindNativeThread.instance.requestMap.get(request1.mover) == request1) {
                PathfindNativeThread.instance.requestMap.remove(request1.mover);
            }

            if (!request1.cancel) {
                if (request1.path.isEmpty()) {
                    request1.finder.Failed(request1.mover);
                } else {
                    request1.finder.Succeeded(request1.path, request1.mover);
                }
            }

            if (!request1.doNotRelease) {
                request1.release();
            }
        }
    }

    public int findPath(PathFindRequest request, ByteBuffer pathBB, boolean bRender) {
        this.requestBb.clear();
        this.requestBb.putFloat(request.startX);
        this.requestBb.putFloat(request.startY);
        this.requestBb.putFloat(request.startZ + 32.0F);
        this.requestBb.putFloat(request.targetX);
        this.requestBb.putFloat(request.targetY);
        this.requestBb.putFloat(request.targetZ + 32.0F);
        boolean bNPC = false;
        int moverType = 0;
        if (request.mover instanceof IsoPlayer isoPlayer && !(request.mover instanceof IsoAnimal)) {
            moverType = 1;
            bNPC = isoPlayer.isNPC();
        }

        if (request.mover instanceof IsoZombie) {
            moverType = 2;
        }

        this.requestBb.putInt(moverType);
        this.requestBb.put((byte)(bNPC ? 1 : 0));
        this.requestBb.put((byte)(request.canCrawl ? 1 : 0));
        this.requestBb.put((byte)(request.crawling ? 1 : 0));
        this.requestBb.put((byte)(request.ignoreCrawlCost ? 1 : 0));
        this.requestBb.put((byte)(request.canThump ? 1 : 0));
        this.requestBb.put((byte)(request.canClimbFences ? 1 : 0));
        this.requestBb.put((byte)(request.hasTarget ? 1 : 0));
        this.requestBb.put((byte)(request.canBend ? 1 : 0));
        this.requestBb.putInt(request.minLevel);
        this.requestBb.putInt(request.maxLevel);
        this.requestBb.put((byte)(bRender ? 1 : 0));
        this.requestBb.put((byte)(request.canClimbTallFences ? 1 : 0));
        pathBB.clear();
        return findPath(this.requestBb, pathBB);
    }

    public void render() {
        if (Core.debug) {
            if (IsoCamera.frameState.playerIndex == 0) {
                if (DebugOptions.instance.pathfindPathToMouseEnable.getValue()) {
                    IsoPlayer player = IsoPlayer.players[0];
                    if (player == null || player.isDead()) {
                        return;
                    }

                    if (GameKeyboard.isKeyPressed(209)) {
                        this.testZ = Math.max(this.testZ - 1, -32);
                    }

                    if (GameKeyboard.isKeyPressed(201)) {
                        this.testZ = Math.min(this.testZ + 1, 31);
                    }

                    float x = Mouse.getX();
                    float y = Mouse.getY();
                    int z = this.testZ;
                    float targetX = IsoUtils.XToIso(x, y, z);
                    float targetY = IsoUtils.YToIso(x, y, z);
                    float targetZ = z;
                    this.renderGridAtMouse(targetX, targetY, targetZ);
                    this.pathToMouse(player.getX(), player.getY(), player.getZ(), targetX, targetY, targetZ);
                }
            }
        }
    }

    private void renderGridAtMouse(float targetX, float targetY, float targetZ) {
        int targetXi = PZMath.fastfloor(targetX);
        int targetYi = PZMath.fastfloor(targetY);
        int targetZi = PZMath.fastfloor(targetZ);

        for (int dy = -1; dy <= 2; dy++) {
            LineDrawer.addLine(targetXi - 1, targetYi + dy, targetZi, targetXi + 2, targetYi + dy, targetZi, 0.3F, 0.3F, 0.3F, null, false);
        }

        for (int dx = -1; dx <= 2; dx++) {
            LineDrawer.addLine(targetXi + dx, targetYi - 1, targetZi, targetXi + dx, targetYi + 2, targetZi, 0.3F, 0.3F, 0.3F, null, false);
        }

        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                float r = 0.3F;
                float g = 0.0F;
                float b = 0.0F;
                IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(targetXi + dx, targetYi + dy, targetZi);
                if (sq == null || sq.isSolid() || sq.isSolidTrans() || sq.HasStairs()) {
                    LineDrawer.addLine(targetXi + dx, targetYi + dy, targetZi, targetXi + dx + 1, targetYi + dy + 1, targetZi, 0.3F, 0.0F, 0.0F, null, false);
                }
            }
        }

        float rgb = 0.5F;
        if (this.testZ < PZMath.fastfloor(IsoPlayer.getInstance().getZ())) {
            LineDrawer.addLine(
                targetXi + 0.5F,
                targetYi + 0.5F,
                targetZi,
                targetXi + 0.5F,
                targetYi + 0.5F,
                PZMath.fastfloor(IsoPlayer.getInstance().getZ()),
                0.5F,
                0.5F,
                0.5F,
                null,
                true
            );
        } else if (this.testZ > PZMath.fastfloor(IsoPlayer.getInstance().getZ())) {
            LineDrawer.addLine(
                targetXi + 0.5F,
                targetYi + 0.5F,
                targetZi,
                targetXi + 0.5F,
                targetYi + 0.5F,
                PZMath.fastfloor(IsoPlayer.getInstance().getZ()),
                0.5F,
                0.5F,
                0.5F,
                null,
                true
            );
        }
    }

    private void pathToMouse(float startX, float startY, float startZ, float targetX, float targetY, float targetZ) {
        if (this.testRequestAdded) {
            if (this.finder.done) {
                this.testRequestAdded = false;
                if (GameWindow.states.current == IngameState.instance && !GameTime.isGamePaused() && Mouse.isButtonDown(0) && GameKeyboard.isKeyDown(42)) {
                    IsoPlayer.players[0].StopAllActionQueue();
                    Object obj = LuaManager.env.rawget("ISPathFindAction_pathToLocationF");
                    if (obj != null) {
                        LuaManager.caller.pcall(LuaManager.thread, obj, this.request.targetX, this.request.targetY, this.request.targetZ);
                    }
                }
            }
        } else {
            this.finder.path.clear();
            this.finder.done = false;
            this.request.init(this.finder, IsoPlayer.getInstance(), startX, startY, startZ, targetX, targetY, targetZ);
            this.request.doNotRelease = true;
            if (DebugOptions.instance.pathfindPathToMouseAllowCrawl.getValue()) {
                this.request.canCrawl = true;
                if (DebugOptions.instance.pathfindPathToMouseIgnoreCrawlCost.getValue()) {
                    this.request.ignoreCrawlCost = true;
                }
            }

            if (DebugOptions.instance.pathfindPathToMouseAllowThump.getValue()) {
                this.request.canThump = true;
            }

            PathRequestTask task = PathRequestTask.alloc();
            task.init(this.request);
            PathfindNativeThread.instance.requestTaskQueue.add(task);
            this.testRequestAdded = true;
            PathfindNativeThread.instance.wake();
        }

        if (GameWindow.states.current == DebugChunkState.instance) {
            this.updateMain();
        }
    }
}
