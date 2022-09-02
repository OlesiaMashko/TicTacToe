/* 
 Assignment 04
 Course Codes/Lab: Database Programming using Java - ITC-5201-RNB
 Group 5
 Groupmember 1: Parth Antala - n01452392
 Groupmember 2: Olesia Mashkovtseva - n01454607
*/

package tictactoe;

import java.awt.BorderLayout;
import java.awt.Font;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.LinkedList;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class Server extends JFrame{

	//an array to hold all players connection
    public static LinkedList<HandleASession> playerList = new LinkedList<>();
    
    //create a two dimensional array to keep all players moves
    //-1 - empty
    //1 - cross
    //0 - zero
    public static int[][] gameHistory = {{-1, -1, -1}, {-1, -1, -1}, {-1, -1, -1}};

    //declare the players turn in the game
    public int playerTurn;
    
	//initialize the first played whose move is set to be always the first one
    // 1 - first player
    // 2 - second player
    public int playerFirstMove = 1;

    // Text area for displaying contents
    private JTextArea jta = new JTextArea();

    
    //construct 
    public Server() {
    	
    	// Place text area on the frame
        setLayout(new BorderLayout());
        jta.setFont(new Font("Dialog", Font.PLAIN, 18));
        add(new JScrollPane(jta), BorderLayout.CENTER);

        setTitle("Server");
        setSize(600, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    	
        try {
            // Create a server socket within the port 1234
            ServerSocket serverSocket = new ServerSocket(1234);
            //display the server work status
            jta.append("Server started at " + new Date() + '\n');
            
            playerTurn = 0;
            int playerNo = 1;
            
            while (true) {
                // Listen for a new connection request
                Socket socket = serverSocket.accept();

                // Sets the maximum number of players
                if (playerNo == 3) {
                	jta.append("Maximum players!");
                    continue;
                }

                // Display the new player info 
                jta.append("Starting thread for player " + playerNo +
                        " at " + new Date() + '\n');
                jta.append("Player " + playerNo + " has jooined \n");

                // Create a new thread for the connection for each player, define a thread
                HandleASession task = new HandleASession(socket, playerNo); 
                //adding connection to the player list
                playerList.add(task);
                // Start the new thread
                new Thread(task).start();
                // Increment player number
                playerNo++;
            }
        } catch (IOException ex) {
            System.err.println(ex); // it prints the error in red, it just differentiate the errors from the printed messages
        }
    }

    // Inner class defines the thread class for handling new connection
    class HandleASession implements Runnable {
        private Socket socket; // A connected socket
        public DataInputStream inputFromClient;
        public DataOutputStream outputToClient;
        public int playerNo;
        public boolean isNewUser = true;

        /**
         * Construct a thread
         */
        public HandleASession(Socket socket, int playerNo) {
            this.socket = socket;
            this.playerNo = playerNo;
            try {
                // Create data input and output streams
                inputFromClient = new DataInputStream(
                        this.socket.getInputStream());
                outputToClient = new DataOutputStream(
                        this.socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        /** Run a thread */
        public void run() {
            try {
                // Continuously serve the player
                while (true) {
                    if (isNewUser) {
                        isNewUser = false;
                        //sent player number
                        inputFromClient.readUTF();
                        outputToClient.writeInt(playerNo);
                        //sent the players turn of the move
                        inputFromClient.readUTF();
                        outputToClient.writeInt(playerFirstMove);
                        continue;
                    }

                    //the player's response consists of a column and a row
                    String playerResponse = inputFromClient.readUTF();
                    int column = Integer.parseInt(String.valueOf(playerResponse.charAt(0)));
                    int row = Integer.parseInt(String.valueOf(playerResponse.charAt(1)));

                    //sets the turn to move
                    gameHistory[column][row] = playerTurn;
                    
                    String status = checkStatus();

                    //checks the status of the game
                    switch (status) {
                    	//if the status is continue we switch the turn to another player and keep playing 
                        case Status.CONTINUE: {
                        	jta.append("\nGame status: " + "CONTINUE");
                            changeQueue();
                            playerList.get(playerTurn).outputToClient.writeUTF(playerResponse);
                            break;
                        }
                        case Status.WIN: {
                        	//if the status is WIN the message with a winner will appear and send it to both players and server
                            String msg;
                            msg = "PLAYER" + (playerTurn + 1) + "_WON";
                            for (HandleASession player : playerList) {
                                player.outputToClient.writeUTF(msg);
                            }
                            jta.append("\nGame status: " + "PLAYER" + (playerTurn + 1) + "_WON");
                            changeQueue();
                            playerList.get(playerTurn).outputToClient.writeUTF(playerResponse);
                            break;
                        }
                        case Status.DRAW: {
                        	//if status is DRAW the game is over
                            String msg;
                            msg = "GAME_OVER";
                            for (HandleASession player : playerList) {
                                player.outputToClient.writeUTF(msg);
                            }
                            jta.append("\nGame status: " + "DRAW");
                            changeQueue();
                            playerList.get(playerTurn).outputToClient.writeUTF(playerResponse);
                            break;
                        }
                    }

                    jta.append("\nPlayer turn: " + playerTurn);
                    jta.append("\nPlayer move: " + playerResponse);

                }
            } catch (IOException e) {
                System.err.println(e);
            }
        }

        //method changes the turn between players 
        private void changeQueue() {
            if (playerTurn == 1) {
                playerTurn = 0;
            } else {
                playerTurn = 1;
            }
        }

        //method checks the status of the game
        private String checkStatus() {
            Boolean draw = true;

            //checks whether all cells are filled and if they have the same index then the status will change to WIN
            for (int i = 0; i < gameHistory.length; i++) {
            	//checks horizontally 
                if (gameHistory[i][0] != -1 || gameHistory[i][1] != -1 || gameHistory[i][2] != -1) {
                    if (gameHistory[i][0] == gameHistory[i][1] && gameHistory[i][1] == gameHistory[i][2]) {
                        return Status.WIN;
                    }
                }
                //checks vertically 
                if (gameHistory[0][i] != -1 || gameHistory[1][i] != -1 || gameHistory[2][i] != -1) {
                    if (gameHistory[0][i] == gameHistory[1][i] && gameHistory[1][i] == gameHistory[2][i]) {
                        return Status.WIN;
                    }
                }
                //looks for any empty cells if there is non of them and still no winner its a draw
                for (int j = 0; j < gameHistory[i].length; j++) {
                    if (gameHistory[i][j] == -1) {
                        draw = false;
                    }
                }
            }
            if (draw) {
                return Status.DRAW;
            }
            
            
            int size = gameHistory.length - 1;
            boolean firstDiagonalWin = true;
            boolean secondDiagonalWin = true;
            //checks diagonal cells and if they have the same index then the status will change to WIN
            for (int i = 0; i < gameHistory.length - 1; i++) {
                if (gameHistory[i][i] == -1 || gameHistory[i + 1][i + 1] == -1) {
                    firstDiagonalWin = false;
                } else {
                    if (gameHistory[i][i] != gameHistory[i + 1][i + 1]) {
                        firstDiagonalWin = false;
                    }
                }
                if (gameHistory[size - i][size - i] == -1 || gameHistory[size - i - 1][size - i - 1] == -1) {
                    secondDiagonalWin = false;
                } else {
                    if (gameHistory[i][size - i] != gameHistory[i + 1][size - i - 1]) {
                        secondDiagonalWin = false;
                    }
                }
            }
            if (firstDiagonalWin || secondDiagonalWin) { 
                return Status.WIN;
            }
            return Status.CONTINUE;
        }

    }

    public static void main(String[] args) { //main method, calls Server
        new Server();
    }

}
