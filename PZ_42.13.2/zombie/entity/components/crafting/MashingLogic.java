// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.crafting;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.GameEntityNetwork;
import zombie.entity.components.crafting.recipe.CraftRecipeData;
import zombie.entity.components.crafting.recipe.CraftRecipeManager;
import zombie.entity.components.resources.Resource;
import zombie.entity.components.resources.ResourceFluid;
import zombie.entity.components.resources.ResourceGroup;
import zombie.entity.components.resources.ResourceIO;
import zombie.entity.components.resources.Resources;
import zombie.entity.events.ComponentEvent;
import zombie.entity.events.EntityEvent;
import zombie.entity.network.EntityPacketData;
import zombie.entity.network.EntityPacketType;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.fields.character.PlayerID;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.entity.components.crafting.MashingLogicScript;
import zombie.ui.ObjectTooltip;
import zombie.util.StringUtils;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;
import zombie.util.list.PZUnmodifiableList;

@UsedFromLua
public class MashingLogic extends Component {
    private static final List<CraftRecipe> _emptyRecipeList = PZUnmodifiableList.wrap(new ArrayList<>());
    private static final List<Resource> _emptyResourceList = PZUnmodifiableList.wrap(new ArrayList<>());
    private String recipeTagQuery;
    private List<CraftRecipe> recipes;
    private final CraftRecipeData craftData;
    private final CraftRecipeData craftTestData;
    private CraftRecipe currentRecipe;
    private String resourceFluidId;
    private String inputsGroupName;
    private double elapsedTime;
    private double lastWorldAge = -1.0;
    private final List<Resource> internalResourceList = new ArrayList<>();
    private boolean startRequested;
    private boolean stopRequested;
    private IsoPlayer requestingPlayer;
    private float barrelConsumedAmount;

    private MashingLogic() {
        super(ComponentType.MashingLogic);
        this.craftData = CraftRecipeData.Alloc(CraftMode.Automation, true, false, true, false);
        this.craftTestData = CraftRecipeData.Alloc(CraftMode.Automation, true, false, true, false);
    }

    @Override
    protected void readFromScript(ComponentScript componentScript) {
        super.readFromScript(componentScript);
        MashingLogicScript script = (MashingLogicScript)componentScript;
        this.recipeTagQuery = null;
        this.setRecipeTagQuery(script.getRecipeTagQuery());
        this.inputsGroupName = script.getInputsGroupName();
        this.resourceFluidId = script.getResourceFluidID();
    }

    @Override
    public void DoTooltip(ObjectTooltip tooltipUI, ObjectTooltip.Layout layout) {
    }

    @Override
    protected void renderlast() {
    }

    @Override
    protected void reset() {
        super.reset();
        this.recipeTagQuery = null;
        this.recipes = null;
        this.craftData.reset();
        this.craftTestData.reset();
        this.startRequested = false;
        this.stopRequested = false;
        this.requestingPlayer = null;
        this.inputsGroupName = null;
        this.resourceFluidId = null;
        this.barrelConsumedAmount = 0.0F;
        this.internalResourceList.clear();
        this.clearRecipe();
    }

    private void clearRecipe() {
        this.elapsedTime = 0.0;
        this.lastWorldAge = -1.0;
        this.currentRecipe = null;
    }

    public double getElapsedTime() {
        return this.elapsedTime;
    }

    public void setElapsedTime(double time) {
        this.elapsedTime = time;
    }

    public double getLastWorldAge() {
        return this.lastWorldAge;
    }

