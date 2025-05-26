import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

  public static final String TEXT_PLAIN_CONTENT_TYPE = "text/plain";
  public static final String NOT_FOUND_ERROR_STRING = "HTTP/1.1 404 Not Found\r\n\r\n";
  private static final ExecutorService threadPool = Executors.newFixedThreadPool(10);
  private static final String OCTET_STREAM_CONTENT_TYPE = "application/octet-stream";
  private static final String STATUS_200_OK = "200 OK";
  private static final String STATUS_201_CREATED = "HTTP/1.1 201 Created\r\n\r\n";

  public static void main(String[] args) {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.out.println("Logs from your program will appear here!");
     try {
       System.out.println("INFO: Starting the program");
       ServerSocket serverSocket = new ServerSocket(4221);
       serverSocket.setReuseAddress(true);

       while(true){
         System.out.println("INFO: Accepting clients");
         Socket clientSocket = serverSocket.accept();
         System.out.println("INFO: Executing thread pool in order to run concurrent requests");
         threadPool.execute(new ClientHandler(clientSocket, args));
//         serverSocket.close();
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

  public static void writeFileHandler(File file, StringBuilder reqBody){
    try{
      FileWriter fileWriter = new FileWriter(file);
      fileWriter.write(String.valueOf(reqBody));
      fileWriter.close();
    }catch (IOException e){
      System.out.println("ERROR: writing in file, post request: " + e.getMessage());
    }
  }

  private static class ClientHandler implements  Runnable{

    private final Socket clientSocket;
    private String[] args;

    public ClientHandler(Socket clientSocket, String[] args){
      this.clientSocket = clientSocket;
      this.args = args;
    }

    @Override
    public void run() {
//      This will run for every thread pool
      try{

//      BufferedReader to provide efficient reading of characters, arrays, and lines.
        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

//        TODO: GETTING REQUEST
        String request = reader.readLine();

//        TODO: GETTING REQUEST HEADER
        String inputLine;
        String header = "";
        String cntLength = "";
        String cntType = "";
        String acceptEncoding = "";
        while ((inputLine = reader.readLine()) != null && !inputLine.isEmpty()){
//         Getting header
          if(inputLine.contains("User-Agent")){
            header = inputLine;
          }
          if(inputLine.contains("Content-Length")){
            cntLength = inputLine;
          }
          if(inputLine.contains("Content-Type")){
            cntType = inputLine;
          }
          if(inputLine.contains("Accept-Encoding")){
            acceptEncoding = inputLine;
          }
        }

//        TODO: GETTING REQUEST BODY
        int reqBodyReader;
        int count = 0;
        if(cntLength.isEmpty()){
          System.out.println("Something went wrong");
        }else{
          count = Integer.parseInt(cntLength.split(":")[1].trim());
        }
        StringBuilder reqBody = new StringBuilder();
        for(int i = 0; i< count; i++){
          reqBodyReader = reader.read();
          if(reqBodyReader == -1){
            System.out.println("ERROR: No request Body found, Something went wrong");
          }
          reqBody.append((char)reqBodyReader);
        }


//        TODO: PRINTING NETWORK REQUEST INFORMATION
        System.out.println("Request Information");
        System.out.println("===================================================================");
        System.out.println("Request: " + request);
        System.out.println(header);
        System.out.println(cntLength);
        System.out.println(cntType);
        System.out.println(acceptEncoding);
        System.out.println("Request Body: " + reqBody);
        System.out.println("===================================================================");

//        TODO: GETTING READY RESPONSE WRITER - OUTPUT STREAM
        OutputStream writer = clientSocket.getOutputStream();
        String[] splittedRequest = request.split(" ");
        boolean isRequestTargetExist = splittedRequest[1].length() != 1;

        if(isRequestTargetExist){
          String[] splittedRequestTarget = splittedRequest[1].split("/");
          int n = splittedRequestTarget.length;

            switch (splittedRequestTarget[1]) {
                case "echo" -> {
                  int contentLength = splittedRequestTarget[n - 1].length();
                  String content = splittedRequestTarget[n - 1];
                  String responseMessage = "HTTP/1.1 200 OK\r\nContent-Type: " + TEXT_PLAIN_CONTENT_TYPE + "\r\nContent-Length: " + contentLength + "\r\n\r\n" + content;
                  if(acceptEncoding.equals("Accept-Encoding: gzip")) {
                    String encodingResponseMessage = "HTTP/1.1 200 OK\r\nContent-Type: " + TEXT_PLAIN_CONTENT_TYPE + "\r\nContent-Encoding: gzip" + "\r\nContent-Length: " + contentLength + "\r\n\r\n" + content;
                    writer.write(encodingResponseMessage.getBytes());
                  }else{
                    writer.write(responseMessage.getBytes());
                  }
                }

//          Handle User agent, Request headers
                case "user-agent" -> writer.write(workWithHeaders(header).getBytes());


//          TODO: HANDLE FILES
                case "files" -> {
                    String fileName = splittedRequestTarget[n - 1];
                    if (request.contains("POST")) {
//            TODO: POST REQUEST LOGIC
                        try {
                            File newFile = new File(args[1], fileName);
                            if (newFile.createNewFile()) {
                                System.out.println("File created: " + newFile.getName());
                                writeFileHandler(newFile, reqBody);
                                writer.write(STATUS_201_CREATED.getBytes());
                            } else {
//                  TODO: EDGE CASE WHAT IF FILE EXISTS, AND WE HAVE TO OVER WRITE THE FILE
                                System.out.println("File Already exists");
                                writeFileHandler(newFile, reqBody);
                                writer.write(STATUS_201_CREATED.getBytes());
                            }
                        } catch (IOException e) {
                            System.out.println("ERROR: working with post request files" + e.getMessage());
                        }
                    } else {
//            TODO: GET REQUEST LOGIC
                        File file = null;
                        if (args.length != 0) {
                            file = new File(args[1], fileName);
                        }
                        if (file != null) {
                            try {
//                  System.out.println(file.isFile());
                                if (!file.isFile()) {
                                    writer.write(NOT_FOUND_ERROR_STRING.getBytes());
                                }
                                BufferedReader file_reader = new BufferedReader(new FileReader(file));
                                String line;
                                StringBuilder file_content = new StringBuilder();
                                while ((line = file_reader.readLine()) != null) {
                                    file_content.append(line);
                                }
                                String responseMessage = "HTTP/1.1 " + STATUS_200_OK + "\r\n" + "Content-Type: " + OCTET_STREAM_CONTENT_TYPE + "\r\nContent-Length: " + file_content.length() + "\r\n\r\n" + file_content.toString();
                                writer.write(responseMessage.getBytes());
                            } catch (IOException e) {
                                System.out.println("Error file not found: " + e.getMessage());
                            }
                        } else {
                            writer.write(NOT_FOUND_ERROR_STRING.getBytes());
                        }
                    }
                }
                default -> writer.write(NOT_FOUND_ERROR_STRING.getBytes());
            }

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
