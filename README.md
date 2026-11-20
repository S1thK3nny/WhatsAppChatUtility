# WhatsAppChatUtility

A small Java library for analyzing exported WhatsApp chats: message counts, activity per participant, word frequencies, and chainable queries to filter messages by participant, word, or timeframe.

## Requirements

- Java 21 or newer
- A WhatsApp chat export from a phone set to German (see the [supported formats](#supported-format) section)

## Exporting a chat

To export a chat, open WhatsApp and the desired chat. Click on the three dots -> More -> Export Chat -> Without media.
This will give you a `.txt` file that can be analyzed by the library.

## Running

```bash
javac -d out $(find src -name "*.java")
java -cp out Main "path/to/chat.txt"
```

Or open the project in IntelliJ IDEA and set the path as a program argument in the run configuration.

Example output:

```
Chat Analysis:
----------------
Total Messages: 3
Participants (sorted by message count): Alice, Bob
Earliest Message: 01.01.24, 10:00
Latest Message: 02.01.24, 12:00
Messages per Participant:
	- Alice: 2
	- Bob: 1
```

## Usage

`Main.java` only prints a summary. For anything else, use the API directly:

```java
ChatAnalysis chat = new ChatAnalysis("path/to/chat.txt");

chat.getParticipants();                    // Set of all senders
chat.getMostActiveUser();                  // sender with the most messages
chat.getAverageMessagePerDay();            // average messages per day, per participant
chat.getMessagesByParticipant("Alice");    // all messages from Alice
chat.getMessagesByWord("pizza");           // messages containing "pizza" (case-insensitive)
chat.getWordFrequency("pizza");            // how often each participant used "pizza"
chat.getMessagesByTimeframe("01.01.24, 00:00", "02.01.24, 00:00");
chat.printTopAmountUsedWords(10);          // prints each participant's top 10 words
```

### Queries

`query()` returns a `ChatQuery`, so you can chain filters:

```java
chat.query()
    .filterByParticipant("Alice")
    .filterByTimeframe("01.01.24, 00:00", "31.01.24, 23:59")
    .filterByExactWord("pizza")
    .toList()                              // sorted by timestamp
    .forEach(System.out::println);

chat.query().filterByParticipant("Bob").getTopNWordFrequency(20);
chat.query().filterByWord("birthday").getMessageCount();
```

A query can be turned back into a `ChatAnalysis` with `toChatAnalysis()` to get the full summary for a subset of the chat.

## Supported format

The parser expects the German export format:

```
01.01.24, 10:00 - Alice: Hello there
01.01.24, 10:05 - Bob: Hi Alice - how are you?
this line is appended to Bob's previous message
```

- Dates are `dd.MM.yy, HH:mm`, and timeframe arguments use the same format.
- Lines without a date are treated as a continuation of the previous message.
- System messages (e.g. "X added Y") and media placeholders (`<Medien ausgeschlossen>`) are skipped.

Phones set to other languages use different date formats (e.g. 1/1/24, 10:00 am) and are not yet supported.

## Privacy

Chat exports contain other people's messages. Don't commit or share them. `*.txt` is already in `.gitignore`.

## License

Released under the [MIT License](LICENSE), © 2025 S1thK3nny.