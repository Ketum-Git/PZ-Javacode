// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.crafting;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import zombie.GameWindow;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.GameEntityNetwork;
import zombie.entity.components.crafting.recipe.CraftRecipeData;
import zombie.entity.components.crafting.recipe.CraftRecipeManager;
import zombie.entity.components.resources.Resource;
import zombie.entity.components.resources.ResourceGroup;
import zombie.entity.components.resources.ResourceItem;
import zombie.entity.components.resources.Resources;
import zombie.entity.network.EntityPacketData;
import zombie.entity.network.EntityPacketType;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.fields.character.PlayerID;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.entity.components.crafting.DryingLogicScript;
import zombie.util.StringUtils;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;
import zombie.util.list.PZUnmodifiableList;

public class DryingLogic extends Component {
    private static final ConcurrentLinkedDeque<DryingLogic.DryingSlot> SLOT_POOL = new ConcurrentLinkedDeque<>();
    private static final List<CraftRecipe> _emptyRecipeList = PZUnmodifiableList.wrap(new ArrayList<>());
    private static final List<Resource> _emptyResourceList = PZUnmodifiableList.wrap(new ArrayList<>());
    private String dryingRecipeTagQuery;
    private String fuelRecipeTagQuery;
    private List<CraftRecipe> dryingRecipes;
    private List<CraftRecipe> fuelRecipes;
    private StartMode startMode = StartMode.Manual;
    private CraftRecipe currentRecipe;
    private int elapsedTime;
    private boolean doAutomaticCraftCheck = true;
    private boolean startRequested;
    private boolean stopRequested;
    private IsoPlayer requestingPlayer;
    private final CraftRecipeData craftData;
    private final CraftRecipeData craftTestData;
    private String dryingInputsGroupName;
    private String dryingOutputsGroupName;
    private String fuelInputsGroupName;
    private String fuelOutputsGroupName;
    private final DryingLogic.DryingSlot[] dryingSlots = new DryingLogic.DryingSlot[16];
    private int dryingSlotSize;

    private static DryingLogic.DryingSlot allocDryingSlot(int index) {
        DryingLogic.DryingSlot o = SLOT_POOL.poll();
        if (o == null) {
            o = new DryingLogic.DryingSlot();
        }

        o.index = index;
        return o;
    }

    private static void releaseDryingSlot(DryingLogic.DryingSlot o) {
        try {
            if (o == null) {
                throw new IllegalArgumentException("Object cannot be null.");
            }

            assert !SLOT_POOL.contains(o);

            o.reset();
            SLOT_POOL.offer(o);
        } catch (Exception var2) {
            var2.printStackTrace();
        }
    }

    private DryingLogic() {
        super(ComponentType.DryingLogic);
        this.craftData = CraftRecipeData.Alloc(CraftMode.Automation, true, false, true, false);
        this.craftTestData = CraftRecipeData.Alloc(CraftMode.Automation, true, false, true, false);
    }

    @Override
    protected void readFromScript(ComponentScript componentScript) {
        super.readFromScript(componentScript);
        DryingLogicScript script = (DryingLogicScript)componentScript;
        this.startMode = script.getStartMode();
        this.dryingRecipeTagQuery = null;
        this.setDryingRecipeTagQuery(script.getDryingRecipeTagQuery());
        this.fuelRecipeTagQuery = null;
        this.setFuelRecipeTagQuery(script.getFuelRecipeTagQuery());
        this.dryingInputsGroupName = script.getInputsGroupName();
        this.dryingOutputsGroupName = script.getOutputsGroupName();
        this.fuelInputsGroupName = script.getFuelInputsGroupName();
        this.fuelOutputsGroupName = script.getFuelOutputsGroupName();
        this.doAutomaticCraftCheck = this.startMode == StartMode.Automatic;
    }

    @Override
    public boolean isValid() {
        return super.isValid() && this.owner.hasComponent(ComponentType.Resources);
    }

    @Override
    protected void reset() {
        super.reset();
        this.dryingRecipeTagQuery = null;
        this.fuelRecipeTagQuery = null;
        this.dryingRecipes = null;
        this.fuelRecipes = null;
        this.craftData.reset();
        this.craftTestData.reset();
        this.startMode = StartMode.Manual;
        this.doAutomaticCraftCheck = true;
        this.startRequested = false;
        this.stopRequested = false;
        this.requestingPlayer = null;
        this.dryingInputsGroupName = null;
        this.dryingOutputsGroupName = null;
        this.clearSlots();
        this.clearRecipe();
    }

