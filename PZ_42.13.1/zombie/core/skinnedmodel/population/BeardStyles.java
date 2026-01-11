// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.population;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.characters.HairOutfitDefinitions;
import zombie.core.logger.ExceptionLogger;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.gameStates.ChooseGameInfo;

@XmlRootElement(name = "beardStyles")
@UsedFromLua
public class BeardStyles {
    @XmlElement(name = "style")
    public final ArrayList<BeardStyle> styles = new ArrayList<>();
    @XmlTransient
    public static BeardStyles instance;

    public static void init() {
        instance = Parse(
            ZomboidFileSystem.instance.base.canonicalFile.getAbsolutePath()
                + File.separator
                + ZomboidFileSystem.processFilePath("media/hairStyles/beardStyles.xml", File.separatorChar)
        );
        if (instance != null) {
            instance.styles.add(0, new BeardStyle());

            for (String modID : ZomboidFileSystem.instance.getModIDs()) {
                ChooseGameInfo.Mod mod = ChooseGameInfo.getAvailableModDetails(modID);
                if (mod != null) {
                    String modDir = mod.getVersionDir();
                    BeardStyles beardStyles = Parse(
                        modDir + File.separator + ZomboidFileSystem.processFilePath("media/hairStyles/beardStyles.xml", File.separatorChar)
                    );
                    if (beardStyles != null) {
                        for (BeardStyle styleMod : beardStyles.styles) {
                            BeardStyle style = instance.FindStyle(styleMod.name);
                            if (style == null) {
                                instance.styles.add(styleMod);
                            } else {
                                if (DebugLog.isEnabled(DebugType.Clothing)) {
                                    DebugLog.Clothing.println("mod \"%s\" overrides beard \"%s\"", modID, styleMod.name);
                                }

                                int index = instance.styles.indexOf(style);
                                instance.styles.set(index, styleMod);
                            }
                        }
                    } else {
                        modDir = mod.getCommonDir();
                        beardStyles = Parse(modDir + File.separator + ZomboidFileSystem.processFilePath("media/hairStyles/beardStyles.xml", File.separatorChar));
                        if (beardStyles != null) {
                            for (BeardStyle styleModx : beardStyles.styles) {
                                BeardStyle style = instance.FindStyle(styleModx.name);
                                if (style == null) {
                                    instance.styles.add(styleModx);
                                } else {
                                    if (DebugLog.isEnabled(DebugType.Clothing)) {
                                        DebugLog.Clothing.println("mod \"%s\" overrides beard \"%s\"", modID, styleModx.name);
                                    }

                                    int index = instance.styles.indexOf(style);
                                    instance.styles.set(index, styleModx);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void Reset() {
        if (instance != null) {
            instance.styles.clear();
            instance = null;
        }
    }

    public static BeardStyles Parse(String filename) {
        try {
            return parse(filename);
        } catch (FileNotFoundException var2) {
        } catch (IOException | JAXBException var3) {
            ExceptionLogger.logException(var3);
        }

        return null;
    }

    public static BeardStyles parse(String filename) throws JAXBException, IOException {
        BeardStyles var4;
        try (FileInputStream adrFile = new FileInputStream(filename)) {
            JAXBContext ctx = JAXBContext.newInstance(BeardStyles.class);
            Unmarshaller um = ctx.createUnmarshaller();
            var4 = (BeardStyles)um.unmarshal(adrFile);
        }

        return var4;
    }

    public BeardStyle FindStyle(String name) {
        for (int i = 0; i < this.styles.size(); i++) {
            BeardStyle style = this.styles.get(i);
            if (style.name.equalsIgnoreCase(name)) {
                return style;
            }
        }

        return null;
    }

    public String getRandomStyle(String outfitName) {
        return HairOutfitDefinitions.instance.getRandomBeard(outfitName, this.styles);
    }

    public BeardStyles getInstance() {
        return instance;
    }

    public ArrayList<BeardStyle> getAllStyles() {
        return this.styles;
    }
}
