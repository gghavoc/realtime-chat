package RealTimeChat;

import java.io.Serializable;

public class Packet implements Serializable {
    // enums
    public static final int PING;
    public static final int MESSAGE;
    public static final int SET_USERNAME;
    public static final int DISCONNECT;

    // static block
    static {
        PING = 0;
        MESSAGE = 1;
        SET_USERNAME = 2;
        DISCONNECT = 3;
    }

    // content
    private final int packetType;
    private final String packetContent;

    public Packet(int packetType, String packetContent) {
        this.packetType = packetType;
        this.packetContent = packetContent;
    }

    public int getPacketType() {
        return this.packetType;
    }
    public String getPacketContent() {
        return this.packetContent;
    }
}
