import Util.ChatAnalysis;

public class Main {
    public static void main(String[] args) {
        // ChatAnalysis chatAnalysis = new ChatAnalysis("path/to/chat.txt");
        ChatAnalysis chatAnalysis = new ChatAnalysis("path/to/chat.txt");
        System.out.println(chatAnalysis);
        System.out.println(chatAnalysis.getMessagesByParticipant("Alice"));
    }
}