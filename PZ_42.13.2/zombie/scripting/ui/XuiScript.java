// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import zombie.UsedFromLua;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.scripting.ScriptParser;
import zombie.ui.UIFont;

@UsedFromLua
public class XuiScript {
    private static final String xui_prefix = "xui_";
    protected HashMap<String, XuiScript.XuiVar<?, ?>> varsMap = new HashMap<>();
    protected ArrayList<XuiScript.XuiVar<?, ?>> vars = new ArrayList<>();
    protected final ArrayList<XuiScript> children = new ArrayList<>();
    protected XuiSkin xuiSkin;
    protected final boolean readAltKeys;
    protected final XuiScriptType scriptType;
    protected final String xuiLayoutName;
    private XuiScript defaultStyle;
    private XuiScript style;
    public final String xuiUuid;
    public final XuiScript.XuiString xuiKey;
    public final XuiScript.XuiString xuiLuaClass;
    public final XuiScript.XuiString xuiStyle;
    public final XuiScript.XuiString xuiCustomDebug;
    public final XuiScript.XuiUnit x;
    public final XuiScript.XuiUnit y;
    public final XuiScript.XuiUnit width;
    public final XuiScript.XuiUnit height;
    public final XuiScript.XuiVector vector;
    public final XuiScript.XuiVectorPosAlign posAlign;
    public final XuiScript.XuiFloat minimumWidth;
    public final XuiScript.XuiFloat minimumHeight;
    public final XuiScript.XuiFloat maximumWidth;
    public final XuiScript.XuiFloat maximumHeight;
    public final XuiScript.XuiUnit paddingTop;
    public final XuiScript.XuiUnit paddingRight;
    public final XuiScript.XuiUnit paddingBottom;
    public final XuiScript.XuiUnit paddingLeft;
    public final XuiScript.XuiSpacing padding;
    public final XuiScript.XuiUnit marginTop;
    public final XuiScript.XuiUnit marginRight;
    public final XuiScript.XuiUnit marginBottom;
    public final XuiScript.XuiUnit marginLeft;
    public final XuiScript.XuiSpacing margin;
    public final XuiScript.XuiTranslateString title;
    public final XuiScript.XuiTranslateString name;
    public final XuiScript.XuiFontType font;
    public final XuiScript.XuiFontType font2;
    public final XuiScript.XuiFontType font3;
    public final XuiScript.XuiTexture icon;
    public final XuiScript.XuiUnit iconX;
    public final XuiScript.XuiUnit iconY;
    public final XuiScript.XuiUnit iconWidth;
    public final XuiScript.XuiUnit iconHeight;
    public final XuiScript.XuiVector iconVector;
    public final XuiScript.XuiTexture image;
    public final XuiScript.XuiUnit imageX;
    public final XuiScript.XuiUnit imageY;
    public final XuiScript.XuiUnit imageWidth;
    public final XuiScript.XuiUnit imageHeight;
    public final XuiScript.XuiVector imageVector;
    public final XuiScript.XuiBoolean anchorLeft;
    public final XuiScript.XuiBoolean anchorRight;
    public final XuiScript.XuiBoolean anchorTop;
    public final XuiScript.XuiBoolean anchorBottom;
    public final XuiScript.XuiStringList animationList;
    public final XuiScript.XuiFloat animationTime;
    public final XuiScript.XuiTexture textureBackground;
    public final XuiScript.XuiTexture texture;
    public final XuiScript.XuiTexture textureOverride;
    public final XuiScript.XuiTexture tickTexture;
    public final XuiScript.XuiColor textColor;
    public final XuiScript.XuiColor backgroundColor;
    public final XuiScript.XuiColor backgroundColorMouseOver;
    public final XuiScript.XuiColor borderColor;
    public final XuiScript.XuiColor textureColor;
    public final XuiScript.XuiColor choicesColor;
    public final XuiScript.XuiColor gridColor;
    public final XuiScript.XuiBoolean displayBackground;
    public final XuiScript.XuiBoolean background;
    public final XuiScript.XuiBoolean drawGrid;
    public final XuiScript.XuiBoolean drawBackground;
    public final XuiScript.XuiBoolean drawBorder;
    public final XuiScript.XuiTranslateString tooltip;
    public final XuiScript.XuiColor hsbFactor;
    public final XuiScript.XuiBoolean moveWithMouse;
    public final XuiScript.XuiBoolean mouseOver;
    public final XuiScript.XuiTranslateString mouseOverText;
    public final XuiScript.XuiTextAlign textAlign;
    public final XuiScript.XuiBoolean doHighlight;
    public final XuiScript.XuiColor backgroundColorHl;
    public final XuiScript.XuiColor borderColorHl;
    public final XuiScript.XuiBoolean doValidHighlight;
    public final XuiScript.XuiColor backgroundColorHlVal;
    public final XuiScript.XuiColor borderColorHlVal;
    public final XuiScript.XuiBoolean doInvalidHighlight;
    public final XuiScript.XuiColor backgroundColorHlInv;
    public final XuiScript.XuiColor borderColorHlInv;
    public final XuiScript.XuiBoolean storeItem;
    public final XuiScript.XuiBoolean doBackDropTex;
    public final XuiScript.XuiColor backDropTexCol;
    public final XuiScript.XuiBoolean doToolTip;
    public final XuiScript.XuiBoolean mouseEnabled;
    public final XuiScript.XuiBoolean allowDropAlways;
    public final XuiScript.XuiTranslateString toolTipTextItem;
    public final XuiScript.XuiTranslateString toolTipTextLocked;
    public final XuiScript.XuiColor backgroundEmpty;
    public final XuiScript.XuiColor backgroundHover;
    public final XuiScript.XuiColor borderInput;
    public final XuiScript.XuiColor borderOutput;
    public final XuiScript.XuiColor borderValid;
    public final XuiScript.XuiColor borderInvalid;
    public final XuiScript.XuiColor borderLocked;
    public final XuiScript.XuiBoolean doBorderLocked;
    public final XuiScript.XuiBoolean pin;
    public final XuiScript.XuiBoolean resizable;
    public final XuiScript.XuiBoolean enableHeader;
    public final XuiScript.XuiFloat scaledWidth;
    public final XuiScript.XuiFloat scaledHeight;

    public XuiScript(String xuiLayoutName, boolean readAltKeys, String xuiLuaClass) {
        this(xuiLayoutName, readAltKeys, xuiLuaClass, XuiScriptType.Layout);
    }

