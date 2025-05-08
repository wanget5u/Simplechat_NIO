import java.util.List;
import java.util.concurrent.FutureTask;
/**<h3>Wymagania konstrukcyjne dla klasy ChatClientTask:</h3>

 - umożliwia uruchamianie klientów w odrębnych wątkach poprzez ExecutorService. */
public class ChatClientTask extends FutureTask<String>
{
    ChatClient chatClient;
    /**Kod działający w wątku ma wykonywać następując działania: <br>
     * - łączy się z serwerem i loguje się (c.login() <br>
     - wysyła kolejne wiadomości z listy msgs (c.send(...)) <br>
     - wylogowuje klienta (c.logout())*/
    public ChatClientTask(ChatClient client, List<String> messages, int wait)
    {
        super(() ->
        {
            client.login();
            if (wait != 0) {Thread.sleep(wait);}

            for (String message : messages)
            {
                client.send(message);
                if (wait != 0) {Thread.sleep(wait);}
            }

            client.logout();
            if (wait != 0) {Thread.sleep(wait);}

            return "";
        });

        this.chatClient = client;
    }
    /**public static ChatClientTask create(Client c, List<String> msgs, int wait) gdzie: <br>
     * - c - obiekt klasy Client <br>
     * - msgs - lista wiadomości do wysłania przez klienta c <br>
     * - wait - czas wstrzymania pomiędzy wysyłaniem żądań.
     * <br> <br>
     * Parametr wait w sygnaturze metody create oznacza czas w milisekundach, na jaki wątek danego klienta jest wstrzymywany po każdym żądaniu. Jeśli wait jest 0, wątek klienta nie jest wstrzymywany,*/
    public static ChatClientTask create(ChatClient c, List<String> messages, int wait)
    {return new ChatClientTask(c, messages, wait);}

    public ChatClient getClient()
    {return chatClient;}
}
