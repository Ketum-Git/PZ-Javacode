// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap.network;

import gnu.trove.map.hash.TIntObjectHashMap;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import zombie.ZomboidFileSystem;
import zombie.core.logger.ExceptionLogger;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.SliceY;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.spnetwork.SinglePlayerServer;
import zombie.util.StringUtils;
import zombie.worldMap.symbols.SymbolSaveData;
import zombie.worldMap.symbols.WorldMapBaseSymbol;
import zombie.worldMap.symbols.WorldMapSymbols;
import zombie.worldMap.symbols.WorldMapTextSymbol;
import zombie.worldMap.symbols.WorldMapTextureSymbol;

public final class WorldMapServer {
    public static final WorldMapServer instance = new WorldMapServer();
    public static final int SAVEFILE_VERSION = 2;
    private static final byte[] FILE_MAGIC = new byte[]{87, 77, 83, 89};
    public static final byte PACKET_AddMarker = 1;
    public static final byte PACKET_RemoveMarker = 2;
    public static final byte PACKET_AddSymbol = 3;
    public static final byte PACKET_RemoveSymbol = 4;
    public static final byte PACKET_ModifySymbol = 5;
    public static final byte PACKET_SetPrivateSymbol = 6;
    public static final byte PACKET_ModifySharing = 7;
    private static final ByteBuffer BYTE_BUFFER = ByteBuffer.allocate(1048576);
    private static final ByteBufferWriter BYTE_BUFFER_WRITER = new ByteBufferWriter(BYTE_BUFFER);
    private final WorldMapSymbols symbols = new WorldMapSymbols();
    private int nextElementId = 1;
    private final TIntObjectHashMap<WorldMapBaseSymbol> symbolById = new TIntObjectHashMap<>();

    public void receive(ByteBuffer bb, UdpConnection connection) throws IOException {
        int packet = bb.get();
        switch (packet) {
            case 1:
                this.receiveAddMarker(bb, connection);
                break;
            case 2:
                this.receiveRemoveMarker(bb, connection);
                break;
            case 3:
                this.receiveAddSymbol(bb, connection);
                break;
            case 4:
                this.receiveRemoveSymbol(bb, connection);
                break;
            case 5:
                this.receiveModifySymbol(bb, connection);
                break;
            case 6:
                this.receiveSetPrivateSymbol(bb, connection);
                break;
            case 7:
                this.receiveModifySharing(bb, connection);
        }
    }

    private void receiveAddMarker(ByteBuffer bb, UdpConnection connection) {
    }

    private void receiveRemoveMarker(ByteBuffer bb, UdpConnection connection) {
    }

    private void receiveAddSymbol(ByteBuffer bb, UdpConnection connection) throws IOException {
        SymbolSaveData saveData = new SymbolSaveData(241, 2);
        saveData.load(bb);
        WorldMapSymbolNetworkInfo networkInfo = new WorldMapSymbolNetworkInfo();
        networkInfo.load(bb, 241, 2);
        if (this.canClientModify(networkInfo, connection)) {
            int symbolType = bb.get();
            WorldMapBaseSymbol symbol;
            if (symbolType == WorldMapSymbols.WorldMapSymbolType.Text.index()) {
                symbol = new WorldMapTextSymbol(this.symbols);
            } else {
                if (symbolType != WorldMapSymbols.WorldMapSymbolType.Texture.index()) {
                    throw new IOException("unknown map symbol type " + symbolType);
                }

                symbol = new WorldMapTextureSymbol(this.symbols);
            }

            symbol.load(bb, saveData);
            networkInfo.setID(this.nextElementId++);
            symbol.setNetworkInfo(networkInfo);
            this.symbols.addSymbol(symbol);
            this.symbolById.put(symbol.getNetworkInfo().getID(), symbol);
            this.addSymbolOnClient(symbol);
        }
    }

    private void receiveRemoveSymbol(ByteBuffer bb, UdpConnection connection) {
        int id = bb.getInt();
        WorldMapBaseSymbol symbol = this.symbolById.get(id);
        if (symbol != null) {
            if (this.canClientModify(symbol.getNetworkInfo(), connection)) {
                this.symbolById.remove(id);
                this.symbols.removeSymbol(symbol);
                this.removeSymbolOnClient(id);
                symbol.release();
            }
        }
    }