    private void clearRecipe() {
        this.elapsedTime = 0;
        this.currentRecipe = null;
    }

    protected void clearSlots() {
        if (this.getSlotSize() != 0) {
            for (int i = 0; i < this.dryingSlotSize; i++) {
                DryingLogic.DryingSlot slot = this.dryingSlots[i];
                if (slot != null) {
                    releaseDryingSlot(slot);
                    this.dryingSlots[i] = null;
                }
            }

            this.dryingSlotSize = 0;
        }
    }

    public int getSlotSize() {
        return this.dryingSlotSize;
    }

    public DryingLogic.DryingSlot getSlot(int index) {
        return index >= 0 && index < this.dryingSlotSize ? this.dryingSlots[index] : null;
    }

    protected DryingLogic.DryingSlot createSlot(int index) {
        if (index >= 0 && index < this.dryingSlots.length) {
            DryingLogic.DryingSlot slot = allocDryingSlot(index);
            if (this.dryingSlots[index] != null) {
                releaseDryingSlot(this.dryingSlots[index]);
            }

            this.dryingSlots[index] = slot;
            this.dryingSlotSize = PZMath.max(index + 1, this.dryingSlotSize);
            return slot;
        } else {
            return null;
        }
    }

    public ResourceItem getInputSlotResource(int index) {
        Resources resources = this.getComponent(ComponentType.Resources);
        DryingLogic.DryingSlot slot = this.getSlot(index);
        if (resources != null && slot != null) {
            ResourceItem slotResource = (ResourceItem)resources.getResource(slot.inputResourceId);
            if (slotResource != null) {
                return slotResource;
            }
        }

        return null;
    }

    public ResourceItem getOutputSlotResource(int index) {
        Resources resources = this.getComponent(ComponentType.Resources);
        DryingLogic.DryingSlot slot = this.getSlot(index);
        if (resources != null && slot != null) {
            ResourceItem slotResource = (ResourceItem)resources.getResource(slot.outputResourceId);
            if (slotResource != null) {
                return slotResource;
            }
        }

        return null;
    }

    public StartMode getStartMode() {
        return this.startMode;
    }

    public int getElapsedTime() {
        return this.elapsedTime;
    }

    void setElapsedTime(int elapsedTime) {
        this.elapsedTime = elapsedTime;
    }

    public boolean isStartRequested() {
        return this.startRequested;
    }

    void setStartRequested(boolean b) {
        this.startRequested = b;
    }

    public boolean isStopRequested() {
        return this.stopRequested;
    }

    void setStopRequested(boolean b) {
        this.stopRequested = b;
    }

    public IsoPlayer getRequestingPlayer() {
        return this.requestingPlayer;
    }

    void setRequestingPlayer(IsoPlayer player) {
        this.requestingPlayer = player;
    }

    public boolean isDoAutomaticCraftCheck() {
        return this.doAutomaticCraftCheck;
    }

    void setDoAutomaticCraftCheck(boolean b) {
        this.doAutomaticCraftCheck = b;
    }

    CraftRecipeData getCraftData() {
        return this.craftData;
    }

    CraftRecipeData getCraftTestData() {
        return this.craftTestData;
    }

    public String getDryingInputsGroupName() {
        return this.dryingInputsGroupName;
    }

    public String getDryingOutputsGroupName() {
        return this.dryingOutputsGroupName;
    }

    public String getFuelInputsGroupName() {
        return this.fuelInputsGroupName;
    }

    public String getFuelOutputsGroupName() {
        return this.fuelOutputsGroupName;
    }

    public String getDryingRecipeTagQuery() {
        return this.dryingRecipeTagQuery;
    }

    public boolean isUsesFuel() {
        return this.fuelRecipeTagQuery != null;
    }