    public XuiScript(String xuiLayoutName, boolean readAltKeys, String xuiLuaClass, XuiScriptType type) {
        this.xuiLayoutName = xuiLayoutName;
        this.readAltKeys = readAltKeys;
        this.scriptType = type;
        this.xuiUuid = UUID.randomUUID().toString();
        int order = 9000;
        this.xuiLuaClass = this.addVar(new XuiScript.XuiString(this, "xuiLuaClass", "ISUIElement"));
        if (xuiLuaClass != null) {
            this.xuiLuaClass.setValue(xuiLuaClass);
        }

        this.xuiLuaClass.setAutoApplyMode(XuiAutoApply.Forbidden);
        this.xuiLuaClass.setIgnoreStyling(true);
        order = this.xuiLuaClass.setUiOrder(--order);
        this.xuiKey = this.addVar(new XuiScript.XuiString(this, "xuiKey", UUID.randomUUID().toString()));
        this.xuiKey.setAutoApplyMode(XuiAutoApply.Forbidden);
        order = this.xuiKey.setUiOrder(--order);
        this.xuiStyle = this.addVar(new XuiScript.XuiString(this, "xuiStyle"));
        this.xuiStyle.setAutoApplyMode(XuiAutoApply.Forbidden);
        this.xuiStyle.setScriptLoadEnabled(false);
        this.xuiStyle.setIgnoreStyling(true);
        order = this.xuiStyle.setUiOrder(--order);
        this.xuiCustomDebug = this.addVar(new XuiScript.XuiString(this, "xuiCustomDebug"));
        this.xuiCustomDebug.setAutoApplyMode(XuiAutoApply.Forbidden);
        this.xuiCustomDebug.setIgnoreStyling(true);
        order = this.xuiCustomDebug.setUiOrder(--order);
        this.x = this.addVar(new XuiScript.XuiUnit(this, "x", 0.0F));
        this.x.setAutoApplyMode(XuiAutoApply.Forbidden);
        order = this.x.setUiOrder(--order);
        this.y = this.addVar(new XuiScript.XuiUnit(this, "y", 0.0F));
        this.y.setAutoApplyMode(XuiAutoApply.Forbidden);
        order = this.y.setUiOrder(--order);
        this.width = this.addVar(new XuiScript.XuiUnit(this, "width", 0.0F));
        this.width.setAutoApplyMode(XuiAutoApply.No);
        order = this.width.setUiOrder(--order);
        this.height = this.addVar(new XuiScript.XuiUnit(this, "height", 0.0F));
        this.height.setAutoApplyMode(XuiAutoApply.No);
        order = this.height.setUiOrder(--order);
        this.vector = this.addVar(new XuiScript.XuiVector(this, "vector", this.x, this.y, this.width, this.height));
        order = this.vector.setUiOrder(--order);
        this.posAlign = this.addVar(new XuiScript.XuiVectorPosAlign(this, "vectorPosAlign", VectorPosAlign.TopLeft));
        this.posAlign.setAutoApplyMode(XuiAutoApply.Always);
        order = this.posAlign.setUiOrder(--order);
        this.textAlign = this.addVar(new XuiScript.XuiTextAlign(this, "textAlign"));
        this.textAlign.setAutoApplyMode(XuiAutoApply.No);
        this.minimumWidth = this.addVar(new XuiScript.XuiFloat(this, "minimumWidth"));
        this.minimumHeight = this.addVar(new XuiScript.XuiFloat(this, "minimumHeight"));
        this.maximumWidth = this.addVar(new XuiScript.XuiFloat(this, "maximumWidth"));
        this.maximumHeight = this.addVar(new XuiScript.XuiFloat(this, "maximumHeight"));
        this.scaledWidth = this.addVar(new XuiScript.XuiFloat(this, "scaledWidth"));
        this.scaledHeight = this.addVar(new XuiScript.XuiFloat(this, "scaledHeight"));
        this.addVar(new XuiScript.XuiUnit(this, "maximumHeightPercent", -1.0F));
        this.paddingTop = this.addVar(new XuiScript.XuiUnit(this, "paddingTop", 0.0F));
        this.paddingRight = this.addVar(new XuiScript.XuiUnit(this, "paddingRight", 0.0F));
        this.paddingBottom = this.addVar(new XuiScript.XuiUnit(this, "paddingBottom", 0.0F));
        this.paddingLeft = this.addVar(new XuiScript.XuiUnit(this, "paddingLeft", 0.0F));
        this.padding = this.addVar(new XuiScript.XuiSpacing(this, "padding", this.paddingTop, this.paddingRight, this.paddingBottom, this.paddingLeft));
        this.marginTop = this.addVar(new XuiScript.XuiUnit(this, "marginTop", 0.0F));
        this.marginRight = this.addVar(new XuiScript.XuiUnit(this, "marginRight", 0.0F));
        this.marginBottom = this.addVar(new XuiScript.XuiUnit(this, "marginBottom", 0.0F));
        this.marginLeft = this.addVar(new XuiScript.XuiUnit(this, "marginLeft", 0.0F));
        this.margin = this.addVar(new XuiScript.XuiSpacing(this, "margin", this.marginTop, this.marginRight, this.marginBottom, this.marginLeft));
        this.icon = this.addVar(new XuiScript.XuiTexture(this, "icon"));
        this.iconX = this.addVar(new XuiScript.XuiUnit(this, "icon_x", 0.0F));
        this.iconX.setAutoApplyMode(XuiAutoApply.No);
        this.iconY = this.addVar(new XuiScript.XuiUnit(this, "icon_y", 0.0F));
        this.iconY.setAutoApplyMode(XuiAutoApply.No);
        this.iconWidth = this.addVar(new XuiScript.XuiUnit(this, "icon_width", 0.0F));
        this.iconWidth.setAutoApplyMode(XuiAutoApply.No);
        this.iconHeight = this.addVar(new XuiScript.XuiUnit(this, "icon_height", 0.0F));
        this.iconHeight.setAutoApplyMode(XuiAutoApply.No);
        this.iconVector = this.addVar(new XuiScript.XuiVector(this, "icon_vector", this.iconX, this.iconY, this.iconWidth, this.iconHeight));
        this.image = this.addVar(new XuiScript.XuiTexture(this, "image"));
        this.imageX = this.addVar(new XuiScript.XuiUnit(this, "image_x", 0.0F));
        this.imageX.setAutoApplyMode(XuiAutoApply.No);
        this.imageY = this.addVar(new XuiScript.XuiUnit(this, "image_y", 0.0F));
        this.imageY.setAutoApplyMode(XuiAutoApply.No);
        this.imageWidth = this.addVar(new XuiScript.XuiUnit(this, "image_width", 0.0F));
        this.imageWidth.setAutoApplyMode(XuiAutoApply.No);
        this.imageHeight = this.addVar(new XuiScript.XuiUnit(this, "image_height", 0.0F));
        this.imageHeight.setAutoApplyMode(XuiAutoApply.No);
        this.imageVector = this.addVar(new XuiScript.XuiVector(this, "image_vector", this.imageX, this.imageY, this.imageWidth, this.imageHeight));
        this.anchorLeft = this.addVar(new XuiScript.XuiBoolean(this, "anchorLeft"));
        this.anchorRight = this.addVar(new XuiScript.XuiBoolean(this, "anchorRight"));
        this.anchorTop = this.addVar(new XuiScript.XuiBoolean(this, "anchorTop"));
        this.anchorBottom = this.addVar(new XuiScript.XuiBoolean(this, "anchorBottom"));
        this.animationList = this.addVar(new XuiScript.XuiStringList(this, "animationList"));
        this.addVar(new XuiScript.XuiFloat(this, "r", 1.0F));
        this.addVar(new XuiScript.XuiFloat(this, "g", 1.0F));
        this.addVar(new XuiScript.XuiFloat(this, "b", 1.0F));
        this.addVar(new XuiScript.XuiFloat(this, "a", 1.0F));
        this.addVar(new XuiScript.XuiFloat(this, "textR", 1.0F));
        this.addVar(new XuiScript.XuiFloat(this, "textG", 1.0F));
        this.addVar(new XuiScript.XuiFloat(this, "textB", 1.0F));
        this.animationTime = this.addVar(new XuiScript.XuiFloat(this, "animationTime", 1.0F));
        this.addVar(new XuiScript.XuiFloat(this, "boxSize", 16.0F));
        this.addVar(new XuiScript.XuiFloat(this, "bubblesAlpha", 1.0F));
        this.addVar(new XuiScript.XuiFloat(this, "contentTransparency", 1.0F));
        this.addVar(new XuiScript.XuiFloat(this, "currentValue", 1.0F));
        this.addVar(new XuiScript.XuiFloat(this, "differenceAlpha", 1.0F));
        this.addVar(new XuiScript.XuiFloat(this, "gradientAlpha", 1.0F));
        this.addVar(new XuiScript.XuiFloat(this, "itemGap", 4.0F));
        this.addVar(new XuiScript.XuiFloat(this, "itemheight", 30.0F));
        this.addVar(new XuiScript.XuiFloat(this, "itemHgt", 30.0F));
        this.addVar(new XuiScript.XuiFloat(this, "itemPadY", 10.0F));
        this.addVar(new XuiScript.XuiFloat(this, "ledBlinkSpeed", 0.0F));
        this.addVar(new XuiScript.XuiFloat(this, "leftMargin", 0.0F));
        this.addVar(new XuiScript.XuiFloat(this, "minValue", 0.0F));
        this.addVar(new XuiScript.XuiInteger(this, "maxLength", 1));
        this.addVar(new XuiScript.XuiInteger(this, "maxLines", 1));
        this.addVar(new XuiScript.XuiFloat(this, "maxValue", 0.0F));
        this.addVar(new XuiScript.XuiFloat(this, "scrollX", 0.0F));
        this.addVar(new XuiScript.XuiFloat(this, "shiftValue", 0.0F));
        this.addVar(new XuiScript.XuiFloat(this, "stepValue", 0.0F));
        this.addVar(new XuiScript.XuiFloat(this, "tabHeight", 0.0F));
        this.addVar(new XuiScript.XuiFloat(this, "tabPadX", 20.0F));
        this.addVar(new XuiScript.XuiFloat(this, "tabTransparency", 1.0F));
        this.addVar(new XuiScript.XuiFloat(this, "textTransparency", 1.0F));
        this.addVar(new XuiScript.XuiFloat(this, "textGap", 4.0F));
        this.addVar(new XuiScript.XuiFloat(this, "triangleWidth", 1.0F));
        this.addVar(new XuiScript.XuiFontType(this, "defaultFont", UIFont.NewSmall));
        this.font = this.addVar(new XuiScript.XuiFontType(this, "font", UIFont.Small));
        this.font2 = this.addVar(new XuiScript.XuiFontType(this, "font2", UIFont.Small));
        this.font3 = this.addVar(new XuiScript.XuiFontType(this, "font3", UIFont.Small));
        this.addVar(new XuiScript.XuiFontType(this, "titleFont", UIFont.Small));
        this.addVar(new XuiScript.XuiTexture(this, "bubblesTex"));
        this.addVar(new XuiScript.XuiTexture(this, "closeButtonTexture"));
        this.addVar(new XuiScript.XuiTexture(this, "collapseButtonTexture"));
        this.addVar(new XuiScript.XuiTexture(this, "gradientTex"));
        this.addVar(new XuiScript.XuiTexture(this, "infoBtn"));
        this.addVar(new XuiScript.XuiTexture(this, "invbasic"));
        this.addVar(new XuiScript.XuiTexture(this, "lcdback"));
        this.addVar(new XuiScript.XuiTexture(this, "lcdfont"));
        this.addVar(new XuiScript.XuiTexture(this, "ledBackTexture"));
        this.addVar(new XuiScript.XuiTexture(this, "ledTexture"));
        this.addVar(new XuiScript.XuiTexture(this, "resizeimage"));
        this.addVar(new XuiScript.XuiTexture(this, "pinButtonTexture"));
        this.addVar(new XuiScript.XuiTexture(this, "progressTexture"));
        this.addVar(new XuiScript.XuiTexture(this, "statusbarbkg"));
        this.texture = this.addVar(new XuiScript.XuiTexture(this, "texture"));
        this.textureBackground = this.addVar(new XuiScript.XuiTexture(this, "textureBackground"));
        this.addVar(new XuiScript.XuiTexture(this, "texBtnLeft"));
        this.addVar(new XuiScript.XuiTexture(this, "texBtnRight"));
        this.textureOverride = this.addVar(new XuiScript.XuiTexture(this, "textureOverride"));
        this.tickTexture = this.addVar(new XuiScript.XuiTexture(this, "tickTexture"));
        this.addVar(new XuiScript.XuiTexture(this, "titlebarbkg"));
        this.addVar(new XuiScript.XuiColor(this, "altBgColor"));
        this.backDropTexCol = this.addVar(new XuiScript.XuiColor(this, "backDropTexCol"));
        this.backgroundColor = this.addVar(new XuiScript.XuiColor(this, "backgroundColor"));
        this.backgroundColorHl = this.addVar(new XuiScript.XuiColor(this, "backgroundColorHL"));
        this.backgroundColorHlInv = this.addVar(new XuiScript.XuiColor(this, "backgroundColorHLInv"));
        this.backgroundColorHlVal = this.addVar(new XuiScript.XuiColor(this, "backgroundColorHLVal"));
        this.backgroundColorMouseOver = this.addVar(new XuiScript.XuiColor(this, "backgroundColorMouseOver"));
        this.backgroundEmpty = this.addVar(new XuiScript.XuiColor(this, "backgroundEmpty"));
        this.backgroundHover = this.addVar(new XuiScript.XuiColor(this, "backgroundHover"));
        this.borderColor = this.addVar(new XuiScript.XuiColor(this, "borderColor"));
        this.borderColorHl = this.addVar(new XuiScript.XuiColor(this, "borderColorHL"));
        this.borderColorHlInv = this.addVar(new XuiScript.XuiColor(this, "borderColorHLInv "));
        this.borderColorHlVal = this.addVar(new XuiScript.XuiColor(this, "borderColorHLVal"));
        this.borderInput = this.addVar(new XuiScript.XuiColor(this, "borderInput"));
        this.borderInvalid = this.addVar(new XuiScript.XuiColor(this, "borderInvalid"));
        this.borderLocked = this.addVar(new XuiScript.XuiColor(this, "borderLocked"));
        this.borderOutput = this.addVar(new XuiScript.XuiColor(this, "borderOutput"));
        this.borderValid = this.addVar(new XuiScript.XuiColor(this, "borderValid"));
        this.addVar(new XuiScript.XuiColor(this, "buttonColor"));
        this.addVar(new XuiScript.XuiColor(this, "buttonMouseOverColor"));
        this.choicesColor = this.addVar(new XuiScript.XuiColor(this, "choicesColor"));
        this.addVar(new XuiScript.XuiColor(this, "detailInnerColor"));
        this.addVar(new XuiScript.XuiColor(this, "greyCol"));
        this.gridColor = this.addVar(new XuiScript.XuiColor(this, "gridColor"));
        this.hsbFactor = this.addVar(new XuiScript.XuiColor(this, "hsbFactor"));
        this.hsbFactor.setAutoApplyMode(XuiAutoApply.No);
        this.addVar(new XuiScript.XuiColor(this, "ledCol"));
        this.addVar(new XuiScript.XuiColor(this, "ledColor"));
        this.addVar(new XuiScript.XuiColor(this, "ledColOff"));
        this.addVar(new XuiScript.XuiColor(this, "ledTextColor"));
        this.addVar(new XuiScript.XuiColor(this, "listHeaderColor"));
        this.addVar(new XuiScript.XuiColor(this, "progressColor"));
        this.addVar(new XuiScript.XuiColor(this, "sliderColor"));
        this.addVar(new XuiScript.XuiColor(this, "sliderBarBorderColor"));
        this.addVar(new XuiScript.XuiColor(this, "sliderBarColor"));
        this.addVar(new XuiScript.XuiColor(this, "sliderBorderColor"));
        this.addVar(new XuiScript.XuiColor(this, "sliderMouseOverColor"));
        this.textColor = this.addVar(new XuiScript.XuiColor(this, "textColor"));
        this.addVar(new XuiScript.XuiColor(this, "textBackColor"));
        this.textureColor = this.addVar(new XuiScript.XuiColor(this, "textureColor"));
        this.addVar(new XuiScript.XuiColor(this, "widgetTextureColor"));
        this.addVar(new XuiScript.XuiBoolean(this, "allowDraggingTabs"));
        this.allowDropAlways = this.addVar(new XuiScript.XuiBoolean(this, "allowDropAlways"));
        this.addVar(new XuiScript.XuiBoolean(this, "allowTornOffTabs"));
        this.addVar(new XuiScript.XuiBoolean(this, "autoScale"));
        this.addVar(new XuiScript.XuiBoolean(this, "autosetheight"));
        this.background = this.addVar(new XuiScript.XuiBoolean(this, "background"));
        this.addVar(new XuiScript.XuiBoolean(this, "center"));
        this.addVar(new XuiScript.XuiBoolean(this, "centerTabs"));
        this.addVar(new XuiScript.XuiBoolean(this, "clearStentil"));
        this.addVar(new XuiScript.XuiBoolean(this, "clip"));
        this.displayBackground = this.addVar(new XuiScript.XuiBoolean(this, "displayBackground"));
        this.doBackDropTex = this.addVar(new XuiScript.XuiBoolean(this, "doBackDropTex"));
        this.doBorderLocked = this.addVar(new XuiScript.XuiBoolean(this, "doBorderLocked"));
        this.addVar(new XuiScript.XuiBoolean(this, "doButtons"));
        this.doHighlight = this.addVar(new XuiScript.XuiBoolean(this, "doHighlight"));
        this.doInvalidHighlight = this.addVar(new XuiScript.XuiBoolean(this, "doInvalidHighlight"));
        this.addVar(new XuiScript.XuiBoolean(this, "doLedBlink"));
        this.addVar(new XuiScript.XuiBoolean(this, "doScroll"));
        this.addVar(new XuiScript.XuiBoolean(this, "doTextBackdrop"));
        this.doToolTip = this.addVar(new XuiScript.XuiBoolean(this, "doToolTip"));
        this.doValidHighlight = this.addVar(new XuiScript.XuiBoolean(this, "doValidHighlight"));
        this.addVar(new XuiScript.XuiBoolean(this, "dragInside"));
        this.addVar(new XuiScript.XuiBoolean(this, "drawFrame"));
        this.drawGrid = this.addVar(new XuiScript.XuiBoolean(this, "drawGrid"));
        this.drawBackground = this.addVar(new XuiScript.XuiBoolean(this, "drawBackground"));
        this.drawBorder = this.addVar(new XuiScript.XuiBoolean(this, "drawBorder"));
        this.addVar(new XuiScript.XuiBoolean(this, "drawMeasures"));
        this.addVar(new XuiScript.XuiBoolean(this, "editable"));
        this.addVar(new XuiScript.XuiBoolean(this, "enable"));
        this.enableHeader = this.addVar(new XuiScript.XuiBoolean(this, "enableHeader"));
        this.addVar(new XuiScript.XuiBoolean(this, "equalTabWidth"));
        this.addVar(new XuiScript.XuiBoolean(this, "keeplog"));
        this.addVar(new XuiScript.XuiBoolean(this, "isOn", false));
        this.addVar(new XuiScript.XuiBoolean(this, "isVertical", false));
        this.addVar(new XuiScript.XuiBoolean(this, "ledIsOn", false));
        this.addVar(new XuiScript.XuiBoolean(this, "left", false));
        this.moveWithMouse = this.addVar(new XuiScript.XuiBoolean(this, "moveWithMouse", false));
        this.mouseEnabled = this.addVar(new XuiScript.XuiBoolean(this, "mouseEnabled"));
        this.mouseOver = this.addVar(new XuiScript.XuiBoolean(this, "mouseover", false));
        this.pin = this.addVar(new XuiScript.XuiBoolean(this, "pin"));
        this.resizable = this.addVar(new XuiScript.XuiBoolean(this, "resizable"));
        this.storeItem = this.addVar(new XuiScript.XuiBoolean(this, "storeItem"));
        this.addVar(new XuiScript.XuiTranslateString(this, "description"));
        this.addVar(new XuiScript.XuiTranslateString(this, "footNote"));
        this.mouseOverText = this.addVar(new XuiScript.XuiTranslateString(this, "mouseovertext"));
        this.name = this.addVar(new XuiScript.XuiTranslateString(this, "name", "NAME_NOT_SET"));
        this.title = this.addVar(new XuiScript.XuiTranslateString(this, "title", "TITLE_NOT_SET"));
        this.tooltip = this.addVar(new XuiScript.XuiTranslateString(this, "tooltip"));
        this.addVar(new XuiScript.XuiTranslateString(this, "toolTipText"));
        this.toolTipTextItem = this.addVar(new XuiScript.XuiTranslateString(this, "toolTipTextItem"));
        this.toolTipTextLocked = this.addVar(new XuiScript.XuiTranslateString(this, "toolTipTextLocked"));
        this.addVar(new XuiScript.XuiTranslateString(this, "translation"));
    }

