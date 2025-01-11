package Util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import DataStructures.Message;

import static Util.Functions.*;

public class ChatAnalysis {
    private final HashMap<String, List<Message>> chat;

    //================================================================================
    // Initialize Chat
    //================================================================================
    // region Initialize Chat

    public ChatAnalysis(String path) {
        this.chat = readChat(path);
    }

    public ChatAnalysis(ChatQuery query) {
        this.chat = readChat(query);
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

                // Skip empty lines
                if (parts.length < 1) continue;

                // If the line starts with a date, it's a new message
                if (parts[0].matches("^\\d{2}\\.\\d{2}\\.\\d{2}, \\d{2}:\\d{2}.*")) {
                    int dashIndex = line.indexOf(" - ");
                    if (dashIndex == -1) continue;

                    String dateTimePart = line.substring(0, dashIndex).trim();
                    String messagePart = line.substring(dashIndex + 3).trim();

                    String[] senderAndMessage = messagePart.split(": ", 2); // Only split the first colon

                    if (senderAndMessage.length < 2 || senderAndMessage[1].equals("<Medien ausgeschlossen>")) {
                        // Skip invalid messages or media placeholders
                        continue;
                    }

                    lastSender = senderAndMessage[0].trim();
                    messages.putIfAbsent(lastSender, new ArrayList<>());
                    messages.get(lastSender).add(
                            new Message(lastSender, senderAndMessage[1], parseDateTime(dateTimePart))
                    );
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

    public HashMap<String, List<Message>> readChat(ChatQuery query) {
        if (query == null) throw new IllegalArgumentException("Query is null");

        HashMap<String, List<Message>> messages = new HashMap<>();
        query.toList().forEach(message -> {
            messages.putIfAbsent(message.getSender(), new ArrayList<>());
            messages.get(message.getSender()).add(message);
        });

        return messages;
    }
    //endregion

    //================================================================================
    // Get-Methods
    //================================================================================
    // region Get-Methods

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

    public ArrayList<Message> getMessagesByWord(String word) {
        if (word.isEmpty()) throw new IllegalArgumentException("Word is empty");
        if (chat.isEmpty()) throw new IllegalArgumentException("Chat is empty");

        return chat.keySet().stream()
                .map(participant -> getMessagesByWord(word, participant))
                .flatMap(ArrayList::stream)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public ArrayList<Message> getMessagesByWord(String word, String participant) {
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
    public ArrayList<Message> getMessagesByExactWord(String word) {
        if (word.isEmpty()) throw new IllegalArgumentException("Word is empty");
        if (chat.isEmpty()) throw new IllegalArgumentException("Chat is empty");

        return getParticipants().stream()
                .map(participant -> getMessagesByExactWord(word, participant))
                .flatMap(ArrayList::stream)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    // Broken method
    public ArrayList<Message> getMessagesByExactWord(String word, String participant) {
        if (word.isEmpty()) throw new IllegalArgumentException("Word is empty");
        if (chat.isEmpty()) throw new IllegalArgumentException("Chat is empty");

        return getMessagesByWord(word, participant).stream()
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

    public ArrayList<Message> getMessagesByParticipant(String participant) {
        if (participant == null || participant.trim().isEmpty()) throw new IllegalArgumentException("Participant is empty or null");
        if (chat == null || chat.isEmpty()) throw new IllegalArgumentException("Chat is empty or not initialized");
        if (!chat.containsKey(participant)) throw new IllegalArgumentException("Participant not found");

        return new ArrayList<>(chat.get(participant));
    }

    public ArrayList<Message> getMessagesByTimeframe(String start, String end) {
        if (start == null || end == null) throw new IllegalArgumentException("Start or end date is null");
        if (!isValidTimeFormat(start) || !isValidTimeFormat(end)) throw new IllegalArgumentException("Invalid date format");
        if (chat.isEmpty()) throw new IllegalArgumentException("Chat is empty");

        return chat.values().stream()
                .flatMap(List::stream)
                .filter(message -> message.getTimestamp().isAfter(parseDateTime(start)) && message.getTimestamp().isBefore(parseDateTime(end)))
                .sorted(Comparator.comparing(Message::getTimestamp)) // Sort by timestamp
                .collect(Collectors.toCollection(ArrayList::new));
    }


    public String getMostActiveUser() {
        if (chat.isEmpty()) throw new IllegalArgumentException("Chat is empty");

        return chat.entrySet().stream()
                .max(Comparator.comparingInt(entry -> entry.getValue().size()))
                .map(Map.Entry::getKey)
                .orElse("No messages found");
    }

    public HashMap<String, Double> getAverageMessagePerDay() {
        if (chat.isEmpty()) return new HashMap<>();

        HashMap<String, Double> averageMessagesPerDay = new HashMap<>();
        chat.keySet().forEach(participant -> averageMessagesPerDay.put(participant, getAverageMessagePerDay(participant)));

        return averageMessagesPerDay;
    }

    public double getAverageMessagePerDay(String participant) {
        if (participant == null || participant.trim().isEmpty()) throw new IllegalArgumentException("Participant is empty or null");
        if (chat == null || chat.isEmpty()) return 0;
        if (!chat.containsKey(participant)) throw new IllegalArgumentException("Participant not found");

        List<Message> messages = chat.get(participant);
        if (messages == null || messages.isEmpty()) return 0;

        messages.sort(Comparator.comparing(Message::getTimestamp));

        // Get the first and last message timestamps
        LocalDateTime firstMessageTime = messages.get(0).getTimestamp();
        LocalDateTime lastMessageTime = messages.get(messages.size() - 1).getTimestamp();

        // Calculate the number of days between the first and last message (inclusive)
        long daysBetween = Duration.between(firstMessageTime, lastMessageTime).toDays() + 1;

        // Return the average messages per day
        return (double) messages.size() / daysBetween;
    }
    //endregion

    //================================================================================
    // Print-Methods
    //================================================================================
    // region Print-Methods

    // Prints the top <amount> most used words per person
    public void printTopAmountUsedWords(int amount) {
        if (amount < 1) throw new IllegalArgumentException("Amount must be greater than 0");
        if (chat == null || chat.isEmpty()) throw new IllegalArgumentException("Chat is empty or not initialized");
        if (amount > getMessagesCount()) throw new IllegalArgumentException("Amount is greater than the number of messages");

        chat.keySet().forEach(participant -> printTopAmountUsedWords(amount, participant));
    }

    public void printTopAmountUsedWords(int amount, String participant) {
        if (amount < 1) throw new IllegalArgumentException("Amount must be greater than 0");
        if (chat == null || chat.isEmpty()) throw new IllegalArgumentException("Chat is empty or not initialized");
        if (participant == null || participant.trim().isEmpty()) throw new IllegalArgumentException("Participant is empty or null");
        if (!chat.containsKey(participant)) throw new IllegalArgumentException("Participant not found");

        List<Message> messages = chat.get(participant);
        if (messages == null || messages.isEmpty()) throw new IllegalArgumentException("No messages found for participant");
        if (amount > messages.size()) amount = messages.size();

        // Map: Word -> Frequency
        Map<String, Integer> frequencyMap = new HashMap<>();

        // Process each message from the participant
        for (Message message : messages) {
            if (message == null || message.getContent() == null) continue;

            // Extract words and update frequency
            List<String> words = extractWords(message.getContent());
            for (String word : words) {
                frequencyMap.merge(word, 1, Integer::sum);
            }
        }

        // Display top 'amount' words for the participant
        System.out.println("\nTop " + amount + " words for " + participant + ":");
        frequencyMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(amount)
                .forEach(entry -> System.out.println(entry.getKey() + ": " + entry.getValue()));
    }
    //endregion

    //================================================================================
    // Query Methods for Function Chaining
    //================================================================================
    // region Query Methods

    /**
     * Initiates a new ChatQuery with all messages.
     * @return a new ChatQuery instance containing all messages.
     */
    public ChatQuery query() {
        // Flatten all messages into a single collection
        Collection<Message> allMessages = chat.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
        return new ChatQuery(allMessages);
    }

    //endregion

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Chat Analysis:\n")
                .append("----------------\n")
                .append("Total Messages: ").append(getMessagesCount()).append("\n");

        // Get all participants and sort them by their message count in descending order
        List<String> sortedParticipants = new ArrayList<>(getParticipants());
        sortedParticipants.sort((p1, p2) ->
                Integer.compare(getMessagesByParticipant(p2).size(), getMessagesByParticipant(p1).size()));

        sb.append("Participants (sorted by message count): ").append(
                String.join(", ", sortedParticipants)).append("\n");

        sb.append("Messages per Participant:\n");

        sortedParticipants.forEach(participant ->
                sb.append("\t- ").append(participant).append(": ")
                        .append(getMessagesByParticipant(participant).size()).append("\n")
        );

        return sb.toString();
    }
}