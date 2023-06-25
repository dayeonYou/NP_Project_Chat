import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.*;

public class Client extends JFrame {
    private JTextField userText;
    private JTextArea chatWindow;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private String serverIP;
    private Socket connection;
    private JButton stockButton;
    private JButton rateButton;

    public Client(String host) {
        super("채팅 프로그램 - 클라이언트");

        serverIP = host;
        userText = new JTextField();
        userText.setEditable(false);
        userText.setBackground(Color.PINK);
        userText.setPreferredSize(new Dimension(200, 30)); // 가로 200, 세로 30 크기로 설정

        userText.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                sendMessage(event.getActionCommand());
                userText.setText("");
            }
        });
        add(userText, BorderLayout.NORTH);
        chatWindow = new JTextArea();
        add(new JScrollPane(chatWindow), BorderLayout.CENTER);
        setSize(700, 500);
        setVisible(true);

        JButton sendButton = new JButton("Send File");
        sendButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                JFileChooser fileChooser = new JFileChooser();
                int result = fileChooser.showOpenDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    sendFile(file);
                }
            }
        });
        add(sendButton, BorderLayout.SOUTH);
        stockButton = new JButton("주식");
        stockButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String stockName = JOptionPane.showInputDialog("주식 이름을 입력하세요:");
                requestStockInfo(stockName);
            }
        });
        add(stockButton, BorderLayout.WEST);
        rateButton = new JButton("환율");
        rateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String country = JOptionPane.showInputDialog("국가를 입력하세요:");
                requestRateInfo(country);
            }
        });
        add(rateButton, BorderLayout.EAST);
    }

    private void requestStockReserve(String stockName, String trend, String priceOfReserve, String notification) {

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String message = "StockReserve: " + stockName + "&&&" + trend + "&&&" + priceOfReserve + "&&&" + notification;
                    output.writeObject(message);
                    output.flush();

                    showMessage("\n주식 예약 요청: " + stockName);

                } catch (IOException e) {
                    showMessage("\n주식 예약 요청 중 오류: " + e.getMessage());
                }
            }
        });

        thread.start();
    }

    private void requestStockInfo(String stockName) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String message = "stockName: " + stockName;
                    output.writeObject(message);
                    output.flush();

                    showMessage("\n주식 정보 요청: " + stockName);

                } catch (IOException e) {
                    showMessage("\n주식 정보 요청 중 오류: " + e.getMessage());
                }
            }
        });

        thread.start();
    }

    private void requestRateInfo(String country){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String message = "country: " + country;
                    output.writeObject(message);
                    output.flush();

                    showMessage("\n환율 정보 요청: " + country);

                } catch (IOException e) {
                    showMessage("\n환율 정보 요청 중 오류: " + e.getMessage());
                }
            }
        });

        thread.start();
    }

    private void displayStockInfo(String stockName, String price, String imageUrl) {
        JFrame stockInfoFrame = new JFrame(stockName + " 주식 정보");
        stockInfoFrame.setSize(720, 400);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BorderLayout());

        JLabel nameLabel = new JLabel("     이름: " + stockName + "                         현재 가격: " + price);
        nameLabel.setFont(new Font("default", Font.PLAIN, 20));

        AtomicReference<String> notificationRef = new AtomicReference<>(""); // 기본값 설정

        JButton reservationButton = new JButton("예약");
        reservationButton.addActionListener(e -> {
            JFrame reservationFrame = new JFrame("주식 예약");
            reservationFrame.setSize(400, 300);

            JPanel reservationPanel = new JPanel();
            reservationPanel.setLayout(new GridLayout(5, 2, 10, 10));

            JRadioButton increaseRadioButton = new JRadioButton("상승");
            JRadioButton decreaseRadioButton = new JRadioButton("하락");
            ButtonGroup trendButtonGroup = new ButtonGroup();
            trendButtonGroup.add(increaseRadioButton);
            trendButtonGroup.add(decreaseRadioButton);

            JLabel priceLabel = new JLabel("가격:");
            JTextField priceTextField = new JTextField();

            JLabel alarmLabel = new JLabel("알람 방식:");
            JLabel alarmMethodLabel = new JLabel();

            JRadioButton chatRadioButton = new JRadioButton("채팅으로 받기");
            JRadioButton emailRadioButton = new JRadioButton("이메일로 받기");
            ButtonGroup notificationButtonGroup = new ButtonGroup();
            notificationButtonGroup.add(chatRadioButton);
            notificationButtonGroup.add(emailRadioButton);

            JButton confirmButton = new JButton("확인");
            JButton cancelButton = new JButton("취소");
            confirmButton.addActionListener(confirmEvent -> {
                // 선택한 값 검색
                String trend = increaseRadioButton.isSelected() ? "상승" : "하락";
                String priceOfReserve = priceTextField.getText();
                //String alarmMethod = alarmMethodLabel.getText();
                String notification = chatRadioButton.isSelected() ? "채팅" : "이메일";

                if (notification.equals("이메일")) {
                    JFrame frameEmail = new JFrame("이메일 입력");

                    JTextField textField = new JTextField();
                    textField.setPreferredSize(new Dimension(200, 30)); // 가로 200, 세로 30 크기로 지정

                    JButton confirmButtonEmail = new JButton("확인");
                    JButton cancelButtonEmail = new JButton("취소");

                    JPanel panelEmail = new JPanel(new BorderLayout());
                    panelEmail.add(new JLabel("이메일 주소:"), BorderLayout.WEST);
                    panelEmail.add(textField, BorderLayout.CENTER);
                    panelEmail.add(confirmButtonEmail, BorderLayout.EAST);
                    panelEmail.add(cancelButtonEmail, BorderLayout.SOUTH);

                    confirmButtonEmail.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            String address = textField.getText();
                            System.out.println("Before setting notificationRef: " + notificationRef.get());
                            notificationRef.set("이메일" + address);
                            requestStockReserve(stockName, trend, priceOfReserve, notificationRef.get());

                            System.out.println("After setting notificationRef: " + notificationRef.get());
                            frameEmail.dispose();
                        }
                    });

                    cancelButtonEmail.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            notificationRef.set("cancel");
                            frameEmail.dispose();
                        }
                    });

                    frameEmail.getContentPane().add(panelEmail);
                    frameEmail.pack();
                    frameEmail.setVisible(true);
                }

                // 예약 창 끄기
                reservationFrame.dispose();
            });

            cancelButton.addActionListener(cancelEvent -> {
                reservationFrame.dispose();
            });

            reservationPanel.add(increaseRadioButton);
            reservationPanel.add(decreaseRadioButton);
            reservationPanel.add(priceLabel);
            reservationPanel.add(priceTextField);
            reservationPanel.add(alarmLabel);
            reservationPanel.add(alarmMethodLabel);
            reservationPanel.add(chatRadioButton);
            reservationPanel.add(emailRadioButton);
            reservationPanel.add(confirmButton);
            reservationPanel.add(cancelButton);

            reservationFrame.add(reservationPanel);
            reservationFrame.setVisible(true);
        });

        infoPanel.add(nameLabel, BorderLayout.CENTER);
        infoPanel.add(reservationButton, BorderLayout.EAST);

        panel.add(infoPanel, BorderLayout.NORTH);

        try {
            Image image = ImageIO.read(new java.net.URL(imageUrl));

            JLabel imageLabel = new JLabel(new ImageIcon(image));
            panel.add(imageLabel, BorderLayout.WEST);
        } catch (IOException e) {
            showMessage("\n주식 정보를 가져오는 중 오류: " + e.getMessage());
        }

        stockInfoFrame.add(panel);
        stockInfoFrame.setVisible(true);
    }

    private void displayRate(String country, String price, String imageUrl) {
        JFrame rateInfoFrame = new JFrame(country + " 환율 정보");
        rateInfoFrame.setSize(720, 400);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        JLabel nameLabel = new JLabel("     국가: " + country  + "                         현재 가격: " + price);
        panel.add(nameLabel, BorderLayout.NORTH);
        nameLabel.setFont(new Font("default", Font.PLAIN, 20));

        try {
            Image image = ImageIO.read(new java.net.URL(imageUrl));
            JLabel imageLabel = new JLabel(new ImageIcon(image));
            panel.add(imageLabel, BorderLayout.CENTER);
        } catch (IOException e) {
            showMessage("\n환율 정보를 가져오는 중 오류: " + e.getMessage());
        }

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());

        JLabel amountLabel = new JLabel("금액: ");
        amountLabel.setFont(new Font("default", Font.PLAIN, 20));
        bottomPanel.add(amountLabel, BorderLayout.WEST);

        JTextField amountInput = new JTextField();
        bottomPanel.add(amountInput, BorderLayout.CENTER);

        JLabel resultLabel = new JLabel("환전 결과: ");
        resultLabel.setFont(new Font("default", Font.PLAIN, 20));
        bottomPanel.add(resultLabel, BorderLayout.SOUTH);

        JButton button = new JButton("확인");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                String amountS = amountInput.getText();
                double amount = Double.parseDouble(amountS);
                double rate = Double.parseDouble(price);
                double convertedAmount = amount * rate;
                resultLabel.setText("환전 결과: " + convertedAmount);
            }
        });
        bottomPanel.add(button, BorderLayout.EAST);

        panel.add(bottomPanel, BorderLayout.SOUTH);

        rateInfoFrame.add(panel);
        rateInfoFrame.setVisible(true);
    }

    private void sendFile(File file) {
        try {
            // 파일 이름과 크기 전송
            String fileName = file.getName();
            long fileSize = file.length();
            output.writeObject("파일 전송 시작");
            output.writeObject(fileName);
            output.writeObject(fileSize);

            // 파일 데이터 전송
            FileInputStream fileInput = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fileInput.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
            output.flush();
            fileInput.close();

            showMessage("\n파일 전송 완료: " + fileName);

        } catch (IOException e) {
            showMessage("\n파일 전송 중 오류: " + e.getMessage());
        }
    }

    private void whileChatting() throws IOException {
        String message = "연결됨";
        sendMessage(message);
        ableToType(true);
        while(true) {
            try {
                message = (String) input.readObject();

                if (message.startsWith("파일 전송")) {
                    receiveFile();
                    continue;  // 파일 전송 후 다시 반복문의 처음으로 이동하여 메시지를 수신할 수 있도록 함
                }
                if (message.startsWith("price")) {
                    message=message.replace("price: ","");
                    if(!message.equals("notFound")){
                        String[] parts = message.split("&&&");
                        if (parts.length == 3) {
                            String name = parts[0];
                            String price = parts[1];
                            String link = parts[2];
                            displayStockInfo(name,price,link);
                        } else {
                            // 분리된 문자열이 3개가 아닌 경우 예외 처리
                            JOptionPane.showMessageDialog(this, "다른 주식을 입력해주세요.", "Error",
                                    JOptionPane.ERROR_MESSAGE);
                            System.out.println("errS");
                        }
                        message="현재가격: " + parts[1];
                    }
                    else{
                        JOptionPane.showMessageDialog(this, "다른 주식을 입력해주세요.", "Error",
                                JOptionPane.ERROR_MESSAGE);
                        message = "";
                    }

                    continue;
                }
                if (message.startsWith("rate")) {
                    message=message.replace("rate: ","");
                    if(!message.equals("notFound")){
                        String[] parts = message.split("&&&");
                        if (parts.length == 3) {
                            String name = parts[0];
                            String price = parts[1];
                            String link = parts[2];
                            displayRate(name,price,link);
                        } else {
                            // 분리된 문자열이 3개가 아닌 경우 예외 처리
                            showMessage("\n정보를 가져오는 중 오류가 발생했습니다.");
                            JOptionPane.showMessageDialog(this, "다른 국가를 입력해주세요.", "Error",
                                    JOptionPane.ERROR_MESSAGE);
                            System.out.println("errR");
                            System.out.println(message);

                        }
                        message="현재가격: " + parts[1];
                    }
                    else{
                        JOptionPane.showMessageDialog(this, "다른 국가를 입력해주세요.", "Error",
                                JOptionPane.ERROR_MESSAGE);
                        message = "";
                    }
                    continue;
                }
                if (message.equals("서버 - quit")) {
                    System.out.println(message);
                    break;
                }
                showMessage("\n" + message);
            } catch (ClassNotFoundException classNotFoundException) {
                showMessage("\n알 수 없는 객체를 받았습니다.");
            }
        }
        closeConnection(); // 연결 종료
    }

    // 클라이언트 실행
    public void startRunning() {
        try {
            connectToServer();
            setupStreams();
            whileChatting();
        } catch (EOFException eofException) {
            showMessage("\n서버와 연결이 끊어졌습니다.\n");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } finally {
            closeConnection();
        }
    }

    private void receiveFile() {
        try {
            // 파일 이름과 크기 수신
            String fileName = (String) input.readObject();
            long fileSize = (long) input.readObject();

            // 파일 데이터 수신
            FileOutputStream fileOutput = new FileOutputStream(fileName);
            byte[] buffer = new byte[1024];
            int bytesRead;
            long totalBytesRead = 0;
            while (totalBytesRead < fileSize) {
                bytesRead = input.read(buffer);
                fileOutput.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
            }
            fileOutput.close();

            // 파일 저장 대화 상자 표시
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setSelectedFile(new File(fileName));
            int choice = fileChooser.showSaveDialog(null);

            if (choice == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                File savedFile;
                if (!selectedFile.getName().endsWith(fileName.substring(fileName.lastIndexOf(".")))) {
                    String newFileName = selectedFile.getAbsolutePath() + fileName.substring(fileName.lastIndexOf("."));
                    savedFile = new File(newFileName);
                } else {
                    savedFile = selectedFile;
                }

                File receivedFile = new File(fileName);
                if (receivedFile.renameTo(savedFile)) {
                    showMessage("\n파일 저장 완료: " + savedFile.getAbsolutePath());
                } else {
                    showMessage("\n파일 저장 실패");
                }
            } else {
                showMessage("\n파일 저장이 취소되었습니다.");
            }

            showMessage("\n파일 수신 완료: " + fileName);
        } catch (IOException | ClassNotFoundException e) {
            showMessage("\n파일 수신 중 오류: " + e.getMessage());
        }
    }

    // 서버에 연결
    private void connectToServer() throws IOException {
        showMessage("연결 중...\n");
        connection = new Socket(InetAddress.getByName(serverIP), 12345);
        showMessage("연결됨: " + connection.getInetAddress().getHostName());
    }

    // 스트림 설정
    private void setupStreams() throws IOException {
        output = new ObjectOutputStream(connection.getOutputStream());
        output.flush();
        input = new ObjectInputStream(connection.getInputStream());
        showMessage("\n스트림 설정 완료\n");
    }

    // 연결 종료 및 스트림 닫기
    private void closeConnection() {
        showMessage("\n연결 종료\n");
        ableToType(false);
        try {
            output.close();
            input.close();
            connection.close();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    // 메시지 전송
    private void sendMessage(String message) {
        try {
            output.writeObject("클라이언트 - " + message);
            output.flush();
            showMessage("\n클라이언트 - " + message);
        } catch (IOException ioException) {
            chatWindow.append("\n전송 실패");
        }
    }

    // 채팅 창 업데이트
    private void showMessage(final String text) {
        SwingUtilities.invokeLater(() -> {
            chatWindow.append(text);
        });
    }

    // 사용자가 메시지를 입력할 수 있는지 여부 설정
    private void ableToType(final boolean tof) {
        SwingUtilities.invokeLater(() -> {
            userText.setEditable(tof);
        });
    }
}