    public String getXuiUUID() {
        return this.xuiUuid;
    }

    public String getXuiKey() {
        return this.xuiKey.value();
    }

    public XuiScript setXuiKey(String xuiKey) {
        this.xuiKey.setValue(xuiKey);
        return this;
    }

    public String getXuiLuaClass() {
        return this.xuiLuaClass.value();
    }

    public XuiScript setXuiLuaClass(String xuiLuaClass) {
        this.xuiLuaClass.setValue(xuiLuaClass);
        return this;
    }

    public String getXuiStyle() {
        return this.xuiStyle.value();
    }

    public XuiScript setXuiStyle(String xuiStyle) {
        this.xuiStyle.setValue(xuiStyle);
        return this;
    }

    public String getXuiCustomDebug() {
        return this.xuiCustomDebug.value();
    }

    public XuiScript.XuiVector getVector() {
        return this.vector;
    }

    public XuiScript.XuiSpacing getPadding() {
        return this.padding;
    }

    public XuiScript.XuiSpacing getMargin() {
        return this.margin;
    }

    public XuiScript.XuiVectorPosAlign getPosAlign() {
        return this.posAlign;
    }

    public XuiScript.XuiFloat getMinimumWidth() {
        return this.minimumWidth;
    }

    public XuiScript.XuiFloat getMinimumHeight() {
        return this.minimumHeight;
    }

