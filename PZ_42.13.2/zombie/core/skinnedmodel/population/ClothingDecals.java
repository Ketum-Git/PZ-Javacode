// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.population;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import zombie.ZomboidFileSystem;
import zombie.core.logger.ExceptionLogger;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.gameStates.ChooseGameInfo;
import zombie.util.PZXmlParserException;
import zombie.util.PZXmlUtil;
import zombie.util.StringUtils;

@XmlRootElement(name = "clothingDecals")
public class ClothingDecals {
    @XmlElement(name = "group")
    public final ArrayList<ClothingDecalGroup> groups = new ArrayList<>();
    @XmlTransient
    public static ClothingDecals instance;
    private final HashMap<String, ClothingDecals.CachedDecal> cachedDecals = new HashMap<>();

    public static void init() {
        if (instance != null) {
            throw new IllegalStateException("ClothingDecals Already Initialized.");
        } else {
            instance = Parse(
                ZomboidFileSystem.instance.base.canonicalFile.getAbsolutePath()
                    + File.separator
                    + ZomboidFileSystem.processFilePath("media/clothing/clothingDecals.xml", File.separatorChar)
            );
            if (instance != null) {
                for (String modID : ZomboidFileSystem.instance.getModIDs()) {
                    ChooseGameInfo.Mod mod = ChooseGameInfo.getAvailableModDetails(modID);
                    if (mod != null) {
                        String modDir = mod.getVersionDir();
                        ClothingDecals clothingDecals = Parse(
                            modDir + File.separator + ZomboidFileSystem.processFilePath("media/clothing/clothingDecals.xml", File.separatorChar)
                        );
                        if (clothingDecals != null) {
                            for (ClothingDecalGroup groupMod : clothingDecals.groups) {
                                ClothingDecalGroup group = instance.FindGroup(groupMod.name);
                                if (group == null) {
                                    instance.groups.add(groupMod);
                                } else {
                                    if (DebugLog.isEnabled(DebugType.Clothing)) {
                                        DebugLog.Clothing.println("mod \"%s\" overrides decal group \"%s\"", modID, groupMod.name);
                                    }

                                    int index = instance.groups.indexOf(group);
                                    instance.groups.set(index, groupMod);
                                }
                            }
                        } else {
                            modDir = mod.getCommonDir();
                            clothingDecals = Parse(
                                modDir + File.separator + ZomboidFileSystem.processFilePath("media/clothing/clothingDecals.xml", File.separatorChar)
                            );
                            if (clothingDecals != null) {
                                for (ClothingDecalGroup groupModx : clothingDecals.groups) {
                                    ClothingDecalGroup group = instance.FindGroup(groupModx.name);
                                    if (group == null) {
                                        instance.groups.add(groupModx);
                                    } else {
                                        if (DebugLog.isEnabled(DebugType.Clothing)) {
                                            DebugLog.Clothing.println("mod \"%s\" overrides decal group \"%s\"", modID, groupModx.name);
                                        }

                                        int index = instance.groups.indexOf(group);
                                        instance.groups.set(index, groupModx);
                                    }
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
            instance.cachedDecals.clear();
            instance.groups.clear();
            instance = null;
        }
    }

    public static ClothingDecals Parse(String filename) {
        try {
            return parse(filename);
        } catch (FileNotFoundException var2) {
        } catch (JAXBException | IOException var3) {
            ExceptionLogger.logException(var3);
        }

        return null;
    }

    public static ClothingDecals parse(String filename) throws JAXBException, IOException {
        ClothingDecals var4;
        try (FileInputStream adrFile = new FileInputStream(filename)) {
            JAXBContext ctx = JAXBContext.newInstance(ClothingDecals.class);
            Unmarshaller um = ctx.createUnmarshaller();
            var4 = (ClothingDecals)um.unmarshal(adrFile);
        }

        return var4;
    }

    public ClothingDecal getDecal(String name) {
        if (StringUtils.isNullOrWhitespace(name)) {
            return null;
        } else {
            ClothingDecals.CachedDecal cachedDecal = this.cachedDecals.get(name);
            if (cachedDecal == null) {
                cachedDecal = new ClothingDecals.CachedDecal();
                this.cachedDecals.put(name, cachedDecal);
            }

            if (cachedDecal.decal != null) {
                return cachedDecal.decal;
            } else {
                String filePath = ZomboidFileSystem.instance.getString("media/clothing/clothingDecals/" + name + ".xml");

                try {
                    cachedDecal.decal = PZXmlUtil.parse(ClothingDecal.class, filePath);
                    cachedDecal.decal.name = name;
                } catch (PZXmlParserException var5) {
                    System.err.println("Failed to load ClothingDecal: " + filePath);
                    ExceptionLogger.logException(var5);
                    return null;
                }

                return cachedDecal.decal;
            }
        }
    }

    public ClothingDecalGroup FindGroup(String name) {
        if (StringUtils.isNullOrWhitespace(name)) {
            return null;
        } else {
            for (int i = 0; i < this.groups.size(); i++) {
                ClothingDecalGroup group = this.groups.get(i);
                if (group.name.equalsIgnoreCase(name)) {
                    return group;
                }
            }

            return null;
        }
    }

    public String getRandomDecal(String groupName) {
        ClothingDecalGroup group = this.FindGroup(groupName);
        return group == null ? null : group.getRandomDecal();
    }

    private static final class CachedDecal {
        ClothingDecal decal;
    }
}
