import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
/**<h3>Wymagania konstrukcyjne dla klasy ChatClient:</h3>

 - nieblokujące wejście - wyjście*/
public class ChatClient
{
    private final String host;
    private final int port;
    private final String id;
    private final int TIMEOUT = 100;

    private SocketChannel socketChannel;
    private final StringBuilder chatView = new StringBuilder();
    /** public ChatClient(String host, int port, String id), gdzie id - id klienta*/
    public ChatClient(String host, int port, String id)
    {
        this.host = host;
        this.port = port;
        this.id = id;
    }
   /**public void login() - loguje klienta na serwer*/
    public void login()
    {
        try
        {
            socketChannel = SocketChannel.open();
            socketChannel.connect(new InetSocketAddress(host, port));
            socketChannel.configureBlocking(false);

            send(id + " logged in");
//            chatView.append(id).append(" logged in").append("\n");
        }
        catch (IOException exception)
        {System.err.println("ChatClient login() exception: " + exception.getMessage());}
    }
    /**public void logout() - wylogowuje klienta*/
    public void logout()
    {
        try
        {
            send(id + " logged out");
            chatView.append(id).append(" logged out").append("\n");

            if (socketChannel != null && socketChannel.isConnected())
            {socketChannel.close();}
        }
        catch (IOException exception)
        {System.err.println("ChatClient logout() exception: " + exception.getMessage());}
    }
    /**public void send(String req)  - wysyła do serwera żądanie req*/
    public void send(String request)
    {
        if (socketChannel == null || !socketChannel.isConnected())
        {throw new IllegalStateException("ChatClient send() exception: Client is not connected to the server.");}

        try
        {
            ByteBuffer wrapBuffer = ByteBuffer.wrap(request.getBytes(StandardCharsets.UTF_8));

            while (wrapBuffer.hasRemaining())
            {socketChannel.write(wrapBuffer);}

            ByteBuffer readBuffer = ByteBuffer.allocate(2048);
            long lastTime = System.currentTimeMillis();

            while (System.currentTimeMillis() - lastTime < TIMEOUT)
            {
                int bytesRead = socketChannel.read(readBuffer);

                if (bytesRead > 0)
                {
                    lastTime = System.currentTimeMillis();

                    readBuffer.flip();
                    String response = StandardCharsets.UTF_8.decode(readBuffer).toString();
                    chatView.append(response);

                    readBuffer.clear();
                }
                if (bytesRead == -1)
                {break;}
            }
        }
        catch (IOException exception)
        {System.err.println("ChatClient send() exception: " + exception.getMessage());}
    }
    /**public String getChatView() - zwraca dotychczasowy widok czatu z pozycji danego klienta (czyli wszystkie infomacje, jakie dostaje on po kolei od serwera)*/
    public String getChatView()
    {return "=== " + id + " chat view\n" + chatView;}
}
