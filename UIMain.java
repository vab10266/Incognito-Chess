import java.awt.Color;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.*;
import java.awt.Image;
import java.util.ArrayList;
import java.util.Random;

import javax.swing.*;

import javafx.scene.control.Button;

class ClickListener implements ActionListener {

    public static JButton lastPressed;

    public void actionPerformed(ActionEvent e) {
        if (UIMain.startColor == UIMain.b.getTurn()) {
            JButton curr = (JButton) e.getSource();
            lastPressed = curr;
            UIMain.takeTurn();
        }
        //curr.setText("LMAO");
    }
}

class UIMain extends JFrame {

    public static Board b;
    public static JButton[][] buttonArr;
    public static ArrayList<JButton> presses = new ArrayList<>();
    static Server server;
    static Client client;
    public static double currentval;
    public static boolean startColor;

    // Constructor:
    public UIMain() {
        setTitle("Incognito Chess");
        setSize(800, 800);
        setLocation(200, 200);

        // Window Listeners
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }

    public static void main(String[] args) {
        b = new Board();

        currentval = (new Random()).nextDouble();

        client = new Client(args[0], 5000);

        Thread clientThread = new Thread(client);
        clientThread.start();

        server = new Server(5000);

        Thread serverThread = new Thread(server);
        serverThread.start();
    }

    public static void initHandshake() {
        client.sendDouble(currentval);
    }

    public static boolean handshake(double d) {
        if (currentval == d) {
            initHandshake();
            return false;
        } else if (currentval > d) {
            startColor = true;
        } else {
            startColor = false;
            b.setTurn(false);
        }

        return true;
    }

    public static void startgame() {
        System.out.println("You are: " + (startColor ? "White":"Black"));

        JFrame f = new UIMain();

        Container contentPane = f.getContentPane();
        JPanel chessPanel = new JPanel(new GridLayout(8, 8));

        buttonArr = new JButton[8][8];

        for (int i = 7; i >= 0; i--) {
            for (int j = 0; j < 8; j++) {
                //Icon warnIcon = new ImageIcon("pawn.png");
                JButton square = new JButton();
                char c = b.getBoard()[j][i];
                String s = "" + Character.toLowerCase(c);
                if (Character.isUpperCase(c)) {
                    s = s + "2";
                }
                ImageIcon icon = new ImageIcon("pieces/" + s + ".png");
                Image piece = icon.getImage();
                Image newimg = piece.getScaledInstance(100, 100,  java.awt.Image.SCALE_SMOOTH);
                icon = new ImageIcon(newimg);
                square.setIcon(icon);

                square.addActionListener(new ClickListener());
                square.setActionCommand(j + " " + i);
                square.setFocusPainted(false);
                square.setRolloverEnabled(true);
                square.setBorderPainted(false);

                buttonArr[j][i] = square;

                if (i % 2 == 0) {
                    if (j % 2 == 0) {
                        square.setBackground(Color.WHITE);
                    } else {
                        square.setBackground(Color.GRAY);
                    }
                } else {
                    if (j % 2 == 0) {
                        square.setBackground(Color.GRAY);
                    } else {
                        square.setBackground(Color.WHITE);
                    }
                }
                chessPanel.add(square);
            }
        }

        contentPane.add(chessPanel);

        f.setVisible(true);
    }

    public static void takeTurn() {
        presses.add(ClickListener.lastPressed);

        if (presses.size() >= 2) {
            String start = presses.get(0).getActionCommand();
            String end = presses.get(1).getActionCommand();

            int startx = (start.charAt(0) - '0');
            int starty = (start.charAt(2) - '0');
            int endx = (end.charAt(0) - '0');
            int endy = (end.charAt(2) - '0');

            int[] sending = {startx, starty, endx, endy};

            boolean check = b.move(startx, starty, endx, endy);

            if (check) {
                refreshBoard();

                presses.clear();
                System.out.println("Send initializing...");
                client.sendMove(sending);
            } else {
                presses.clear();
            }
        }
    }

    public static void recieveMove(int[] move) {
        b.move(move[0], move[1], move[2], move[3]);

        refreshBoard();
    }

    public static void refreshBoard() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {

                char c = b.getBoard()[i][j];
                String s = "" + Character.toLowerCase(c);
                if (Character.isUpperCase(c)) {
                    s = s + "2";
                }
                ImageIcon icon = new ImageIcon("pieces/" + s + ".png");
                Image piece = icon.getImage();
                Image newimg = piece.getScaledInstance(100, 100,  java.awt.Image.SCALE_SMOOTH);
                icon = new ImageIcon(newimg);
                buttonArr[i][j].setIcon(icon);

            }
        }
    }
}