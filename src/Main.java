import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class Main {

  public static void main(String[] args) throws Exception
  {
    String testFileName = "src/ChatTest.txt";
    List<String> test = Files.readAllLines(Paths.get(testFileName));
    String host = test.remove(0);
    int port = Integer.valueOf(test.remove(0));
    ChatServer chatServer = new ChatServer(host, port);
    chatServer.startServer();

    ExecutorService executorService = Executors.newCachedThreadPool();
    List<ChatClientTask> clientTasks = new ArrayList<>();

    for (String line : test)
    {
      String[] strings = line.split("\t");
      String id = strings[0];
      int wait = Integer.valueOf(strings[1]);
      List<String> messages = new ArrayList<>();
      for (int i = 2; i < strings.length; i++) messages.add(strings[i] + ", mówię ja, " +id);
      ChatClient c = new ChatClient(host, port, id);
      ChatClientTask chatClientTask = ChatClientTask.create(c, messages, wait);
      clientTasks.add(chatClientTask);
      executorService.execute(chatClientTask);
    }
    clientTasks.forEach( task ->
    {
      try
      {
        task.get();
      }
      catch (InterruptedException | ExecutionException exception)
      {System.out.println("*** " + exception);}
    });
    executorService.shutdown();
    chatServer.stopServer();

    System.out.println("\n=== Server log ===");
    System.out.println(chatServer.getServerLog());

    clientTasks.forEach(clientTask -> System.out.println(clientTask.getClient().getChatView()));
  }
}
