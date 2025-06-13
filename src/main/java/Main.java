import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPOutputStream;


//TEST REQUESTS
//curl --http1.1 -v http://localhost:4221/ --next http://localhost:4221/echo/raspberry -H "Connection: close"
//curl --http1.1 -v http://localhost:4221/user-agent -H "User-Agent: raspberry/raspberry-pineapple" --next http://localhost:4221/echo/blueberry
//curl --http1.1 -v http://localhost:4221/user-agent -H "User-Agent: raspberry/raspberry-pineapple" --next http://localhost:4221/echo/blueberry
//curl --http1.1 -v http://localhost:4221/user-agent -H "User-Agent: mango/orange" --next http://localhost:4221/
//curl -v http://localhost:4221/echo/raspberry -H "Accept-Encoding: gzip"
//curl -v http://localhost:4221/echo/raspberry -H "Accept-Encoding: encoding-1, gzip, encoding-2"
//curl -v http://localhost:4221/echo/raspberry -H "Accept-Encoding: encoding-1, encoding-2"
//curl -v http://localhost:4221/echo/pineapple -H "Accept-Encoding: gzip"
//curl -v http://localhost:4221/echo/pineapple -H "Accept-Encoding: invalid-encoding"
//curl -v -X POST http://localhost:4221/files/grape_mango_pear_mango -H "Content-Length: 55" -H "Content-Type: application/octet-stream" -d 'grape pear raspberry grape mango orange apple blueberry'
//curl -v http://localhost:4221/files/mango_apple_strawberry_strawberry
//curl -v http://localhost:4221/files/non-existentblueberry_blueberry_pear_raspberry
//curl -v http://localhost:4221/user-agent -H "User-Agent: apple/pineapple-banana"
//curl -v http://localhost:4221/echo/pineapple
//curl -v http://localhost:4221/orange


//Pool of routes and from them need to server the request, if it is there then server it or send 404 not found response


public class Main {

  private static final String TEXT_PLAIN_CONTENT_TYPE = "text/plain";
  private static final String NOT_FOUND_ERROR_STRING = "HTTP/1.1 404 Not Found\r\n\r\n";
  private static final String OCTET_STREAM_CONTENT_TYPE = "application/octet-stream";
  private static final String STATUS_200_OK = "200 OK";
  private static final String HTTP_1_1 = "HTTP/1.1";
  private static final String GET_STRING = "GET";
  private static final String POST_STRING = "POST";
  private static final String STATUS_201_CREATED = "HTTP/1.1 201 Created\r\n\r\n";
  private static final String USER_AGENT = "User-Agent";
  private static final String CONTENT_LENGTH = "Content-Length";
  private static final String CONTENT_TYPE = "Content-Type";
  private static final String ACCEPT_ENCODING = "Accept-Encoding";
  private static final String CONNECTION_CLOSE_STRING = "Connection: close";
  private static final ExecutorService executorService = Executors.newCachedThreadPool();

  public static void main(String[] args) {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.out.println("Logs from your program will appear here!");
     try {
       System.out.println("INFO: Starting the program");
       try(ServerSocket serverSocket = new ServerSocket(4221)){
       serverSocket.setReuseAddress(true);
           while(true){
               System.out.println("INFO: Accepting clients");
               Socket clientSocket = serverSocket.accept();
               clientSocket.setKeepAlive(true);
               System.out.println("INFO: Executing thread pool in order to run concurrent requests");
//         threadPool.execute(new ClientHandler(clientSocket, args));
               executorService.execute(new ClientHandler(clientSocket, args));

           }
       }
     } catch (IOException e) {
       System.out.println("IOException: " + e.getMessage());
     }
  }

//  To format a response we need content type, content length, and content
  public static String formatResponseString(String content, String status){
    return HTTP_1_1+ " " + status + "\r\nContent-Type: " + TEXT_PLAIN_CONTENT_TYPE + "\r\nContent-Length: " + content.trim().length() + "\r\n\r\n" + content.trim();
  }

