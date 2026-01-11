// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.core.skinnedmodel.visual.IHumanVisual;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.debug.DebugLog;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;

public final class SharedDescriptors {
    private static final int DESCRIPTOR_COUNT = 500;
    private static final int DESCRIPTOR_ID_START = 500;
    private static final byte[] DESCRIPTOR_MAGIC = new byte[]{68, 69, 83, 67};
    private static final int VERSION_1 = 1;
    private static final int VERSION_2 = 2;
    private static final int VERSION = 2;
    private static SharedDescriptors.Descriptor[] playerZombieDescriptors = new SharedDescriptors.Descriptor[10];
    private static final int FIRST_PLAYER_ZOMBIE_DESCRIPTOR_ID = 1000;

    public static void initSharedDescriptors() {
        if (GameServer.server) {
            ;
        }
    }

    private static void noise(String s) {
        DebugLog.log("shared-descriptor: " + s);
    }

    public static void createPlayerZombieDescriptor(IsoZombie zombie) {
        if (GameServer.server) {
            if (zombie.isReanimatedPlayer()) {
                if (zombie.getDescriptor().getID() == 0) {
                    int index = -1;

                    for (int i = 0; i < playerZombieDescriptors.length; i++) {
                        if (playerZombieDescriptors[i] == null) {
                            index = i;
                            break;
                        }
                    }

                    if (index == -1) {
                        SharedDescriptors.Descriptor[] newDescs = new SharedDescriptors.Descriptor[playerZombieDescriptors.length + 10];
                        System.arraycopy(playerZombieDescriptors, 0, newDescs, 0, playerZombieDescriptors.length);
                        index = playerZombieDescriptors.length;
                        playerZombieDescriptors = newDescs;
                        noise("resized PlayerZombieDescriptors array size=" + playerZombieDescriptors.length);
                    }

                    zombie.getDescriptor().setID(1000 + index);
                    int outfitID = PersistentOutfits.instance.pickOutfit("ReanimatedPlayer", zombie.isFemale());
                    outfitID = outfitID & -65536 | index + 1;
                    zombie.setPersistentOutfitID(outfitID);
                    SharedDescriptors.Descriptor descriptor = new SharedDescriptors.Descriptor();
                    descriptor.female = zombie.isFemale();
                    descriptor.zombie = false;
                    descriptor.id = 1000 + index;
                    descriptor.persistentOutfitId = outfitID;
                    descriptor.getHumanVisual().copyFrom(zombie.getHumanVisual());
                    ItemVisuals itemVisuals = new ItemVisuals();
                    zombie.getItemVisuals(itemVisuals);

                    for (int ix = 0; ix < itemVisuals.size(); ix++) {
                        ItemVisual itemVisual = new ItemVisual(itemVisuals.get(ix));
                        descriptor.itemVisuals.add(itemVisual);
                    }

                    playerZombieDescriptors[index] = descriptor;
                    noise("added id=" + descriptor.getID());

                    for (int n = 0; n < GameServer.udpEngine.connections.size(); n++) {
                        UdpConnection connection = GameServer.udpEngine.connections.get(n);
                        ByteBufferWriter bbw = connection.startPacket();

                        try {
                            PacketTypes.PacketType.ZombieDescriptors.doPacket(bbw);
                            descriptor.save(bbw.bb);
                            PacketTypes.PacketType.ZombieDescriptors.send(connection);
                        } catch (Exception var9) {
                            var9.printStackTrace();
                            connection.cancelPacket();
                        }
                    }
                }
            }
        }
    }

    public static void releasePlayerZombieDescriptor(IsoZombie zombie) {
        if (GameServer.server) {
            if (zombie.isReanimatedPlayer()) {
                int index = zombie.getDescriptor().getID() - 1000;
                if (index >= 0 && index < playerZombieDescriptors.length) {
                    noise("released id=" + zombie.getDescriptor().getID());
                    zombie.getDescriptor().setID(0);
                    playerZombieDescriptors[index] = null;
                }
            }
        }
    }

    public static SharedDescriptors.Descriptor[] getPlayerZombieDescriptors() {
        return playerZombieDescriptors;
    }

    public static void registerPlayerZombieDescriptor(SharedDescriptors.Descriptor desc) {
        if (GameClient.client) {
            int index = desc.getID() - 1000;
            if (index >= 0 && index < 32767) {
                if (playerZombieDescriptors.length <= index) {
                    int capacity = (index + 10) / 10 * 10;
                    SharedDescriptors.Descriptor[] newDescs = new SharedDescriptors.Descriptor[capacity];
                    System.arraycopy(playerZombieDescriptors, 0, newDescs, 0, playerZombieDescriptors.length);
                    playerZombieDescriptors = newDescs;
                    noise("resized PlayerZombieDescriptors array size=" + playerZombieDescriptors.length);
                }

                playerZombieDescriptors[index] = desc;
                noise("registered id=" + desc.getID());
            }
        }
    }

    public static void ApplyReanimatedPlayerOutfit(int outfitID, String outfitName, IsoGameCharacter chr) {
        if (chr instanceof IsoZombie zombie) {
            short variant = (short)(outfitID & 65535);
            if (variant >= 1 && variant <= playerZombieDescriptors.length) {
                SharedDescriptors.Descriptor sharedDesc = playerZombieDescriptors[variant - 1];
                if (sharedDesc != null) {
                    zombie.useDescriptor(sharedDesc);
                }
            }
        }
    }

    public static final class Descriptor implements IHumanVisual {
        public int id;
        public int persistentOutfitId = 0;
        public String outfitName;
        public final HumanVisual humanVisual = new HumanVisual(this);
        public final ItemVisuals itemVisuals = new ItemVisuals();
        public boolean female;
        public boolean zombie;

        public int getID() {
            return this.id;
        }

        public int getPersistentOutfitID() {
            return this.persistentOutfitId;
        }

        @Override
        public HumanVisual getHumanVisual() {
            return this.humanVisual;
        }

        @Override
        public void getItemVisuals(ItemVisuals itemVisuals) {
            itemVisuals.clear();
            itemVisuals.addAll(this.itemVisuals);
        }

        @Override
        public boolean isFemale() {
            return this.female;
        }

        @Override
        public boolean isZombie() {
            return this.zombie;
        }

        @Override
        public boolean isSkeleton() {
            return false;
        }

        public void save(ByteBuffer output) throws IOException {
            byte flags1 = 0;
            if (this.female) {
                flags1 = (byte)(flags1 | 1);
            }

            if (this.zombie) {
                flags1 = (byte)(flags1 | 2);
            }

            output.put(flags1);
            output.putInt(this.id);
            output.putInt(this.persistentOutfitId);
            GameWindow.WriteStringUTF(output, this.outfitName);
            this.humanVisual.save(output);
            this.itemVisuals.save(output);
        }

        public void load(ByteBuffer input, int WorldVersion) throws IOException {
            this.humanVisual.clear();
            this.itemVisuals.clear();
            byte flags1 = input.get();
            this.female = (flags1 & 1) != 0;
            this.zombie = (flags1 & 2) != 0;
            this.id = input.getInt();
            this.persistentOutfitId = input.getInt();
            this.outfitName = GameWindow.ReadStringUTF(input);
            this.humanVisual.load(input, WorldVersion);
            int count = input.getShort();

            for (int i = 0; i < count; i++) {
                ItemVisual itemVisual = new ItemVisual();
                itemVisual.load(input, WorldVersion);
                this.itemVisuals.add(itemVisual);
            }
        }
    }

    private static final class DescriptorList extends ArrayList<SharedDescriptors.Descriptor> {
    }
}
