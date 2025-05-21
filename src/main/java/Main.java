import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {

  public static final String TEXT_PLAIN_CONTENT_TYPE = "text/plain";
  public static final String NOT_FOUND_ERROR_STRING = "HTTP/1.1 404 Not Found\r\n\r\n";

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

//       BufferedReader to provide efficient reading of characters, arrays, and lines.
       BufferedReader reader = new BufferedReader(new InputStreamReader(connectionFromClient.getInputStream()));
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
       OutputStream writer = connectionFromClient.getOutputStream();
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
       serverSocket.close();
     } catch (IOException e) {
       System.out.println("IOException: " + e.getMessage());
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

}
