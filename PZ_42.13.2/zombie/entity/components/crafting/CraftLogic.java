// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.crafting;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.Translator;
import zombie.core.raknet.UdpConnection;
import zombie.core.textures.Texture;
import zombie.core.utils.UpdateLimit;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.GameEntityNetwork;
import zombie.entity.components.crafting.recipe.CraftRecipeData;
import zombie.entity.components.crafting.recipe.CraftRecipeManager;
import zombie.entity.components.crafting.recipe.ItemDataList;
import zombie.entity.components.resources.Resource;
import zombie.entity.components.resources.ResourceGroup;
import zombie.entity.components.resources.ResourceIO;
import zombie.entity.components.resources.ResourceItem;
import zombie.entity.components.resources.ResourceType;
import zombie.entity.components.resources.Resources;
import zombie.entity.components.spriteconfig.SpriteOverlayConfig;
import zombie.entity.events.ComponentEvent;
import zombie.entity.network.EntityPacketData;
import zombie.entity.network.EntityPacketType;
import zombie.inventory.InventoryItem;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.fields.character.PlayerID;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.entity.components.crafting.CraftLogicScript;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.objects.Item;
import zombie.ui.ObjectTooltip;
import zombie.util.StringUtils;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;
import zombie.util.list.PZUnmodifiableList;

@DebugClassFields
@UsedFromLua
public class CraftLogic extends Component {
    private static final List<CraftRecipe> _emptyRecipeList = PZUnmodifiableList.wrap(new ArrayList<>());
    private static final List<Resource> _emptyResourceList = PZUnmodifiableList.wrap(new ArrayList<>());
    private String recipeTagQuery;
    private List<CraftRecipe> recipes;
    private StartMode startMode = StartMode.Manual;
    private boolean doAutomaticCraftCheck = true;
    private boolean startRequested;
    private boolean stopRequested;
    private IsoPlayer requestingPlayer;
    protected CraftRecipeData craftData;
    private final CraftRecipeData craftTestData;
    private String inputsGroupName;
    private String outputsGroupName;
    private String actionAnimOverride;
    private final ArrayList<CraftRecipeData> craftDataInProgress = new ArrayList<>();
    UpdateLimit limit = new UpdateLimit(1000L);

    private CraftLogic() {
        super(ComponentType.CraftLogic);
        this.craftData = CraftRecipeData.Alloc(CraftMode.Automation, true, false, true, false);
        this.craftTestData = CraftRecipeData.Alloc(CraftMode.Automation, true, false, true, false);
    }

    protected CraftLogic(ComponentType type) {
        super(type);
        this.craftData = CraftRecipeData.Alloc(CraftMode.Automation, true, false, true, false);
        this.craftTestData = CraftRecipeData.Alloc(CraftMode.Automation, true, false, true, false);
    }

    @Override
    protected void readFromScript(ComponentScript componentScript) {
        super.readFromScript(componentScript);
        CraftLogicScript script = (CraftLogicScript)componentScript;
        this.startMode = script.getStartMode();
        this.recipeTagQuery = null;
        this.setRecipeTagQuery(script.getRecipeTagQuery());
        this.inputsGroupName = script.getInputsGroupName();
        this.outputsGroupName = script.getOutputsGroupName();
        this.actionAnimOverride = script.getActionAnim();
        this.doAutomaticCraftCheck = this.startMode == StartMode.Automatic;
    }

    @Override
    public boolean isValid() {
        return super.isValid() && this.owner.hasComponent(ComponentType.Resources);
    }

    @Override
    protected void reset() {
        super.reset();
        this.recipeTagQuery = null;
        this.recipes = null;
        this.craftData.reset();
        this.craftTestData.reset();
        this.startMode = StartMode.Manual;
        this.doAutomaticCraftCheck = true;
        this.startRequested = false;
        this.stopRequested = false;
        this.requestingPlayer = null;
        this.inputsGroupName = null;
        this.outputsGroupName = null;
        this.actionAnimOverride = null;
        this.craftDataInProgress.clear();
    }

