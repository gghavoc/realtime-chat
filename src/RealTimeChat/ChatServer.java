package RealTimeChat;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;

// <send_message>
// <send_message>
// <set_username>
// <get_username>

public class ChatServer {
    JFrame mainFrame;
    JPanel northPanel;
    JPanel statusPanel;
    JTextField portField;
    JTextField statusMessageField;
    JTextField connectedUsersField;
    JTextField serverStatusField;
    JLabel portLabel;
    JLabel connectedUsersLabel;
    JButton stopButton;
    JButton runButton;
    JTextArea messageArea;
    JScrollPane messageScrollPane;
    FlowLayout statusLayout;

    ServerSocket serverSocket;
    HashMap<Socket, User> socketToUserMap;
    boolean serverIsRunning;

    ChatServer() {
//        assert false : "Assert Message";

        this.serverIsRunning = false;

        this.socketToUserMap = new HashMap<Socket, User>();

        this.mainFrame = new JFrame("Chat Server");
        this.northPanel = new JPanel();
        this.messageArea = new JTextArea();
        this.messageArea.setEditable(false);
        this.messageArea.setLineWrap(true);
        this.messageScrollPane = new JScrollPane(this.messageArea);
        this.messageScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        this.messageScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        this.portField = new JTextField(10);
        this.portField.grabFocus();
        this.portLabel = new JLabel("Port");
        this.stopButton = new JButton("Stop");
        this.runButton = new JButton("Run");

        this.stopButton.setEnabled(false);
        this.runButton.setEnabled(true);

        this.northPanel.add(this.portLabel);
        this.northPanel.add(this.portField);
        this.northPanel.add(this.runButton);
        this.northPanel.add(this.stopButton);

        this.statusLayout = new FlowLayout();
        this.statusLayout.setHgap(0);
        this.statusLayout.setVgap(0);
        this.statusLayout.setAlignment(FlowLayout.LEFT);

        this.serverStatusField = new JTextField(10);
        this.serverStatusField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1, false));
        this.serverStatusField.setEditable(false);
        this.serverStatusField.setHorizontalAlignment(JTextField.CENTER);
        this.statusMessageField = new JTextField(10);
        this.statusMessageField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1, false));
        this.statusMessageField.setEditable(false);
        this.connectedUsersField = new JTextField(10);
        this.connectedUsersField.setEditable(false);
        this.connectedUsersField.setHorizontalAlignment(JTextField.CENTER);
        this.connectedUsersField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1, false));
        this.connectedUsersLabel = new JLabel("Connected Users:");
        this.statusPanel = new JPanel(this.statusLayout);
        this.statusPanel.add(this.connectedUsersLabel);
        this.statusPanel.add(this.connectedUsersField);
        this.statusPanel.add(this.serverStatusField);

        this.mainFrame.getContentPane().add(this.messageScrollPane, BorderLayout.CENTER);
        this.mainFrame.getContentPane().add(this.northPanel, BorderLayout.NORTH);
        this.mainFrame.getContentPane().add(this.statusPanel, BorderLayout.SOUTH);
