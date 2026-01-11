// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import zombie.util.StringUtils;

public class RCONClient {
    private Socket socket;

    public boolean disconnect() {
        try {
            this.socket.close();
            return true;
        } catch (IOException var2) {
            System.out.println("Disconnect failed: " + var2.getMessage());
            return false;
        }
    }

    public boolean connect(String ip, String port) {
        try {
            this.socket = new Socket();
            this.socket.setSoTimeout(5000);
            InetSocketAddress address = new InetSocketAddress(ip, Integer.parseInt(port));
            this.socket.connect(address, 1000);
            return true;
        } catch (IOException var4) {
            System.out.println("Connect failed: " + var4.getMessage());
            return false;
        }
    }

    public boolean auth(String password) {
        try {
            int id = (int)(65535L & System.currentTimeMillis());
            RCONClient.RCONMessage authRequest = new RCONClient.RCONMessage(id, 3, password);
            authRequest.writeObject(this.socket.getOutputStream());
            RCONClient.RCONMessage responseValue = new RCONClient.RCONMessage();
            responseValue.readObject(this.socket.getInputStream(), 14);
            if (responseValue.type == 0 && responseValue.id == id) {
                RCONClient.RCONMessage authResponse = new RCONClient.RCONMessage();
                authResponse.readObject(this.socket.getInputStream(), 14);
                if (authResponse.type == 2 && responseValue.id == id) {
                    return true;
                } else {
                    System.out.println("Authentication failed: auth response");
                    return false;
                }
            } else {
                System.out.println("Authentication failed: response value");
                return false;
            }
        } catch (IOException var6) {
            System.out.println("Authentication failed: timeout");
            return false;
        }
    }

    public String exec(String command) {
        try {
            int id = (int)(65535L & System.currentTimeMillis());
            RCONClient.RCONMessage commandRequest = new RCONClient.RCONMessage(id, 2, command);
            commandRequest.writeObject(this.socket.getOutputStream());
            RCONClient.RCONMessage commandResponse = new RCONClient.RCONMessage();
            commandResponse.readObject(this.socket.getInputStream(), 0);
            return new String(commandResponse.body);
        } catch (IOException var5) {
            System.out.println("Command execution failed");
            return null;
        }
    }

    public boolean send(String url, String text) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .setHeader("Content-type", "application/json")
                .uri(URI.create(url))
                .POST(BodyPublishers.ofString("{\"text\":\"" + text + "\"}"))
                .build();
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            if (response != null && response.statusCode() != 200) {
                System.out.println(response.body());
                return false;
            } else {
                return true;
            }
        } catch (Exception var6) {
            System.out.println("Result post failed");
            return false;
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception var3) {
        }
    }

    public static void main(String[] args) {
        String ip = null;
        String port = null;
        String password = null;
        String command = null;
        String webhook = null;
        boolean isAgent = false;

        for (int n = 0; n < args.length; n++) {
            if (!StringUtils.isNullOrEmpty(args[n])) {
                if (args[n].equals("-ip")) {
                    ip = args[++n].trim();
                } else if (args[n].equals("-port")) {
                    port = args[++n].trim();
                } else if (args[n].equals("-password")) {
                    password = args[++n].trim();
                } else if (args[n].equals("-command")) {
                    command = args[++n].trim();
                } else if (args[n].equals("-webhook")) {
                    webhook = args[++n].trim();
                }
            }
        }

        if (!StringUtils.isNullOrEmpty(ip) && !StringUtils.isNullOrEmpty(port) && !StringUtils.isNullOrEmpty(password) && !StringUtils.isNullOrEmpty(command)) {
            if (!StringUtils.isNullOrEmpty(webhook)) {
                isAgent = true;
            }

            RCONClient client = new RCONClient();

            do {
                if (client.connect(ip, port)) {
                    if (client.auth(password)) {
                        if (isAgent && !client.send(webhook, String.format("Connected to server %s:%s", ip, port))) {
                            break;
                        }

                        String previous = null;

                        do {
                            String current = client.exec(command);
                            if (StringUtils.isNullOrEmpty(current)) {
                                break;
                            }

                            if (!current.equals(previous)) {
                                if (isAgent) {
                                    if (!client.send(webhook, current)) {
                                        break;
                                    }

                                    sleep(5000L);
                                } else {
                                    System.out.println(current);
                                }
                            }

                            previous = current;
                        } while (isAgent);

                        if (isAgent && !client.send(webhook, "Connection to server lost")) {
                            break;
                        }
                    }

                    client.disconnect();
                }

                if (isAgent) {
                    sleep(60000L);
                }
            } while (isAgent);
        } else {
            System.out.println("Incorrect arguments");
        }
    }

    private static class RCONMessage {
        private static final byte[] input = new byte[4096];
        private static final ByteBuffer bbr = ByteBuffer.wrap(input);
        private static final byte[] output = new byte[4096];
        private static final ByteBuffer bbw = ByteBuffer.wrap(output);
        static final int baseSize = 10;
        int size;
        int id;
        int type;
        byte[] body;

        RCONMessage() {
        }

        RCONMessage(int id, int type, String body) throws UnsupportedEncodingException {
            this.id = id;
            this.type = type;
            this.body = body.getBytes();
            this.size = 10 + body.length();
        }

        private void writeObject(OutputStream out) throws IOException {
            bbw.putInt(this.size);
            bbw.putInt(this.id);
            bbw.putInt(this.type);
            bbw.put(this.body);
            bbw.put((byte)0);
            bbw.put((byte)0);
            out.write(output, 0, this.size + 4);
            bbw.clear();
        }

        private void readObject(InputStream in, int maxSize) throws IOException {
            if (maxSize == 0) {
                in.read(input);
            } else {
                in.read(input, 0, maxSize);
            }

            this.size = Math.min(bbr.getInt(), bbr.capacity() - 4);
            this.id = bbr.getInt();
            this.type = bbr.getInt();
            if (this.size > 10) {
                this.body = new byte[this.size - 10];
                bbr.get(this.body, 0, this.size - 10);
            }

            bbr.get();
            bbr.get();
            bbr.clear();
        }

        static {
            bbr.order(ByteOrder.LITTLE_ENDIAN);
            bbw.order(ByteOrder.LITTLE_ENDIAN);
        }
    }
}
