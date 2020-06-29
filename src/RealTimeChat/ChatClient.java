package RealTimeChat;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.*;
import java.net.Socket;

public class ChatClient {
    String username;  // placeholder username to check if user has set his own username
    JFrame mainFrame;
    JPanel centralPanel;
    JLabel messageLabel;
    JLabel portLabel;
    JLabel addressLabel;
    JLabel usernameLabel;
    JScrollPane chatScroller;
    JTextArea chatTextArea;
    JTextField messageField;
    JTextField addressField;
    JTextField portField;
    JTextField usernameField;
    JTextField topTextField;
    JButton sendButton;
    FlowLayout statusBarLayout;
    JPanel statusBarPanel;
    JTextField statusBarMessage;
    JTextField statusBarConnectionStatus;
    JButton setUsernameButton;
    JButton connectButton;
    JButton disconnectButton;

    Socket socket;

    boolean connectedToServer;
    int noOfShowingStatusMessages;

    private static final int NORMAL;
    private static final int WARNING;
    private static final int ERROR;
    private static final int GOOD;

    static {
        NORMAL = 0;
        WARNING = 1;
        ERROR = 2;
        GOOD = 3;
    }

    ChatClient() {
        this.connectedToServer = false;
        this.mainFrame = new JFrame("Chat Client");
        this.messageLabel = new JLabel("Message");
        this.portLabel = new JLabel("Port");
        this.addressLabel = new JLabel("Address");
        this.usernameLabel = new JLabel("Username ");

        this.statusBarMessage = new JTextField(30);
        this.statusBarMessage.setEditable(false);
        this.statusBarMessage.setBackground(Color.LIGHT_GRAY);
        this.statusBarMessage.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1, false));
        this.statusBarConnectionStatus = new JTextField(10);
        this.statusBarConnectionStatus.setEditable(false);
        this.statusBarConnectionStatus.setBackground(Color.RED);
        this.statusBarConnectionStatus.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1, false));
        this.statusBarConnectionStatus.setHorizontalAlignment(JTextField.CENTER);
        this.statusBarPanel = new JPanel();
        this.statusBarLayout = new FlowLayout(FlowLayout.CENTER);
        this.statusBarLayout.setVgap(0);
        this.statusBarLayout.setHgap(0);
        this.statusBarPanel.setLayout(this.statusBarLayout);
        this.statusBarPanel.add(this.statusBarMessage);
        this.statusBarPanel.add(this.statusBarConnectionStatus);

        this.chatTextArea = new JTextArea(22, 40);
        this.chatScroller = new JScrollPane(this.chatTextArea);
        this.chatScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        this.chatScroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        this.chatTextArea.setLineWrap(true);
        this.chatTextArea.setEditable(false);

        this.messageField = new JTextField(26);
        this.messageField.requestFocus();

        this.topTextField = new JTextField();
        this.topTextField.setEditable(false);
        this.topTextField.setHorizontalAlignment(JTextField.CENTER);

        this.addressField = new JTextField(16);
        this.portField = new JTextField(5);
        this.usernameField = new JTextField(26);
        this.sendButton = new JButton("Send");
        this.setUsernameButton = new JButton("Set");
        this.connectButton = new JButton("Connect");
        this.disconnectButton = new JButton("Disconnect");

        this.setUsernameButton.setEnabled(true);

        this.centralPanel = new JPanel();
        this.centralPanel.add(this.chatScroller);
        this.centralPanel.add(this.messageLabel);
        this.centralPanel.add(this.messageField);
        this.centralPanel.add(this.sendButton);
        this.centralPanel.add(this.usernameLabel);
        this.centralPanel.add(this.usernameField);
        this.centralPanel.add(this.setUsernameButton);
        this.centralPanel.add(this.addressLabel);
        this.centralPanel.add(this.addressField);
        this.centralPanel.add(this.portLabel);
        this.centralPanel.add(this.portField);
        this.centralPanel.add(this.connectButton);
        this.centralPanel.add(this.disconnectButton);

        // INTERFACE CONNECTION
        this.sendButton.addActionListener(new MessageListener());
        this.setUsernameButton.addActionListener(new SetButtonListener());
        this.connectButton.addActionListener(new ConnectButtonListener());
        this.usernameField.addKeyListener(new UsernameFieldListener());
        this.messageField.addKeyListener(new MessageFieldListener());
        this.addressField.addKeyListener(new AddressPortFieldListener());
        this.portField.addKeyListener(new AddressPortFieldListener());
        this.disconnectButton.addActionListener(new DisconnectButtonListener());

        // updateUI
        this.updateUI();

        this.mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.mainFrame.getContentPane().add(this.centralPanel, BorderLayout.CENTER);
        this.mainFrame.getContentPane().add(this.statusBarPanel, BorderLayout.SOUTH);
        this.mainFrame.getContentPane().add(this.topTextField, BorderLayout.NORTH);
        this.mainFrame.setSize(450 , 500);
        this.mainFrame.setResizable(false);
        this.mainFrame.setVisible(true);
    }
    public static void main(String[] args) {
        new ChatClient();
    }

    // TODO Fix back-end of client

    public void tryConnect(String ipAddress, int port) {
        try {
            this.socket = new Socket(ipAddress, port);
            this.connectedToServer = true;
            this.updateUI();
            new Thread(new ServerListener()).start();
        } catch (Exception e) {
            this.reportToStatusBar(e.getMessage(), ChatClient.ERROR);
        }
    }

    public void closeConnection() throws IOException {
        if (this.connectedToServer) {
            this.connectedToServer = false;
            this.socket.close();
            this.updateUI();
        }
    }

    public void updateUI() {
        if (this.connectedToServer) {
            // status bar
            this.statusBarConnectionStatus.setForeground(Color.BLACK);
            this.statusBarConnectionStatus.setBackground(Color.GREEN);
            this.statusBarConnectionStatus.setText("CONNECTED");

            // disconnect and connect
            this.disconnectButton.setVisible(true);
            this.connectButton.setVisible(false);
            this.addressField.setVisible(false);
            this.addressLabel.setVisible(false);
            this.portField.setVisible(false);
            this.portLabel.setVisible(false);

            // top bar
            this.setTopBarText("Connected to "
                    + this.socket.getInetAddress()
                    + "::"
                    + this.socket.getPort()
                    + " as "
                    + this.getUsername(), 0);
        } else {
            // status bar
            this.statusBarConnectionStatus.setBackground(Color.RED);
            this.statusBarConnectionStatus.setForeground(Color.WHITE);
            this.statusBarConnectionStatus.setText("DISCONNECTED");

            // disconnect and connect
            this.disconnectButton.setVisible(false);
            this.connectButton.setVisible(true);
            this.addressField.setVisible(true);
            this.addressLabel.setVisible(true);
            this.portField.setVisible(true);
            this.portLabel.setVisible(true);

            // top bar
            this.setTopBarText("Not Connected", 0);
        }
    }

    public void reportToStatusBar(String message, int verbosityLevel) {
            switch (verbosityLevel) {
                case 0:
                    this.setStatusText(message, Color.LIGHT_GRAY, Color.BLACK);
                    break;
                case 1:
                    this.setStatusText(message, Color.YELLOW, Color.BLACK);
                    break;
                case 2:
                    this.setStatusText(message, Color.RED, Color.WHITE);
                    break;
                case 3:
                    this.setStatusText(message, Color.GREEN, Color.BLACK);
                    break;
                default:
                    this.setStatusText("UNKNOWN VERBOSITY LEVEL", Color.LIGHT_GRAY, Color.BLACK);
                    break;
            }
    }

    public void setStatusText(String message, Color backgroundColor, Color foregroundColor) {
        new Thread(new ChangeStatusMessage(message, backgroundColor, foregroundColor)).start();
    }

    public void sendToServer(Packet packet) {
        try {
            synchronized (this.socket) {
                new ObjectOutputStream(this.socket.getOutputStream())
                        .writeObject(packet);
            }
        } catch (Exception e) {
            this.reportToStatusBar("Not connected to server", ChatClient.ERROR);
        }
    }

    public void setUsername(String username) {
        if (!username.equals(this.getUsername()) && username != null && !username.isBlank()) {
            this.username = username.strip();
            this.usernameField.setText(this.getUsername());
//            this.setUsernameButton.setEnabled(false);
            this.reportToStatusBar("Changed username to " + this.getUsername(), ChatClient.GOOD);
            if (this.connectedToServer) {
                this.sendToServer(new Packet(Packet.SET_USERNAME, this.getUsername()));
            }
        } else if (username.equals(this.getUsername())) {
            this.reportToStatusBar("Username is already set to " + this.getUsername(), ChatClient.WARNING);
        } else {
            this.reportToStatusBar("Invalid username", ChatClient.WARNING);
        }
    }

    public boolean usernameIsAlreadySet() {
        return this.getUsername().equals(this.usernameField.getText());
    }

    public void sendMessage() {
        if (!this.messageField.getText().isBlank()) {
            this.sendToServer(new Packet(Packet.MESSAGE, this.messageField.getText()));
            this.messageField.setText("");
        } else {
            this.reportToStatusBar("WARNING: Message is blank.", ChatClient.WARNING);
        }
    }

    public void setTopBarText(String s, int command) {
            this.topTextField.setText(s);
    }

    public String getUsername() {
        return this.username;
    }

    public void connect() {
        if (ChatClient.this.connectedToServer) {
            try {
                ChatClient.this.closeConnection();
            } catch (IOException e) {
                ChatClient.this.reportToStatusBar("Reconnecting to server.", ChatClient.ERROR);
            }
        }

        try {
            ChatClient.this.tryConnect(ChatClient.this.addressField.getText(),
                    Integer.parseInt(ChatClient.this.portField.getText()));
        } catch (Exception e) {
            ChatClient.this.reportToStatusBar("Invalid Address and/or Port",
                    ChatClient.ERROR);
//            e.printStackTrace();
        }
    }

    // runnable server handler
    class ServerListener implements Runnable {
        @Override
        public void run() {
            try {
                ChatClient.this.sendToServer(new Packet(Packet.SET_USERNAME
                        , ChatClient.this.getUsername()));

                while (ChatClient.this.connectedToServer) {
                    Packet packet = (Packet) new ObjectInputStream(socket.getInputStream())
                            .readObject();
                    switch (packet.getPacketType()) {
                        case 0:
                            break;
                        case 1:
                            ChatClient.this.chatTextArea.append(packet.getPacketContent() + "\n");
                            break;
                        case 2:
                            if (ChatClient.this.username == null
                                    || !ChatClient.this.username.equals(packet.getPacketContent())) {
                                ChatClient.this.setUsername(packet.getPacketContent());
                            }
                            ChatClient.this.updateUI();
                            break;
                        case 3:
                            ChatClient.this.closeConnection();
                            break;
                        default:
                            ChatClient.this.reportToStatusBar("Invalid packet received"
                                    , ChatClient.WARNING);
                            break;
                    }
                }

            } catch (ClassNotFoundException | IOException e) {
                ChatClient.this.reportToStatusBar(e.getMessage(), ChatClient.ERROR);
                e.printStackTrace();
            }

            // for closing
            try {
                ChatClient.this.closeConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // working nicely
    class ChangeStatusMessage implements Runnable {
        String message;
        Color backgroundColor;
        Color foregroundColor;
        public ChangeStatusMessage(String message, Color backgroundColor, Color foregroundColor) {
            this.message = message;
            this.backgroundColor = backgroundColor;
            this.foregroundColor = foregroundColor;
        }

        @Override
        public void run() {
            try {
                ChatClient.this.noOfShowingStatusMessages++;
                ChatClient.this.statusBarMessage.setText(this.message);
                ChatClient.this.statusBarMessage.setBackground(this.backgroundColor);
                ChatClient.this.statusBarMessage.setForeground(this.foregroundColor);
                Thread.sleep(3000);
                ChatClient.this.noOfShowingStatusMessages--;
                if (ChatClient.this.noOfShowingStatusMessages == 0) {
                    ChatClient.this.statusBarMessage.setText("");
                    ChatClient.this.statusBarMessage.setBackground(Color.LIGHT_GRAY);
                    ChatClient.this.statusBarMessage.setForeground(Color.BLACK);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    // LISTENERS
    class ConnectButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            ChatClient.this.connect();
        }
    }

    class DisconnectButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent actionEvent) {
            ChatClient.this.sendToServer(new Packet(Packet.DISCONNECT, ""));
        }
    }

    class SetButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            ChatClient.this.setUsername(ChatClient.this.usernameField.getText());
        }
    }

    class UsernameFieldListener implements KeyListener {
        @Override
        public void keyTyped(KeyEvent keyEvent) {
        }

        @Override
        public void keyPressed(KeyEvent keyEvent) {
            if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER) {
                ChatClient.this.setUsername(ChatClient.this.usernameField.getText());
            }
// TODO determine what conditions to disable a
//            if (ChatClient.this.usernameField.getText().equals("")
//                    || ChatClient.this.usernameField.getText().equals(ChatClient.this.getUsername())) {
//                ChatClient.this.setUsernameButton.setEnabled(false);
//            } else ChatClient.this.setUsernameButton.setEnabled(true);
        }

        @Override
        public void keyReleased(KeyEvent keyEvent) {

        }
    }

    class MessageListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            ChatClient.this.sendMessage();
        }
    }

    class MessageFieldListener implements KeyListener {
        @Override
        public void keyTyped(KeyEvent keyEvent) {

        }

        @Override
        public void keyPressed(KeyEvent keyEvent) {
            if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER) {
                ChatClient.this.sendMessage();
            }
        }

        @Override
        public void keyReleased(KeyEvent keyEvent) {

        }
    }

    class AddressPortFieldListener implements KeyListener {
        @Override
        public void keyTyped(KeyEvent keyEvent) {

        }

        @Override
        public void keyPressed(KeyEvent keyEvent) {
            if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER) {
                ChatClient.this.connect();
            }
        }

        @Override
        public void keyReleased(KeyEvent keyEvent) {

        }
    }
}