    private void receiveModifySymbol(ByteBuffer bb, UdpConnection connection) throws IOException {
        SymbolSaveData saveData = new SymbolSaveData(241, 2);
        saveData.load(bb);
        int id = bb.getInt();
        WorldMapBaseSymbol symbol = this.symbolById.get(id);
        if (symbol != null) {
            if (this.canClientModify(symbol.getNetworkInfo(), connection)) {
                symbol.load(bb, saveData);
                this.modifySymbolOnClient(symbol);
            }
        }
    }

    private void receiveSetPrivateSymbol(ByteBuffer bb, UdpConnection connection) throws IOException {
        int id = bb.getInt();
        WorldMapBaseSymbol symbol = this.symbolById.get(id);
        if (symbol != null) {
            if (this.canClientModify(symbol.getNetworkInfo(), connection)) {
                this.setPrivateSymbolOnClient(symbol);
                this.symbolById.remove(id);
                this.symbols.removeSymbol(symbol);
                symbol.release();
            }
        }
    }

    private void receiveModifySharing(ByteBuffer bb, UdpConnection connection) throws IOException {
        int id = bb.getInt();
        WorldMapBaseSymbol symbol = this.symbolById.get(id);
        if (symbol != null) {
            if (this.canClientModify(symbol.getNetworkInfo(), connection)) {
                symbol.getNetworkInfo().load(bb, 241, 2);
                this.modifySymbolOnClient(symbol);
            }
        }
    }

    private boolean canClientModify(WorldMapSymbolNetworkInfo networkInfo, UdpConnection connection) {
        return StringUtils.equals(networkInfo.getAuthor(), connection.username);
    }

    private void sendPacket(ByteBuffer bb) {
        if (GameServer.server) {
            for (int n = 0; n < GameServer.udpEngine.connections.size(); n++) {
                UdpConnection c = GameServer.udpEngine.connections.get(n);
                ByteBufferWriter b = c.startPacket();
                bb.flip();
                b.bb.put(bb);
                c.endPacketImmediate();
            }
        } else {
            if (GameClient.client) {
                throw new IllegalStateException("can't call this method on the client");
            }

            for (int n = 0; n < SinglePlayerServer.udpEngine.connections.size(); n++) {
                zombie.spnetwork.UdpConnection c = SinglePlayerServer.udpEngine.connections.get(n);
                ByteBufferWriter b = c.startPacket();
                bb.flip();
                b.bb.put(bb);
                c.endPacketImmediate();
            }
        }
    }

    public void addMarkerOnClient(WorldMapBaseSymbol symbol) throws IOException {
        BYTE_BUFFER.clear();
        ByteBufferWriter b = BYTE_BUFFER_WRITER;
        PacketTypes.PacketType.WorldMap.doPacket(b);
        b.putByte((byte)1);
        this.sendPacket(BYTE_BUFFER);
    }

    public void removeMarkerOnClient(int id) {
        BYTE_BUFFER.clear();
        ByteBufferWriter b = BYTE_BUFFER_WRITER;
        PacketTypes.PacketType.WorldMap.doPacket(b);
        b.putByte((byte)2);
        this.sendPacket(BYTE_BUFFER);
    }

    public void addSymbolOnClient(WorldMapBaseSymbol symbol) throws IOException {
        BYTE_BUFFER.clear();
        ByteBufferWriter b = BYTE_BUFFER_WRITER;
        PacketTypes.PacketType.WorldMap.doPacket(b);
        b.putByte((byte)3);
        SymbolSaveData saveData = new SymbolSaveData(241, 2);
        saveData.save(b.bb, symbol);
        symbol.getNetworkInfo().save(b.bb);
        b.putByte((byte)symbol.getType().index());
        symbol.save(b.bb, saveData);
        this.sendPacket(BYTE_BUFFER);
    }

    public void removeSymbolOnClient(int id) {
        BYTE_BUFFER.clear();
        ByteBufferWriter b = BYTE_BUFFER_WRITER;
        PacketTypes.PacketType.WorldMap.doPacket(b);
        b.putByte((byte)4);
        b.putInt(id);
        this.sendPacket(BYTE_BUFFER);
    }

    public void modifySymbolOnClient(WorldMapBaseSymbol symbol) throws IOException {
        BYTE_BUFFER.clear();
        ByteBufferWriter b = BYTE_BUFFER_WRITER;
        PacketTypes.PacketType.WorldMap.doPacket(b);
        b.putByte((byte)5);
        SymbolSaveData saveData = new SymbolSaveData(241, 2);
        saveData.save(b.bb, symbol);
        b.putInt(symbol.getNetworkInfo().getID());
        symbol.getNetworkInfo().save(b.bb);
        symbol.save(b.bb, saveData);
        this.sendPacket(BYTE_BUFFER);
    }

