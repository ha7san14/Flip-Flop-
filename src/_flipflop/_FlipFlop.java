package _flipflop;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.*;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class _FlipFlop extends JFrame implements ActionListener {
    // Constants for the game
    private static final int NUM_PAIRS = 8; // Number of matching pairs
    private static final int BOARD_SIZE = NUM_PAIRS * 2; // Total number of cards
    
    // Game components
    private JButton[] tiles; // Buttons representing the tiles
    private int[] tileValues; // Values of the tiles (matching pairs)
    private int currentPlayer; // Current player
    private int score; // Current score of the player
    private boolean isClickable; // Indicates if the tiles can be clicked
    private JLabel scoreLabel; // Label to display the score
    private JLabel timerLabel; // Label to display the elapsed time
    private Timer timer; // Timer to track the elapsed time
    private int elapsedSeconds; // Elapsed time in seconds
    
    
    // Player information and database-related variables
    private String playerName; // Name of the player
    private String password; // Password of the player
    private String highestScoreFilePath = "highest_score.txt"; //File path to store highscores
    private String scoreFilePath = "scores.txt"; // File path to store scores
    private String connectionString = "jdbc:ucanaccess:////D:\\PUCIT PDF'S\\_FlipFlop\\db\\Users.accdb";
    private Connection connection; // Database connection
    private Statement statement; // Statement to execute SQL queries
    
    // Game logic variables
    private int flippedIndex; // Index of the first flipped tile
    private int matchedPairs; // Number of matched pairs
    
    // Thread for timekeeping
    private Thread timerThread;

    // Constructor
    public _FlipFlop() {
        // Initialize game state
        this.currentPlayer = 1;
        this.score = 0;
        this.isClickable = true;
        this.elapsedSeconds = 0;
        this.flippedIndex = -1;
        this.matchedPairs = 0;
        
        // Step 1: Import required packages

        // Step 2: Load driver
        try {
            Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to load database driver.");
            System.exit(1);
        }

        // Step 3: Define Connection URL

        // Step 4: Establish Connection
        try {
            connection = DriverManager.getConnection(connectionString);
            statement = connection.createStatement();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to establish database connection.");
            System.exit(1);
        }
        
        // Ask for the player's name
        askPlayerNameAndPassword();
        
        // Set up the GUI
        setTitle("Flip-Flop Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel boardPanel = new JPanel(new GridLayout(4, 4)); // 4x4 grid for 8 cards/tiles
        tiles = new JButton[BOARD_SIZE];
        tileValues = new int[BOARD_SIZE];

        // Initialize the tiles and assign values to them
        for (int i = 0; i < BOARD_SIZE; i++) {
            tiles[i] = new JButton();
            tiles[i].addActionListener(this);
            boardPanel.add(tiles[i]);
            tileValues[i] = (i / 2) + 1;
        }

        add(boardPanel, BorderLayout.CENTER);

        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        scoreLabel = new JLabel("Score: " + score);
        infoPanel.add(scoreLabel);

        timerLabel = new JLabel("Time: 0 seconds");
        infoPanel.add(timerLabel);

        add(infoPanel, BorderLayout.SOUTH);

        pack();
        setVisible(true);

        startTimerThread(); 
        shuffleAndDisplayTiles(); 
    }

private void askPlayerNameAndPassword() {
    JPanel panel = new JPanel();
    panel.setLayout(new GridLayout(3, 2));

    JLabel nameLabel = new JLabel("Username:");
    JTextField nameField = new JTextField();
    panel.add(nameLabel);
    panel.add(nameField);

    JLabel passwordLabel = new JLabel("Password:");
    JPasswordField passwordField = new JPasswordField();
    panel.add(passwordLabel);
    panel.add(passwordField);

    String[] options = {"Login", "Signup"};
    int choice = JOptionPane.showOptionDialog(
            this,
            panel,
            "Flip-Flop Game",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null,
            options,
            options[0]);

    if (choice == 0) {
        // Login
        playerName = nameField.getText();
        password = new String(passwordField.getPassword());

        // Check if the login information is valid
        if (playerName.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a username and password.");
            askPlayerNameAndPassword();
            return;
        }

        boolean loginValid = checkLogin(playerName, password);
        if (!loginValid) {
            JOptionPane.showMessageDialog(this, "Invalid login information. Please try again.");
            askPlayerNameAndPassword();
            return;
        }
    } else if (choice == 1) {
        // Signup
        playerName = nameField.getText();
        password = new String(passwordField.getPassword());

        // Check if the signup information is valid
        if (playerName.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a username and password.");
            askPlayerNameAndPassword();
            return;
        }

        boolean signupValid = checkSignup(playerName);
        if (!signupValid) {
            JOptionPane.showMessageDialog(this, "Username already exists. Please choose a different username.");
            askPlayerNameAndPassword();
            return;
        }

        // Insert the new user into the database
        insertUser(playerName, password);
    } else {
        // Cancel or close dialog
        System.exit(0);
    }
}




// Check if the login information is valid
private boolean checkLogin(String username, String password) {
    try {
        String selectQuery = "SELECT * FROM Users WHERE Username = ? AND Password = ?";
        PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
        selectStatement.setString(1, username);
        selectStatement.setString(2, password);
        ResultSet resultSet = selectStatement.executeQuery();
        return resultSet.next();
    } catch (SQLException e) {
        e.printStackTrace();
        return false;
    }
}

// Check if the username already exists
private boolean checkSignup(String username) {
    try {
        String selectQuery = "SELECT * FROM Users WHERE Username = ?";
        PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
        selectStatement.setString(1, username);
        ResultSet resultSet = selectStatement.executeQuery();
        return !resultSet.next();
    } catch (SQLException e) {
        e.printStackTrace();
        return false;
    }
}

// Insert a new user into the database
private void insertUser(String username, String password) {
    try {
        String insertQuery = "INSERT INTO Users (Username, Password) VALUES (?, ?)";
        PreparedStatement insertStatement = connection.prepareStatement(insertQuery);
        insertStatement.setString(1, username);
        insertStatement.setString(2, password);
        insertStatement.executeUpdate();
    } catch (SQLException e) {
        e.printStackTrace();
    }
}


    // Start the timer thread
    private void startTimerThread() {
        timerThread = new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(1000);
                    elapsedSeconds++;
                    SwingUtilities.invokeLater(() -> {
                        timerLabel.setText("Time: " + elapsedSeconds + " seconds");
                    });
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        timerThread.start();
    }

    // Shuffle and display the tiles
    private void shuffleAndDisplayTiles() {
        // Shuffle the tile values
        for (int i = 0; i < BOARD_SIZE; i++) {
            int randomIndex = (int) (Math.random() * BOARD_SIZE);
            int tempValue = tileValues[i];
            tileValues[i] = tileValues[randomIndex];
            tileValues[randomIndex] = tempValue;
        }

        // Display the shuffled tile values
        for (int i = 0; i < BOARD_SIZE; i++) {
            tiles[i].setText(String.valueOf(tileValues[i]));
        }

        // Use a SwingWorker to introduce a delay before hiding the tiles
        SwingWorker<Void, Void> delayWorker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    Thread.sleep(3000); // Delay for 3 seconds
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                // Hide the tiles after the delay
                for (int i = 0; i < BOARD_SIZE; i++) {
                    tiles[i].setText("");
                }
                isClickable = true;
            }
        };
        delayWorker.execute();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isClickable) {
            return;
        }

        JButton source = (JButton) e.getSource();
        int index = -1;

        // Find the index of the clicked tile
        for (int i = 0; i < BOARD_SIZE; i++) {
            if (tiles[i] == source) {
                index = i;
                break;
            }
        }

        if (index != -1 && tiles[index].getText().isEmpty()) {
            flipTile(index);
            checkMatch(index);
        }
    }

    // Flip the tile at the given index
    private void flipTile(int index) {
        if (tiles[index].getText().isEmpty()) {
            int value = tileValues[index];
            tiles[index].setText(String.valueOf(value));
            tiles[index].setEnabled(false);
        } else {
            tiles[index].setText("");
            tiles[index].setEnabled(true);
        }
    }

    // Check if the flipped tile matches the previously flipped tile
    private void checkMatch(int index) {
        if (flippedIndex == -1) {
            flippedIndex = index;
        } else {
            int flippedValue = tileValues[flippedIndex];
            if (tileValues[index] == flippedValue) {
                // Matched
                tiles[index].setEnabled(false);
                tiles[flippedIndex].setEnabled(false);
                score++;
                scoreLabel.setText("Score: " + score);
                matchedPairs++;

                if (matchedPairs == NUM_PAIRS) {
                    updateUser();
                    showDialogAndExit();
                }
            } else {
                // Not matched
                isClickable = false;
                final int finalFlippedIndex = flippedIndex;
                final int finalIndex = index;
                SwingWorker<Void, Void> delayWorker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() {
                        try {
                            Thread.sleep(1000); // Delay for 1 second
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    @Override
                    protected void done() {
                        flipTile(finalFlippedIndex);
                        flipTile(finalIndex);
                        isClickable = true;
                    }
                };
                delayWorker.execute();
            }

            flippedIndex = -1; // Reset the flipped index
        }
    }

    // Show the game over dialog and ask if the player wants to play again
    private void showDialogAndExit() {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Do you want to play again?",
                "Game Over",
                JOptionPane.YES_NO_OPTION
        );

        if (choice == JOptionPane.YES_OPTION) {
            writeScoresToFile();
            resetGame();
        } else {
            writeScoresToFile();
            exitGame();
        }
    }

    // Write the player's score to a file
 private void writeScoresToFile() {
    try {
        // Check if the file exists, create it if necessary
        File file = new File(scoreFilePath);
        if (!file.exists()) {
            file.createNewFile();
        }

        FileWriter writer = new FileWriter(file, true);
        String timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String scoreData = playerName + "," + score + "," + elapsedSeconds + "," + timeStamp + "\n";
        writer.write(scoreData);
        writer.close();

        // Read the existing scores from the file and find the highest score
        int highestScore = Integer.MIN_VALUE;
        int leastSeconds = Integer.MAX_VALUE;

        BufferedReader reader = new BufferedReader(new FileReader(scoreFilePath));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] scoreInfo = line.split(",");
            int currentScore = Integer.parseInt(scoreInfo[1]);
            int currentSeconds = Integer.parseInt(scoreInfo[2]);

            if (currentScore > highestScore || (currentScore == highestScore && currentSeconds < leastSeconds)) {
                highestScore = currentScore;
                leastSeconds = currentSeconds;
            }
        }
        reader.close();

        // File to store the highest score
        File highestScoreFile = new File("highest_score.txt");
        if (!highestScoreFile.exists()) {
            highestScoreFile.createNewFile();
        }

        FileWriter highestScoreWriter = new FileWriter(highestScoreFile, false);
        String highestScoreData = playerName + "," + highestScore + "," + leastSeconds + "," + timeStamp + "\n";
        highestScoreWriter.write(highestScoreData);
        highestScoreWriter.close();
    } catch (IOException e) {
        e.printStackTrace();
    }
}

    // Reset the game to its initial state
    private void resetGame() {
        // Reset game state
        currentPlayer = 1;
        score = 0;
        isClickable = true;
        elapsedSeconds = 0;
        flippedIndex = -1;
        matchedPairs = 0;

        // Clear the tiles
        for (int i = 0; i < BOARD_SIZE; i++) {
            tiles[i].setText("");
            tiles[i].setEnabled(true);
        }

        scoreLabel.setText("Score: " + score);
        timerLabel.setText("Time: 0 seconds");

        askPlayerNameAndPassword();
        shuffleAndDisplayTiles();
    }

   // Update the users in the database
    private void updateUser() {
    try {
        // Check if the player's username already exists
        String selectQuery = "SELECT * FROM Users WHERE Username = ?";
        PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
        selectStatement.setString(1, playerName);
        ResultSet resultSet = selectStatement.executeQuery();

        if (resultSet.next()) {
            // If the username exists, do nothing
        } else {
            // If the username doesn't exist, insert a new row
            String insertQuery = "INSERT INTO Users (Username, Password) VALUES (?, ?)";
            PreparedStatement insertStatement = connection.prepareStatement(insertQuery);
            insertStatement.setString(1, playerName);
            insertStatement.setString(2, password);
            insertStatement.executeUpdate();
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
}

    // Exit the game and close the database connection
    private void exitGame() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    // Main method to start the game
    public static void main(String[] args) {
        // Execute the application on the event dispatch thread
        SwingUtilities.invokeLater(_FlipFlop::new);
    }
}