  public static String workWithHeaders(String header){
    String[] splittedHeader = header.split(":");
    String content = splittedHeader[1].trim();
    return formatResponseString(content, STATUS_200_OK);
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
    private final String[] args;

    public ClientHandler(Socket clientSocket, String[] args){
      this.clientSocket = clientSocket;
      this.args = args;
    }

    @Override
    public void run() {
        while (!clientSocket.isClosed()) {
//      This will run for every thread pool
            try {
//      BufferedReader to provide efficient reading of characters, arrays, and lines.
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

//        TODO: GETTING REQUEST
//                TODO: ADDING COMPRESSION ALGORITHMS, GZIP, HUFFMAN ENCODING,
                String request = reader.readLine();

                if(request == null){
                    clientSocket.close();
                }

//        TODO: GETTING REQUEST HEADER
                String inputLine;
                String header = "";
                String cntLength = "";
                String cntType = "";
                String acceptEncoding = "";
                boolean isConnectionClose = false;
                while ((inputLine = reader.readLine()) != null && !inputLine.isEmpty()) {
//         Getting header
                    if (inputLine.contains(USER_AGENT)) {
                        header = inputLine;
                    }
                    if (inputLine.contains(CONTENT_LENGTH)) {
                        cntLength = inputLine;
                    }
                    if (inputLine.contains(CONTENT_TYPE)) {
                        cntType = inputLine;
                    }
                    if (inputLine.contains(ACCEPT_ENCODING)) {
                        acceptEncoding = inputLine;
                    }
                    if(inputLine.contains(CONNECTION_CLOSE_STRING)){
                        isConnectionClose = true;
                    }
                }

//        TODO: GETTING REQUEST BODY
                int reqBodyReader;
                int count = 0;
                if (cntLength.isEmpty()) {
                    System.out.println("Something went wrong");
                } else {
                    count = Integer.parseInt(cntLength.split(":")[1].trim());
                }
                StringBuilder reqBody = new StringBuilder();
                for (int i = 0; i < count; i++) {
                    reqBodyReader = reader.read();
                    if (reqBodyReader == -1) {
                        System.out.println("ERROR: No request Body found, Something went wrong");
                    }
                    reqBody.append((char) reqBodyReader);
                }


//        TODO: PRINTING NETWORK REQUEST INFORMATION
                System.out.println("Request Information");
                System.out.println("===================================================================");
                System.out.println("Request: " + request);
                System.out.println("Request Headers: "+header);
                System.out.println("Content Length: "+cntLength);
                System.out.println("Content Type: "+cntType);
                System.out.println(acceptEncoding);
                if(isConnectionClose){
                    System.out.println(CONNECTION_CLOSE_STRING);
                }
                System.out.println("Request Body: " + reqBody);
                System.out.println(clientSocket.getKeepAlive());
                System.out.println("===================================================================");

//        TODO: GETTING READY RESPONSE WRITER - OUTPUT STREAM
                OutputStream writer = clientSocket.getOutputStream();
                String[] splittedRequest = request.split(" ");
                boolean isRequestTargetExist = splittedRequest[1].length() != 1;

                if (isRequestTargetExist) {
                    String[] splittedRequestTarget = splittedRequest[1].split("/");
                    int n = splittedRequestTarget.length;

                    switch (splittedRequestTarget[1]) {
                        case "echo" -> {
                            int contentLength = splittedRequestTarget[n - 1].length();
                            String content = splittedRequestTarget[n - 1];
                            String responseMessage = "HTTP/1.1 " + STATUS_200_OK + "\r\nContent-Type: " + TEXT_PLAIN_CONTENT_TYPE + "\r\nContent-Length: " + contentLength + "\r\n\r\n" + content;
                            String connectionCloseResponseMessage = "HTTP/1.1 " + STATUS_200_OK + "\r\nContent-Type: " + TEXT_PLAIN_CONTENT_TYPE + "\r\nContent-Length: " + contentLength + "\r\n" + CONNECTION_CLOSE_STRING+ "\r\n\r\n" + content;
                            if (acceptEncoding.contains("gzip")) {
                                String[] first_part = acceptEncoding.split(":");
                                String[] second_part = first_part[1].split(",");
                                boolean isGzipEncoding = false;
                                for (String encodingAlgorithm : second_part) {
                                    if (encodingAlgorithm.trim().equals("gzip")) {
                                        isGzipEncoding = true;
//                              TODO: RECEIVED CONTENT COMPRESSED WITH GZIP
//                              TODO: COMPRESSION ALGORITHMS ALMOST ALWAYS HAVE SOME FORM OF SPACE OVERHEAD, WHICH MEANS THAT THEY ARE ONLY EFFECTIVE WHEN COMPRESSING DATA WHICH IS SUFFICIENTLY LARGE THAT THE OVERHEAD IS SMALLER THAN THE AMOUNT OF SAVED SPACE
//                              TODO: COMPRESSING A STRING WHICH IS ONLY 20 CHARACTERS LONG IS NOT TOO EASY, AND IT IS NOT ALWAYS POSSIBLE. IF YOU HAVE REPETITION. HUFFMAN CODING OR SIMPLE RUN-LENGTH ENCODING MIGHT BE ABLE TO COMPRESS BUT PROBABLY NOT BY VERY MUCH.
//                              TODO: CHECK IS CONTENT LENGTH IS NOT ZERO
                                        break;
                                    }
                                }
                                if (isGzipEncoding) {
                                    byte[] byteContent = content.getBytes();
                                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                    try (GZIPOutputStream out = new GZIPOutputStream(byteArrayOutputStream)) {
                                        out.write(byteContent);
                                        out.flush();
                                    }
                                    byte[] encodedData = byteArrayOutputStream.toByteArray();
                                    writer.write((HTTP_1_1 + " " + STATUS_200_OK).getBytes());
                                    writer.write(("\r\n").getBytes());
                                    writer.write(("Content-Encoding: gzip").getBytes());
                                    writer.write(("\r\n").getBytes());
                                    writer.write(("Content-Type: text/plain").getBytes());
                                    writer.write(("\r\n").getBytes());
                                    writer.write(("Content-Length: " + encodedData.length).getBytes());
                                    if(isConnectionClose){
                                        writer.write(("\r\n").getBytes());
                                        writer.write(CONNECTION_CLOSE_STRING.getBytes());
                                    }
                                    writer.write(("\r\n").getBytes());
                                    writer.write(("\r\n").getBytes());
                                    writer.write(encodedData);

                                    if(isConnectionClose){
                                        writer.close();
                                        byteArrayOutputStream.flush();
                                        clientSocket.close();
                                    }
                                }
                            } else {
                                if(isConnectionClose){
                                    System.out.println("I am here");
                                    writer.write(connectionCloseResponseMessage.getBytes());
                                    writer.close();
                                    clientSocket.close();
                                }else{
                                    writer.write(responseMessage.getBytes());
                                }
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
                                    } else {
//                  TODO: EDGE CASE WHAT IF FILE EXISTS, AND WE HAVE TO OVER WRITE THE FILE
                                        System.out.println("File Already exists");
                                    }
                                    writeFileHandler(newFile, reqBody);
                                    writer.write(STATUS_201_CREATED.getBytes());
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
                                        if (!file.isFile()) {
                                            writer.write(NOT_FOUND_ERROR_STRING.getBytes());
                                        }
                                        BufferedReader file_reader = new BufferedReader(new FileReader(file));
                                        String line;
                                        StringBuilder file_content = new StringBuilder();
                                        while ((line = file_reader.readLine()) != null) {
                                            file_content.append(line);
                                        }
                                        String responseMessage = HTTP_1_1 + " " + STATUS_200_OK + "\r\n" + "Content-Type: " + OCTET_STREAM_CONTENT_TYPE + "\r\nContent-Length: " + file_content.length() + "\r\n\r\n" + file_content.toString();
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

                } else {
                    if(isConnectionClose){
                        writer.write((HTTP_1_1 + " " + STATUS_200_OK + "\r\n" + CONNECTION_CLOSE_STRING).getBytes());
                        writer.write(("\r\n\r\n").getBytes());

                        writer.close();
                        clientSocket.close();
                    }else{
                        writer.write((HTTP_1_1 + " " + STATUS_200_OK).getBytes());
                        writer.write(("\r\n\r\n").getBytes());
                    }
                }
            } catch (IOException e) {
                System.err.println("Error handling client: " + e.getMessage());
            }
        }
    }
  }

}
