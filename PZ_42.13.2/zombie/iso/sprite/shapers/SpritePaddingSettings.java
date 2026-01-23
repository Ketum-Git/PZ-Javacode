// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.sprite.shapers;

import java.io.File;
import java.io.IOException;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.transform.TransformerException;
import zombie.DebugFileWatcher;
import zombie.PredicatedFileWatcher;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.util.PZXmlParserException;
import zombie.util.PZXmlUtil;

public class SpritePaddingSettings {
    private static SpritePaddingSettings.Settings settings;
    private static String settingsFilePath;
    private static PredicatedFileWatcher fileWatcher;

    public static void settingsFileChanged(SpritePaddingSettings.Settings settings) {
        DebugLog.General.println("Settings file changed.");
        SpritePaddingSettings.settings = settings;
    }

    private static void loadSettings() {
        String settingsFilePath = getSettingsFilePath();
        File settingsFile = new File(settingsFilePath).getAbsoluteFile();
        if (settingsFile.isFile()) {
            try {
                settings = PZXmlUtil.parse(SpritePaddingSettings.Settings.class, settingsFile.getPath());
            } catch (PZXmlParserException var3) {
                DebugLog.General.printException(var3, "Error parsing file: " + settingsFilePath, LogSeverity.Warning);
                settings = new SpritePaddingSettings.Settings();
            }
        } else {
            settings = new SpritePaddingSettings.Settings();
            saveSettings();
        }

        if (fileWatcher == null) {
            fileWatcher = new PredicatedFileWatcher(settingsFilePath, SpritePaddingSettings.Settings.class, SpritePaddingSettings::settingsFileChanged);
            DebugFileWatcher.instance.add(fileWatcher);
        }
    }

    private static String getSettingsFilePath() {
        if (settingsFilePath == null) {
            settingsFilePath = ZomboidFileSystem.instance.getLocalWorkDirSub("SpritePaddingSettings.xml");
        }

        return settingsFilePath;
    }

    private static void saveSettings() {
        try {
            PZXmlUtil.write(settings, new File(getSettingsFilePath()).getAbsoluteFile());
        } catch (IOException | JAXBException | TransformerException var1) {
            var1.printStackTrace();
        }
    }

    public static SpritePaddingSettings.Settings getSettings() {
        if (settings == null) {
            loadSettings();
        }

        return settings;
    }

    public abstract static class GenericZoomBasedSettingGroup {
        public abstract <ZoomBasedSetting> ZoomBasedSetting getCurrentZoomSetting();

        public static <ZoomBasedSetting> ZoomBasedSetting getCurrentZoomSetting(
            ZoomBasedSetting zoomedIn, ZoomBasedSetting notZoomed, ZoomBasedSetting zoomedOut
        ) {
            float currentZoom = Core.getInstance().getCurrentPlayerZoom();
            if (currentZoom < 1.0F) {
                return zoomedIn;
            } else {
                return currentZoom == 1.0F ? notZoomed : zoomedOut;
            }
        }
    }

    @XmlRootElement(name = "FloorShaperDeDiamondSettings")
    public static class Settings {
        @XmlElement(name = "IsoPadding")
        public SpritePadding.IsoPaddingSettings isoPadding = new SpritePadding.IsoPaddingSettings();
        @XmlElement(name = "FloorDeDiamond")
        public FloorShaperDeDiamond.Settings floorDeDiamond = new FloorShaperDeDiamond.Settings();
        @XmlElement(name = "AttachedSprites")
        public FloorShaperAttachedSprites.Settings attachedSprites = new FloorShaperAttachedSprites.Settings();
    }
}
