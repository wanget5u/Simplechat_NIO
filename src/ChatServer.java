import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/**<h3>Wymagania konstrukcyjne dla klasy ChatServer:</h3>

 - Multipleksowania kanałów gniazd (użycie selektora), <br>
 - Serwer może obsługiwać równolegle wielu klientów, ale obsługa żądań klientów odbywa się w jednym wątku*/
public class ChatServer implements Runnable
{
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final String host;
    private final int port;
    boolean isRunning = false;

    private final List<String> chatHistory = new CopyOnWriteArrayList<>();
    private final Map<SocketChannel, String> activeClients = new ConcurrentHashMap<>();
    private final Map<SocketChannel, LocalTime> loginTimestamps = new ConcurrentHashMap<>();
    private final Map<SocketChannel, Integer> lastSentIndex = new ConcurrentHashMap<>();
    private final StringBuilder serverLog = new StringBuilder();
    /**public ChatServer(String host, int port)*/
    public ChatServer(String host, int port)
    {
        this.host = host;
        this.port = port;
    }
    /**public void startServer(), która uruchamia serwer w odrębnym wątku*/
    public void startServer()
    {isRunning = true; System.out.println("Server started"); executorService.submit(this);}
    /**public void stopServer(), która zatrzymuje serwer i wątek, w którym działa*/
    public void stopServer()
    {isRunning = false; System.out.println("Server stopped"); executorService.shutdown();}
    /**String getServerLog() - zwraca  log serwera*/
    public String getServerLog()
    {return serverLog.toString();}

    @Override
    public void run()
    {
        try
        (
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            Selector selector = Selector.open()
        )
        {
            serverSocketChannel.bind(new InetSocketAddress(host, port));
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            while (isRunning)
            {
                selector.select(200);

                for (Iterator<SelectionKey> iterator = selector.selectedKeys().iterator(); iterator.hasNext();)
                {
                    SelectionKey selectionKey = iterator.next();
                    iterator.remove();

                    if (selectionKey.isAcceptable())
                    {accept(selector, serverSocketChannel);}

                    else if (selectionKey.isReadable())
                    {read(selectionKey);}
                }
            }
        }
        catch (IOException exception)
        {System.err.println("ChatServer run() exception: " + exception.getMessage());}
    }
    private void accept(Selector selector, ServerSocketChannel serverSocketChannel)
    {
        try
        {
            serverSocketChannel
                    .accept()
                    .configureBlocking(false)
                    .register(selector, SelectionKey.OP_READ);
        }
        catch (IOException exception)
        {System.err.println("ChatServer accept() exception: " + exception.getMessage());}
    }
    private void read(SelectionKey selectionKey)
    {
        try
        {
            SocketChannel clientChannel = (SocketChannel) selectionKey.channel();
            ByteBuffer byteBuffer = ByteBuffer.allocate(2048);
            int bytesRead = clientChannel.read(byteBuffer);

            if (bytesRead == -1)
            {
                selectionKey.cancel();
                clientChannel.close();
                return;
            }

            byteBuffer.flip();
            String clientMessage = StandardCharsets.UTF_8.decode(byteBuffer).toString();
            byteBuffer.clear();

            String chatEntry;

            if (clientMessage.endsWith("logged in"))
            {
                String clientId = clientMessage.trim().split(" ")[0];
                activeClients.put(clientChannel, clientId);
                loginTimestamps.put(clientChannel, LocalTime.now());
                lastSentIndex.put(clientChannel, chatHistory.size());

                chatEntry = LocalTime.now() + " " + clientMessage;
            }
            else if (clientMessage.endsWith("logged out"))
            {
                activeClients.remove(clientChannel);
                loginTimestamps.remove(clientChannel);
                lastSentIndex.remove(clientChannel);
                selectionKey.cancel();
                clientChannel.close();

                chatEntry = LocalTime.now() + " " + clientMessage;
            }
            else
            {chatEntry = LocalTime.now() + " " + activeClients.get(clientChannel) + ": " + clientMessage;}

            serverLog.append(chatEntry).append("\n");
            chatHistory.add(chatEntry);

            broadcastChat();
        }
        catch (IOException exception)
        {
            System.err.println("ChatServer read() exception: " + exception.getMessage());
            selectionKey.cancel();
        }
    }
    private void broadcastChat()
    {
        for (SocketChannel clientChannel : activeClients.keySet())
        {
            Integer lastIndex = lastSentIndex.getOrDefault(clientChannel, 0);
            LocalTime loginTime = loginTimestamps.get(clientChannel);
            List<String> chatToSend = new ArrayList<>();

            for (int x = lastIndex; x < chatHistory.size(); x++)
            {
                String entry = chatHistory.get(x);
                String[] entryChatParts = entry.split(" ");

                String timestamp = entryChatParts[0];

                if (LocalTime.parse(timestamp).isAfter(loginTime.minusNanos(1)))
                {chatToSend.add(String.join(" ", Arrays.copyOfRange(entryChatParts, 1, entryChatParts.length)));}
            }

            if (!chatToSend.isEmpty())
            {
                String message = String.join("\n", chatToSend) + "\n";
                ByteBuffer byteBuffer = ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8));

                try
                {
                    byteBuffer.rewind();
                    clientChannel.write(byteBuffer);
                    lastSentIndex.put(clientChannel, chatHistory.size());
                }
                catch (IOException exception)
                {System.err.println("ChatServer broadcastChat() exception: " + exception.getMessage());}
            }
        }
    }
}