    public XuiScript.XuiTranslateString getTitle() {
        return this.title;
    }

    public XuiScript.XuiTranslateString getName() {
        return this.name;
    }

    public XuiScript.XuiFontType getFont() {
        return this.font;
    }

    public XuiScript.XuiFontType getFont2() {
        return this.font2;
    }

    public XuiScript.XuiFontType getFont3() {
        return this.font3;
    }

    public XuiScript.XuiTexture getIcon() {
        return this.icon;
    }

    public XuiScript.XuiVector getIconVector() {
        return this.iconVector;
    }

    public XuiScript.XuiBoolean getAnchorLeft() {
        return this.anchorLeft;
    }

    public XuiScript.XuiBoolean getAnchorRight() {
        return this.anchorRight;
    }

    public XuiScript.XuiBoolean getAnchorTop() {
        return this.anchorTop;
    }

    public XuiScript.XuiBoolean getAnchorBottom() {
        return this.anchorBottom;
    }

    public XuiScript.XuiStringList getAnimationList() {
        return this.animationList;
    }

    public XuiScript.XuiFloat getAnimationTime() {
        return this.animationTime;
    }

    public XuiScript.XuiTexture getTextureBackground() {
        return this.textureBackground;
    }

    public XuiScript.XuiTexture getTexture() {
        return this.texture;
    }

    public XuiScript.XuiTexture getTextureOverride() {
        return this.textureOverride;
    }

    public XuiScript.XuiTexture getTickTexture() {
        return this.tickTexture;
    }

    public XuiScript.XuiColor getTextColor() {
        return this.textColor;
    }

    public XuiScript.XuiColor getBackgroundColor() {
        return this.backgroundColor;
    }

    public XuiScript.XuiColor getBackgroundColorMouseOver() {
        return this.backgroundColorMouseOver;
    }

    public XuiScript.XuiColor getBorderColor() {
        return this.borderColor;
    }

    public XuiScript.XuiColor getTextureColor() {
        return this.textureColor;
    }

    public XuiScript.XuiColor getChoicesColor() {
        return this.choicesColor;
    }

    public XuiScript.XuiColor getGridColor() {
        return this.gridColor;
    }

    public XuiScript.XuiBoolean getDisplayBackground() {
        return this.displayBackground;
    }

    public XuiScript.XuiBoolean getBackground() {
        return this.background;
    }

    public XuiScript.XuiBoolean getDrawGrid() {
        return this.drawGrid;
    }

    public XuiScript.XuiBoolean getDrawBackground() {
        return this.drawBackground;
    }

    public XuiScript.XuiBoolean getDrawBorder() {
        return this.drawBorder;
    }

    public XuiScript.XuiTranslateString getTooltip() {
        return this.tooltip;
    }

    public XuiScript.XuiTranslateString getMouseOverText() {
        return this.mouseOverText;
    }

    public XuiScript.XuiColor getHsbFactor() {
        return this.hsbFactor;
    }

    public XuiScript.XuiBoolean getMoveWithMouse() {
        return this.moveWithMouse;
    }

    public XuiScript.XuiTextAlign getTextAlign() {
        return this.textAlign;
    }

    public XuiScript.XuiBoolean getDoHighlight() {
        return this.doHighlight;
    }

    public XuiScript.XuiColor getBackgroundColorHL() {
        return this.backgroundColorHl;
    }

    public XuiScript.XuiColor getBorderColorHL() {
        return this.borderColorHl;
    }

