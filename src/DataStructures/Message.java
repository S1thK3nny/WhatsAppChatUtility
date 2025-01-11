package DataStructures;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Message implements Comparable<Message> {
    private String sender;
    private String content;
    private LocalDateTime timestamp;

    public Message(String sender, String content, LocalDateTime timestamp) {
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yy, HH:mm");
        return timestamp.format(formatter) + " - " + sender + ": " + content;
    }

    @Override
    public int compareTo(Message other) {
        int timeComparison = this.timestamp.compareTo(other.timestamp);
        if (timeComparison != 0) {
            return timeComparison; // Sort by timestamp
        }
        return this.sender.compareTo(other.sender); // Sort by sender if timestamps are equal
    }
}
