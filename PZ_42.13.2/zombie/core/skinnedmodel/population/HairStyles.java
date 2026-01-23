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

@XmlRootElement(name = "hairStyles")
@UsedFromLua
public class HairStyles {
    @XmlElement(name = "male")
    public final ArrayList<HairStyle> maleStyles = new ArrayList<>();
    @XmlElement(name = "female")
    public final ArrayList<HairStyle> femaleStyles = new ArrayList<>();
    @XmlTransient
    public static HairStyles instance;

    public static void init() {
        instance = Parse(
            ZomboidFileSystem.instance.base.canonicalFile.getAbsolutePath()
                + File.separator
                + ZomboidFileSystem.processFilePath("media/hairStyles/hairStyles.xml", File.separatorChar)
        );
        if (instance != null) {
            for (String modID : ZomboidFileSystem.instance.getModIDs()) {
                ChooseGameInfo.Mod mod = ChooseGameInfo.getAvailableModDetails(modID);
                if (mod != null) {
                    String modDir = mod.getVersionDir();
                    HairStyles hairStyles = Parse(
                        modDir + File.separator + ZomboidFileSystem.processFilePath("media/hairStyles/hairStyles.xml", File.separatorChar)
                    );
                    if (hairStyles != null) {
                        for (HairStyle styleMod : hairStyles.femaleStyles) {
                            HairStyle style = instance.FindFemaleStyle(styleMod.name);
                            if (style == null) {
                                instance.femaleStyles.add(styleMod);
                            } else {
                                if (DebugLog.isEnabled(DebugType.Clothing)) {
                                    DebugLog.Clothing.println("mod \"%s\" overrides hair \"%s\"", modID, styleMod.name);
                                }

                                int index = instance.femaleStyles.indexOf(style);
                                instance.femaleStyles.set(index, styleMod);
                            }
                        }

                        for (HairStyle styleModx : hairStyles.maleStyles) {
                            HairStyle style = instance.FindMaleStyle(styleModx.name);
                            if (style == null) {
                                instance.maleStyles.add(styleModx);
                            } else {
                                if (DebugLog.isEnabled(DebugType.Clothing)) {
                                    DebugLog.Clothing.println("mod \"%s\" overrides hair \"%s\"", modID, styleModx.name);
                                }

                                int index = instance.maleStyles.indexOf(style);
                                instance.maleStyles.set(index, styleModx);
                            }
                        }
                    } else {
                        modDir = mod.getCommonDir();
                        hairStyles = Parse(modDir + File.separator + ZomboidFileSystem.processFilePath("media/hairStyles/hairStyles.xml", File.separatorChar));
                        if (hairStyles != null) {
                            for (HairStyle styleModxx : hairStyles.femaleStyles) {
                                HairStyle style = instance.FindFemaleStyle(styleModxx.name);
                                if (style == null) {
                                    instance.femaleStyles.add(styleModxx);
                                } else {
                                    if (DebugLog.isEnabled(DebugType.Clothing)) {
                                        DebugLog.Clothing.println("mod \"%s\" overrides hair \"%s\"", modID, styleModxx.name);
                                    }

                                    int index = instance.femaleStyles.indexOf(style);
                                    instance.femaleStyles.set(index, styleModxx);
                                }
                            }

                            for (HairStyle styleModxxx : hairStyles.maleStyles) {
                                HairStyle style = instance.FindMaleStyle(styleModxxx.name);
                                if (style == null) {
                                    instance.maleStyles.add(styleModxxx);
                                } else {
                                    if (DebugLog.isEnabled(DebugType.Clothing)) {
                                        DebugLog.Clothing.println("mod \"%s\" overrides hair \"%s\"", modID, styleModxxx.name);
                                    }

                                    int index = instance.maleStyles.indexOf(style);
                                    instance.maleStyles.set(index, styleModxxx);
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
            instance.femaleStyles.clear();
            instance.maleStyles.clear();
            instance = null;
        }
    }

    public static HairStyles Parse(String filename) {
        try {
            return parse(filename);
        } catch (FileNotFoundException var2) {
        } catch (IOException | JAXBException var3) {
            ExceptionLogger.logException(var3);
        }

        return null;
    }

    public static HairStyles parse(String filename) throws JAXBException, IOException {
        HairStyles var4;
        try (FileInputStream adrFile = new FileInputStream(filename)) {
            JAXBContext ctx = JAXBContext.newInstance(HairStyles.class);
            Unmarshaller um = ctx.createUnmarshaller();
            var4 = (HairStyles)um.unmarshal(adrFile);
        }

        return var4;
    }

    public HairStyle FindMaleStyle(String name) {
        return this.FindStyle(this.maleStyles, name);
    }

    public HairStyle FindFemaleStyle(String name) {
        return this.FindStyle(this.femaleStyles, name);
    }

    private HairStyle FindStyle(ArrayList<HairStyle> list, String name) {
        for (int i = 0; i < list.size(); i++) {
            HairStyle style = list.get(i);
            if (style.name.equalsIgnoreCase(name)) {
                return style;
            }

            if ("".equals(name) && style.name.equalsIgnoreCase("bald")) {
                return style;
            }
        }

        return null;
    }

    public String getRandomMaleStyle(String outfitName) {
        return HairOutfitDefinitions.instance.getRandomMaleHaircut(outfitName, this.maleStyles);
    }

    public String getRandomFemaleStyle(String outfitName) {
        return HairOutfitDefinitions.instance.getRandomFemaleHaircut(outfitName, this.femaleStyles);
    }

    public HairStyle getAlternateForHat(HairStyle style, String category) {
        if ("nohair".equalsIgnoreCase(category) || "nohairnobeard".equalsIgnoreCase(category)) {
            return null;
        } else if (this.femaleStyles.contains(style)) {
            return this.FindFemaleStyle(style.getAlternate(category));
        } else {
            return this.maleStyles.contains(style) ? this.FindMaleStyle(style.getAlternate(category)) : style;
        }
    }

    public ArrayList<HairStyle> getAllMaleStyles() {
        return this.maleStyles;
    }

    public ArrayList<HairStyle> getAllFemaleStyles() {
        return this.femaleStyles;
    }
}
