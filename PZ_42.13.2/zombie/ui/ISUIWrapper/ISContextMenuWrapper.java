// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ui.ISUIWrapper;

import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.Lua.LuaManager;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.textures.Texture;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.ui.TextManager;
import zombie.ui.UIFont;

public class ISContextMenuWrapper extends ISPanelWrapper {
    public ISContextMenuWrapper(KahluaTable table) {
        super(table);
    }

    public ISContextMenuWrapper(double x, double y, double width, double height, double zoom) {
        super(x, y, width, height);
        KahluaTable self = (KahluaTable)LuaManager.env.rawget("ISContextMenu");
        this.table.setMetatable(self);
        self.rawset("__index", self);
        UIFont font = UIFont.Medium;
        double fontHgt = TextManager.instance.getFontFromEnum(font).getLineHeight();
        double padY = LuaHelpers.castDouble(this.table.rawget("padY"));
        this.table.rawset("x", x);
        this.table.rawset("y", y);
        this.table.rawset("zoom", zoom);
        this.table.rawset("font", font);
        this.table.rawset("padY", 6.0);
        this.table.rawset("fontHgt", fontHgt);
        this.table.rawset("itemHgt", fontHgt + padY * 2.0);
        this.table.rawset("padTopBottom", 0.0);
        this.table.rawset("borderColor", this.setRGBA(LuaManager.platform.newTable(), 1.0, 1.0, 1.0, 0.15));
        this.table.rawset("backgroundColor", this.setRGBA(LuaManager.platform.newTable(), 0.1, 0.1, 0.1, 0.7));
        this.table.rawset("backgroundColorMouseOver", this.setRGBA(LuaManager.platform.newTable(), 0.3, 0.3, 0.3, 1.0));
        this.table.rawset("width", width);
        this.table.rawset("height", height);
        this.table.rawset("anchorLeft", true);
        this.table.rawset("anchorRight", false);
        this.table.rawset("anchorTop", true);
        this.table.rawset("anchorBottom", false);
        this.table.rawset("parent", LuaManager.platform.newTable());
        this.table.rawset("keepOnScreen", true);
        this.table.rawset("options", LuaManager.platform.newTable());
        this.table.rawset("numOptions", 1.0);
        this.table.rawset("optionPool", LuaManager.platform.newTable());
        this.table.rawset("visibleCheck", false);
        this.table.rawset("forceVisible", true);
        this.table.rawset("toolTip", null);
        this.table.rawset("subOptionNums", 0.0);
        this.table.rawset("player", 0.0);
        this.table.rawset("scrollIndicatorHgt", 14.0);
        this.table.rawset("arrowUp", Texture.getSharedTexture("media/ui/ArrowUp.png"));
        this.table.rawset("arrowDown", Texture.getSharedTexture("media/ui/ArrowDown.png"));
        this.table.rawset("tickTexture", Texture.getSharedTexture("media/ui/inventoryPanes/Tickbox_Tick.png"));
    }

    private void clear() {
        KahluaTable options = this.getOptions();
        KahluaTable optionPool = this.getOptionPool();
        KahluaTableIterator iterator = options.iterator();

        while (iterator.advance()) {
            Object option = iterator.getValue();
            optionPool.rawset(optionPool.size() + 1, option);
        }

        options.wipe();
        this.table.rawset("numOptions", 1.0);
        this.table.rawset("mouseOver", -1.0);
        this.table.rawset("subMenu", null);
        this.setHeight(0.0);
        this.table.rawset("addedDefaultOptions", false);
    }

    private KahluaTable getOptions() {
        return (KahluaTable)this.table.rawget("options");
    }

    private KahluaTable getOptionPool() {
        return (KahluaTable)this.table.rawget("optionPool");
    }

    public Double getNumOptions() {
        return (Double)this.table.rawget("numOptions");
    }

    private void setNumOptions(Double value) {
        this.table.rawset("numOptions", value);
    }

    private KahluaTable allocOption(String name, Object target, Object onSelect, Object... params) {
        KahluaTable option = null;
        KahluaTable optionPool = this.getOptionPool();
        if (optionPool.isEmpty()) {
            option = LuaManager.platform.newTable();
        } else {
            option = (KahluaTable)optionPool.rawget(optionPool.size());
            optionPool.rawset(optionPool.size(), null);
        }

        option.wipe();
        option.rawset("id", this.getNumOptions());
        option.rawset("name", name);
        option.rawset("onSelect", onSelect);
        option.rawset("target", target);

        for (int i = 0; i < params.length; i++) {
            String key = String.format("param%s", i + 1);
            option.rawset(key, params[i]);
        }

        option.rawset("subOption", null);
        return option;
    }

    public void addSubMenu(KahluaTable option, KahluaTable menu) {
        option.rawset("subOption", menu.rawget("subOptionNums"));
    }

