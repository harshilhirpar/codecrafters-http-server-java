import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
  public static void main(String[] args) {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.out.println("Logs from your program will appear here!");

    // Uncomment this block to pass the first stage
    //
     try {
       ServerSocket serverSocket = new ServerSocket(4221);
       serverSocket.setReuseAddress(true);
//       Wait for connection from client.
       Socket connectionFromClient = serverSocket.accept();
       BufferedReader reader = new BufferedReader(new InputStreamReader(connectionFromClient.getInputStream()));
       String request = reader.readLine();
       System.out.println("Message from client: " + request);
       OutputStream writer = connectionFromClient.getOutputStream();
       String responseMessage = "HTTP/1.1 200 OK\r\n\r\n";
       writer.write(responseMessage.getBytes());
       System.out.println("Message sent to client: "+ responseMessage);
//       serverSocket.close();
     } catch (IOException e) {
       System.out.println("IOException: " + e.getMessage());
     }
  }
}