    public void setPrivateSymbolOnClient(WorldMapBaseSymbol symbol) throws IOException {
        BYTE_BUFFER.clear();
        ByteBufferWriter b = BYTE_BUFFER_WRITER;
        PacketTypes.PacketType.WorldMap.doPacket(b);
        b.putByte((byte)6);
        b.putInt(symbol.getNetworkInfo().getID());
        this.sendPacket(BYTE_BUFFER);
    }

    public void sendRequestData(ByteBuffer bb) throws IOException {
        SymbolSaveData saveData = new SymbolSaveData(241, 2);
        saveData.save(bb, this.symbols);
        bb.putInt(this.symbols.getSymbolCount());

        for (int i = 0; i < this.symbols.getSymbolCount(); i++) {
            WorldMapBaseSymbol symbol = this.symbols.getSymbolByIndex(i);
            symbol.getNetworkInfo().save(bb);
            bb.put((byte)symbol.getType().index());
            symbol.save(bb, saveData);
        }
    }

    public void writeSavefile() {
        try {
            ByteBuffer out = SliceY.SliceBuffer;
            out.clear();
            out.put(FILE_MAGIC);
            out.putInt(241);
            out.putInt(2);
            this.writeSavefile(out);
            File file = new File(ZomboidFileSystem.instance.getFileNameInCurrentSave("servermap_symbols.bin"));

            try (
                FileOutputStream fos = new FileOutputStream(file);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
            ) {
                bos.write(out.array(), 0, out.position());
            }
        } catch (Exception var11) {
            ExceptionLogger.logException(var11);
        }
    }

    private void writeSavefile(ByteBuffer bb) throws IOException {
        SymbolSaveData saveData = new SymbolSaveData(241, 2);
        saveData.save(bb, this.symbols);
        bb.putInt(this.symbols.getSymbolCount());

        for (int i = 0; i < this.symbols.getSymbolCount(); i++) {
            WorldMapBaseSymbol symbol = this.symbols.getSymbolByIndex(i);
            symbol.getNetworkInfo().save(bb);
            bb.put((byte)symbol.getType().index());
            symbol.save(bb, saveData);
        }
    }

    public void readSavefile() {
        File file = new File(ZomboidFileSystem.instance.getFileNameInCurrentSave("servermap_symbols.bin"));

        try (
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);
        ) {
            ByteBuffer in = SliceY.SliceBuffer;
            in.clear();
            int numBytes = bis.read(in.array());
            in.limit(numBytes);
            byte[] magic = new byte[4];
            in.get(magic);
            if (!Arrays.equals(magic, FILE_MAGIC)) {
                throw new IOException(file.getAbsolutePath() + " does not appear to be servermap_symbols.bin");
            }

            int WorldVersion = in.getInt();
            int SymbolsVersion = in.getInt();
            this.readSavefile(in, WorldVersion, SymbolsVersion);
        } catch (FileNotFoundException var13) {
        } catch (Exception var14) {
            ExceptionLogger.logException(var14);
        }
    }

    private void readSavefile(ByteBuffer bb, int WorldVersion, int SymbolsVersion) throws IOException {
        SymbolSaveData saveData = new SymbolSaveData(WorldVersion, SymbolsVersion);
        saveData.load(bb);
        int count = bb.getInt();

        for (int i = 0; i < count; i++) {
            WorldMapSymbolNetworkInfo networkInfo = new WorldMapSymbolNetworkInfo();
            networkInfo.load(bb, WorldVersion, SymbolsVersion);
            int symbolType = bb.get();
            WorldMapBaseSymbol symbol;
            if (symbolType == WorldMapSymbols.WorldMapSymbolType.Text.index()) {
                symbol = new WorldMapTextSymbol(this.symbols);
            } else {
                if (symbolType != WorldMapSymbols.WorldMapSymbolType.Texture.index()) {
                    throw new IOException("unknown map symbol type " + symbolType);
                }

                symbol = new WorldMapTextureSymbol(this.symbols);
            }

            symbol.load(bb, saveData);
            networkInfo.setID(this.nextElementId++);
            symbol.setNetworkInfo(networkInfo);
            this.symbols.addSymbol(symbol);
            this.symbolById.put(symbol.getNetworkInfo().getID(), symbol);
        }
    }
}