    public void setDryingRecipeTagQuery(String recipeTagQuery) {
        if (this.dryingRecipeTagQuery == null || !this.dryingRecipeTagQuery.equalsIgnoreCase(recipeTagQuery)) {
            this.dryingRecipeTagQuery = recipeTagQuery;
            this.dryingRecipes = null;
            if (!StringUtils.isNullOrWhitespace(this.dryingRecipeTagQuery)) {
                this.dryingRecipes = CraftRecipeManager.queryRecipes(recipeTagQuery);
            }
        }
    }

    public String getFuelRecipeTagQuery() {
        return this.fuelRecipeTagQuery;
    }

    public void setFuelRecipeTagQuery(String recipeTagQuery) {
        if (this.fuelRecipeTagQuery == null || !this.fuelRecipeTagQuery.equalsIgnoreCase(recipeTagQuery)) {
            this.fuelRecipeTagQuery = recipeTagQuery;
            this.fuelRecipes = null;
            if (!StringUtils.isNullOrWhitespace(this.fuelRecipeTagQuery)) {
                this.fuelRecipes = CraftRecipeManager.queryRecipes(recipeTagQuery);
            }
        }
    }

    public ArrayList<CraftRecipe> getDryingRecipes(ArrayList<CraftRecipe> list) {
        list.clear();
        if (this.dryingRecipes != null) {
            list.addAll(this.dryingRecipes);
        }

        return list;
    }

    protected List<CraftRecipe> getDryingRecipes() {
        return this.dryingRecipes != null ? this.dryingRecipes : _emptyRecipeList;
    }

    public ArrayList<CraftRecipe> getFuelRecipes(ArrayList<CraftRecipe> list) {
        list.clear();
        if (this.fuelRecipes != null) {
            list.addAll(this.fuelRecipes);
        }

        return list;
    }

    protected List<CraftRecipe> getFuelRecipes() {
        return this.fuelRecipes != null ? this.fuelRecipes : _emptyRecipeList;
    }

    public List<Resource> getDryingInputResources() {
        Resources resources = this.getComponent(ComponentType.Resources);
        if (resources != null) {
            ResourceGroup group = resources.getResourceGroup(this.dryingInputsGroupName);
            if (group != null) {
                return group.getResources();
            }
        }

        return _emptyResourceList;
    }

    public List<Resource> getDryingOutputResources() {
        Resources resources = this.getComponent(ComponentType.Resources);
        if (resources != null) {
            ResourceGroup group = resources.getResourceGroup(this.dryingOutputsGroupName);
            if (group != null) {
                return group.getResources();
            }
        }

        return _emptyResourceList;
    }

    public List<Resource> getFuelInputResources() {
        Resources resources = this.getComponent(ComponentType.Resources);
        if (resources != null) {
            ResourceGroup group = resources.getResourceGroup(this.fuelInputsGroupName);
            if (group != null) {
                return group.getResources();
            }
        }

        return _emptyResourceList;
    }

    public List<Resource> getFuelOutputResources() {
        Resources resources = this.getComponent(ComponentType.Resources);
        if (resources != null) {
            ResourceGroup group = resources.getResourceGroup(this.fuelOutputsGroupName);
            if (group != null) {
                return group.getResources();
            }
        }

        return _emptyResourceList;
    }

    public boolean isRunning() {
        return this.currentRecipe != null;
    }

    public boolean isFinished() {
        return this.isRunning() ? this.elapsedTime >= this.currentRecipe.getTime() : false;
    }

    public CraftRecipe getCurrentRecipe() {
        return this.currentRecipe;
    }

    public double getProgress() {
        if (this.isRunning() && this.elapsedTime != 0) {
            return this.elapsedTime >= this.currentRecipe.getTime() ? 1.0 : (double)this.elapsedTime / this.currentRecipe.getTime();
        } else {
            return 0.0;
        }
    }

    protected void setRecipe(CraftRecipe recipe) {
        if (recipe == null) {
            this.clearRecipe();
        } else if (this.currentRecipe != recipe) {
            this.clearRecipe();
            this.currentRecipe = recipe;
            this.doAutomaticCraftCheck = this.startMode == StartMode.Automatic;
            this.craftData.setRecipe(this.currentRecipe);
            this.craftTestData.setRecipe(this.currentRecipe);
        }
    }

