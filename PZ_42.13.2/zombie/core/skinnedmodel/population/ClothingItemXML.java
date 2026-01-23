// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.population;

import java.util.ArrayList;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "clothingItem")
public class ClothingItemXML {
    @XmlElement(name = "m_GUID")
    public String guid;
    @XmlElement(name = "m_MaleModel")
    public String maleModel;
    @XmlElement(name = "m_FemaleModel")
    public String femaleModel;
    @XmlElement(name = "m_AltMaleModel")
    public String altMaleModel;
    @XmlElement(name = "m_AltFemaleModel")
    public String altFemaleModel;
    @XmlElement(name = "m_Static")
    public boolean isStatic;
    @XmlElement(name = "m_BaseTextures")
    public ArrayList<String> baseTextures = new ArrayList<>();
    @XmlElement(name = "m_AttachBone")
    public String attachBone;
    @XmlElement(name = "m_Masks")
    public ArrayList<Integer> masks = new ArrayList<>();
    @XmlElement(name = "m_MasksFolder")
    public String masksFolder = "media/textures/Body/Masks";
    @XmlElement(name = "m_UnderlayMasksFolder")
    public String underlayMasksFolder = "media/textures/Body/Masks";
    @XmlElement(name = "textureChoices")
    public ArrayList<String> textureChoices = new ArrayList<>();
    @XmlElement(name = "m_AllowRandomHue")
    public boolean allowRandomHue;
    @XmlElement(name = "m_AllowRandomTint")
    public boolean allowRandomTint;
    @XmlElement(name = "m_DecalGroup")
    public String decalGroup;
    @XmlElement(name = "m_Shader")
    public String shader;
    @XmlElement(name = "m_HatCategory")
    public String hatCategory;
    @XmlElement(name = "m_SpawnWith")
    public ArrayList<String> spawnWith = new ArrayList<>();
}
