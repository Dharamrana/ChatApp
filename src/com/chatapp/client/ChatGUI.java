package com.chatapp.client;
import com.chatapp.model.Message;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.sound.sampled.*;

public class ChatGUI extends JFrame {
    private boolean isDarkMode = true;
    private Color BG_MAIN, BG_SIDEBAR, BG_TOPBAR, BG_INPUT;
    private Color BUBBLE_SENT, BUBBLE_RECV, BUBBLE_SERVER, BUBBLE_AI;
    private Color TEXT_PRIMARY, TEXT_SECONDARY, TEXT_TIME;
    private Color ACCENT, ONLINE_DOT;
    private JPanel chatPanel;
    private JScrollPane chatScroll;
    private JTextField inputField;
    private JLabel typingLabel, statusLabel, channelLabel;
    private JPanel userListPanel, channelListPanel;
    private String currentTarget  = null;
    private String currentChannel = "general";
    private ChatClient client;
    private String username;
    private TargetDataLine micLine;
    private boolean isRecording = false;
    private ByteArrayOutputStream audioBuffer;
    private javax.swing.Timer typingTimer;
    private boolean isSendingTyping = false;

    // Called from LoginDialog after successful auth
    public ChatGUI(String username, Socket socket, ObjectOutputStream out, ObjectInputStream in) {
        this.username = username;
        applyTheme();
        initUI();
        client = new ChatClient(username, this, socket, out, in);
        client.startListening();
    }

    private void applyTheme() {
        if (isDarkMode) {
            BG_MAIN       = new Color(18, 18, 24);
            BG_SIDEBAR    = new Color(26, 26, 36);
            BG_TOPBAR     = new Color(32, 32, 44);
            BG_INPUT      = new Color(36, 36, 50);
            BUBBLE_SENT   = new Color(0, 150, 120);
            BUBBLE_RECV   = new Color(44, 44, 60);
            BUBBLE_SERVER = new Color(30, 30, 50);
            BUBBLE_AI     = new Color(40, 60, 100);
            TEXT_PRIMARY  = new Color(235, 235, 245);
            TEXT_SECONDARY= new Color(160, 160, 185);
            TEXT_TIME     = new Color(110, 110, 140);
            ACCENT        = new Color(0, 168, 132);
            ONLINE_DOT    = new Color(0, 210, 110);
        } else {
            BG_MAIN       = new Color(248, 248, 252);
            BG_SIDEBAR    = new Color(255, 255, 255);
            BG_TOPBAR     = new Color(0, 150, 120);
            BG_INPUT      = new Color(255, 255, 255);
            BUBBLE_SENT   = new Color(0, 168, 132);
            BUBBLE_RECV   = new Color(255, 255, 255);
            BUBBLE_SERVER = new Color(230, 230, 245);
            BUBBLE_AI     = new Color(210, 230, 255);
            TEXT_PRIMARY  = new Color(20, 20, 30);
            TEXT_SECONDARY= new Color(90, 90, 110);
            TEXT_TIME     = new Color(140, 140, 160);
            ACCENT        = new Color(0, 168, 132);
            ONLINE_DOT    = new Color(0, 190, 90);
        }
    }