    public CraftRecipe getPossibleRecipe() {
        List<Resource> inputs = this.getFuelInputResources();
        List<Resource> outputs = this.getFuelOutputResources();
        return inputs != null && !inputs.isEmpty() ? CraftUtil.getPossibleRecipe(this.craftTestData, this.fuelRecipes, inputs, outputs) : null;
    }

    @Override
    protected void onRemovedFromOwner() {
    }

    @Override
    protected void onConnectComponents() {
    }

    public CraftRecipeMonitor debugCanStart(IsoPlayer player) {
        CraftRecipeMonitor inputs;
        try {
            DebugLog.General.println("=== Starting debug canStart test ===");
            CraftRecipeMonitor monitor = CraftRecipeMonitor.Create();
            if (!this.isValid()) {
                monitor.warn("Unable to start (not valid).");
                monitor.close();
                return monitor.seal();
            }

            if (this.startMode != StartMode.Manual || this.owner.isUsingPlayer(player)) {
                monitor.logDryingLogic(this);
                List<Resource> inputsx = this.getFuelInputResources();
                List<Resource> outputs = this.getFuelOutputResources();
                monitor.logResources(inputsx, outputs);
                return CraftUtil.debugCanStart(player, this.craftTestData, this.fuelRecipes, inputsx, outputs, monitor);
            }

            monitor.warn("Player is not the using player.");
            monitor.close();
            inputs = monitor.seal();
        } catch (Exception var9) {
            var9.printStackTrace();
            return null;
        } finally {
            this.craftData.setMonitor(null);
            this.craftTestData.setMonitor(null);
        }

        return inputs;
    }

    public boolean canStart(IsoPlayer player) {
        return this.canStart(StartMode.Manual, player);
    }

    protected boolean canStart(StartMode startMode, IsoPlayer player) {
        if (!this.isValid()) {
            return false;
        } else if (this.isRunning() || this.startMode == StartMode.Passive) {
            return false;
        } else if (this.startMode != startMode) {
            return false;
        } else if (this.startMode == StartMode.Manual && !this.owner.isUsingPlayer(player)) {
            return false;
        } else {
            List<Resource> inputs = this.getFuelInputResources();
            List<Resource> outputs = this.getFuelOutputResources();
            return inputs == null || inputs.isEmpty() ? false : CraftUtil.canStart(this.craftTestData, this.fuelRecipes, inputs, outputs);
        }
    }

    public void start(IsoPlayer player) {
        if (this.startMode != StartMode.Manual || this.owner.isUsingPlayer(player)) {
            if (GameClient.client) {
                if (this.canStart(StartMode.Manual, player)) {
                    this.sendStartRequest(player);
                }
            } else {
                this.startRequested = true;
                this.requestingPlayer = player;
            }
        }
    }

    public void stop(IsoPlayer player) {
        this.stop(player, false);
    }

    public void stop(IsoPlayer player, boolean force) {
        if (this.isValid()) {
            if (this.startMode != StartMode.Manual || force || this.owner.isUsingPlayer(player)) {
                if (GameClient.client) {
                    this.sendStopRequest(player);
                } else {
                    this.stopRequested = true;
                    this.requestingPlayer = player;
                }
            }
        }
    }

    @Override
    protected boolean onReceivePacket(ByteBuffer input, EntityPacketType type, UdpConnection senderConnection) throws IOException {
        switch (type) {
            case CraftLogicStartRequest:
                this.receiveStartRequest(input, senderConnection);
                return true;
            case CraftLogicStopRequest:
                this.receiveStopRequest(input, senderConnection);
                return true;
            default:
                return false;
        }
    }

    public void sendStartRequest(IsoPlayer player) {
        if (GameClient.client) {
            EntityPacketData data = GameEntityNetwork.createPacketData(EntityPacketType.CraftLogicStartRequest);
            data.bb.put((byte)(player != null ? 1 : 0));
            if (player != null) {
                PlayerID playerID = new PlayerID();
                playerID.set(player);
                playerID.write(data.bb);
            }

            this.sendClientPacket(data);
        }
    }

    protected void receiveStartRequest(ByteBuffer input, UdpConnection senderConnection) throws IOException {
        if (GameServer.server) {
            IsoPlayer player = null;
            if (input.get() == 1) {
                PlayerID playerID = new PlayerID();
                playerID.parse(input, senderConnection);
                player = playerID.getPlayer();
                if (player == null) {
                    throw new IOException("Player not found.");
                }
            }

            this.start(player);
        }
    }