    public XuiScript.XuiBoolean getDoValidHighlight() {
        return this.doValidHighlight;
    }

    public XuiScript.XuiColor getBackgroundColorHLVal() {
        return this.backgroundColorHlVal;
    }

    public XuiScript.XuiColor getBorderColorHLVal() {
        return this.borderColorHlVal;
    }

    public XuiScript.XuiBoolean getDoInvalidHighlight() {
        return this.doInvalidHighlight;
    }

    public XuiScript.XuiColor getBackgroundColorHLInv() {
        return this.backgroundColorHlInv;
    }

    public XuiScript.XuiColor getBorderColorHLInv() {
        return this.borderColorHlInv;
    }

    public XuiScript.XuiBoolean getStoreItem() {
        return this.storeItem;
    }

    public XuiScript.XuiBoolean getDoBackDropTex() {
        return this.doBackDropTex;
    }

    public XuiScript.XuiColor getBackDropTexCol() {
        return this.backDropTexCol;
    }

    public XuiScript.XuiBoolean getDoToolTip() {
        return this.doToolTip;
    }

    public XuiScript.XuiBoolean getMouseEnabled() {
        return this.mouseEnabled;
    }

    public XuiScript.XuiBoolean getAllowDropAlways() {
        return this.allowDropAlways;
    }

    public XuiScript.XuiTranslateString getToolTipTextItem() {
        return this.toolTipTextItem;
    }

    public XuiScript.XuiTranslateString getToolTipTextLocked() {
        return this.toolTipTextLocked;
    }

    public XuiScript.XuiColor getBackgroundEmpty() {
        return this.backgroundEmpty;
    }

    public XuiScript.XuiColor getBackgroundHover() {
        return this.backgroundHover;
    }

    public XuiScript.XuiColor getBorderInput() {
        return this.borderInput;
    }

    public XuiScript.XuiColor getBorderOutput() {
        return this.borderOutput;
    }

    public XuiScript.XuiColor getBorderValid() {
        return this.borderValid;
    }

    public XuiScript.XuiColor getBorderInvalid() {
        return this.borderInvalid;
    }

    public XuiScript.XuiColor getBorderLocked() {
        return this.borderLocked;
    }

    public XuiScript.XuiBoolean getDoBorderLocked() {
        return this.doBorderLocked;
    }

    public String getXuiLayoutName() {
        return this.xuiLayoutName != null ? this.xuiLayoutName : "null";
    }

    @Override
    public String toString() {
        String orig = super.toString();
        String config = this.getXuiLayoutName();
        String type = this.scriptType != null ? this.scriptType.toString() : "null";
        String luaClass = this.xuiLuaClass != null && this.xuiLuaClass.value() != null ? this.xuiLuaClass.value() : "null";
        String key = this.xuiKey != null && this.xuiKey.value() != null ? this.xuiKey.value() : "null";
        return "XuiScript [config=" + config + ", type=" + type + ", class=" + luaClass + ", key=" + key + ", u=" + orig + "]";
    }

    protected void logWithInfo(String s) {
        DebugLog.General.debugln(s);
        this.logInfo();
    }

    protected void warnWithInfo(String s) {
        DebugLog.General.debugln(s);
        this.logInfo();
    }

    protected void errorWithInfo(String s) {
        DebugLog.General.error(s);
        this.logInfo();
    }

    private void logInfo() {
        DebugLog.log(this.toString());
    }

    public XuiScript getStyle() {
        return this.style;
    }

    public void setStyle(XuiScript style) {
        if (style != null && !style.isStyle()) {
            this.errorWithInfo("XuiScript is not a style.");
            DebugLog.log("StyleScript = " + style);
        } else if (style == null || this.style != style) {
            this.style = style;

            for (XuiScript.XuiVar var : this.vars) {
                if (style != null) {
                    var.style = (XuiScript.XuiVar<T, C>)style.getVar(var.getScriptKey());
                } else {
                    var.style = null;
                }
            }
        }
    }

    public XuiScript getDefaultStyle() {
        return this.defaultStyle;
    }

    public void setDefaultStyle(XuiScript defaultStyle) {
        if (defaultStyle != null && !defaultStyle.isDefaultStyle()) {
            this.errorWithInfo("XuiScript is not style.");
            DebugLog.log("StyleScript = " + defaultStyle);
        } else if (defaultStyle == null || this.defaultStyle != defaultStyle) {
            this.defaultStyle = defaultStyle;

            for (XuiScript.XuiVar var : this.vars) {
                if (defaultStyle != null) {
                    var.defaultStyle = (XuiScript.XuiVar<T, C>)defaultStyle.getVar(var.getScriptKey());
                } else {
                    var.defaultStyle = null;
                }
            }
        }
    }

    public boolean isLayout() {
        return this.scriptType == XuiScriptType.Layout;
    }

    public boolean isAnyStyle() {
        return this.scriptType == XuiScriptType.Style || this.scriptType == XuiScriptType.DefaultStyle;
    }

    public boolean isStyle() {
        return this.scriptType == XuiScriptType.Style;
    }

    public boolean isDefaultStyle() {
        return this.scriptType == XuiScriptType.DefaultStyle;
    }

    public XuiScriptType getScriptType() {
        return this.scriptType;
    }

    protected <T extends XuiScript.XuiVar<?, ?>> T addVar(T var) {
        if (this.varsMap.containsKey(var.getScriptKey())) {
            this.logInfo();
            throw new RuntimeException("Double script key");
        } else {
            this.vars.add(var);
            this.varsMap.put(var.getScriptKey(), var);
            return var;
        }
    }

    public XuiScript.XuiVar<?, ?> getVar(String key) {
        return this.varsMap.get(key);
    }

    public ArrayList<XuiScript.XuiVar<?, ?>> getVars() {
        return this.vars;
    }

    public void addChild(XuiScript child) {
        this.children.add(child);
    }

    public ArrayList<XuiScript> getChildren() {
        return this.children;
    }

    public static String ReadLuaClassValue(ScriptParser.Block block) {
        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (!key.isEmpty() && !val.isEmpty() && key.equalsIgnoreCase("xuiLuaClass")) {
                return val;
            }
        }

