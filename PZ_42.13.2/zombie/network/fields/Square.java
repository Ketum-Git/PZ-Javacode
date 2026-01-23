// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields;

import java.nio.ByteBuffer;
import zombie.characters.IsoGameCharacter;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.network.GameServer;
import zombie.network.ServerMap;

public class Square extends Position implements IPositional, INetworkPacketField {
    protected IsoGridSquare square;

    public void set(IsoGameCharacter character) {
        this.set(character.getAttackTargetSquare());
    }

    public void set(IsoGridSquare square) {
        if (square != null) {
            this.set(square.getX(), square.getY(), square.getZ());
        } else {
            this.set(0.0F, 0.0F, 0.0F);
        }

        this.square = square;
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        super.parse(b, connection);
        if (GameServer.server) {
            this.square = ServerMap.instance.getGridSquare(PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()));
        } else {
            this.square = IsoWorld.instance
                .currentCell
                .getGridSquare(PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()));
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
    }

    public void process(IsoGameCharacter character) {
        character.setAttackTargetSquare(character.getCell().getGridSquare((double)this.getX(), (double)this.getY(), (double)this.getZ()));
    }

    public IsoGridSquare getSquare() {
        return this.square;
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.square != null;
    }
}