    public void sendStopRequest(IsoPlayer player) {
        if (GameClient.client) {
            EntityPacketData data = GameEntityNetwork.createPacketData(EntityPacketType.CraftLogicStopRequest);
            data.bb.put((byte)(player != null ? 1 : 0));
            if (player != null) {
                PlayerID playerID = new PlayerID();
                playerID.set(player);
                playerID.write(data.bb);
            }

            this.sendClientPacket(data);
        }
    }

    protected void receiveStopRequest(ByteBuffer input, UdpConnection senderConnection) throws IOException {
        if (GameServer.server) {
            IsoPlayer player = null;
            if (input.get() == 1) {
                PlayerID playerID = new PlayerID();
                playerID.parse(input, senderConnection);
                player = playerID.getPlayer();
                if (player == null) {
                    throw new IOException("Player not found.");
                }
            }

            this.stop(player);
        }
    }

    @Override
    protected void saveSyncData(ByteBuffer output) throws IOException {
        this.save(output);
    }

    @Override
    protected void loadSyncData(ByteBuffer input) throws IOException {
        this.load(input, 241);
    }

    @Override
    protected void save(ByteBuffer output) throws IOException {
        super.save(output);
        BitHeaderWrite header = BitHeader.allocWrite(BitHeader.HeaderSize.Short, output);
        if (this.dryingRecipeTagQuery != null) {
            header.addFlags(1);
            GameWindow.WriteString(output, this.dryingRecipeTagQuery);
        }

        if (this.fuelRecipeTagQuery != null) {
            header.addFlags(2);
            GameWindow.WriteString(output, this.fuelRecipeTagQuery);
        }

        if (this.startMode != StartMode.Manual) {
            header.addFlags(4);
            output.put(this.startMode.getByteId());
        }

        if (this.fuelInputsGroupName != null) {
            header.addFlags(8);
            GameWindow.WriteString(output, this.fuelInputsGroupName);
        }

        if (this.fuelOutputsGroupName != null) {
            header.addFlags(16);
            GameWindow.WriteString(output, this.fuelOutputsGroupName);
        }

        if (this.dryingInputsGroupName != null) {
            header.addFlags(32);
            GameWindow.WriteString(output, this.dryingInputsGroupName);
        }

        if (this.dryingOutputsGroupName != null) {
            header.addFlags(64);
            GameWindow.WriteString(output, this.dryingOutputsGroupName);
        }

        if (this.currentRecipe != null) {
            header.addFlags(128);
            GameWindow.WriteString(output, this.currentRecipe.getScriptObjectFullType());
            output.putLong(this.currentRecipe.getScriptVersion());
            output.putInt(this.elapsedTime);
            this.craftData.save(output);
        }

        header.write();
        header.release();
    }

    @Override
    protected void load(ByteBuffer input, int WorldVersion) throws IOException {
        super.load(input, WorldVersion);
        BitHeaderRead header = BitHeader.allocRead(BitHeader.HeaderSize.Short, input);
        this.dryingRecipeTagQuery = null;
        this.fuelRecipeTagQuery = null;
        this.startMode = StartMode.Manual;
        this.fuelInputsGroupName = null;
        this.fuelOutputsGroupName = null;
        this.dryingInputsGroupName = null;
        this.dryingOutputsGroupName = null;
        boolean recipeInvalidated = false;
        if (header.hasFlags(1)) {
            String loadedRecipeTagQuery = GameWindow.ReadString(input);
            this.setDryingRecipeTagQuery(loadedRecipeTagQuery);
        }

        if (header.hasFlags(2)) {
            String loadedRecipeTagQuery = GameWindow.ReadString(input);
            this.setFuelRecipeTagQuery(loadedRecipeTagQuery);
        }

        if (header.hasFlags(4)) {
            this.startMode = StartMode.fromByteId(input.get());
        }

        if (header.hasFlags(8)) {
            this.fuelInputsGroupName = GameWindow.ReadString(input);
        }

        if (header.hasFlags(16)) {
            this.fuelOutputsGroupName = GameWindow.ReadString(input);
        }

        if (header.hasFlags(32)) {
            this.dryingInputsGroupName = GameWindow.ReadString(input);
        }

        if (header.hasFlags(64)) {
            this.dryingOutputsGroupName = GameWindow.ReadString(input);
        }

        if (header.hasFlags(128)) {
            String recipeName = GameWindow.ReadString(input);
            CraftRecipe recipe = ScriptManager.instance.getCraftRecipe(recipeName);
            this.setRecipe(recipe);
            long scriptVersion = input.getLong();
            if (recipe == null || scriptVersion != recipe.getScriptVersion()) {
                recipeInvalidated = true;
                DebugLog.General
                    .warn("CraftRecipe '" + recipeName + "' is null (" + (recipe == null) + ", or has script version mismatch. Cancelling current craft.");
            }

            this.elapsedTime = input.getInt();
            if (this.currentRecipe == null) {
                this.elapsedTime = 0;
            }

            if (!this.craftData.load(input, WorldVersion, recipe, recipeInvalidated)) {
                recipeInvalidated = true;
                this.craftData.setRecipe(null);
            }
        } else {
            this.setRecipe(null);
            this.elapsedTime = 0;
        }

        header.release();
        if (recipeInvalidated) {
            this.clearRecipe();
            this.clearSlots();
        }

        this.doAutomaticCraftCheck = true;
    }