//        this.mainFrame.getContentPane().add(this.statusMessageField, BorderLayout.SOUTH);

        // updateUI
        this.updateUI();

        // setting listeners
        this.runButton.addActionListener(new RunButtonListener());
        this.stopButton.addActionListener(new StopButtonListener());
        this.portField.addKeyListener(new PortFieldKeyListener());

        this.mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.mainFrame.setMinimumSize(new Dimension(400, 400));
        this.mainFrame.setVisible(true);
    }

    public static void main(String[] args) {
        ChatServer chatServer = new ChatServer();
    }

    public void log(String log) {
        this.messageArea.append(String.format("[ %tr ] %s\n", new Date(), log));
    }

    // TODO Fix back-end of server

    public void runAtPort(int port) {
        try {
            this.serverSocket = new ServerSocket(port);
            this.log("Server is running at port: " + port);
            this.serverIsRunning = true;
            new Thread(new ConnectionListener()).start();
        } catch (Exception e) {
            this.log(e.getMessage());
        }
    }

    public void closeServer() {
        try {
            ChatServer.this.serverIsRunning = false;
            this.sendPacketToAll(new Packet(Packet.DISCONNECT, null));
            this.socketToUserMap.clear();
            this.serverSocket.close();
        } catch (Exception e) {
            this.log(e.getMessage());
        }
    }

    public void updateUI() {
        // checking for buttons
        if (this.serverIsRunning) {
            this.runButton.setEnabled(false);
            this.portField.setEnabled(false);
            this.stopButton.setEnabled(true);
        } else {
            this.runButton.setEnabled(true);
            this.portField.setEnabled(true);
            this.stopButton.setEnabled(false);
        }

        // setting the amount of users
        this.connectedUsersField.setText(Integer.toString(this.socketToUserMap.size()));

        if (this.serverIsRunning) {
            this.serverStatusField.setText("RUNNING");
            this.serverStatusField.setForeground(Color.BLACK);
            this.serverStatusField.setBackground(Color.GREEN);
        } else {
            this.serverStatusField.setText("STOPPED");
            this.serverStatusField.setForeground(Color.WHITE);
            this.serverStatusField.setBackground(Color.RED);
        }
    }

    public synchronized void sendPacketToAll(Packet packet) {
        for (HashMap.Entry<Socket, User> entry : this.socketToUserMap.entrySet()) {
            try {
                new ObjectOutputStream(entry.getKey().getOutputStream()).writeObject(packet);
            } catch (Exception e) {
//                this.log("Encountered an error when sending packet to all: " + e.getMessage());
            }
        }
    }

    public void removeSocketFromMap(Socket socket) {
        this.socketToUserMap.remove(socket);
    }

    // server listener, runs only on one thread
    class ConnectionListener implements Runnable {
        @Override
        public void run() {
            ChatServer.this.updateUI();
            while (ChatServer.this.serverIsRunning) {
                try {
                    Socket socket = ChatServer.this.serverSocket.accept();
                    new Thread(new ClientHandler(socket)).start();
                } catch (Exception e) {
                    ChatServer.this.log(e.getMessage());
                }
            }

            ChatServer.this.closeServer();
            ChatServer.this.updateUI();
        }
    }

    // client handler, multiple threads, runs client listener
    class ClientHandler implements Runnable {
        private final Socket socket;
        private User user;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                synchronized (this.socket) {
                    // will probably get locked around here too
                    Packet packet = (Packet) new ObjectInputStream(this.socket.getInputStream()).readObject();
                    synchronized (ChatServer.this.socketToUserMap) {
                        User user = new User(packet.getPacketContent());
                        ChatServer.this.socketToUserMap.put(socket, user);

                        new ObjectOutputStream(socket.getOutputStream()).writeObject(
                                new Packet(Packet.SET_USERNAME, user.getName()));

                        ChatServer.this.updateUI();
                        this.user = (User) ChatServer.this.socketToUserMap.get(this.socket);
                    }
                }

                new Thread(new ClientListener(this.socket, this.user)).start();
            } catch (Exception e) {
                ChatServer.this.log(e.getMessage());
            }
        }

        @Override
        public void run() {
            try {
                while (ChatServer.this.serverIsRunning) {
                    Thread.sleep(1000);     // ping per 1 sec
                    synchronized (this.socket) {
                        new ObjectOutputStream(this.socket.getOutputStream())
                                .writeObject(new Packet(Packet.PING, null));
                    }
                }
            } catch (Exception e) {
                synchronized (ChatServer.this.socketToUserMap) {
                    ChatServer.this.sendPacketToAll(new Packet(Packet.MESSAGE,
                            user.getName() + " disconnected from the server."));
                    ChatServer.this.log(user.getName() + " disconnected from the server.");
                    ChatServer.this.removeSocketFromMap(this.socket);
                    ChatServer.this.log(ChatServer.this.socketToUserMap.size() + " users connected.");
                    ChatServer.this.updateUI();
                }
            }
        }
    }

    //
    class ClientListener implements Runnable {
        private Socket socket;
        private User user;

        public ClientListener(Socket socket, User user) {
            this.socket = socket;
            this.user = user;
        }
        @Override
        public void run() {
            try {
                while (ChatServer.this.serverIsRunning) {
                    synchronized (this.socket) {
                        Packet packet  = (Packet) new ObjectInputStream(this.socket.getInputStream()).readObject();
                        switch (packet.getPacketType()) {
                            case 0:
                                ChatServer.this.log(user.getName() + " pinged the server.");
                                break;
                            case 1:
                                ChatServer.this.log(user.getName() + " sent a message: " + packet.getPacketContent());
                                ChatServer.this.sendPacketToAll(new Packet(Packet.MESSAGE,
                                        user.getName() + ": " + packet.getPacketContent()));
                                break;
                            case 2:
                                if (!user.getName().equals(packet.getPacketContent())) {
                                    ChatServer.this.log(user.getName() + " set his username to: " + packet.getPacketContent());
                                    ChatServer.this.sendPacketToAll(new Packet(Packet.MESSAGE,
                                            user.getName() + " changed his username to " + packet.getPacketContent()));
                                    user.setName(packet.getPacketContent());
                                }
                                break;
                            case 3:
                                new ObjectOutputStream(socket.getOutputStream()).writeObject(
                                        new Packet(Packet.DISCONNECT, ""));
                                break;
                            default:
                        }
                    }
                }
            } catch (Exception e) {
                ChatServer.this.log(e.getMessage());
//                e.printStackTrace();
            }
        }
    }

    // listeners
    class StopButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            ChatServer.this.sendPacketToAll(new Packet(Packet.DISCONNECT, null));
            ChatServer.this.closeServer();
        }
    }

    class RunButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            try {
                ChatServer.this.runAtPort(Integer.parseInt(ChatServer.this.portField.getText()));
            } catch (NumberFormatException e) {
                ChatServer.this.log("Invalid port");
            }
        }
    }

    class PortFieldKeyListener implements KeyListener {

        @Override
        public void keyTyped(KeyEvent keyEvent) {

        }

        @Override
        public void keyPressed(KeyEvent keyEvent) {
            try {
                if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER) {
                    ChatServer.this.runAtPort(Integer.parseInt(ChatServer.this.portField.getText()));
                }
            }
            catch (NumberFormatException e) {
                ChatServer.this.log("Invalid port");
            }
        }

        @Override
        public void keyReleased(KeyEvent keyEvent) {

        }
    }

    // user class of ChatServer
    private class User {
        String name;
        public User(String name) {
            if (name != null) {
                this.name = name;
            } else {
                this.name = "Anonymous" + (int) (Math.random() * 10000);
            }
            ChatServer.this.log("User " + this.name + " connected to the server.");
            ChatServer.this.sendPacketToAll(new Packet(Packet.MESSAGE, this.name + " connected to the server."));
        }

        String getName() {
            return this.name;
        }
        void setName(String name) {
            this.name = name;
        }
    }
}