    public StartMode getStartMode() {
        return this.startMode;
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

    CraftRecipeData getPendingCraftData() {
        return this.craftData;
    }

    CraftRecipeData getFirstInProgressCraftData() {
        return !this.craftDataInProgress.isEmpty() ? this.craftDataInProgress.get(0) : null;
    }

    ArrayList<CraftRecipeData> getAllInProgressCraftData() {
        return this.craftDataInProgress;
    }

    boolean isCraftingMixedRecipes() {
        Item firstOutputItem = null;

        for (Resource resource : this.getOutputResources()) {
            if (resource instanceof ResourceItem) {
                if (firstOutputItem == null) {
                    InventoryItem inventoryItem = resource.peekItem();
                    if (inventoryItem != null) {
                        firstOutputItem = inventoryItem.getScriptItem();
                    }
                }

                if (firstOutputItem != null) {
                    for (InventoryItem inventoryItem : ((ResourceItem)resource).getStoredItems()) {
                        if (!firstOutputItem.equals(inventoryItem.getScriptItem())) {
                            return true;
                        }
                    }
                }
            }
        }

        if (!this.craftDataInProgress.isEmpty()) {
            CraftRecipe firstRecipe = this.craftDataInProgress.getFirst().getRecipe();

            for (CraftRecipeData recipeData : this.craftDataInProgress) {
                if (!firstRecipe.equals(recipeData.getRecipe())) {
                    return true;
                }

                if (firstOutputItem != null) {
                    for (int i = 0; i < recipeData.getToOutputItems().size(); i++) {
                        Item item = recipeData.getToOutputItems().getItem(i);
                        if (!firstOutputItem.equals(item)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    public int getActiveCraftCount() {
        return this.craftDataInProgress.size();
    }

    CraftRecipeData getCraftTestData() {
        return this.craftTestData;
    }

    public String getInputsGroupName() {
        return this.inputsGroupName;
    }

    public String getOutputsGroupName() {
        return this.outputsGroupName;
    }

    public String getActionAnimOverride() {
        return this.actionAnimOverride;
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

    public ArrayList<CraftRecipe> getRecipes(ArrayList<CraftRecipe> list) {
        list.clear();
        if (this.recipes != null) {
            list.addAll(this.recipes);
        }

        return list;
    }

    public List<CraftRecipe> getRecipes() {
        return this.recipes != null ? this.recipes : _emptyRecipeList;
    }

    public List<Resource> getInputResources() {
        Resources resources = this.getComponent(ComponentType.Resources);
        if (resources != null) {
            ResourceGroup group = resources.getResourceGroup(this.inputsGroupName);
            if (group != null) {
                return group.getResources();
            }
        }

        return _emptyResourceList;
    }

    public List<Resource> getOutputResources() {
        Resources resources = this.getComponent(ComponentType.Resources);
        if (resources != null) {
            ResourceGroup group = resources.getResourceGroup(this.outputsGroupName);
            if (group != null) {
                return group.getResources();
            }
        }

        return _emptyResourceList;
    }

    public boolean isRunning() {
        return !this.craftDataInProgress.isEmpty();
    }

    public CraftRecipe getCurrentRecipe() {
        return this.getCraftTestData().getRecipe();
    }

    public double getProgress(CraftRecipeData craftRecipeData) {
        if (craftRecipeData == null || craftRecipeData.getElapsedTime() == 0.0) {
            return 0.0;
        } else {
            return craftRecipeData.getElapsedTime() >= craftRecipeData.getRecipe().getTime()
                ? 1.0
                : craftRecipeData.getElapsedTime() / craftRecipeData.getRecipe().getTime();
        }
    }

    public void setRecipe(CraftRecipe recipe) {
        if (this.craftData.getRecipe() != recipe) {
            this.doAutomaticCraftCheck = this.startMode == StartMode.Automatic;
            this.craftData.setRecipe(recipe);
            this.craftTestData.setRecipe(recipe);
        }
    }

    public CraftRecipe getPossibleRecipe() {
        List<Resource> inputs = this.getInputResources();
        List<Resource> outputs = this.getOutputResources();
        return inputs != null && !inputs.isEmpty() ? CraftUtil.getPossibleRecipe(this.craftTestData, this.recipes, inputs, outputs) : null;
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
                monitor.logCraftLogic(this);
                List<Resource> inputsx = this.getInputResources();
                List<Resource> outputs = this.getOutputResources();
                monitor.logResources(inputsx, outputs);
                return CraftUtil.debugCanStart(player, this.craftTestData, this.recipes, inputsx, outputs, monitor);
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
        } else if (this.startMode == StartMode.Passive) {
            return false;
        } else if (this.startMode != startMode) {
            return false;
        } else if (this.startMode == StartMode.Manual && !this.owner.isUsingPlayer(player)) {
            return false;
        } else {
            List<Resource> inputs = this.getInputResources();
            List<Resource> outputs = this.getOutputResources();
            if (inputs == null || inputs.isEmpty()) {
                return false;
            } else {
                return !this.willOutputsAccommodate(this.craftTestData.getToOutputItems())
                    ? false
                    : CraftUtil.canPerformRecipe(this.craftTestData.getRecipe(), this.craftTestData, inputs, outputs);
            }
        }
    }

    private boolean willOutputsAccommodate(ItemDataList pendingItems) {
        return this.getFreeOutputSlotCount() >= pendingItems.size();
    }

    public int getFreeOutputSlotCount() {
        int pendingOutputCount = 0;

        for (CraftRecipeData craftRecipeData : this.getAllInProgressCraftData()) {
            pendingOutputCount += craftRecipeData.getToOutputItems().size();
        }

        int resourceFreeItemSlotCount = 0;

        for (Resource resource : this.getOutputResources()) {
            resourceFreeItemSlotCount += resource.getFreeItemCapacity();
        }

        return resourceFreeItemSlotCount - pendingOutputCount;
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

    protected void forceStopInternal() {
        if (!GameClient.client) {
            this.stopRequested = true;
            this.requestingPlayer = null;
        }
    }

    public void onStart() {
        this.craftData.setTargetVariableInputRatio(this.craftData.getVariableInputRatio());
        this.craftData.createRecipeOutputs(true, null, null);
        this.craftDataInProgress.add(this.craftData);
        this.craftData = CraftRecipeData.Alloc(CraftMode.Automation, true, false, true, false);
        this.updateSpriteOverlay(0.0);
    }

    public void onUpdate(CraftRecipeData craftRecipeData) {
        this.updateSpriteOverlay(this.getProgress(this.getFirstInProgressCraftData()));
        if (GameServer.server && this.limit.Check()) {
            this.sendCraftLogicSync();
        }
    }

    public void onStop(CraftRecipeData craftRecipeData, boolean isCancelled) {
        if (isCancelled) {
            this.clearSpriteOverlay();
        } else {
            this.updateSpriteOverlay(1.0);
        }
    }

    public void finaliseRecipe(CraftRecipeData craftRecipeData) {
        this.craftDataInProgress.remove(craftRecipeData);
        CraftRecipeData.Release(craftRecipeData);
    }

    @Override
    protected void onComponentEvent(ComponentEvent event) {
        if (event.getSender() instanceof Resources resources) {
            switch (event.getEventType()) {
                case OnContentsChanged:
                    ArrayList<Resource> outputs = new ArrayList<>();
                    resources.getResources(outputs, ResourceIO.Output, ResourceType.Item);
                    if (this.getFirstInProgressCraftData() == null && outputs.stream().allMatch(Resource::isEmpty)) {
                        this.clearSpriteOverlay();
                    }
            }
        }
    }

    private void clearSpriteOverlay() {
        SpriteOverlayConfig overlayConfig = this.getGameEntity().getComponent(ComponentType.SpriteOverlayConfig);
        if (overlayConfig != null) {
            overlayConfig.clearStyle();
        }
    }

    private void updateSpriteOverlay(double percentageComplete) {
        SpriteOverlayConfig overlayConfig = this.getGameEntity().getComponent(ComponentType.SpriteOverlayConfig);
        if (overlayConfig != null) {
            ArrayList<String> availableStyles = overlayConfig.getAvailableStyles();
            String bestStyle = this.getBestStyle(percentageComplete, availableStyles);
            overlayConfig.setStyle(bestStyle);
        }
    }

    private String getBestStyleName(List<String> availableStyles) {
        if (this.isCraftingMixedRecipes()) {
            for (String style : availableStyles) {
                if (style.startsWith("Default")) {
                    return "Default";
                }
            }
        }

        CraftRecipeData oldestRecipeData = this.getFirstInProgressCraftData();
        return oldestRecipeData != null && oldestRecipeData.getRecipe() != null
            ? oldestRecipeData.getRecipe().getOverlayMapper().getStyle(availableStyles, oldestRecipeData)
            : null;
    }

    private String getBestStyle(double percentageComplete, List<String> availableStyles) {
        String bestStyle = null;
        String styleName = this.getBestStyleName(availableStyles);
        if (styleName != null) {
            List<String> viableStyles = new ArrayList<>();

            for (String style : availableStyles) {
                if (style.startsWith(styleName)) {
                    viableStyles.add(style);
                }
            }

            double bestDiff = 2.0;

            for (String stylex : viableStyles) {
                String[] parts = stylex.split("_");
                double progressComponent = 0.0;
                if (parts.length > 1) {
                    progressComponent = Double.parseDouble(parts[1]);
                }

                double diff = percentageComplete - progressComponent;
                if (diff < bestDiff && diff >= 0.0) {
                    bestStyle = stylex;
                    bestDiff = diff;
                }
            }
        }

        return bestStyle;
    }

    @Override
    public void dumpContentsInSquare() {
        if (!GameClient.client) {
            if (this.isRunning()) {
                ArrayList<InventoryItem> consumedItems = new ArrayList<>();

                for (CraftRecipeData _craftData : this.craftDataInProgress) {
                    _craftData.getAllConsumedItems(consumedItems, true);
                }

                for (InventoryItem item : consumedItems) {
                    this.owner.getSquare().AddWorldInventoryItem(item, 0.0F, 0.0F, 0.0F);
                }

                this.forceStopInternal();
            }
        }
    }

    public void returnConsumedItemsToResourcesOrSquare(CraftRecipeData craftRecipeData) {
        if (!GameClient.client) {
            if (this.isRunning()) {
                ArrayList<InventoryItem> consumedItems = new ArrayList<>();
                craftRecipeData.getAllConsumedItems(consumedItems, true);
                List<Resource> resources = new ArrayList<>();
                resources.addAll(this.getInputResources());
                resources.addAll(this.getOutputResources());

                for (InventoryItem item : consumedItems) {
                    for (Resource resource : resources) {
                        if (resource.offerItem(item) == null) {
                            item = null;
                            break;
                        }
                    }

                    if (item != null) {
                        this.owner.getSquare().AddWorldInventoryItem(item, 0.0F, 0.0F, 0.0F);
                    }
                }
            }
        }
    }

    @Override
    public boolean isNoContainerOrEmpty() {
        return !this.isRunning();
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
            case CraftLogicSync:
                this.receiveCraftLogicSync(input, senderConnection);
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

            if (this.craftTestData != null && this.craftTestData.getRecipe() != null) {
                GameWindow.WriteString(data.bb, this.craftTestData.getRecipe().getName());
            } else {
                GameWindow.WriteString(data.bb, "");
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

            String recipeName = GameWindow.ReadStringUTF(input);
            if (!recipeName.isEmpty()) {
                CraftRecipe recipe = this.getRecipes().stream().filter(craftRecipe -> craftRecipe.getName().equals(recipeName)).findFirst().orElse(null);
                this.setRecipe(recipe);
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

    public void sendCraftLogicSync() {
        if (GameServer.server) {
            EntityPacketData data = GameEntityNetwork.createPacketData(EntityPacketType.CraftLogicSync);

            try {
                this.save(data.bb);
            } catch (IOException var3) {
                throw new RuntimeException(var3);
            }

            if (this.craftTestData != null && this.craftTestData.getRecipe() != null) {
                GameWindow.WriteString(data.bb, this.craftTestData.getRecipe().getName());
            } else {
                GameWindow.WriteString(data.bb, "");
            }

            this.sendServerPacket(data, null);
        }
    }

    protected void receiveCraftLogicSync(ByteBuffer input, UdpConnection senderConnection) throws IOException {
        if (GameClient.client) {
            this.load(input, 241);
            String recipeName = GameWindow.ReadStringUTF(input);
            if (!recipeName.isEmpty() && (this.craftData.getRecipe() == null || !recipeName.equals(this.craftData.getRecipe().getName()))) {
                CraftRecipe recipe = this.getRecipes().stream().filter(craftRecipe -> craftRecipe.getName().equals(recipeName)).findFirst().orElse(null);
                this.setRecipe(recipe);
            }
        }
    }

    public void doProgressTooltip(ObjectTooltip.Layout layout, Resource resource, CraftRecipeData craftRecipeData) {
        if (!this.isRunning()) {
            ObjectTooltip.LayoutItem item = layout.addItem();
            item.setLabel(Translator.getText("EC_CraftLogicTooltip_NoCraftInProgress"), 1.0F, 1.0F, 1.0F, 1.0F);
        } else {
            if (craftRecipeData != null) {
                ObjectTooltip.LayoutItem item = layout.addItem();
                item.setLabel(Translator.getText("EC_CraftLogicTooltip_Progress") + ":", 1.0F, 1.0F, 1.0F, 1.0F);
                double progress = this.getProgress(craftRecipeData) * 100.0;
                item.setValue(String.format(Locale.ENGLISH, "%.0f%%", progress), 1.0F, 1.0F, 0.8F, 1.0F);
                item = layout.addItem();
                item.setLabel(Translator.getText("EC_CraftLogicTooltip_TimeRemaining") + ":", 1.0F, 1.0F, 1.0F, 1.0F);
                int timeRemaining = craftRecipeData.getRecipe().getTime() - (int)craftRecipeData.getElapsedTime();
                int ss = timeRemaining % 60;
                int mm = timeRemaining / 60 % 60;
                int hh = timeRemaining / 3600 % 24;
                int dd = Math.floorDiv(timeRemaining, 86400);
                item.setValue(String.format(Locale.ENGLISH, "%02dd %02dh %02dm %02ds", dd, hh, mm, ss), 1.0F, 1.0F, 0.8F, 1.0F);
            }
        }
    }

    public ArrayList<Texture> getStatusIconsForInputItem(InventoryItem item, CraftRecipeData craftRecipeData) {
        return null;
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

        if (this.startMode != StartMode.Manual) {
            header.addFlags(2);
            output.put(this.startMode.getByteId());
        }

        if (this.inputsGroupName != null) {
            header.addFlags(4);
            GameWindow.WriteString(output, this.inputsGroupName);
        }

        if (this.outputsGroupName != null) {
            header.addFlags(8);
            GameWindow.WriteString(output, this.outputsGroupName);
        }

        if (this.isRunning()) {
            header.addFlags(16);
            this.saveInProgessCraftData(output);
        }

        if (this.actionAnimOverride != null) {
            header.addFlags(32);
            GameWindow.WriteString(output, this.actionAnimOverride);
        }

        header.write();
        header.release();
    }

    protected void saveInProgessCraftData(ByteBuffer output) throws IOException {
        output.putInt(this.getAllInProgressCraftData().size());

        for (int i = 0; i < this.getAllInProgressCraftData().size(); i++) {
            CraftRecipeData craftRecipeData = this.getAllInProgressCraftData().get(i);
            craftRecipeData.save(output);
        }
    }

    @Override
    protected void load(ByteBuffer input, int WorldVersion) throws IOException {
        super.load(input, WorldVersion);
        BitHeaderRead header = BitHeader.allocRead(BitHeader.HeaderSize.Byte, input);
        this.recipeTagQuery = null;
        this.startMode = StartMode.Manual;
        this.inputsGroupName = null;
        this.outputsGroupName = null;
        if (header.hasFlags(1)) {
            String loadedRecipeTagQuery = GameWindow.ReadString(input);
            this.setRecipeTagQuery(loadedRecipeTagQuery);
        }

        if (header.hasFlags(2)) {
            this.startMode = StartMode.fromByteId(input.get());
        }

        if (header.hasFlags(4)) {
            this.inputsGroupName = GameWindow.ReadString(input);
        }

        if (header.hasFlags(8)) {
            this.outputsGroupName = GameWindow.ReadString(input);
        }

        if (header.hasFlags(16)) {
            this.loadInProgressCraftData(input, WorldVersion);
        } else {
            this.craftDataInProgress.clear();
        }

        if (header.hasFlags(32)) {
            this.actionAnimOverride = GameWindow.ReadString(input);
        }

        header.release();
        this.doAutomaticCraftCheck = true;
    }

    protected void loadInProgressCraftData(ByteBuffer input, int WorldVersion) throws IOException {
        boolean recipeInvalidated = false;
        int numberOfInProgressCrafts = 1;
        if (WorldVersion >= 238) {
            numberOfInProgressCrafts = input.getInt();
        }

        for (int i = 0; i < this.craftDataInProgress.size() - numberOfInProgressCrafts; i++) {
            this.craftDataInProgress.removeFirst();
        }

        for (int i = 0; i < numberOfInProgressCrafts; i++) {
            CraftRecipeData craftRecipeData = null;
            boolean needToAdd = false;
            if (i < this.craftDataInProgress.size()) {
                craftRecipeData = this.craftDataInProgress.get(i);
            } else {
                craftRecipeData = CraftRecipeData.Alloc(CraftMode.Automation, true, false, true, false);
                needToAdd = true;
            }

            if (!craftRecipeData.load(input, WorldVersion, null, recipeInvalidated)) {
                recipeInvalidated = true;
            }

            if (recipeInvalidated) {
                CraftRecipeData.Release(craftRecipeData);
            } else {
                craftRecipeData.createRecipeOutputs(true, null, null);
                if (needToAdd) {
                    this.craftDataInProgress.add(craftRecipeData);
                }
            }
        }
    }
}
