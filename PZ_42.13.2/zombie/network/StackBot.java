// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import zombie.network.server.IEventController;
import zombie.util.PublicServerUtil;

public class StackBot implements IEventController {
    String webhookUrl = "";

    public StackBot(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    @Override
    public void process(String event) {
        PublicServerUtil.callPostJson(
            this.webhookUrl, "{ \"text\": \"" + event.replace("\"", "\\\"").replace("\t", "\\t").replace("\r", "\\r").replace("\n", "\\n") + "\" }"
        );
    }
}
