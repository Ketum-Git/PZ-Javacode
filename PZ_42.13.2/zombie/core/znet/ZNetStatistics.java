// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.znet;

public class ZNetStatistics {
    public static final int LAST_USER_MESSAGE_BYTES_PUSHED = 0;
    public static final int LAST_USER_MESSAGE_BYTES_SENT = 1;
    public static final int LAST_USER_MESSAGE_BYTES_RESENT = 2;
    public static final int LAST_USER_MESSAGE_BYTES_RECEIVED_PROCESSED = 3;
    public static final int LAST_USER_MESSAGE_BYTES_RECEIVED_IGNORED = 4;
    public static final int LAST_ACTUAL_BYTES_SENT = 5;
    public static final int LAST_ACTUAL_BYTES_RECEIVED = 6;
    public static final int TOTAL_USER_MESSAGE_BYTES_PUSHED = 7;
    public static final int TOTAL_USER_MESSAGE_BYTES_SENT = 8;
    public static final int TOTAL_USER_MESSAGE_BYTES_RESENT = 9;
    public static final int TOTAL_USER_MESSAGE_BYTES_RECEIVED_PROCESSED = 10;
    public static final int TOTAL_USER_MESSAGE_BYTES_RECEIVED_IGNORED = 11;
    public static final int TOTAL_ACTUAL_BYTES_SENT = 12;
    public static final int TOTAL_ACTUAL_BYTES_RECEIVED = 13;
    public static final int CONNECTION_START_TIME = 14;
    public static final int IS_LIMITED_BY_CONGESTION_CONTROL = 15;
    public static final int BPS_LIMIT_BY_CONGESTION_CONTROL = 16;
    public static final int IS_LIMITED_BY_OUTGOING_BANDWIDTH_LIMIT = 17;
    public static final int BPS_LIMIT_BY_OUTGOING_BANDWIDTH_LIMIT = 18;
    public static final int MESSAGE_IN_SEND_BUFFER_IMMEDIATE = 19;
    public static final int MESSAGE_IN_SEND_BUFFER_HIGH = 20;
    public static final int MESSAGE_IN_SEND_BUFFER_MEDIUM = 21;
    public static final int MESSAGE_IN_SEND_BUFFER_LOW = 22;
    public static final int BYTES_IN_SEND_BUFFER_IMMEDIATE = 23;
    public static final int BYTES_IN_SEND_BUFFER_HIGH = 24;
    public static final int BYTES_IN_SEND_BUFFER_MEDIUM = 25;
    public static final int BYTES_IN_SEND_BUFFER_LOW = 26;
    public static final int MESSAGES_IN_RESEND_BUFFER = 27;
    public static final int BYTES_IN_RESEND_BUFFER = 28;
    public static final int PACKETLOSS_LAST_SECOND = 29;
    public static final int PACKETLOSS_TOTAL = 30;
    public long lastUserMessageBytesPushed;
    public long lastUserMessageBytesSent;
    public long lastUserMessageBytesResent;
    public long lastUserMessageBytesReceivedProcessed;
    public long lastUserMessageBytesReceivedIgnored;
    public long lastActualBytesSent;
    public long lastActualBytesReceived;
    public long totalUserMessageBytesPushed;
    public long totalUserMessageBytesSent;
    public long totalUserMessageBytesResent;
    public long totalUserMessageBytesReceivedProcessed;
    public long totalUserMessageBytesReceivedIgnored;
    public long totalActualBytesSent;
    public long totalActualBytesReceived;
    public long connectionStartTime;
    public boolean isLimitedByCongestionControl;
    public long bpsLimitByCongestionControl;
    public boolean isLimitedByOutgoingBandwidthLimit;
    public long bpsLimitByOutgoingBandwidthLimit;
    public long messageInSendBufferImmediate;
    public long messageInSendBufferHigh;
    public long messageInSendBufferMedium;
    public long messageInSendBufferLow;
    public double bytesInSendBufferImmediate;
    public double bytesInSendBufferHigh;
    public double bytesInSendBufferMedium;
    public double bytesInSendBufferLow;
    public long messagesInResendBuffer;
    public long bytesInResendBuffer;
    public double packetlossLastSecond;
    public double packetlossTotal;

    public void setField(int field, boolean value) {
        switch (field) {
            case 15:
                this.isLimitedByCongestionControl = value;
                break;
            case 17:
                this.isLimitedByOutgoingBandwidthLimit = value;
                break;
            default:
                throw new IllegalArgumentException("unknown boolean field %d".formatted(field));
        }
    }

    public void setField(int field, double value) {
        switch (field) {
            case 23:
                this.bytesInSendBufferImmediate = value;
                break;
            case 24:
                this.bytesInSendBufferHigh = value;
                break;
            case 25:
                this.bytesInSendBufferMedium = value;
                break;
            case 26:
                this.bytesInSendBufferLow = value;
                break;
            case 27:
            case 28:
            default:
                throw new IllegalArgumentException("unknown double field %d".formatted(field));
            case 29:
                this.packetlossLastSecond = value;
                break;
            case 30:
                this.packetlossTotal = value;
        }
    }

    public void setField(int field, long value) {
        switch (field) {
            case 0:
                this.lastUserMessageBytesPushed = value;
                break;
            case 1:
                this.lastUserMessageBytesSent = value;
                break;
            case 2:
                this.lastUserMessageBytesResent = value;
                break;
            case 3:
                this.lastUserMessageBytesReceivedProcessed = value;
                break;
            case 4:
                this.lastUserMessageBytesReceivedIgnored = value;
                break;
            case 5:
                this.lastActualBytesSent = value;
                break;
            case 6:
                this.lastActualBytesReceived = value;
                break;
            case 7:
                this.totalUserMessageBytesPushed = value;
                break;
            case 8:
                this.totalUserMessageBytesSent = value;
                break;
            case 9:
                this.totalUserMessageBytesResent = value;
                break;
            case 10:
                this.totalUserMessageBytesReceivedProcessed = value;
                break;
            case 11:
                this.totalUserMessageBytesReceivedIgnored = value;
                break;
            case 12:
                this.totalActualBytesSent = value;
                break;
            case 13:
                this.totalActualBytesReceived = value;
                break;
            case 14:
                this.connectionStartTime = value;
                break;
            case 15:
            case 17:
            case 23:
            case 24:
            case 25:
            case 26:
            default:
                throw new IllegalArgumentException("unknown long field %d".formatted(field));
            case 16:
                this.bpsLimitByCongestionControl = value;
                break;
            case 18:
                this.bpsLimitByOutgoingBandwidthLimit = value;
                break;
            case 19:
                this.messageInSendBufferImmediate = value;
                break;
            case 20:
                this.messageInSendBufferHigh = value;
                break;
            case 21:
                this.messageInSendBufferMedium = value;
                break;
            case 22:
                this.messageInSendBufferLow = value;
                break;
            case 27:
                this.messagesInResendBuffer = value;
                break;
            case 28:
                this.bytesInResendBuffer = value;
        }
    }
}
