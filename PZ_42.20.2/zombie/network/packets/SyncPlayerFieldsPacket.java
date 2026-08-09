// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.traits.CharacterTraits;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugType;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.fields.character.PlayerID;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class SyncPlayerFieldsPacket implements INetworkPacket {
    public static final byte PF_Recipes = 1;
    public static final byte PF_Traits = 2;
    public static final byte PF_AlreadyReadBook = 4;
    public static final byte PF_BodyDamage = 8;
    public static final byte PF_Reading = 16;
    public static final byte PF_Fitness = 32;
    public static final byte PF_Count = 6;
    @JSONField
    PlayerID playerId = new PlayerID();
    @JSONField
    byte syncParams;

    public void set(IsoPlayer player, byte syncParams) {
        this.playerId.set(player);
        this.syncParams = syncParams;
    }

    @Override
    public void setData(Object... values) {
        if (values.length == 2 && values[0] instanceof IsoPlayer) {
            this.set((IsoPlayer)values[0], (Byte)values[1]);
        } else {
            DebugType.Multiplayer.warn(this.getClass().getSimpleName() + ".set get invalid arguments");
            DebugType.Multiplayer.printStackTrace();
        }
    }

    void writeParam(byte param, ByteBufferWriter b) {
        switch (param) {
            case 1:
                b.putInt(this.playerId.getPlayer().getKnownRecipes().size());

                for (String recipeStr : this.playerId.getPlayer().getKnownRecipes()) {
                    b.putUTF(recipeStr);
                }
                break;
            case 2:
                CharacterTraits characterTraits = this.playerId.getPlayer().getCharacterTraits();
                characterTraits.write(b);
                break;
            case 4:
                b.putInt(this.playerId.getPlayer().getAlreadyReadBook().size());

                for (String bookStr : this.playerId.getPlayer().getAlreadyReadBook()) {
                    b.putUTF(bookStr);
                }
                break;
            case 8:
                this.playerId.getPlayer().getBodyDamage().saveMainFields(b.bb);
                break;
            case 16:
                b.putBoolean(this.playerId.getPlayer().isReading());
                break;
            case 32:
                this.playerId.getPlayer().getFitness().save(b.bb);
                break;
            default:
                DebugType.Multiplayer.warn(this.getClass().getSimpleName() + ".writeParam get invalid syncParam");
        }
    }

    void parseParam(byte param, ByteBufferReader b) {
        switch (param) {
            case 1:
                int recipesSize = b.getInt();

                for (int ix = 0; ix < recipesSize; ix++) {
                    String recipeStr = b.getUTF();
                    if (!this.playerId.getPlayer().getKnownRecipes().contains(recipeStr)) {
                        this.playerId.getPlayer().getKnownRecipes().add(recipeStr);
                    }
                }
                break;
            case 2:
                CharacterTraits characterTraits = this.playerId.getPlayer().getCharacterTraits();
                characterTraits.read(b);
                break;
            case 4:
                int booksSize = b.getInt();

                for (int i = 0; i < booksSize; i++) {
                    String bookStr = b.getUTF();
                    if (!this.playerId.getPlayer().getAlreadyReadBook().contains(bookStr)) {
                        this.playerId.getPlayer().getAlreadyReadBook().add(bookStr);
                    }
                }
                break;
            case 8:
                this.playerId.getPlayer().getBodyDamage().loadMainFields(b.bb, 249);
                break;
            case 16:
                this.playerId.getPlayer().setReading(b.getBoolean());
                break;
            case 32:
                this.playerId.getPlayer().getFitness().load(b.bb, 249);
                break;
            default:
                DebugType.Multiplayer.warn(this.getClass().getSimpleName() + ".parseParam get invalid syncParam");
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.playerId.write(b);
        b.putByte(this.syncParams);

        for (int i = 0; i < 6; i++) {
            if ((this.syncParams & (byte)(1 << i)) != 0) {
                this.writeParam((byte)(1 << i), b);
            }
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.playerId.parse(b, connection);
        this.syncParams = b.getByte();

        for (int i = 0; i < 6; i++) {
            if ((this.syncParams & (byte)(1 << i)) != 0) {
                this.parseParam((byte)(1 << i), b);
            }
        }
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return this.playerId.getPlayer() != null;
    }
}