    public void setLastWorldAge(double time) {
        this.lastWorldAge = time;
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

    CraftRecipeData getCraftData() {
        return this.craftData;
    }

    CraftRecipeData getCraftTestData() {
        return this.craftTestData;
    }

    public String getInputsGroupName() {
        return this.inputsGroupName;
    }

    public String getResourceFluidID() {
        return this.resourceFluidId;
    }

    public float getBarrelConsumedAmount() {
        return this.barrelConsumedAmount;
    }

    protected void setBarrelConsumedAmount(float amount) {
        this.barrelConsumedAmount = amount;
    }

    @Override
    public boolean isValid() {
        return super.isValid();
    }

    public String getRecipeTagQuery() {
        return this.recipeTagQuery;
    }

    public void setRecipeTagQuery(String recipeTagQuery) {
        if (this.recipeTagQuery == null || !this.recipeTagQuery.equalsIgnoreCase(recipeTagQuery)) {
            this.recipeTagQuery = recipeTagQuery;
            this.recipes = null;
            if (!StringUtils.isNullOrWhitespace(this.recipeTagQuery)) {
                this.recipes = CraftRecipeManager.queryRecipes(recipeTagQuery);
            }
        }
    }

    public List<CraftRecipe> getRecipes(List<CraftRecipe> list) {
        list.clear();
        if (this.recipes != null) {
            list.addAll(this.recipes);
        }

        return list;
    }

    protected List<CraftRecipe> getRecipes() {
        return this.recipes != null ? this.recipes : _emptyRecipeList;
    }

    public List<Resource> getInputResources(List<Resource> list) {
        Resources resources = this.getComponent(ComponentType.Resources);
        if (resources != null) {
            ResourceGroup group = resources.getResourceGroup(this.inputsGroupName);
            if (group != null) {
                return group.getResources(list, ResourceIO.Input);
            }
        }

        return _emptyResourceList;
    }

    public ResourceFluid getFluidBarrel() {
        Resources resources = this.getComponent(ComponentType.Resources);
        if (resources != null) {
            resources.getResource(this.resourceFluidId);
        }

        return null;
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
        if (this.isRunning() && this.elapsedTime != 0.0) {
            return this.elapsedTime >= this.currentRecipe.getTime() ? 1.0 : this.elapsedTime / this.currentRecipe.getTime();
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
            this.craftData.setRecipe(this.currentRecipe);
            this.craftTestData.setRecipe(this.currentRecipe);
        }
    }

    public CraftRecipe getPossibleRecipe() {
        List<Resource> inputs = this.getInputResources(this.internalResourceList);
        return inputs != null && !inputs.isEmpty() ? CraftUtil.getPossibleRecipe(this.craftTestData, this.recipes, inputs, null) : null;
    }

    @Override
    protected void onAddedToOwner() {
    }

    @Override
    protected void onRemovedFromOwner() {
    }

    @Override
    protected void onConnectComponents() {
    }

    @Override
    protected void onFirstCreation() {
    }

    @Override
    protected void onComponentEvent(ComponentEvent event) {
    }

    @Override
    protected void onEntityEvent(EntityEvent event) {
    }

    public CraftRecipeMonitor debugCanStart(IsoPlayer player) {
        CraftRecipeMonitor var4;
        try {
            DebugLog.General.println("=== Starting debug canStart test ===");
            CraftRecipeMonitor monitor = CraftRecipeMonitor.Create();
            if (!this.isValid()) {
                monitor.warn("Unable to start (not valid).");
                monitor.close();
                return monitor.seal();
            }

            if (!this.owner.isUsingPlayer(player)) {
                monitor.warn("Player is not the using player.");
                monitor.close();
                return monitor.seal();
            }

            monitor.logMashingLogic(this);
            List<Resource> inputs = this.getInputResources(this.internalResourceList);
            if (inputs != null && !inputs.isEmpty()) {
                monitor.logResources(inputs, null);
                return CraftUtil.debugCanStart(player, this.craftTestData, this.recipes, inputs, null, monitor);
            }

            monitor.warn("Inputs = " + inputs + ", size = " + (inputs != null ? inputs.size() : "NaN") + ".");
            monitor.close();
            var4 = monitor.seal();
        } catch (Exception var8) {
            var8.printStackTrace();
            return null;
        } finally {
            this.craftData.setMonitor(null);
            this.craftTestData.setMonitor(null);
        }

        return var4;
    }

    public boolean canStart(IsoPlayer player) {
        return this.canStart(StartMode.Manual, player);
    }

    protected boolean canStart(StartMode startMode, IsoPlayer player) {
        if (!this.isValid()) {
            return false;
        } else if (this.isRunning()) {
            return false;
        } else if (player != null && this.owner.isUsingPlayer(player)) {
            List<Resource> inputs = this.getInputResources(this.internalResourceList);
            return inputs == null || inputs.isEmpty() ? false : CraftUtil.canStart(this.craftTestData, this.recipes, inputs, null);
        } else {
            return false;
        }
    }

    public void start(IsoPlayer player) {
        if (player != null && this.owner.isUsingPlayer(player)) {
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
            if (force || this.owner.isUsingPlayer(player)) {
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
            case MashingLogicStartRequest:
                this.receiveStartRequest(input, senderConnection);
                return true;
            case MashingLogicStopRequest:
                this.receiveStopRequest(input, senderConnection);
                return true;
            default:
                return false;
        }
    }

    public void sendStartRequest(IsoPlayer player) {
        if (GameClient.client) {
            EntityPacketData data = GameEntityNetwork.createPacketData(EntityPacketType.MashingLogicStartRequest);
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
            EntityPacketData data = GameEntityNetwork.createPacketData(EntityPacketType.MashingLogicStopRequest);
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
        BitHeaderWrite header = BitHeader.allocWrite(BitHeader.HeaderSize.Byte, output);
        if (this.recipeTagQuery != null) {
            header.addFlags(1);
            GameWindow.WriteString(output, this.recipeTagQuery);
        }

        if (this.inputsGroupName != null) {
            header.addFlags(2);
            GameWindow.WriteString(output, this.inputsGroupName);
        }

        if (this.resourceFluidId != null) {
            header.addFlags(4);
            GameWindow.WriteString(output, this.resourceFluidId);
        }

        if (this.currentRecipe != null) {
            header.addFlags(8);
            GameWindow.WriteString(output, this.currentRecipe.getScriptObjectFullType());
            output.putLong(this.currentRecipe.getScriptVersion());
            output.putDouble(this.elapsedTime);
            output.putDouble(this.lastWorldAge);
            this.craftData.save(output);
        }

        if (this.barrelConsumedAmount > 0.0F) {
            header.addFlags(16);
            output.putFloat(this.barrelConsumedAmount);
        }

        header.write();
        header.release();
    }

    @Override
    protected void load(ByteBuffer input, int WorldVersion) throws IOException {
        super.load(input, WorldVersion);
        BitHeaderRead header = BitHeader.allocRead(BitHeader.HeaderSize.Byte, input);
        this.recipeTagQuery = null;
        this.inputsGroupName = null;
        this.resourceFluidId = null;
        this.barrelConsumedAmount = 0.0F;
        boolean recipeInvalidated = false;
        if (header.hasFlags(1)) {
            String loadedRecipeTagQuery = GameWindow.ReadString(input);
            this.setRecipeTagQuery(loadedRecipeTagQuery);
        }

        if (header.hasFlags(2)) {
            this.inputsGroupName = GameWindow.ReadString(input);
        }

        if (header.hasFlags(4)) {
            this.resourceFluidId = GameWindow.ReadString(input);
        }

        if (header.hasFlags(8)) {
            String recipeName = GameWindow.ReadString(input);
            CraftRecipe recipe = ScriptManager.instance.getCraftRecipe(recipeName);
            this.setRecipe(recipe);
            long scriptVersion = input.getLong();
            if (recipe == null || scriptVersion != recipe.getScriptVersion()) {
                recipeInvalidated = true;
                DebugLog.General
                    .warn("CraftRecipe '" + recipeName + "' is null (" + (recipe == null) + ", or has script version mismatch. Cancelling current craft.");
            }

            this.elapsedTime = input.getDouble();
            this.lastWorldAge = input.getDouble();
            if (this.currentRecipe == null) {
                this.elapsedTime = 0.0;
                this.lastWorldAge = -1.0;
            }

            if (!this.craftData.load(input, WorldVersion, recipe, recipeInvalidated)) {
                recipeInvalidated = true;
                this.craftData.setRecipe(null);
            }
        } else {
            this.setRecipe(null);
            this.elapsedTime = 0.0;
            this.lastWorldAge = -1.0;
        }

        if (header.hasFlags(16)) {
            this.barrelConsumedAmount = input.getFloat();
        }

        header.release();
        if (recipeInvalidated) {
            this.clearRecipe();
        }
    }
}
