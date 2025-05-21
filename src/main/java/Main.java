import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

  public static final String TEXT_PLAIN_CONTENT_TYPE = "text/plain";
  public static final String NOT_FOUND_ERROR_STRING = "HTTP/1.1 404 Not Found\r\n\r\n";
  private static final ExecutorService threadPool = Executors.newFixedThreadPool(10);

  public static void main(String[] args) {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.out.println("Logs from your program will appear here!");
     try {
       ServerSocket serverSocket = new ServerSocket(4221);
       serverSocket.setReuseAddress(true);

       while(true){
         Socket clientSocket = serverSocket.accept();
         threadPool.execute(new ClientHandler(clientSocket));
       }
     } catch (IOException e) {
       System.out.println("IOException: " + e.getMessage());
     }finally {
       threadPool.shutdown();
     }
  }


//  To format a response we need content type, content length, and content
  public static String formatResponseString(String content, String status){
    return "HTTP/1.1 " + status + "\r\nContent-Type: " + TEXT_PLAIN_CONTENT_TYPE + "\r\nContent-Length: " + content.trim().length() + "\r\n\r\n" + content.trim();
  }

  public static String workWithHeaders(String header){
    String[] splittedHeader = header.split(":");
    String content = splittedHeader[1].trim();
    return formatResponseString(content, "200 OK");
  }

  private static class ClientHandler implements  Runnable{

    private final Socket clientSocket;

    public ClientHandler(Socket clientSocket){
      this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
//      This will run for every thread pool
      try{

        //       BufferedReader to provide efficient reading of characters, arrays, and lines.
        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        String request = reader.readLine();
        String inputLine;
        String header = "";
        while (!(inputLine = reader.readLine()).isEmpty()){
//         Getting header
          if(inputLine.contains("User-Agent")){
            header = inputLine;
          }
        }

        System.out.println("Message from client: " + request);
        OutputStream writer = clientSocket.getOutputStream();
        //       Read the request and split it to get Request Target
        String[] splittedRequest = request.split(" ");
//       Then check for the requirement
        boolean isRequestTargetExist = splittedRequest[1].length() != 1;

        if(isRequestTargetExist){
          String responseMessage = "HTTP/1.1 404 Not Found\r\n\r\n";
//         Write logic for echo
//         Inside if that means request target does exist
          String[] splittedRequestTarget = splittedRequest[1].split("/");
          if(splittedRequestTarget[1].equals("echo")){
            //         And the last index will always be the message to respond with
            int n = splittedRequestTarget.length;
            int contentLength = splittedRequestTarget[n-1].length();
            String content = splittedRequestTarget[n-1];
//         Now we got contentLength, contentType, and content
            responseMessage = "HTTP/1.1 200 OK\r\nContent-Type: " + TEXT_PLAIN_CONTENT_TYPE + "\r\nContent-Length: " + contentLength + "\r\n\r\n" + content;
            writer.write(responseMessage.getBytes());
          }

          if(splittedRequestTarget[1].equals("user-agent")){
            writer.write(workWithHeaders(header).getBytes());
          }
          writer.write(NOT_FOUND_ERROR_STRING.getBytes());
        }else{
          String responseMessage = "HTTP/1.1 200 OK\r\n\r\n";
          writer.write(responseMessage.getBytes());
        }
      }catch (IOException e){
        System.err.println("Error handling client: " + e.getMessage());
      }
    }
  }

}