    public static class DryingSlot {
        private int index = -1;
        private CraftRecipe currentRecipe;
        private int elapsedTime;
        private String inputResourceId;
        private String outputResourceId;

        public int getIndex() {
            return this.index;
        }

        public CraftRecipe getCurrentRecipe() {
            return this.currentRecipe;
        }

        public int getElapsedTime() {
            return this.elapsedTime;
        }

        protected void setElapsedTime(int time) {
            this.elapsedTime = time;
        }

        public String getInputResourceID() {
            return this.inputResourceId;
        }

        public String getOutputResourceID() {
            return this.outputResourceId;
        }

        private void reset() {
            this.index = -1;
            this.inputResourceId = null;
            this.outputResourceId = null;
            this.clearRecipe();
        }

        protected void setRecipe(CraftRecipe recipe) {
            if (this.currentRecipe != null) {
                this.clearRecipe();
            }

            this.currentRecipe = recipe;
        }

        protected void clearRecipe() {
            this.currentRecipe = null;
            this.elapsedTime = 0;
        }

        protected void initialize(String inputResourceID, String outputResourceID) {
            if (this.inputResourceId == null
                || !this.inputResourceId.equals(inputResourceID)
                || this.outputResourceId == null
                || !this.outputResourceId.equals(outputResourceID)) {
                this.inputResourceId = inputResourceID;
                this.outputResourceId = outputResourceID;
                this.elapsedTime = 0;
                this.currentRecipe = null;
            }
        }

        private void save(ByteBuffer output) throws IOException {
            if (this.currentRecipe != null) {
                output.put((byte)1);
                GameWindow.WriteString(output, this.currentRecipe.getScriptObjectFullType());
                output.putLong(this.currentRecipe.getScriptVersion());
                output.putInt(this.elapsedTime);
            } else {
                output.put((byte)0);
            }

            GameWindow.WriteString(output, this.inputResourceId);
            GameWindow.WriteString(output, this.outputResourceId);
        }

        private void load(ByteBuffer input, int WorldVersion) throws IOException {
            if (input.get() == 1) {
                String recipeName = GameWindow.ReadString(input);
                long scriptVersion = input.getLong();
                this.elapsedTime = input.getInt();
                this.currentRecipe = ScriptManager.instance.getCraftRecipe(recipeName);
                if (this.currentRecipe == null || scriptVersion != this.currentRecipe.getScriptVersion()) {
                    DebugLog.General
                        .warn(
                            "DryingSlot["
                                + this.index
                                + "] CraftRecipe '"
                                + recipeName
                                + "' is null ("
                                + (this.currentRecipe == null)
                                + ", or has script version mismatch. Cancelling current craft."
                        );
                    this.currentRecipe = null;
                    this.elapsedTime = 0;
                }
            }

            this.inputResourceId = GameWindow.ReadString(input);
            this.outputResourceId = GameWindow.ReadString(input);
        }
    }
}
