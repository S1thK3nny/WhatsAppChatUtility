package Util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import DataStructures.Message;

public class ChatAnalysis {
    private final ArrayList<Message> chat;
    // private final HashMap<String, List<Message>> chat;

    public ChatAnalysis(String path) {
        this.chat = readChat(path);
    }

    public ArrayList<Message> readChat(String path) {
        ArrayList<Message> messages = new ArrayList<>();
        try {
            BufferedReader reader = Files.newBufferedReader(Paths.get(path).toAbsolutePath());
            String line;
            String lastSender = null;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split("-");
                // If the line starts with a date, it's a new message
                if (parts[0].matches("^\\d{2}\\.\\d{2}\\.\\d{2}, \\d{2}:\\d{2}.*")) {
                    String[] senderAndMessage = parts[1].split(": ", 2); // Only split the first colon to have <sender>: <message>

                    if (senderAndMessage.length < 2 || senderAndMessage[1].equals("<Medien ausgeschlossen>")) {
                        // If the message doesn't contain a colon, it's likely a system message or an image
                        continue;
                    }

                    lastSender = senderAndMessage[0].trim();
                    messages.add(new Message(lastSender, senderAndMessage[1], parseDateTime(parts[0])));
                }
                else {
                    if (lastSender != null) {
                        // If the last message was sent by the same person, append the current line to the last message
                        messages.set(
                                messages.size() - 1,
                                new Message(lastSender,
                                        messages.getLast().getContent() + "\n" + line,
                                        messages.getLast().getTimestamp())
                        );
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return messages;
    }

    // Prints the top <amount> most used words per person
    public void wordFrequency(int amount) {
        if (amount < 1) throw new IllegalArgumentException("Amount must be greater than 0");
        if (chat == null || chat.isEmpty()) throw new IllegalArgumentException("Chat is empty or not initialized");
        if (amount > chat.size()) throw new IllegalArgumentException("Amount is greater than the number of messages");

        // Map: Participant -> (Word -> Frequency)
        Map<String, Map<String, Integer>> wordFrequencyMap = new HashMap<>();

        for (Message message : chat) {
            String sender = message.getSender();
            if (sender == null || sender.trim().isEmpty()) continue; // Skip messages with no sender

            // Initialize the map for the sender if not present
            wordFrequencyMap.computeIfAbsent(sender, k -> new HashMap<>());

            List<String> words = extractWords(message.getContent());
            for (String word : words) {
                wordFrequencyMap.get(sender).merge(word, 1, Integer::sum);
            }
        }

        // Display top 'amount' words for each participant
        wordFrequencyMap.forEach((sender, freqMap) -> {
            System.out.println("\nTop " + amount + " words for " + sender + ":");
            freqMap.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                            .thenComparing(Map.Entry.comparingByKey()))
                    .limit(amount)
                    .forEach(entry -> System.out.println(entry.getKey() + ": " + entry.getValue()));
        });
    }

    public Map<String, Integer> getWordFrequency(String word) {
        if (chat.isEmpty()) throw new IllegalArgumentException("Chat ist leer");
        if (word.isEmpty()) throw new IllegalArgumentException("Wort ist leer");

        // Create a map of participants and their word frequency
        return getParticipants().stream()
                .collect(Collectors.toMap(
                        participant -> participant,
                        participant -> getWordFrequency(word, participant)
                ));
    }

    // Fix Umlaute
    public int getWordFrequency(String wordToSearchFor, String participant) {
        if (wordToSearchFor == null || wordToSearchFor.trim().isEmpty()) throw new IllegalArgumentException("Word to search for is empty or null");
        if (participant == null || participant.trim().isEmpty()) throw new IllegalArgumentException("Participant is empty or null");
        if (chat == null || chat.isEmpty()) throw new IllegalArgumentException("Chat is empty or not initialized");

        // Normalize the search word
        String cleanSearchWord = wordToSearchFor.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_äöüß]", "");

        if (getParticipants().stream()
                .noneMatch(p -> p.equalsIgnoreCase(participant))) {
            System.out.println("Participant \"" + participant + "\" not found");
            return 0;
        }

        int frequency = 0;
        for (Message message : chat) {
            if (!message.getSender().equalsIgnoreCase(participant)) continue;

            List<String> words = extractWords(message.getContent());
            for (String word : words) {
                if (word.equals(cleanSearchWord)) frequency++;
            }
        }
        return frequency;
    }

    public ArrayList<Message> getSentenceByWord(String word) {
        if (word.isEmpty()) throw new IllegalArgumentException("Word is empty");
        if (chat.isEmpty()) throw new IllegalArgumentException("Chat is empty");

        return getParticipants().stream()
                .map(participant -> getSentenceByWord(word, participant))
                .flatMap(ArrayList::stream)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public ArrayList<Message> getSentenceByWord(String word, String participant) {
        if (word.isEmpty()) throw new IllegalArgumentException("Word is empty");
        if (chat.isEmpty()) throw new IllegalArgumentException("Chat is empty");

        return chat.stream()
                .filter(message -> message.getContent().toLowerCase().contains(word.toLowerCase()) && message.getSender().equalsIgnoreCase(participant))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    // Broken method
    public ArrayList<Message> getSentenceByExactWord(String word) {
        if (word.isEmpty()) throw new IllegalArgumentException("Word is empty");
        if (chat.isEmpty()) throw new IllegalArgumentException("Chat is empty");

        return getParticipants().stream()
                .map(participant -> getSentenceByExactWord(word, participant))
                .flatMap(ArrayList::stream)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    // Broken method
    public ArrayList<Message> getSentenceByExactWord(String word, String participant) {
        if (word.isEmpty()) throw new IllegalArgumentException("Word is empty");
        if (chat.isEmpty()) throw new IllegalArgumentException("Chat is empty");

        return getSentenceByWord(word, participant).stream()
                .filter(message -> Objects.equals(extractWords(message.getContent()), word.toLowerCase()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public Set<String> getParticipants() {
        return chat.stream()
                .map(Message::getSender)
                .collect(Collectors.toSet());
    }

    public Long getMessagesCount() {
        return (long) chat.size();
    }

    public Long getAmountOfMessagesPerParticipant(String participant) {
        return chat.stream()
                .filter(message -> message.getSender().equalsIgnoreCase(participant))
                .count();
    }

    private List<String> extractWords(String text) {
        List<String> words = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\b\\p{L}+'?\\p{L}*\\b").matcher(text.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            words.add(matcher.group());
        }
        return words;
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        // Remove any whitespace and split by comma
        String[] parts = dateTimeStr.trim().split(",");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid date-time format. Expected format: DD.MM.YY, HH:mm");
        }

        // Combine date and time parts with proper formatting
        String dateTime = parts[0].trim() + " " + parts[1].trim();

        // Create formatter for the specific pattern
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm");

        try {
            // Parse the string to LocalDateTime
            return LocalDateTime.parse(dateTime, formatter);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Failed to parse date-time: " + dateTimeStr, e);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Chat Analysis:\n----------------\n")
                .append("Total amount of Messages: ")
                .append(getMessagesCount()).append("\n")
                .append("Participants: ")
                .append(getParticipants()).append("\n")
                .append("Amount of messages per participant:\n")
                .append(getParticipants()
                        .stream()
                        .map(participant -> "\t- " +  participant + ": " + getAmountOfMessagesPerParticipant(participant)).collect(Collectors.joining("\n"))
                ).append("\n");
        return sb.toString();
    }
}