    public KahluaTable addOption(String name, Object target, Object onSelect, Object... params) {
        if (Core.getInstance().getGameMode().equals("Tutorial") && this.getOptionFromName(name) != null) {
            return null;
        } else {
            KahluaTable option = this.allocOption(name, target, onSelect, params);
            option.rawset("iconTexture", null);
            option.rawset("color", null);
            this.getOptions().rawset(this.getNumOptions(), option);
            this.setNumOptions(this.getNumOptions() + 1.0);
            this.calcHeight();
            this.setWidth(this.calcWidth());
            return option;
        }
    }

    public KahluaTable addDebugOption(String name, Object target, Object onSelect, Object... params) {
        if (DebugOptions.instance.getBoolean("UI.HideDebugContextMenuOptions")) {
            return null;
        } else {
            KahluaTable option = this.addOption(name, target, onSelect, params);
            option.rawset("iconTexture", Texture.getSharedTexture("media/textures/Item_Plumpabug_Left.png"));
            option.rawset("color", null);
            return option;
        }
    }

    public KahluaTable addGetUpOption(String name, Object target, Object onSelect, Object... params) {
        if (params.length > 9) {
            DebugType.General.error("ISContextMenuLogic:addGetUpOption - only 9 additional arguments are supported");
        }

        Object[] combinedParams = new Object[params.length + 2];
        combinedParams[0] = onSelect;
        combinedParams[1] = target;

        for (int i = 0; i < params.length; i++) {
            combinedParams[i + 2] = params[i];
        }

        return this.addOption(name, this.table, this.table.rawget("onGetUpAndThen"), combinedParams);
    }

    public KahluaTable getOptionFromName(String name) {
        KahluaTableIterator iterator = this.getOptions().iterator();

        while (iterator.advance()) {
            KahluaTableImpl option = (KahluaTableImpl)iterator.getValue();
            if (name.equals(option.rawgetStr("name"))) {
                return option;
            }
        }

        return null;
    }

    public void removeLastOption() {
        KahluaTable optionPool = this.getOptionPool();
        KahluaTable options = this.getOptions();
        KahluaTable lastOption = (KahluaTable)options.rawget(options.size());
        optionPool.rawset(optionPool.size() + 1, lastOption);
        options.rawset(options.size(), null);
        this.setNumOptions(this.getNumOptions() - 1.0);
        Object requestX = this.table.rawget("requestX");
        Object requestY = this.table.rawget("requestY");
        if (requestX != null && requestY != null) {
            double requestXVal = LuaHelpers.castDouble(requestX);
            double requestYVal = LuaHelpers.castDouble(requestY);
            this.setSlideGoalX(requestXVal + 20.0, requestXVal);
            this.setSlideGoalY(requestYVal - 10.0, requestYVal);
        }

        this.calcHeight();
        this.setWidth(this.calcWidth());
    }

    private void calcHeight() {
        double numOptions = LuaHelpers.castDouble(this.table.rawget("numOptions"));
        double itemHgt = LuaHelpers.castDouble(this.table.rawget("itemHgt"));
        double itemsHgt = (numOptions - 1.0) * itemHgt;
        double screenHgt = Core.getInstance().getScreenHeight();
        double padTopBottom = LuaHelpers.castDouble(this.table.rawget("padTopBottom"));
        if (itemsHgt + padTopBottom * 2.0 > screenHgt) {
            double scrollIndicatorHgt = LuaHelpers.castDouble(this.table.rawget("scrollIndicatorHgt"));
            double numVisibleItems = Math.floor((screenHgt - padTopBottom * 2.0 - scrollIndicatorHgt * 2.0) / itemHgt);
            double scrollAreaHeight = numVisibleItems * itemHgt;
            this.table.rawset("scrollAreaHeight", scrollAreaHeight);
            this.setHeight(scrollAreaHeight + padTopBottom * 2.0 + scrollIndicatorHgt * 2.0);
            this.setScrollHeight(itemsHgt);
        } else {
            this.table.rawset("scrollAreaHeight", itemsHgt);
            this.setHeight(itemsHgt + padTopBottom * 2.0);
            this.setScrollHeight(itemsHgt);
        }
    }

    private double calcWidth() {
        double maxWidth = 0.0;
        UIFont font = (UIFont)this.table.rawget("font");
        KahluaTable options = (KahluaTable)this.table.rawget("options");
        KahluaTableIterator iterator = options.iterator();

        while (iterator.advance()) {
            KahluaTable k = (KahluaTable)iterator.getValue();
            String name = LuaHelpers.castString(k.rawget("name"));
            double w = TextManager.instance.MeasureStringX(font, name);
            if (w > maxWidth) {
                maxWidth = w;
            }
        }

        double itemHgt = LuaHelpers.castDouble(this.table.rawget("itemHgt"));
        double iconSize = itemHgt - 12.0;
        double iconShiftX = 2.0;
        double textForIconShift = iconSize + 4.0 + 2.0;
        return Math.max(textForIconShift + maxWidth + 24.0 + TextManager.instance.MeasureStringX(font, ">") + 4.0, 100.0);
    }

