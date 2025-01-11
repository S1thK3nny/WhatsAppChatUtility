package Util;

import DataStructures.Message;
import static Util.Functions.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class ChatQuery {
    private final Collection<Message> messages;

    // Constructor
    public ChatQuery(Collection<Message> messages) {
        this.messages = messages;
    }

    // Filter messages by word
    public ChatQuery filterByWord(String word) {
        Objects.requireNonNull(word, "Word cannot be null");
        String lowerCaseWord = word.toLowerCase(Locale.ROOT);
        List<Message> filtered = messages.stream()
                .filter(m -> m.getContent().toLowerCase(Locale.ROOT).contains(lowerCaseWord))
                .collect(Collectors.toList());
        return new ChatQuery(filtered);
    }

    // Filter messages by exact word
    public ChatQuery filterByExactWord(String word) {
        Objects.requireNonNull(word, "Word cannot be null");
        String lowerCaseWord = word.toLowerCase(Locale.ROOT);
        List<Message> filtered = messages.stream()
                .filter(m -> {
                    List<String> words = extractWords(m.getContent());
                    return words.contains(lowerCaseWord);
                })
                .collect(Collectors.toList());
        return new ChatQuery(filtered);
    }

    // Filter messages by participant
    public ChatQuery filterByParticipant(String participant) {
        Objects.requireNonNull(participant, "Participant cannot be null");
        List<Message> filtered = messages.stream()
                .filter(m -> m.getSender().equalsIgnoreCase(participant))
                .collect(Collectors.toList());
        return new ChatQuery(filtered);
    }

    // Filter messages by timeframe
    public ChatQuery filterByTimeframe(String start, String end) {
        Objects.requireNonNull(start, "Start time cannot be null");
        Objects.requireNonNull(end, "End time cannot be null");
        List<Message> filtered = messages.stream()
                .filter(m -> !m.getTimestamp().isBefore(parseDateTime(start)) && !m.getTimestamp().isAfter(parseDateTime(end)))
                .collect(Collectors.toList());
        return new ChatQuery(filtered);
    }

    // Get word frequency map
    public Map<String, Long> getWordFrequency() {
        return messages.stream()
                .flatMap(m -> extractWords(m.getContent()).stream())
                .collect(Collectors.groupingBy(word -> word, Collectors.counting()));
    }

    // Get most active user
    public Optional<String> getMostActiveUser() {
        return messages.stream()
                .collect(Collectors.groupingBy(Message::getSender, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);
    }

    // Get total message count
    public long getMessageCount() {
        return messages.size();
    }

    // Collect messages to a list
    public List<Message> toList() {
        return new ArrayList<>(messages);
    }
}
