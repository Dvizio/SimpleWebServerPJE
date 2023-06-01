import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;


public class SimpleWebServerPJE extends Thread{
    private static final int DEFAULT_PORT = 8080;
    private static final String CONFIG_FILE_PATH = "config.properties";
    private static final String INDEX_FILE_NAME = "index.html";
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final int MAX_CONNECTIONS = 10;

    private int serverID;
    private int port;
    private String rootDirectory;
    private String hostName;

    public SimpleWebServerPJE(int serverID) {
        this.serverID = serverID + 1;
        newLoadConfiguration();
    }

    private void loadConfiguration() {
        try {
            Properties config = new Properties();
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE_PATH);
            if (inputStream != null) {
                config.load(inputStream);
                port = Integer.parseInt(config.getProperty("port", String.valueOf(DEFAULT_PORT)));
                rootDirectory = config.getProperty("rootDirectory", "");
                hostName = config.getProperty("hostName", "");
                System.out.println("This is hostName " + hostName);
                // System.out.println(rootDirectory);
            } else {
                System.out.println("Configuration file not found. Using default values.");
                port = DEFAULT_PORT;
                rootDirectory = "";
                hostName = "";
            }
        } catch (IOException e) {
            System.out.println("Error loading configuration file. Using default values.");
            port = DEFAULT_PORT;
            rootDirectory = "";
        }
    }

    private void newLoadConfiguration() {
        try {
            Properties config = new Properties();
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE_PATH);
            if (inputStream != null) {
                // Readies the config name
                String portConfigKey = "port" + Integer.toString(serverID);
                String rootDirectoryConfigKey = "rootDirectory" + Integer.toString(serverID);
                String hostNameConfigKey = "hostName" + Integer.toString(serverID);

                config.load(inputStream);
                // port = Integer.parseInt(config.getProperty("port", String.valueOf(DEFAULT_PORT)));
                port = Integer.parseInt(config.getProperty(portConfigKey, String.valueOf(DEFAULT_PORT)));
                // rootDirectory = config.getProperty("rootDirectory", "");
                rootDirectory = config.getProperty(rootDirectoryConfigKey, "");
                // hostName = config.getProperty("hostName", "");
                hostName = config.getProperty(hostNameConfigKey, "");
                System.out.println("This is hostName " + hostName);
                // System.out.println(rootDirectory);
            } else {
                System.out.println("Configuration file not found. Using default values.");
                port = DEFAULT_PORT;
                rootDirectory = "";
                hostName = "";
            }
        } catch (IOException e) {
            System.out.println("Error loading configuration file. Using default values.");
            port = DEFAULT_PORT;
            rootDirectory = "";
        }
    }

    public void run() {
        try {
            ServerSocket serverSocket;
            if(hostName.isEmpty()){
                serverSocket = new ServerSocket(port);
            }
            else{
                serverSocket = new ServerSocket(port, MAX_CONNECTIONS, Inet4Address.getByName(hostName));

            }            
            System.out.println("Server started on port " + serverSocket.getLocalPort() + "...");
            System.out.println("Server started on IPAddress " + serverSocket.getInetAddress() + "...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println(clientSocket);
                // System.out.println(clientSocket);
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            System.out.println("Error starting the server: " + e.getMessage());
        }
    }

    private class ClientHandler extends Thread {
        private final Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (InputStream inputStream = clientSocket.getInputStream();
                    OutputStream outputStream = clientSocket.getOutputStream()) {

                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String request = reader.readLine();
                if (request != null) {
                    String[] requestParts = request.split(" ");
                    if (requestParts.length >= 2) {
                        String method = requestParts[0];
                        String path = requestParts[1];

                        if (method.equalsIgnoreCase("GET")) {
                            handleGetRequest(outputStream, path);
                        } else {
                            sendResponse(outputStream, "HTTP/1.1 501 Not Implemented", "Not Implemented");
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("Error handling client request: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.out.println("Error closing client socket: " + e.getMessage());
                }
            }
        }

        private void handleGetRequest(OutputStream outputStream, String path) throws IOException {
            File file = new File(rootDirectory + path);
            System.out.println(file);
            if (file.exists()) {
                if (file.isDirectory()) {
                    File indexFile = new File(file, INDEX_FILE_NAME);
                    if (indexFile.exists()) {
                        try{
                            sendHTML(outputStream, indexFile);
                        }
                        catch(Exception e){
                            e.printStackTrace();
                        }
                    } else {
                        sendDirectoryListing(outputStream, file);
                    }
                } else {
                    if (file.toString().contains(".html")) {
                        // Buat liatin HTML
                        try {
                            sendHTML(outputStream, file);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    sendFile(outputStream, file);
                }
            } else {
                sendResponse(outputStream, "HTTP/1.1 404 Not Found", "File Not Found");
            }
        }

        private void sendHTML(OutputStream outputStream, File file) throws Exception {
            FileInputStream fileInputStream = null;
            try {
                fileInputStream = new FileInputStream(file);
                byte[] buffer = new byte[1024];
                int bytesRead;

                // Set appropriate content type based on file extension
                String contentType = "text/html";
                // Set appropriate response headers
                String responseHeaders = "HTTP/1.1 200 OK\r\n"
                        + "Content-Type: " + contentType + "\r\n"
                        + "Connection: close\r\n"
                        + "\r\n";

                // Write response headers to output stream
                outputStream.write(responseHeaders.getBytes());

                // Write file data to output stream
                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                outputStream.flush();
            } finally {
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e) {
                        // Ignore
                    }
                }
            }
        }

        private void sendFile(OutputStream outputStream, File file) throws IOException {
            String mimeType = Files.probeContentType(file.toPath());
            String fileName = file.getName();
            long fileSize = file.length();

            FileInputStream fileInputStream = null;
            try {
                fileInputStream = new FileInputStream(file);
                byte[] buffer = new byte[1024];
                int bytesRead;

                // Set appropriate content type based on file extension
                String contentType;
                if (mimeType != null) {
                    contentType = mimeType;
                } else {
                    contentType = "application/octet-stream";
                }

                // Set appropriate response headers
                String responseHeaders = "HTTP/1.1 200 OK\r\n"
                        + "Content-Type: " + contentType + "\r\n"
                        + "Content-Disposition: attachment; filename=\"" + fileName + "\"\r\n"
                        + "Content-Length: " + fileSize + "\r\n"
                        + "Connection: close\r\n"
                        + "\r\n";

                // Write response headers to output stream
                outputStream.write(responseHeaders.getBytes());

                // Write file data to output stream
                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                outputStream.flush();
            } finally {
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e) {
                        // Ignore
                    }
                }
            }
        }

    }

    private void sendDirectoryListing(OutputStream outputStream, File directory) throws IOException {
        File[] files = directory.listFiles();
        String dir = directory.getAbsolutePath();
        String dirCut = dir.replace(rootDirectory, "");
        String dirReplaceSlash = dirCut.replace("\\", "/");
        System.out.println(dirReplaceSlash);
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

        StringBuilder listingBuilder = new StringBuilder();
        listingBuilder.append("<html><head><title>Directory Listing</title></head><body>");
        listingBuilder.append("<h1>Directory Listing</h1>");
        listingBuilder.append("<ul>");

        for (File file : files) {
            String fileName = file.getName();
            String modifiedDate = dateFormat.format(new Date(file.lastModified()));
            String fileSize = String.valueOf(file.length());

            listingBuilder.append("<li><a href=\"").append(dirReplaceSlash).append("/").append(fileName).append("\">").append(fileName).append("</a> (")
                    .append("Size: ").append(fileSize).append(" bytes, ")
                    .append("Last Modified: ").append(modifiedDate).append(")</li>");
        }

        listingBuilder.append("</ul></body></html>");

        String listing = listingBuilder.toString();
        sendResponse(outputStream, "HTTP/1.1 200 OK", "OK", "text/html");
        outputStream.write(listing.getBytes());
    }

    private void sendResponse(OutputStream outputStream, String statusLine, String statusMessage) throws IOException {
        String response = statusLine + "\r\n"
                + "Connection: close\r\n"
                + "\r\n"
                + statusMessage;

        outputStream.write(response.getBytes());
    }

    private void sendResponse(OutputStream outputStream, String statusLine, String statusMessage, String contentType)
            throws IOException {
        String responseHeaders = "HTTP/1.1 " + statusLine + "\r\n"
                + "Content-Type: " + contentType + "\r\n"
                + "Connection: close\r\n"
                + "\r\n";

        String response = responseHeaders + statusMessage;
        outputStream.write(response.getBytes());
    }
}