    private void initUI() {
        setTitle("ChatApp - " + username);
        setSize(1100, 700);
        setMinimumSize(new Dimension(850, 550));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG_MAIN);
        add(buildSidebar(),  BorderLayout.WEST);
        add(buildChatArea(), BorderLayout.CENTER);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { if (client != null) client.disconnect(); }
        });
        setVisible(true);
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(BG_SIDEBAR);
        sidebar.setPreferredSize(new Dimension(240, 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(50, 50, 70)));

        // Workspace header
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setBackground(BG_TOPBAR);
        header.setBorder(new EmptyBorder(14, 14, 14, 14));
        JLabel workspace = new JLabel("ChatApp Workspace");
        workspace.setForeground(Color.WHITE);
        workspace.setFont(new Font("SansSerif", Font.BOLD, 14));
        JLabel themeBtn = new JLabel(isDarkMode ? "Light" : "Dark");
        themeBtn.setForeground(new Color(200, 200, 220));
        themeBtn.setFont(new Font("SansSerif", Font.PLAIN, 11));
        themeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        themeBtn.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { toggleTheme(); }
        });
        header.add(workspace, BorderLayout.CENTER);
        header.add(themeBtn,  BorderLayout.EAST);

        // Channels section
        JPanel channelsSection = new JPanel();
        channelsSection.setLayout(new BoxLayout(channelsSection, BoxLayout.Y_AXIS));
        channelsSection.setBackground(BG_SIDEBAR);

        JLabel channelsTitle = sectionTitle("CHANNELS");
        channelListPanel = new JPanel();
        channelListPanel.setLayout(new BoxLayout(channelListPanel, BoxLayout.Y_AXIS));
        channelListPanel.setBackground(BG_SIDEBAR);
        addChannelRow("general");
        addChannelRow("random");
        addChannelRow("announcements");

        channelsSection.add(channelsTitle);
        channelsSection.add(channelListPanel);

        // DMs section
        JPanel dmsSection = new JPanel();
        dmsSection.setLayout(new BoxLayout(dmsSection, BoxLayout.Y_AXIS));
        dmsSection.setBackground(BG_SIDEBAR);

        JLabel dmsTitle = sectionTitle("DIRECT MESSAGES");
        userListPanel = new JPanel();
        userListPanel.setLayout(new BoxLayout(userListPanel, BoxLayout.Y_AXIS));
        userListPanel.setBackground(BG_SIDEBAR);

        dmsSection.add(dmsTitle);
        dmsSection.add(userListPanel);

        // AI bot entry
        JPanel aiRow = makeSidebarRow("Claude AI", true, false);
        aiRow.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                currentTarget  = null;
                currentChannel = null;
                channelLabel.setText("Claude AI  |  Ask anything with @claude");
                highlightSidebarRow(aiRow);
            }
        });
        dmsSection.add(aiRow);

        // User profile at bottom
        JPanel profile = new JPanel(new BorderLayout(10, 0));
        profile.setBackground(new Color(isDarkMode ? 20 : 240, isDarkMode ? 20 : 240, isDarkMode ? 35 : 250));
        profile.setBorder(new EmptyBorder(10, 12, 10, 12));
        JLabel avatar = makeAvatar(username, 32);
        JLabel uname  = new JLabel(username);
        uname.setForeground(TEXT_PRIMARY);
        uname.setFont(new Font("SansSerif", Font.BOLD, 13));
        JLabel status = new JLabel("Active");
        status.setForeground(ONLINE_DOT);
        status.setFont(new Font("SansSerif", Font.PLAIN, 11));
        JPanel namePanel = new JPanel();
        namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.Y_AXIS));
        namePanel.setBackground(new Color(isDarkMode ? 20 : 240, isDarkMode ? 20 : 240, isDarkMode ? 35 : 250));
        namePanel.add(uname); namePanel.add(status);
        profile.add(avatar,    BorderLayout.WEST);
        profile.add(namePanel, BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane();
        JPanel scrollContent = new JPanel();
        scrollContent.setLayout(new BoxLayout(scrollContent, BoxLayout.Y_AXIS));
        scrollContent.setBackground(BG_SIDEBAR);
        scrollContent.add(channelsSection);
        scrollContent.add(Box.createVerticalStrut(10));
        scrollContent.add(dmsSection);
        scroll.setViewportView(scrollContent);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_SIDEBAR);

        sidebar.add(header,  BorderLayout.NORTH);
        sidebar.add(scroll,  BorderLayout.CENTER);
        sidebar.add(profile, BorderLayout.SOUTH);
        return sidebar;
    }

    private void addChannelRow(String name) {
        JPanel row = makeSidebarRow("# " + name, false, name.equals(currentChannel));
        row.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                currentChannel = name;
                currentTarget  = null;
                channelLabel.setText("# " + name);
                highlightSidebarRow(row);
                client.sendText("/join " + name);
            }
        });
        channelListPanel.add(row);
    }

    private JPanel makeSidebarRow(String label, boolean isUser, boolean isActive) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(isActive ? new Color(isDarkMode ? 50 : 210, isDarkMode ? 60 : 220, isDarkMode ? 80 : 210) : BG_SIDEBAR);
        row.setBorder(new EmptyBorder(7, 14, 7, 14));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JLabel lbl = new JLabel(label);
        lbl.setForeground(isActive ? ACCENT : TEXT_SECONDARY);
        lbl.setFont(new Font("SansSerif", isActive ? Font.BOLD : Font.PLAIN, 13));
        row.add(lbl, BorderLayout.CENTER);
        if (isUser) {
            JPanel dot = new JPanel();
            dot.setBackground(ONLINE_DOT);
            dot.setPreferredSize(new Dimension(7, 7));
            row.add(dot, BorderLayout.EAST);
        }
        row.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (row.getBackground().equals(BG_SIDEBAR))
                    row.setBackground(new Color(isDarkMode ? 38 : 230, isDarkMode ? 38 : 230, isDarkMode ? 55 : 240));
            }
            public void mouseExited(MouseEvent e) {
                if (!row.getBackground().equals(new Color(isDarkMode ? 50 : 210, isDarkMode ? 60 : 220, isDarkMode ? 80 : 210)))
                    row.setBackground(BG_SIDEBAR);
            }
        });
        return row;
    }

    private void highlightSidebarRow(JPanel selected) { /* visual selection */ }

    private JLabel sectionTitle(String text) {
        JLabel l = new JLabel("  " + text);
        l.setForeground(TEXT_SECONDARY);
        l.setFont(new Font("SansSerif", Font.BOLD, 10));
        l.setBorder(new EmptyBorder(14, 4, 4, 0));
        l.setBackground(BG_SIDEBAR);
        l.setOpaque(true);
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        return l;
    }

    private JPanel buildChatArea() {
        JPanel area = new JPanel(new BorderLayout());
        area.setBackground(BG_MAIN);

        // Top bar
        JPanel topBar = new JPanel(new BorderLayout(12, 0));
        topBar.setBackground(BG_TOPBAR);
        topBar.setBorder(new EmptyBorder(12, 18, 12, 18));
        channelLabel = new JLabel("# general");
        channelLabel.setForeground(Color.WHITE);
        channelLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        statusLabel = new JLabel("Connected");
        statusLabel.setForeground(ONLINE_DOT);
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setBackground(BG_TOPBAR);
        titlePanel.add(channelLabel);
        titlePanel.add(statusLabel);
        topBar.add(titlePanel, BorderLayout.CENTER);

        // Messages
        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setBackground(BG_MAIN);
        chatPanel.setBorder(new EmptyBorder(10, 16, 10, 16));
        chatScroll = new JScrollPane(chatPanel);
        chatScroll.setBorder(null);
        chatScroll.getViewport().setBackground(BG_MAIN);
        chatScroll.getVerticalScrollBar().setUnitIncrement(16);

        typingLabel = new JLabel("  ");
        typingLabel.setForeground(TEXT_SECONDARY);
        typingLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
        typingLabel.setBackground(BG_MAIN);
        typingLabel.setOpaque(true);
        typingLabel.setBorder(new EmptyBorder(2, 18, 2, 0));

        JPanel inputBar   = buildInputBar();
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(BG_INPUT);
        bottomPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(50, 50, 70)));
        bottomPanel.add(typingLabel, BorderLayout.NORTH);
        bottomPanel.add(inputBar,    BorderLayout.CENTER);

        area.add(topBar,       BorderLayout.NORTH);
        area.add(chatScroll,   BorderLayout.CENTER);
        area.add(bottomPanel,  BorderLayout.SOUTH);
        return area;
    }

    private JPanel buildInputBar() {
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setBackground(BG_INPUT);
        bar.setBorder(new EmptyBorder(10, 16, 10, 16));

        inputField = new JTextField();
        inputField.setBackground(new Color(isDarkMode ? 48 : 248, isDarkMode ? 48 : 248, isDarkMode ? 65 : 252));
        inputField.setForeground(TEXT_PRIMARY);
        inputField.setCaretColor(TEXT_PRIMARY);
        inputField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        inputField.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(isDarkMode ? 65 : 200, isDarkMode ? 65 : 200, isDarkMode ? 90 : 220), 1, true),
            new EmptyBorder(10, 14, 10, 14)));
        inputField.setToolTipText("Message... (use @claude to ask AI)");
        inputField.addActionListener(e -> sendText());
        inputField.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) { sendTypingIndicator(); }
        });

        JButton emojiBtn = iconBtn("  :)  ", new Color(isDarkMode ? 55 : 230, isDarkMode ? 55 : 230, isDarkMode ? 75 : 240), TEXT_SECONDARY);
        emojiBtn.addActionListener(e -> showEmojiPicker());

        JButton fileBtn  = iconBtn(" + ", ACCENT, Color.WHITE);
        fileBtn.addActionListener(e -> sendFile());

        JButton voiceBtn = iconBtn(" MIC ", new Color(200, 60, 60), Color.WHITE);
        voiceBtn.addActionListener(e -> toggleVoiceRecording(voiceBtn));

        JButton sendBtn  = iconBtn(" SEND ", ACCENT, Color.WHITE);
        sendBtn.addActionListener(e -> sendText());

        JPanel left  = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        left.setBackground(BG_INPUT);
        left.add(emojiBtn); left.add(fileBtn);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        right.setBackground(BG_INPUT);
        right.add(voiceBtn); right.add(sendBtn);

        bar.add(left,       BorderLayout.WEST);
        bar.add(inputField, BorderLayout.CENTER);
        bar.add(right,      BorderLayout.EAST);
        return bar;
    }

    public void addBubble(String sender, String content, String time, boolean isSent, Message.Type type) {
        SwingUtilities.invokeLater(() -> {
            boolean isServer = sender.equals("SERVER");
            boolean isAI     = sender.equals("Claude AI");

            if (isServer) { addSystemMessage(content); return; }

            JPanel row = new JPanel(new FlowLayout(isSent ? FlowLayout.RIGHT : FlowLayout.LEFT, 8, 4));
            row.setBackground(BG_MAIN);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

            if (!isSent) row.add(makeAvatar(sender, 32));

            JPanel bubble = new JPanel() {
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(isSent ? BUBBLE_SENT : isAI ? BUBBLE_AI : BUBBLE_RECV);
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            bubble.setLayout(new BorderLayout(0, 4));
            bubble.setOpaque(false);
            bubble.setBorder(new EmptyBorder(10, 14, 10, 14));
            bubble.setMaximumSize(new Dimension(500, Integer.MAX_VALUE));

            if (!isSent) {
                JLabel senderLbl = new JLabel(isAI ? "Claude AI" : sender);
                senderLbl.setForeground(isAI ? new Color(100, 160, 255) : ACCENT);
                senderLbl.setFont(new Font("SansSerif", Font.BOLD, 11));
                bubble.add(senderLbl, BorderLayout.NORTH);
            }

            String displayContent = type == Message.Type.VOICE
                ? "[Voice Message]"
                : type == Message.Type.FILE
                ? "[File] " + content
                : content;

            JLabel msgLbl = new JLabel("<html><body style='width:300px;font-size:13px'>" + displayContent.replace("\n", "<br>") + "</body></html>");
            msgLbl.setForeground(isSent ? Color.WHITE : TEXT_PRIMARY);

            JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            footer.setOpaque(false);
            JLabel timeLbl = new JLabel(time);
            timeLbl.setFont(new Font("SansSerif", Font.PLAIN, 10));
            timeLbl.setForeground(isSent ? new Color(200, 240, 230) : TEXT_TIME);
            footer.add(timeLbl);
            if (isSent) { JLabel tick = new JLabel("  v"); tick.setForeground(new Color(180,220,210)); tick.setFont(new Font("SansSerif", Font.BOLD, 10)); footer.add(tick); }

            bubble.add(msgLbl,  BorderLayout.CENTER);
            bubble.add(footer,  BorderLayout.SOUTH);

            bubble.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            bubble.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) showReactionMenu(bubble, content);
                }
            });

            row.add(bubble);
            chatPanel.add(row);
            chatPanel.add(Box.createVerticalStrut(3));
            chatPanel.revalidate();
            chatPanel.repaint();
            scrollToBottom();
            if (!isSent && !isServer) showNotification(sender, content);
        });
    }

    private void addSystemMessage(String text) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER));
        row.setBackground(BG_MAIN);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        JLabel lbl = new JLabel(text);
        lbl.setForeground(TEXT_TIME);
        lbl.setFont(new Font("SansSerif", Font.ITALIC, 11));
        row.add(lbl);
        chatPanel.add(row);
        chatPanel.revalidate();
        chatPanel.repaint();
        scrollToBottom();
    }

    private void showEmojiPicker() {
        String[] emojis = {":-)", ":-D", ";-)", ":-P", ":-O", ":-(", ":'(", "<3", ":+1:", ":-*", "B-)", ":-|"};
        JPopupMenu popup = new JPopupMenu();
        popup.setBackground(BG_SIDEBAR);
        JPanel grid = new JPanel(new GridLayout(2, 6, 4, 4));
        grid.setBackground(BG_SIDEBAR);
        grid.setBorder(new EmptyBorder(6, 6, 6, 6));
        for (String em : emojis) {
            JButton btn = new JButton(em);
            btn.setBackground(BG_INPUT);
            btn.setForeground(TEXT_PRIMARY);
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setFont(new Font("SansSerif", Font.PLAIN, 11));
            btn.addActionListener(e -> { inputField.setText(inputField.getText() + em); popup.setVisible(false); inputField.requestFocus(); });
            grid.add(btn);
        }
        popup.add(grid);
        popup.show(inputField, 0, -popup.getPreferredSize().height - 4);
    }

    private void showReactionMenu(JPanel bubble, String content) {
        String[] reactions = {"Like", "Love", "Haha", "Wow", "Sad", "Angry"};
        JPopupMenu popup = new JPopupMenu();
        popup.setBackground(BG_SIDEBAR);
        JPanel grid = new JPanel(new GridLayout(1, 6, 4, 0));
        grid.setBackground(BG_SIDEBAR);
        grid.setBorder(new EmptyBorder(4, 6, 4, 6));
        for (String r : reactions) {
            JButton btn = new JButton(r);
            btn.setBackground(BG_INPUT);
            btn.setForeground(TEXT_PRIMARY);
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setFont(new Font("SansSerif", Font.PLAIN, 10));
            btn.addActionListener(e -> {
                client.sendMessage(new Message(Message.Type.REACTION, username, username + " reacted " + r));
                popup.setVisible(false);
            });
            grid.add(btn);
        }
        popup.add(grid);
        popup.show(bubble, bubble.getWidth() / 2, bubble.getHeight());
    }

    private void toggleVoiceRecording(JButton btn) {
        if (!isRecording) {
            try {
                AudioFormat fmt = new AudioFormat(16000, 16, 1, true, true);
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, fmt);
                if (!AudioSystem.isLineSupported(info)) { showError("Mic not supported!"); return; }
                micLine = (TargetDataLine) AudioSystem.getLine(info);
                micLine.open(fmt); micLine.start();
                isRecording  = true;
                audioBuffer  = new ByteArrayOutputStream();
                btn.setText(" STOP ");
                btn.setBackground(new Color(160, 30, 30));
                new Thread(() -> {
                    byte[] buf = new byte[1024];
                    while (isRecording) { int n = micLine.read(buf, 0, buf.length); if (n > 0) audioBuffer.write(buf, 0, n); }
                }).start();
            } catch (Exception e) { showError("Mic error: " + e.getMessage()); }
        } else {
            isRecording = false;
            micLine.stop(); micLine.close();
            btn.setText(" MIC ");
            btn.setBackground(new Color(200, 60, 60));
            byte[] data     = audioBuffer.toByteArray();
            String fileName = username + "_voice_" + System.currentTimeMillis() + ".wav";
            Message vm = new Message(Message.Type.VOICE, username, "Voice message");
            vm.setFileName(fileName); vm.setFileData(data);
            client.sendMessage(vm);
            addBubble(username, "Voice message sent", nowTime(), true, Message.Type.VOICE);
        }
    }

    private void sendTypingIndicator() {
        if (!isSendingTyping) { isSendingTyping = true; client.sendMessage(new Message(Message.Type.TYPING, username, "typing")); }
        if (typingTimer != null) typingTimer.stop();
        typingTimer = new javax.swing.Timer(2000, e -> { isSendingTyping = false; client.sendMessage(new Message(Message.Type.TYPING, username, "stopped")); });
        typingTimer.setRepeats(false); typingTimer.start();
    }

    private void showNotification(String sender, String content) {
        if (!isFocused()) {
            JWindow notif = new JWindow();
            notif.setAlwaysOnTop(true);
            JPanel panel = new JPanel(new BorderLayout(8, 4));
            panel.setBackground(BG_TOPBAR);
            panel.setBorder(new EmptyBorder(10, 14, 10, 14));
            JLabel title = new JLabel(sender);
            title.setForeground(ACCENT);
            title.setFont(new Font("SansSerif", Font.BOLD, 13));
            JLabel msg = new JLabel(content.length() > 45 ? content.substring(0, 45) + "..." : content);
            msg.setForeground(TEXT_PRIMARY);
            msg.setFont(new Font("SansSerif", Font.PLAIN, 12));
            panel.add(title, BorderLayout.NORTH);
            panel.add(msg,   BorderLayout.CENTER);
            notif.add(panel); notif.pack();
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            notif.setLocation(screen.width - notif.getWidth() - 20, screen.height - notif.getHeight() - 50);
            notif.setVisible(true);
            new javax.swing.Timer(3000, e -> notif.dispose()).start();
        }
    }

    private void sendText() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;
        if (text.startsWith("/")) {
            client.sendText(text);
        }
        else if (currentTarget != null) {
            client.sendPrivate(currentTarget, text);
        }else {
            client.sendText(text);
        }
        inputField.setText("");
    }


    private void sendFile() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            client.sendFile(fc.getSelectedFile().getAbsolutePath(), currentTarget);
        }
    }

    public void receiveMessage(Message msg) {
        SwingUtilities.invokeLater(() -> {
            boolean isSent = msg.getSender().equals(username);
            switch (msg.getType()) {
                case TEXT: case GROUP:
                    addBubble(msg.getSender(), msg.getContent(),
                        msg.getTimestamp(), isSent, msg.getType());
                break;
                case PRIVATE:
                    addBubble(msg.getSender(), "[DM] " + msg.getContent(),
                        msg.getTimestamp(), isSent, msg.getType());
                break;
                case JOIN: case LEAVE:
                    addSystemMessage(msg.getContent()); break;
                case STATUS:
                    String content = msg.getContent();
                    if (content.startsWith("Online"))
                        updateUserList(content.replace("Online users: ","").replace("Online: ",""));
                    else addSystemMessage(content);
                    break;
                case FILE:
                    addBubble(msg.getSender(), msg.getFileName(),
                        msg.getTimestamp(), isSent, Message.Type.FILE);
                    if (!isSent)
                        try { com.chatapp.util.FileUtils.saveFile(msg.getFileName(), msg.getFileData()); }
                        catch (Exception ignored) {}
                    break;
                case VOICE:
                    addBubble(msg.getSender(), "Voice message",
                        msg.getTimestamp(), isSent, Message.Type.VOICE);
                    if (!isSent)
                        try { com.chatapp.util.FileUtils.saveFile(msg.getFileName(), msg.getFileData()); }
                        catch (Exception ignored) {}
                    break;
                case REACTION:
                    addSystemMessage(msg.getContent()); break;
                case TYPING:
                    if (msg.getContent().equals("typing")) {
                        typingLabel.setText("  " + msg.getSender() + " is typing...");
                        new javax.swing.Timer(2500, e -> typingLabel.setText("  ")).start();
                    } else typingLabel.setText("  ");
                    break;
                default: break;
            }
        });
    }

    public void updateUserList(String csv) {
        SwingUtilities.invokeLater(() -> {
            userListPanel.removeAll();
            for (String u : csv.split(",")) {
                if (u.isBlank() || u.trim().equals(username)) continue;
                String name = u.trim();
                JPanel row  = makeSidebarRow(name, true, false);
                row.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        currentTarget  = name;
                        currentChannel = null;
                        channelLabel.setText("DM: " + name);
                    }
                });
                userListPanel.add(row);
            }
            userListPanel.revalidate();
            userListPanel.repaint();
        });
    }

    private void toggleTheme() { isDarkMode = !isDarkMode; dispose(); applyTheme(); initUI(); }

    private JLabel makeAvatar(String name, int size) {
        Color[] colors = { new Color(229,115,115), new Color(100,181,246), new Color(129,199,132), new Color(255,183,77), new Color(186,104,200), new Color(77,182,172) };
        Color   color  = colors[Math.abs(name.hashCode()) % colors.length];
        JLabel av = new JLabel(String.valueOf(name.charAt(0)).toUpperCase(), SwingConstants.CENTER) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color); g2.fillOval(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        av.setForeground(Color.WHITE);
        av.setFont(new Font("SansSerif", Font.BOLD, size / 2));
        av.setOpaque(false);
        av.setPreferredSize(new Dimension(size, size));
        av.setMinimumSize(new Dimension(size, size));
        av.setMaximumSize(new Dimension(size, size));
        return av;
    }

    private JButton iconBtn(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg); btn.setForeground(fg);
        btn.setFocusPainted(false); btn.setBorderPainted(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(6, 10, 6, 10));
        return btn;
    }

    private String nowTime() { return java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")); }
    private void scrollToBottom() { SwingUtilities.invokeLater(() -> { JScrollBar b = chatScroll.getVerticalScrollBar(); b.setValue(b.getMaximum()); }); }
    public void appendToChat(String text) { addSystemMessage(text); }
    public void showError(String err)     { SwingUtilities.invokeLater(() -> statusLabel.setText("[!] " + err)); }
}