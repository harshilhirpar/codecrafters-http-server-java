import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

  public static final String TEXT_PLAIN_CONTENT_TYPE = "text/plain";
  public static final String NOT_FOUND_ERROR_STRING = "HTTP/1.1 404 Not Found\r\n\r\n";
  private static final ExecutorService threadPool = Executors.newFixedThreadPool(10);
  private static final String FILE_PATH_INIT = "tmp/";
  private static final String OCTET_STREAM_CONTENT_TYPE = "application/octet-stream";
  private static final String STATUS_200_OK = "200 OK";

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

//      BufferedReader to provide efficient reading of characters, arrays, and lines.
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
//      Read the request and split it to get Request Target
        String[] splittedRequest = request.split(" ");
//      Then check for the requirement
        boolean isRequestTargetExist = splittedRequest[1].length() != 1;

        if(isRequestTargetExist){
//        Write logic for echo
//        Inside if that means request target does exist
          String[] splittedRequestTarget = splittedRequest[1].split("/");
          int n = splittedRequestTarget.length;

          if(splittedRequestTarget[1].equals("echo")){

//          And the last index will always be the message to respond with
            int contentLength = splittedRequestTarget[n-1].length();
            String content = splittedRequestTarget[n-1];

//          Now we got contentLength, contentType, and content
            String responseMessage = "HTTP/1.1 200 OK\r\nContent-Type: " + TEXT_PLAIN_CONTENT_TYPE + "\r\nContent-Length: " + contentLength + "\r\n\r\n" + content;
            writer.write(responseMessage.getBytes());
          }

//          Handle User agent, Request headers
          if(splittedRequestTarget[1].equals("user-agent")){
            writer.write(workWithHeaders(header).getBytes());
          }

//          Handle Files
          if(splittedRequestTarget[1].equals("files")){

            String fileName = splittedRequestTarget[n-1];
            System.out.println(fileName);
            String filePath = FILE_PATH_INIT + fileName + ".txt";
            System.out.println(filePath);
            File file = new File(filePath);
            System.out.println(file.exists());

            if(file.exists()){
              try{
                BufferedReader file_reader = new BufferedReader(new FileReader(file));
                String line;
                StringBuilder file_content = new StringBuilder();
                while((line = file_reader.readLine()) != null){
                  file_content.append(line);
                }
                String responseMessage = "HTTP/1.1 " + STATUS_200_OK + "\r\n" + "Content-Type: " + OCTET_STREAM_CONTENT_TYPE + "\r\nContent-Length: " + file_content.length() + "\r\n\r\n" + file_content.toString();
                writer.write(responseMessage.getBytes());
              }catch (IOException e){
                System.out.println("Error file not found: " + e.getMessage());
              }

            }else {
              writer.write(NOT_FOUND_ERROR_STRING.getBytes());
            }
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