    private void setSlideGoalX(double startX, double finalX) {
        this.setX(finalX);
        this.table.rawset("slideGoalX", null);
        if (this.isOptionSingleMenu()) {
            if (LuaHelpers.getJoypadState(LuaHelpers.castDouble(this.table.rawget("player")).intValue()) != null) {
                this.setX(startX);
                this.table.rawset("slideGoalX", finalX);
                this.table.rawset("slideGoalTime", System.currentTimeMillis());
            }
        }
    }

    private void setSlideGoalY(double startY, double finalY) {
        this.setY(finalY);
        this.table.rawset("slideGoalY", null);
        if (this.isOptionSingleMenu()) {
            if (LuaHelpers.getJoypadState(LuaHelpers.castDouble(this.table.rawget("player")).intValue()) != null) {
                this.setY(startY);
                this.table.rawset("slideGoalY", finalY);
                this.table.rawset("slideGoalDY", finalY - startY);
                this.table.rawset("slideGoalTime", System.currentTimeMillis());
            }
        }
    }

    private boolean isOptionSingleMenu() {
        return Core.getInstance().getOptionSingleContextMenu(LuaHelpers.castDouble(this.table.rawget("player")).intValue());
    }

    private void setFontFromOption() {
        String font = Core.getInstance().getOptionContextMenuFont();
        if (font.equals("Large")) {
            this.setFont(UIFont.Large);
        } else if (font.equals("Small")) {
            this.setFont(UIFont.Small);
        } else {
            this.setFont(UIFont.Medium);
        }
    }

    private void setFont(UIFont font) {
        double fontHgt = TextManager.instance.getFontHeight(font);
        double padY = LuaHelpers.castDouble(this.table.rawget("padY"));
        this.table.rawset("font", font);
        this.table.rawset("fontHgt", fontHgt);
        this.table.rawset("itemHgt", fontHgt + padY * 2.0);
    }

    public static ISContextMenuWrapper getNew(ISUIElementWrapper parentContext) {
        double player = LuaHelpers.castDouble(parentContext.getTable().rawget("player"));
        KahluaTable context = LuaHelpers.getPlayerContextMenu(player);
        ISUIElementWrapper contextWrapper = new ISContextMenuWrapper(context);
        ISContextMenuWrapper subInstance = null;
        KahluaTable subMenuPool = (KahluaTable)context.rawget("subMenuPool");
        if (subMenuPool.isEmpty()) {
            subInstance = new ISContextMenuWrapper(0.0, 0.0, 1.0, 1.0, 1.5);
        } else {
            subInstance = new ISContextMenuWrapper((KahluaTable)subMenuPool.rawget(subMenuPool.size()));
            subMenuPool.rawset(subMenuPool.size(), null);
        }

        context.rawset("subInstance", subInstance.getTable());
        subInstance.initialise();
        subInstance.instantiate();
        subInstance.addToUIManager();
        subInstance.clear();
        subInstance.setFontFromOption();
        subInstance.setX(parentContext.getX());
        subInstance.setY(parentContext.getY());
        subInstance.getTable().rawset("parent", parentContext.getTable());
        subInstance.getTable().rawset("forceVisible", true);
        subInstance.setVisible(false);
        subInstance.bringToTop();
        subInstance.getTable().rawset("player", player);
        contextWrapper.setForceCursorVisible(player == 0.0);
        double subOptionNums = LuaHelpers.castDouble(context.rawget("subOptionNums")) + 1.0;
        context.rawset("subOptionNums", subOptionNums);
        subInstance.getTable().rawset("subOptionNums", subOptionNums);
        KahluaTable instanceMap = (KahluaTable)context.rawget("instanceMap");
        instanceMap.rawset(subOptionNums, subInstance.getTable());
        return subInstance;
    }

    public KahluaTable addActionsOption(String text, Object getActionsFunction, Object... args) {
        Double player = (Double)this.table.rawget("player");
        IsoPlayer character = LuaManager.GlobalObject.getSpecificPlayer(player.intValue());
        return this.addOption(text, character, LuaManager.getFunctionObject("ISTimedActionQueue.queueActions"), getActionsFunction, args);
    }

    public KahluaTable getContextFromOption(String optionName) {
        return (KahluaTable)((KahluaTable)this.table.rawget("instanceMap")).rawget(this.getOptionFromName(optionName).rawget("subOption"));
    }
}
