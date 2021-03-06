package server;

import java.io.*;
import java.net.Socket;

/**
 * ClientConnection class is responsible for handling individual connections and
 * sending and receiving data from client to server and vice versa. Implements
 * the runnable interface so that each instance can be executed as a separate
 * thread.
 */
public class ClientConnection implements Runnable {

    private final Socket client;
    private PrintWriter outStream;
    private BufferedReader inStream;

    /**
     * Constructor. Takes the connected client socket as a parameter.
     *
     * @param client socket that is connected with the client
     */
    ClientConnection(Socket client) {
        this.client = client;
    }

    /**
     * Default method implemented by the Runnable interface. It gets called when a
     * new instance of ClientConnection is created. Starts clientHandler() function
     */
    public void run() {
        clientHandler();
    }

    /**
     * Responsible for sending and receiving data from a single client. Loops
     * indefinitely until client sends a disconnect request. Calls appropriate
     * functions for other client commands.
     */
    private void clientHandler() {

        boolean run = true;
        // Initialize the current directory file to the program's directory
        File currentDirectory = new File(System.getProperty("user.dir"));
        String rawInput = ""; // Full client request
        String parsedCommand = ""; // Command portion of client request


        try {
            // Create server input and output streams
            outStream = new PrintWriter(client.getOutputStream(), true);
            inStream = new BufferedReader(new InputStreamReader(client.getInputStream()));
        } catch (IOException ex) {
            System.err.println("Error Creating Input and Output Streams");
            ex.printStackTrace();
        }

        System.out.println(Thread.currentThread().getName() +
                ": Client connection from " + client.getInetAddress());

        // Send client a message that they have successfully connected
        outStream.println("HELLO");

        // Loop until client chooses to exit
        while (run) {
            try {
                rawInput = inStream.readLine();

                // Get the command token (first word) from user input
                if (rawInput.split("\\s+").length >= 2) {
                    parsedCommand = rawInput.split("\\s+")[0];
                } else {
                    parsedCommand = rawInput;
                }

            } catch (IOException ex) {
                System.err.println("Error Reading From Input Stream. Closing " +
                        "connection");
                ex.printStackTrace();
                break;
            }

            switch (parsedCommand.toUpperCase()) {
                case "BYE":
                    System.out.println(Thread.currentThread().getName() +
                            ": BYE Received - Closing connection from " + client.getInetAddress());

                    run = false; // break out of loop
                    break;
                case "PWD":
                    System.out.println(Thread.currentThread().getName() +
                            ": PWD Received");
                    outStream.println(currentDirectory.getPath()); // send file's current path
                    break;
                case "DIR":
                    System.out.println(Thread.currentThread().getName() +
                            ": DIR Received");
                    outStream.println(getDirectory(currentDirectory.getPath()));
                    break;
                case "CD":
                    System.out.println(Thread.currentThread().getName() +
                            ": CD Received");

                    // Attempt to change directory and store output in String
                    String output = changeDirectory(rawInput, currentDirectory);

                    // There was an error navigating to new directory
                    if (!output.equals("DDNE") && !output.equals("PD")) {
                        currentDirectory = new File(output);
                    }

                    // Send the function response string regardless, the client will handle
                    // displaying the error codes to the user
                    outStream.println(output);
                    break;
                case "DOWNLOAD":
                    System.out.println(Thread.currentThread().getName() +
                            ": DOWNLOAD Received");
                    sendFile(rawInput, currentDirectory, outStream, inStream);
                    break;
                default:
                    outStream.println("Client Request Error.");
                    System.out.println(Thread.currentThread().getName() +
                            ": Client sent invalid command");
            }
        }

        // Close the client connection when finished.
        try {
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Display the contents of the clients current working directory.
     * Separates the results by file and folder. Users can download files and enter
     * folders.
     *
     * @param directory absolute path String to a file directory
     * @return String delimited by "#" character between tokens. Each
     * file/folder has 3 different attributes.
     * Type: "File: or "Folder"
     * Size: File or Folder Size in Bytes
     * Name: File or Folder Name
     * The client will be responsible for parsing the tokens and
     * formatting the output.
     */
    private String getDirectory(String directory) {
        File folder = new File(directory);
        File[] directoryListing = folder.listFiles();
        StringBuilder output = new StringBuilder();

    /* Build a new string with files at the top and directories at the
       bottom. Delimit each entry with a "#" character. The client will
       parse the tokens from the long string that the server sends. Each
       directory item has a total of 3 different details about it.
    */
        if (directoryListing != null) {
            for (File file : directoryListing) {
                if (file.isFile()) {
                    // Put all the files before the folder listings
                    output.insert(0, "File#" +
                            +file.length() +
                            "#" + file.getName() + "#");
                } else if (file.isDirectory()) {
                    output.append("Folder");
                    output.append("#");
                    output.append("-");
                    output.append("#");
                    output.append(file.getName());
                    output.append("#");
                }
            }

            // If length is 0, the folder was empty
            if (output.length() == 0) {
                output.append("EMPTY");
            }
        }

        return output.toString();
    }

    /**
     * Reads the client request and parses the new directory path given. Makes sure
     * that the given path exists, is a directory, and the server has read permissions
     * Return the path if success, otherwise return an error code.
     *
     * @param rawInput         the entire request string from the client
     * @param currentDirectory the server's current working directory for the client
     * @return Returns the new file's absolute path on success.
     * Returns "PD" if user does not have read permissions
     * Returns "DDNE" if the directory doesn't exist
     */
    private String changeDirectory(String rawInput,
                                   File currentDirectory) {
        File newFilePath;

        //Create a new string excluding the "CD" part of the array
        String targetDir = rawInput.substring(rawInput.indexOf(" ")).trim();


        //Go up a level to parent directory
        if (targetDir.equals("..")) {
            newFilePath = new File(currentDirectory.getParent());
            // If input contains a "/", user entered a full file path
        } else if (targetDir.contains(File.separator)) {
            newFilePath = new File(targetDir);
        } else { // Input is a relative path
            newFilePath = new File(currentDirectory, targetDir);
        }

        // Check if the input file/directory exists, is a directory, and has read permissions
        if (newFilePath.exists() && newFilePath.isDirectory() && newFilePath.canRead()) {
            return newFilePath.getAbsolutePath(); // Success, return new
        } else if (!newFilePath.exists() || !newFilePath.isDirectory()) {
            return "DDNE"; // Directory does not exist
        } else if (!newFilePath.canRead()) {
            return "PD"; // Permission denied
        }
        // Return directory does not exists generic error code otherwise
        return "DDNE";
    }

    /**
     * Send a file to the connected client. Client must send the filename. Server
     * checks if the filename exists, sends READY, client confirms the download,
     * sends READY, server sends the file size, client receives the file size.
     * Server starts sending the file in 1mb chunks. Client receives data.
     *
     * @param rawInput  - The entire input string received from the Client
     * @param directory - The client's current working director
     * @param outStream - Data output stream to the client
     * @param inStream  - Data input stream from the client
     */
    private void sendFile(String rawInput, File directory,
                          PrintWriter outStream, BufferedReader inStream) {

        // Create a new string excluding the command
        String fileName = rawInput.substring(rawInput.indexOf(" ")).trim();

        BufferedInputStream fileReader = null;
        OutputStream bytesOut;
        int bytesSent;
        byte[] buffer = new byte[1000000]; // Can transfer 1mb at a time

        // Set file to the filename the user gives in their request
        File file = new File(directory, fileName);

    /* Make sure that given file exists, is a file not a directory, and the
       server has read permissions for the file */
        if (file.isFile() && file.exists() && file.canRead()) {
            try {
                // Create a new stream to read from source file
                fileReader = new BufferedInputStream(new FileInputStream(file));

            } catch (FileNotFoundException e) {
                System.err.println("Could not create file reader stream");
                e.printStackTrace();
            }

            // Tell the client that the server is ready to send the file
            outStream.println("READY");

            try {
                // Get client response after sending "READY"
                if (inStream.readLine().equals("READY")) {
                    bytesOut = client.getOutputStream();

                    // Send the file length before sending the file
                    outStream.println(file.length());

                    // Make sure fileReader stream has been initialized before reading
                    if (fileReader != null) {
                        // Keep reading from file and sending to client until all data is sent or error
                        while ((bytesSent = fileReader.read(buffer, 0, buffer.length)) != -1) {
                            bytesOut.write(buffer, 0, bytesSent);
                        }

                        System.out.println(Thread.currentThread().getName() + ": "
                                + file.getName() + " sent to client");
                        fileReader.close(); // Close the file input stream
                        bytesOut.flush();   // Flush the data output stream
                    }

                } else {
                    System.out.println("\tClient has aborted the download");
                }
            } catch (IOException e) {
                System.err.println("Download command could not get client response.");
                e.printStackTrace();
            }
        } else {
            outStream.println("FNF");
        }
    }
}

