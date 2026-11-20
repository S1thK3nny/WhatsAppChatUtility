import Util.ChatAnalysis;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java Main <path-to-exported-chat.txt>");
            System.exit(1);
        }

        ChatAnalysis chatAnalysis = new ChatAnalysis(args[0]);
        System.out.println(chatAnalysis);

        // More examples (replace the names/dates with ones from your chat):
        // chatAnalysis.getMessagesByParticipant("Alice").forEach(System.out::println);
        // chatAnalysis.getMessagesByTimeframe("01.01.24, 00:00", "02.01.24, 00:00").forEach(System.out::println);
        // chatAnalysis.printTopAmountUsedWords(10);
        // chatAnalysis.query()
        //         .filterByParticipant("Alice")
        //         .filterByWord("pizza")
        //         .toList()
        //         .forEach(System.out::println);
    }
}