        return null;
    }

    public static XuiScript CreateScriptForClass(String xuiLayoutName, String luaClass, boolean readAltKeys, XuiScriptType scriptType) {
        if (luaClass != null) {
            switch (luaClass) {
                case "ISXuiTableLayout":
                    return new XuiTableScript(xuiLayoutName, readAltKeys, scriptType);
                case "Reference":
                    return new XuiReference(xuiLayoutName, readAltKeys);
                default:
                    return new XuiScript(xuiLayoutName, readAltKeys, luaClass, scriptType);
            }
        } else {
            return new XuiScript(xuiLayoutName, readAltKeys, luaClass, scriptType);
        }
    }

    public void Load(ScriptParser.Block block) {
        if (this.isLayout()) {
            for (ScriptParser.Value value : block.values) {
                String key = value.getKey().trim();
                String val = value.getValue().trim();
                if (!key.isEmpty() && !val.isEmpty()) {
                    if (this.xuiStyle.acceptsKey(key)) {
                        this.xuiStyle.fromString(val);
                    } else if (this.xuiLuaClass.acceptsKey(key)) {
                        if (!this.xuiLuaClass.isValueSet()) {
                            this.xuiLuaClass.fromString(val);
                        } else {
                            this.warnWithInfo("LuaClass defined in script but already set in constructor, class: " + this.xuiLuaClass.value());
                        }
                    }
                }
            }

            XuiScript style = XuiManager.GetStyle(this.xuiStyle.value());
            if (style != null) {
                this.setStyle(style);
            }

            this.tryToSetDefaultStyle();
        }

        for (ScriptParser.Value valuex : block.values) {
            String key = valuex.getKey().trim();
            String val = valuex.getValue().trim();
            if (!key.isEmpty() && !val.isEmpty()) {
                this.loadVar(key, val);
            }
        }

        for (ScriptParser.Block child : block.children) {
            if (this.isLayout() && child.type.equalsIgnoreCase("xui")) {
                XuiScript script = CreateScriptForClass(this.xuiLayoutName, child.id, this.readAltKeys, this.scriptType);
                script.Load(child);
                this.children.add(script);
            }
        }

        this.postLoad();
    }

    public boolean loadVar(String key, String val) {
        return this.loadVar(key, val, true);
    }

    public boolean loadVar(String key, String val, boolean allowNull) {
        for (int i = 0; i < this.vars.size(); i++) {
            if (this.vars.get(i).isScriptLoadEnabled()) {
                if (val != null && this.vars.get(i).acceptsKey(key)) {
                    return this.vars.get(i).load(key, val);
                }

                if (val == null && allowNull && this.vars.get(i).acceptsKey(key)) {
                    this.vars.get(i).setValue(null);
                    return true;
                }
            }
        }

        return false;
    }

    protected void tryToSetDefaultStyle() {
        if (this.isLayout() && this.xuiLuaClass.value() != null) {
            XuiScript def = XuiManager.GetDefaultStyle(this.xuiLuaClass.value());
            if (def != null) {
                this.setDefaultStyle(def);
            }
        }
    }

    protected void postLoad() {
        if (this.xuiLuaClass.value() != null) {
            if (this.backgroundColor.valueSet) {
                if ((
                        this.xuiLuaClass.value().equalsIgnoreCase("ISPanel")
                            || this.xuiLuaClass.value().equalsIgnoreCase("ISCollapsableWindow")
                            || this.xuiLuaClass.value().equalsIgnoreCase("ISCollapsableWindowJoypad")
                    )
                    && !this.background.valueSet) {
                    this.background.setValue(true);
                }

                if ((
                        this.xuiLuaClass.value().equalsIgnoreCase("ISXuiTableLayout")
                            || this.xuiLuaClass.value().equalsIgnoreCase("ISXuiTableLayoutCell")
                            || this.xuiLuaClass.value().equalsIgnoreCase("ISXuiTableLayoutRow")
                            || this.xuiLuaClass.value().equalsIgnoreCase("ISXuiTableLayoutColumn")
                    )
                    && !this.drawBackground.valueSet) {
                    this.drawBackground.setValue(true);
                }
            }

            if (this.borderColor.valueSet) {
                if (this.xuiLuaClass.value().equalsIgnoreCase("ISPanel") && !this.background.valueSet) {
                    this.background.setValue(true);
                }

                if ((
                        this.xuiLuaClass.value().equalsIgnoreCase("ISXuiTableLayout")
                            || this.xuiLuaClass.value().equalsIgnoreCase("ISXuiTableLayoutCell")
                            || this.xuiLuaClass.value().equalsIgnoreCase("ISXuiTableLayoutRow")
                            || this.xuiLuaClass.value().equalsIgnoreCase("ISXuiTableLayoutColumn")
                    )
                    && !this.drawBorder.valueSet) {
                    this.drawBorder.setValue(true);
                }
            }
        }
    }

    @UsedFromLua
    public static class XuiBoolean extends XuiScript.XuiVar<Boolean, XuiScript.XuiBoolean> {
        protected XuiBoolean(XuiScript parent, String key) {
            super(XuiVarType.Boolean, parent, key, false);
        }

        protected XuiBoolean(XuiScript parent, String key, boolean defaultVal) {
            super(XuiVarType.Boolean, parent, key, defaultVal);
        }

        @Override
        protected void fromString(String val) {
            try {
                this.setValue(Boolean.parseBoolean(val));
            } catch (Exception var3) {
                this.parent.logInfo();
                var3.printStackTrace();
            }
        }
    }

    @UsedFromLua
    public static class XuiColor extends XuiScript.XuiVar<Color, XuiScript.XuiColor> {
        protected XuiColor(XuiScript parent, String key) {
            super(XuiVarType.Color, parent, key);
        }

        protected XuiColor(XuiScript parent, String key, Color defaultVal) {
            super(XuiVarType.Color, parent, key, defaultVal);
        }

        @Override
        protected void fromString(String val) {
            try {
                Color color = null;
                if (this.parent.xuiSkin != null) {
                    color = this.parent.xuiSkin.color(val);
                }

                if (color == null) {
                    color = Colors.GetColorByName(val);
                }

                if (color == null && val.contains(":")) {
                    color = new Color();
                    String[] split = val.split(":");
                    if (split.length < 3) {
                        this.parent.errorWithInfo("Warning color has <3 values. color: " + val);
                    }

                    if (split.length > 1 && split[0].trim().equalsIgnoreCase("rgb")) {
                        for (int i = 1; i < split.length; i++) {
                            switch (i) {
                                case 1:
                                    color.r = Float.parseFloat(split[i].trim()) / 255.0F;
                                    break;
                                case 2:
                                    color.g = Float.parseFloat(split[i].trim()) / 255.0F;
                                    break;
                                case 3:
                                    color.b = Float.parseFloat(split[i].trim()) / 255.0F;
                                    break;
                                case 4:
                                    color.a = Float.parseFloat(split[i].trim()) / 255.0F;
                            }
                        }
                    } else {
                        for (int i = 0; i < split.length; i++) {
                            switch (i) {
                                case 0:
                                    color.r = Float.parseFloat(split[i].trim());
                                    break;
                                case 1:
                                    color.g = Float.parseFloat(split[i].trim());
                                    break;
                                case 2:
                                    color.b = Float.parseFloat(split[i].trim());
                                    break;
                                case 3:
                                    color.a = Float.parseFloat(split[i].trim());
                            }
                        }
                    }
                }

                if (color == null) {
                    throw new Exception("Could not read color: " + val);
                }

                this.setValue(color);
            } catch (Exception var5) {
                this.parent.logInfo();
                var5.printStackTrace();
            }
        }

        public float getR() {
            return this.value() != null ? this.value().r : 1.0F;
        }

        public float getG() {
            return this.value() != null ? this.value().g : 1.0F;
        }

        public float getB() {
            return this.value() != null ? this.value().b : 1.0F;
        }

        public float getA() {
            return this.value() != null ? this.value().a : 1.0F;
        }

        @Override
        public String getValueString() {
            return this.getR() + ", " + this.getG() + ", " + this.getB() + ", " + this.getA();
        }
    }

    @UsedFromLua
    public static class XuiDouble extends XuiScript.XuiVar<Double, XuiScript.XuiDouble> {
        protected XuiDouble(XuiScript parent, String key) {
            super(XuiVarType.Double, parent, key, 0.0);
        }

        protected XuiDouble(XuiScript parent, String key, double defaultVal) {
            super(XuiVarType.Double, parent, key, defaultVal);
        }

        @Override
        protected void fromString(String val) {
            try {
                this.setValue(Double.parseDouble(val));
            } catch (Exception var3) {
                this.parent.logInfo();
                var3.printStackTrace();
            }
        }
    }

    @UsedFromLua
    public static class XuiFloat extends XuiScript.XuiVar<Float, XuiScript.XuiFloat> {
        protected XuiFloat(XuiScript parent, String key) {
            super(XuiVarType.Float, parent, key, 0.0F);
        }

        protected XuiFloat(XuiScript parent, String key, float defaultVal) {
            super(XuiVarType.Float, parent, key, defaultVal);
        }

        @Override
        protected void fromString(String val) {
            try {
                this.setValue(Float.parseFloat(val));
            } catch (Exception var3) {
                this.parent.logInfo();
                var3.printStackTrace();
            }
        }
    }

    @UsedFromLua
    public static class XuiFontType extends XuiScript.XuiVar<UIFont, XuiScript.XuiFontType> {
        protected XuiFontType(XuiScript parent, String key) {
            super(XuiVarType.FontType, parent, key, UIFont.Small);
        }

        protected XuiFontType(XuiScript parent, String key, UIFont defaultVal) {
            super(XuiVarType.FontType, parent, key, defaultVal);
        }

        @Override
        protected void fromString(String val) {
            try {
                if (val.startsWith("UIFont.")) {
                    val = val.substring(val.indexOf(".") + 1);
                }

                this.setValue(UIFont.valueOf(val));
            } catch (Exception var3) {
                this.parent.logInfo();
                var3.printStackTrace();
            }
        }
    }

    @UsedFromLua
    public static class XuiFunction extends XuiScript.XuiVar<String, XuiScript.XuiFunction> {
        protected XuiFunction(XuiScript parent, String key) {
            super(XuiVarType.Function, parent, key);
        }

        protected XuiFunction(XuiScript parent, String key, String defaultVal) {
            super(XuiVarType.Function, parent, key, defaultVal);
        }

        @Override
        protected void fromString(String val) {
            this.setValue(val);
        }
    }

    @UsedFromLua
    public static class XuiInteger extends XuiScript.XuiVar<Integer, XuiScript.XuiInteger> {
        protected XuiInteger(XuiScript parent, String key) {
            super(XuiVarType.Integer, parent, key, 0);
        }

        protected XuiInteger(XuiScript parent, String key, int defaultVal) {
            super(XuiVarType.Integer, parent, key, defaultVal);
        }

        @Override
        protected void fromString(String val) {
            try {
                this.setValue(Integer.parseInt(val));
            } catch (Exception var3) {
                this.parent.logInfo();
                var3.printStackTrace();
            }
        }
    }

    @UsedFromLua
    public static class XuiSpacing extends XuiScript.XuiVar<Float, XuiScript.XuiSpacing> {
        private final XuiScript.XuiUnit top;
        private final XuiScript.XuiUnit right;
        private final XuiScript.XuiUnit bottom;
        private final XuiScript.XuiUnit left;

        public XuiSpacing(XuiScript parent, String key, XuiScript.XuiUnit top, XuiScript.XuiUnit right, XuiScript.XuiUnit bottom, XuiScript.XuiUnit left) {
            super(XuiVarType.Vector, parent, key, 0.0F);
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.left = left;
            this.setIgnoreStyling(true);
            this.setAutoApplyMode(XuiAutoApply.Forbidden);
            top.setAutoApplyMode(XuiAutoApply.No);
            right.setAutoApplyMode(XuiAutoApply.No);
            bottom.setAutoApplyMode(XuiAutoApply.No);
            left.setAutoApplyMode(XuiAutoApply.No);
        }

        @Override
        protected void fromString(String val) {
            throw new RuntimeException("Not implemented for XuiSpacing!");
        }

        @Override
        protected boolean load(String key, String val) {
            try {
                if (this.acceptsKey(key)) {
                    String[] split = val.split(":");

                    for (int i = 0; i < split.length; i++) {
                        String s = split[i].trim();
                        switch (i) {
                            case 0:
                                if (split.length == 1) {
                                    this.top.fromString(s);
                                    this.right.fromString(s);
                                    this.bottom.fromString(s);
                                    this.left.fromString(s);
                                } else if (split.length == 2) {
                                    this.top.fromString(s);
                                    this.bottom.fromString(s);
                                } else {
                                    this.top.fromString(s);
                                }
                                break;
                            case 1:
                                if (split.length != 2 && split.length != 3) {
                                    this.right.fromString(s);
                                } else {
                                    this.right.fromString(s);
                                    this.left.fromString(s);
                                }
                                break;
                            case 2:
                                this.bottom.fromString(s);
                                break;
                            case 3:
                                this.left.fromString(s);
                        }
                    }

                    return true;
                }
            } catch (Exception var6) {
                this.parent.logInfo();
                var6.printStackTrace();
            }

            return false;
        }

        public float getTop() {
            return this.top.value();
        }

        public float getRight() {
            return this.right.value();
        }

        public float getBottom() {
            return this.bottom.value();
        }

        public float getLeft() {
            return this.left.value();
        }

        public boolean isTopPercent() {
            return this.top.isPercent();
        }

        public boolean isRightPercent() {
            return this.right.isPercent();
        }

        public boolean isBottomPercent() {
            return this.bottom.isPercent();
        }

        public boolean isLeftPercent() {
            return this.left.isPercent();
        }

        @Override
        public boolean isValueSet() {
            return this.top.isValueSet() || this.right.isValueSet() || this.bottom.isValueSet() || this.left.isValueSet();
        }

        @Override
        public String getValueString() {
            return this.top.getValueString() + ", " + this.right.getValueString() + ", " + this.bottom.getValueString() + ", " + this.left.getValueString();
        }
    }

    @UsedFromLua
    public static class XuiString extends XuiScript.XuiVar<String, XuiScript.XuiString> {
        protected XuiString(XuiScript parent, String key) {
            super(XuiVarType.String, parent, key);
        }

        protected XuiString(XuiScript parent, String key, String defaultVal) {
            super(XuiVarType.String, parent, key, defaultVal);
        }

        @Override
        protected void fromString(String val) {
            this.setValue(val);
        }
    }

    @UsedFromLua
    public static class XuiStringList extends XuiScript.XuiVar<ArrayList<String>, XuiScript.XuiStringList> {
        protected XuiStringList(XuiScript parent, String key) {
            super(XuiVarType.StringList, parent, key, new ArrayList<>());
        }

        protected XuiStringList(XuiScript parent, String key, ArrayList<String> defaultVal) {
            super(XuiVarType.StringList, parent, key, defaultVal);
        }

        @Override
        protected void fromString(String val) {
            try {
                String[] split = val.split(":");
                ArrayList<String> list = new ArrayList<>(split.length);

                for (int i = 0; i < split.length; i++) {
                    list.add(split[i].trim());
                }

                this.setValue(list);
            } catch (Exception var5) {
                this.parent.logInfo();
                var5.printStackTrace();
            }
        }
    }

    @UsedFromLua
    public static class XuiTextAlign extends XuiScript.XuiVar<TextAlign, XuiScript.XuiTextAlign> {
        protected XuiTextAlign(XuiScript parent, String key) {
            super(XuiVarType.TextAlign, parent, key, TextAlign.Left);
        }

        protected XuiTextAlign(XuiScript parent, String key, TextAlign defaultVal) {
            super(XuiVarType.TextAlign, parent, key, defaultVal);
        }

        @Override
        protected void fromString(String val) {
            try {
                this.setValue(TextAlign.valueOf(val));
            } catch (Exception var3) {
                this.parent.logInfo();
                var3.printStackTrace();
            }
        }
    }

    @UsedFromLua
    public static class XuiTexture extends XuiScript.XuiVar<String, XuiScript.XuiTexture> {
        protected XuiTexture(XuiScript parent, String key) {
            super(XuiVarType.Texture, parent, key);
        }

        protected XuiTexture(XuiScript parent, String key, String defaultVal) {
            super(XuiVarType.Texture, parent, key, defaultVal);
        }

        public Texture getTexture() {
            if (this.value() != null) {
                Texture tex = Texture.getSharedTexture(this.value());
                if (tex != null) {
                    return tex;
                }

                if (Core.debug) {
                    DebugLog.General.warn("Could not find texture for: " + this.value());
                }
            }

            return null;
        }

        @Override
        protected void fromString(String val) {
            this.setValue(val);
        }
    }

    @UsedFromLua
    public static class XuiTranslateString extends XuiScript.XuiVar<String, XuiScript.XuiTranslateString> {
        protected XuiTranslateString(XuiScript parent, String key) {
            super(XuiVarType.TranslateString, parent, key);
        }

        protected XuiTranslateString(XuiScript parent, String key, String defaultVal) {
            super(XuiVarType.TranslateString, parent, key, defaultVal);
        }

        public String value() {
            return super.value() == null ? null : Translator.getText((String)super.value());
        }

        @Override
        protected void fromString(String val) {
            this.setValue(val);
        }

        @Override
        public String getValueString() {
            return super.value() != null ? (String)super.value() : "null";
        }
    }

    @UsedFromLua
    public static class XuiUnit extends XuiScript.XuiVar<Float, XuiScript.XuiUnit> {
        protected boolean isPercent;

        protected XuiUnit(XuiScript parent, String key) {
            super(XuiVarType.Unit, parent, key, 0.0F);
        }

        protected XuiUnit(XuiScript parent, String key, float defaultVal) {
            super(XuiVarType.Unit, parent, key, defaultVal);
        }

        @Override
        protected void fromString(String val) {
            try {
                this.isPercent = this.isPercent(val);
                this.setValue(this.getNum(val));
            } catch (Exception var3) {
                var3.printStackTrace();
            }
        }

        public void setValue(float val, boolean isPercent) {
            this.isPercent = isPercent;
            this.setValue(val);
        }

        public boolean isPercent() {
            if (this.parent.isLayout() && !this.valueSet && !this.isIgnoreStyling()) {
                if (this.style != null && this.style.isValueSet()) {
                    return ((XuiScript.XuiUnit)this.style).isPercent;
                }

                if (this.defaultStyle != null && this.defaultStyle.isValueSet()) {
                    return ((XuiScript.XuiUnit)this.defaultStyle).isPercent;
                }
            }

            return this.isPercent;
        }

        private boolean isPercent(String s) {
            return s.endsWith("%");
        }

        private float getNum(String s) {
            try {
                boolean isPercent = this.isPercent(s);
                String ss = isPercent ? s.substring(0, s.length() - 1) : s;
                ss = ss.trim();
                float f = Float.parseFloat(ss);
                if (isPercent) {
                    f /= 100.0F;
                }

                return f;
            } catch (Exception var5) {
                this.parent.logInfo();
                var5.printStackTrace();
                return 0.0F;
            }
        }

        @Override
        public String getValueString() {
            return (this.value() != null ? this.value() : "null") + (this.isPercent() ? "%" : "");
        }
    }

    @UsedFromLua
    public abstract static class XuiVar<T, C extends XuiScript.XuiVar<?, ?>> {
        private int uiOrder = 1000;
        protected final XuiVarType type;
        protected final XuiScript parent;
        protected XuiScript.XuiVar<T, C> style;
        protected XuiScript.XuiVar<T, C> defaultStyle;
        protected boolean valueSet;
        private boolean scriptLoadEnabled = true;
        protected XuiAutoApply autoApply = XuiAutoApply.IfSet;
        protected T defaultValue;
        protected T value;
        protected final String luaTableKey;
        private boolean ignoreStyling;

        protected XuiVar(XuiVarType type, XuiScript parent, String key) {
            this(type, parent, key, null);
        }

        protected XuiVar(XuiVarType type, XuiScript parent, String key, T defaultVal) {
            this.type = Objects.requireNonNull(type);
            this.parent = Objects.requireNonNull(parent);
            this.luaTableKey = Objects.requireNonNull(key);
            this.defaultValue = defaultVal;
        }

        public XuiVarType getType() {
            return this.type;
        }

        public int setUiOrder(int order) {
            this.uiOrder = order;
            return this.uiOrder;
        }

        public int getUiOrder() {
            return this.uiOrder;
        }

        public XuiScript.XuiVar<T, C> getStyle() {
            return this.style;
        }

        public XuiScript.XuiVar<T, C> getDefaultStyle() {
            return this.defaultStyle;
        }

        public boolean isStyle() {
            return this.parent.isAnyStyle();
        }

        public void setScriptLoadEnabled(boolean b) {
            this.scriptLoadEnabled = b;
        }

        public boolean isScriptLoadEnabled() {
            return this.scriptLoadEnabled;
        }

        protected void setIgnoreStyling(boolean b) {
            this.ignoreStyling = b;
        }

        public boolean isIgnoreStyling() {
            return this.ignoreStyling;
        }

        protected void setDefaultValue(T value) {
            this.defaultValue = value;
        }

        protected T getDefaultValue() {
            return this.defaultValue;
        }

        public void setValue(T value) {
            this.value = value;
            this.valueSet = true;
        }

        public void setAutoApplyMode(XuiAutoApply autoApplyMode) {
            this.autoApply = autoApplyMode;
        }

        public XuiAutoApply getAutoApplyMode() {
            return this.autoApply;
        }

        public String getLuaTableKey() {
            return this.luaTableKey;
        }

        protected String getScriptKey() {
            return this.luaTableKey;
        }

        public boolean isValueSet() {
            if (this.parent.isLayout() && !this.valueSet && !this.ignoreStyling) {
                if (this.style != null && this.style.isValueSet()) {
                    return true;
                }

                if (this.defaultStyle != null && this.defaultStyle.isValueSet()) {
                    return true;
                }
            }

            return this.valueSet;
        }

        public XuiScriptType getValueType() {
            if (this.parent.isLayout() && !this.valueSet && !this.ignoreStyling) {
                if (this.style != null && this.style.isValueSet()) {
                    return XuiScriptType.Style;
                }

                if (this.defaultStyle != null && this.defaultStyle.isValueSet()) {
                    return XuiScriptType.DefaultStyle;
                }
            }

            return this.parent.getScriptType();
        }

        public T value() {
            if (this.parent.isLayout() && !this.valueSet && !this.ignoreStyling) {
                if (this.style != null && this.style.isValueSet()) {
                    return this.style.value();
                }

                if (this.defaultStyle != null && this.defaultStyle.isValueSet()) {
                    return this.defaultStyle.value();
                }
            }

            return this.valueSet ? this.value : this.defaultValue;
        }

        public String getValueString() {
            return this.value() != null ? this.value().toString() : "null";
        }

        protected boolean acceptsKey(String key) {
            return this.luaTableKey.equalsIgnoreCase(key);
        }

        protected abstract void fromString(String arg0);

        protected boolean load(String key, String val) {
            if (this.acceptsKey(key)) {
                this.fromString(val);
                return true;
            } else {
                return false;
            }
        }
    }

    @UsedFromLua
    public static class XuiVector extends XuiScript.XuiVar<Float, XuiScript.XuiVector> {
        private final XuiScript.XuiUnit x;
        private final XuiScript.XuiUnit y;
        private final XuiScript.XuiUnit w;
        private final XuiScript.XuiUnit h;

        public XuiVector(XuiScript parent, String key, XuiScript.XuiUnit x, XuiScript.XuiUnit y, XuiScript.XuiUnit w, XuiScript.XuiUnit h) {
            super(XuiVarType.Vector, parent, key, 0.0F);
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.setIgnoreStyling(true);
        }

        @Override
        protected void fromString(String val) {
            throw new RuntimeException("Not implemented for UIVector!");
        }

        @Override
        protected boolean load(String key, String val) {
            try {
                if (this.acceptsKey(key)) {
                    String[] split = val.split(":");

                    for (int i = 0; i < split.length; i++) {
                        String s = split[i].trim();
                        switch (i) {
                            case 0:
                                this.x.fromString(s);
                                break;
                            case 1:
                                this.y.fromString(s);
                                break;
                            case 2:
                                this.w.fromString(s);
                                break;
                            case 3:
                                this.h.fromString(s);
                        }
                    }

                    return true;
                }
            } catch (Exception var6) {
                this.parent.logInfo();
                var6.printStackTrace();
            }

            return false;
        }

        public float getX() {
            return this.x.value();
        }

        public float getY() {
            return this.y.value();
        }

        public float getWidth() {
            return this.w.value();
        }

        public float getHeight() {
            return this.h.value();
        }

        public float getW() {
            return this.w.value();
        }

        public float getH() {
            return this.h.value();
        }

        public boolean isxPercent() {
            return this.x.isPercent();
        }

        public boolean isyPercent() {
            return this.y.isPercent();
        }

        public boolean iswPercent() {
            return this.w.isPercent();
        }

        public boolean ishPercent() {
            return this.h.isPercent();
        }

        @Override
        public boolean isValueSet() {
            return this.x.isValueSet() || this.y.isValueSet() || this.w.isValueSet() || this.h.isValueSet();
        }

        @Override
        public String getValueString() {
            return this.x.getValueString() + ", " + this.y.getValueString() + ", " + this.w.getValueString() + ", " + this.h.getValueString();
        }
    }

    @UsedFromLua
    public static class XuiVectorPosAlign extends XuiScript.XuiVar<VectorPosAlign, XuiScript.XuiVectorPosAlign> {
        protected XuiVectorPosAlign(XuiScript parent, String key) {
            super(XuiVarType.VectorPosAlign, parent, key, VectorPosAlign.None);
        }

        protected XuiVectorPosAlign(XuiScript parent, String key, VectorPosAlign defaultVal) {
            super(XuiVarType.VectorPosAlign, parent, key, defaultVal);
        }

        @Override
        protected void fromString(String val) {
            try {
                this.setValue(VectorPosAlign.valueOf(val));
            } catch (Exception var3) {
                this.parent.logInfo();
                var3.printStackTrace();
            }
        }
    }
}
