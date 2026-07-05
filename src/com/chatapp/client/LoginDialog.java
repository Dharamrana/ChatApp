package com.chatapp.client;
import com.chatapp.model.Message;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
public class LoginDialog extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel statusLabel;
    private ObjectOutputStream out;
    private ObjectInputStream  in;
    private Socket socket;
    private static final String HOST = "localhost";
    private static final int    PORT = 12345;

    private Color BG       = new Color(18, 18, 30);
    private Color CARD     = new Color(28, 28, 45);
    private Color ACCENT   = new Color(0, 168, 132);
    private Color TEXT     = Color.WHITE;
    private Color SUBTEXT  = new Color(160, 160, 180);
    private Color INPUT_BG = new Color(38, 38, 58);

    public LoginDialog() {
        setTitle("ChatApp - Login");
        setSize(420, 540);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setUndecorated(true);
        buildUI();
        setVisible(true);
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        root.setBorder(new LineBorder(new Color(60, 60, 90), 1));

        // Title bar
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(new Color(15, 15, 25));
        titleBar.setBorder(new EmptyBorder(10, 16, 10, 16));
        JLabel appName = new JLabel("ChatApp");
        appName.setForeground(ACCENT);
        appName.setFont(new Font("SansSerif", Font.BOLD, 14));
        JLabel closeBtn = new JLabel("  X  ");
        closeBtn.setForeground(SUBTEXT);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { System.exit(0); }
            public void mouseEntered(MouseEvent e) { closeBtn.setForeground(Color.RED); }
            public void mouseExited(MouseEvent e)  { closeBtn.setForeground(SUBTEXT); }
        });
        titleBar.add(appName,  BorderLayout.WEST);
        titleBar.add(closeBtn, BorderLayout.EAST);
        // Drag to move
        titleBar.addMouseMotionListener(new MouseMotionAdapter() {
            Point offset;
            public void mouseDragged(MouseEvent e) {
                Point loc = LoginDialog.this.getLocation();
                LoginDialog.this.setLocation(loc.x + e.getX() - (offset != null ? offset.x : e.getX()),
                                              loc.y + e.getY() - (offset != null ? offset.y : e.getY()));
            }
        });
        titleBar.addMouseListener(new MouseAdapter() {
            Point offset;
            public void mousePressed(MouseEvent e) { offset = e.getPoint(); }
        });

        // Card
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD);
        card.setBorder(new EmptyBorder(40, 40, 40, 40));

        // Logo
        JLabel logo = new JLabel("C", SwingConstants.CENTER) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ACCENT);
                g2.fillOval(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        logo.setForeground(Color.WHITE);
        logo.setFont(new Font("SansSerif", Font.BOLD, 32));
        logo.setPreferredSize(new Dimension(70, 70));
        logo.setMaximumSize(new Dimension(70, 70));
        logo.setAlignmentX(CENTER_ALIGNMENT);

        JLabel title = new JLabel("Welcome to ChatApp");
        title.setForeground(TEXT);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setAlignmentX(CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Sign in to your workspace");
        subtitle.setForeground(SUBTEXT);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setAlignmentX(CENTER_ALIGNMENT);

        // Fields
        usernameField = makeField("Username");
        passwordField = new JPasswordField();
        styleField(passwordField, "Password");

        // Status
        statusLabel = new JLabel(" ");
        statusLabel.setForeground(new Color(255, 100, 100));
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setAlignmentX(CENTER_ALIGNMENT);

        // Buttons
        JButton loginBtn    = makeBtn("Sign In",   ACCENT);
        JButton registerBtn = makeBtn("Register",  INPUT_BG);

        loginBtn.addActionListener(e    -> authenticate(false));
        registerBtn.addActionListener(e -> authenticate(true));
        usernameField.addActionListener(e -> passwordField.requestFocus());
        passwordField.addActionListener(e -> authenticate(false));

        card.add(logo);
        card.add(Box.createVerticalStrut(16));
        card.add(title);
        card.add(Box.createVerticalStrut(6));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(30));
        card.add(makeLabel("Username"));
        card.add(Box.createVerticalStrut(6));
        card.add(usernameField);
        card.add(Box.createVerticalStrut(14));
        card.add(makeLabel("Password"));
        card.add(Box.createVerticalStrut(6));
        card.add(passwordField);
        card.add(Box.createVerticalStrut(8));
        card.add(statusLabel);
        card.add(Box.createVerticalStrut(16));
        card.add(loginBtn);
        card.add(Box.createVerticalStrut(10));
        card.add(registerBtn);

        root.add(titleBar, BorderLayout.NORTH);
        root.add(card,     BorderLayout.CENTER);
        setContentPane(root);
    }

    private void authenticate(boolean isRegister) {
        String user = usernameField.getText().trim();
        String pass = new String(passwordField.getPassword()).trim();
        if (user.isEmpty() || pass.isEmpty()) {
            statusLabel.setText("Please fill in all fields."); return;
        }
        statusLabel.setText("Connecting...");
        statusLabel.setForeground(ACCENT);
        new Thread(() -> {
            try {
                socket = new Socket(HOST, PORT);
                out    = new ObjectOutputStream(socket.getOutputStream());
                in     = new ObjectInputStream(socket.getInputStream());
                Message.Type authType = isRegister ? Message.Type.AUTH_REGISTER : Message.Type.AUTH_LOGIN;
                Message authMsg = new Message(authType, user, user + "|" + pass);
                out.writeObject(authMsg); out.flush();
                Message response = (Message) in.readObject();
                if (response.getType() == Message.Type.AUTH_SUCCESS) {
                    SwingUtilities.invokeLater(() -> {
                        dispose();
                        new ChatGUI(user, socket, out, in);
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setForeground(new Color(255, 100, 100));
                        statusLabel.setText(response.getContent());
                        try { socket.close(); } catch (Exception ignored) {}
                    });
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setForeground(new Color(255, 100, 100));
                    statusLabel.setText("Cannot connect to server!");
                });
            }
        }).start();
    }

    private JTextField makeField(String placeholder) {
        JTextField f = new JTextField();
        styleField(f, placeholder);
        return f;
    }

    private void styleField(JTextField f, String placeholder) {
        f.setBackground(INPUT_BG);
        f.setForeground(TEXT);
        f.setCaretColor(TEXT);
        f.setFont(new Font("SansSerif", Font.PLAIN, 14));
        f.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(60, 60, 90), 1, true),
            new EmptyBorder(10, 12, 10, 12)));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        f.setAlignmentX(CENTER_ALIGNMENT);
    }

    private JLabel makeLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(SUBTEXT);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private JButton makeBtn(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btn.setAlignmentX(CENTER_ALIGNMENT);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(LoginDialog::new);
    }
}