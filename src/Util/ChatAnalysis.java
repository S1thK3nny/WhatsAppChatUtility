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
    // private final ArrayList<Message> chat;
    private final HashMap<String, List<Message>> chat;

    public ChatAnalysis(String path) {
        this.chat = readChat(path);
    }

    public HashMap<String, List<Message>> readChat(String path) {
        if (path == null || path.trim().isEmpty()) throw new IllegalArgumentException("Path is empty or null");

        HashMap<String, List<Message>> messages = new HashMap<>();

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

                    // Save the last sender and their message
                    lastSender = senderAndMessage[0].trim();
                    messages.putIfAbsent(lastSender, new ArrayList<>());
                    messages.get(lastSender).add(new Message(lastSender, senderAndMessage[1], parseDateTime(parts[0])));
                }
                else {
                    if (lastSender != null) {
                        // If the last message was sent by the same person, append the current line to the last message
                        messages.get(lastSender).set(
                                messages.get(lastSender).size() - 1,
                                new Message(lastSender,
                                        messages.get(lastSender).getLast().getContent() + "\n" + line,
                                        messages.get(lastSender).getLast().getTimestamp())
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
        if (amount > getMessagesCount()) throw new IllegalArgumentException("Amount is greater than the number of messages");

        // Map: Participant -> (Word -> Frequency)
        Map<String, Map<String, Integer>> wordFrequencyMap = new HashMap<>();

        for (Map.Entry<String, List<Message>> entry : chat.entrySet()) {
            String sender = entry.getKey();
            List<Message> messages = entry.getValue();

            if (sender == null || sender.trim().isEmpty() || messages == null) continue; // Skip invalid entries

            // Initialize the word frequency map for the sender
            Map<String, Integer> frequencyMap = wordFrequencyMap.computeIfAbsent(sender, k -> new HashMap<>());

            // Process each message from the sender
            for (Message message : messages) {
                if (message == null || message.getContent() == null) continue;

                // Extract words and update frequency
                List<String> words = extractWords(message.getContent());
                for (String word : words) {
                    frequencyMap.merge(word, 1, Integer::sum);
                }
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
        return chat.keySet().stream()
                .collect(Collectors.toMap(participant -> participant, participant -> getWordFrequency(word, participant)));
    }

    // Fix Umlaute
    public int getWordFrequency(String wordToSearchFor, String participant) {
        if (wordToSearchFor == null || wordToSearchFor.trim().isEmpty()) throw new IllegalArgumentException("Word to search for is empty or null");
        if (participant == null || participant.trim().isEmpty()) throw new IllegalArgumentException("Participant is empty or null");
        if (chat == null || chat.isEmpty()) throw new IllegalArgumentException("Chat is empty or not initialized");

        // Normalize the search word
        String cleanSearchWord = wordToSearchFor.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_äöüß]", "");

        // Check if the participant exists
        if (!chat.containsKey(participant)) {
            System.out.println("Participant \"" + participant + "\" not found");
            return 0;
        }

        int frequency = 0;

        // Retrieve the participant's messages
        List<Message> messages = chat.get(participant);

        if (messages != null) {
            for (Message message : messages) {
                List<String> words = extractWords(message.getContent());
                for (String word : words) {
                    if (word.equalsIgnoreCase(cleanSearchWord)) {
                        frequency++;
                    }
                }
            }
        }

        return frequency;
    }

    public ArrayList<Message> getSentenceByWord(String word) {
        if (word.isEmpty()) throw new IllegalArgumentException("Word is empty");
        if (chat.isEmpty()) throw new IllegalArgumentException("Chat is empty");

        return chat.keySet().stream()
                .map(participant -> getSentenceByWord(word, participant))
                .flatMap(ArrayList::stream)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public ArrayList<Message> getSentenceByWord(String word, String participant) {
        if (word == null || word.isEmpty()) throw new IllegalArgumentException("Word is empty or null");
        if (participant == null || participant.isEmpty()) throw new IllegalArgumentException("Participant is empty or null");
        if (chat == null || chat.isEmpty()) throw new IllegalArgumentException("Chat is empty or not initialized");

        // Normalize the word for case-insensitive comparison
        String lowerCaseWord = word.toLowerCase(Locale.ROOT);

        // Check if the participant exists
        if (!chat.containsKey(participant)) {
            System.out.println("Participant \"" + participant + "\" not found");
            return new ArrayList<>();
        }

        List<Message> messages = chat.get(participant);

        // Filter messages containing the word and return as ArrayList
        return messages.stream()
                .filter(message -> message.getContent() != null &&
                        message.getContent().toLowerCase(Locale.ROOT).contains(lowerCaseWord))
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
        return chat.keySet();
    }

    public Long getMessagesCount() {
        return (long) chat.values().stream().mapToInt(List::size).sum();
    }

    public Long getAmountOfMessagesPerParticipant(String participant) {
        if (participant == null || participant.trim().isEmpty()) throw new IllegalArgumentException("Participant is empty or null");
        if (chat == null || chat.isEmpty()) throw new IllegalArgumentException("Chat is empty or not initialized");

        return (long) chat.get(participant).size();
    }

    public String getMostActiveUser() {
        if (chat.isEmpty()) throw new IllegalArgumentException("Chat is empty");

        return chat.entrySet().stream()
                .max(Comparator.comparingInt(entry -> entry.getValue().size()))
                .map(Map.Entry::getKey)
                .orElse("No messages found");
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
        sb.append("Chat Analysis:\n")
                .append("----------------\n")
                .append("Total Messages: ").append(getMessagesCount()).append("\n")
                .append("Participants: ").append(String.join(", ", getParticipants())).append("\n")
                .append("Messages per Participant:\n");

        getParticipants().forEach(participant ->
                sb.append("\t- ").append(participant).append(": ")
                        .append(getAmountOfMessagesPerParticipant(participant)).append("\n")
        );

        return sb.toString();
    }
}
