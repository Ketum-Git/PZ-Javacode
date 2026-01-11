// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import zombie.UsedFromLua;

@UsedFromLua
public class DBTicket {
    private String author;
    private String message = "";
    private int ticketId;
    private boolean viewed;
    private DBTicket answer;
    private boolean isAnswer;

    public DBTicket(String author, String message, int ticketId) {
        this.author = author;
        this.message = message;
        this.ticketId = ticketId;
        this.viewed = this.viewed;
    }

    public DBTicket(String author, String message, int ticketId, boolean viewed) {
        this.author = author;
        this.message = message;
        this.ticketId = ticketId;
        this.viewed = viewed;
    }

    public String getAuthor() {
        return this.author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getTicketID() {
        return this.ticketId;
    }

    public void setTicketID(int ticketId) {
        this.ticketId = ticketId;
    }

    public boolean isViewed() {
        return this.viewed;
    }

    public void setViewed(boolean viewed) {
        this.viewed = viewed;
    }

    public DBTicket getAnswer() {
        return this.answer;
    }

    public void setAnswer(DBTicket answer) {
        this.answer = answer;
    }

    public boolean isAnswer() {
        return this.isAnswer;
    }

    public void setIsAnswer(boolean isAnswer) {
        this.isAnswer = isAnswer;
    }
}
