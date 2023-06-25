import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Objects;
import javax.swing.*;
import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

public class Server extends JFrame {
    private JTextField userText;
    private JTextArea chatWindow;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private ServerSocket server;
    private Socket connection;

    public Server() {
        super("채팅 프로그램 - 서버");
        userText = new JTextField();
        userText.setEditable(false);
        userText.setBackground(Color.LIGHT_GRAY);
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

    private void sendInfo(String stockName){
        String information = getInfo(stockName);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String message = "price: " + information;
                    output.writeObject(message);
                    output.flush();
                } catch (IOException e) {
                    System.out.println("error");
                }
            }
        });

        thread.start();
    }

    private void sendInfoRate(String country) throws IOException {
        String information = getInfoRate(country);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String message = "rate: " + information;
                    output.writeObject(message);
                    output.flush();
                } catch (IOException e) {
                    System.out.println("error");
                }
            }
        });

        thread.start();
    }

    private String getCountryCode(String country) {
        if (country.equalsIgnoreCase("일본")) {
            return "JPY";
        } else if (country.equalsIgnoreCase("미국")) {
            return "USD";
        } else if (country.equalsIgnoreCase("중국")) {
            return "CNY";
        }else if (country.equalsIgnoreCase("유럽 연합")) {
            return "EUR";
        }else if (country.equalsIgnoreCase("유럽연합")) {
            return "EUR";
        }else if (country.equalsIgnoreCase("홍콩")) {
            return "HKD";
        }else if (country.equalsIgnoreCase("대만")) {
            return "YWD";
        }else if (country.equalsIgnoreCase("영국")) {
            return "GBP";
        }else if (country.equalsIgnoreCase("캐나다")) {
            return "CAD";
        }else if (country.equalsIgnoreCase("스위스")) {
            return "CHF";
        }else if (country.equalsIgnoreCase("스웨덴")) {
            return "SEK";
        }else if (country.equalsIgnoreCase("호주")) {
            return "AUD";
        }else if (country.equalsIgnoreCase("뉴질랜드")) {
            return "NZD";
        }else if (country.equalsIgnoreCase("멕시코")) {
            return "MXN";
        }
        return null;
    }

    private String getImageUrl(String countryCode) {
        String url = "https://finance.naver.com/marketindex/exchangeDetail.naver?marketindexCd=FX_" + countryCode+"KRW";
        System.out.println(url);
        return url;
    }

    private double getExchangeRate(String countryCode) throws IOException {
        String url = "https://finance.naver.com/marketindex/exchangeList.naver";
        Document doc = Jsoup.connect(url).get();
        Elements elements = doc.select("td.tit > a");

        for (Element element : elements) {
            if (element.attr("href").contains(countryCode)) {
                String rateStr = element.parent().parent().select("td.sale").text();
                rateStr = rateStr.replace(",", ""); // 콤마 처리 못함-->rateStr 에서 콤마 지우기
                return Double.parseDouble(rateStr);
            }
        }

        throw new IOException("국가에 대한 환율을 찾을 수 없습니다: " + countryCode);
    }

    private String getInfoRate(String country) throws IOException {
        String countryCode = getCountryCode(country);
        if(countryCode==null){
            return "notFound";
        }
        double rate = getExchangeRate(countryCode);

        String url=getImageUrl(countryCode);
        Document stockInfoDoc = Jsoup.connect(url).get();
        Element imgElement = stockInfoDoc.selectFirst("img[src~=.*chart/marketindex/area/month3/.*]");
        String imageUrl = imgElement.attr("src");

        return country+"&&&"+rate+"&&&"+imageUrl;
    }

    private String getInfo(String stockName){
        String encodedStockName = null;
        try {
            encodedStockName = URLEncoder.encode(stockName, "EUC-KR");
            System.out.println(encodedStockName);
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
        String url = "https://finance.naver.com/search/searchList.naver?query=" + encodedStockName;
        System.out.println(url);
        try {
            Document doc = Jsoup.connect(url).get();
            Element stockLink = doc.select("td.tit a").first();

            System.out.println(stockLink);

            if (stockLink != null) {
                String href = stockLink.attr("href");
                String stockCode = href.substring(href.lastIndexOf("=") + 1);
                String stockInfoUrl = "https://finance.naver.com/item/main.nhn?code=" + stockCode;
                Document stockInfoDoc = Jsoup.connect(stockInfoUrl).get();
                Element stockTitle = stockInfoDoc.select("div.wrap_company h2").first();
                Element currentPrice = stockInfoDoc.select("p.no_today span.blind").first();
                Element stockScreenshot = stockInfoDoc.select("div.chart img").first();
                System.out.println(stockScreenshot);
                String screenshotUrl = stockScreenshot.absUrl("src");
                String title = stockTitle.text(); //주식 이름
                String price = currentPrice.text(); //현재 가격
                return (title +"&&&" +price+"&&&"+screenshotUrl);
            } else {
                return "notFound";
            }
        } catch (IOException ex) {
            return "notFound";
        }
    }

    private String getInfoReserve(String stockReserveInfo){
        String[] parts = stockReserveInfo.split("&&&");
        String stockName = null;
        String trend,priceOfReserve,notification;
        if (parts.length == 4) {
            stockName = parts[0];
            trend = parts[1];
            priceOfReserve = parts[2];
            notification = parts[3];
        } else {
            // 분리된 문자열이 4개가 아닌 경우 예외 처리
            System.out.println("errS");
        }
        String encodedStockName = null;
        try {
            encodedStockName = URLEncoder.encode(stockName, "EUC-KR");
            System.out.println(encodedStockName);
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
        String url = "https://finance.naver.com/search/searchList.naver?query=" + encodedStockName;
        System.out.println(url);
        try {
            Document doc = Jsoup.connect(url).get();
            Element stockLink = doc.select("td.tit a").first();

            System.out.println(stockLink);

            if (stockLink != null) {
                String href = stockLink.attr("href");
                String stockCode = href.substring(href.lastIndexOf("=") + 1);
                String stockInfoUrl = "https://finance.naver.com/item/main.nhn?code=" + stockCode;
                Document stockInfoDoc = Jsoup.connect(stockInfoUrl).get();
                Element stockTitle = stockInfoDoc.select("div.wrap_company h2").first();
                Element currentPrice = stockInfoDoc.select("p.no_today span.blind").first();
                Element stockStatus = stockInfoDoc.select("em.no_up span.ico").first();
                Element upSpan = stockInfoDoc.select("span.ico.up").first();
                Element emElement = upSpan.parent();
                Elements stockNumbers = emElement.select("span[class^=no]");

                String status = stockStatus.text();
                String numberOfR = stockNumbers.text();
                numberOfR = numberOfR.replace(" ","");
                numberOfR = numberOfR.replace(",","");
//                System.out.println(returnReserveResult( (status +"&&&" +numberOfR) ,stockReserveInfo));
                startExecutionThread((status+"&&&"+numberOfR), stockReserveInfo);
                return (status +"&&&" +numberOfR);
            } else {
                return "notFound";
            }
        } catch (IOException ex) {
            return "notFound";
        }
    }
    
    private void startExecutionThread(String currentStatus, String reserveStatus) {
        Thread executionThread = new Thread(() -> {
            while (true) {
                boolean result = returnReserveResult(currentStatus, reserveStatus);
                if (result) {
                    // 알람 보내기
                    String[] partsReserve = reserveStatus.split("&&&");
                    String notification = null;

                    if (partsReserve.length == 4) {
                        notification = partsReserve[3];
                        //partsCurrent[0]는 주식이름, partsCurrent[1]은 trend, partsCurrent[2]은 trendPrice
                    } else {
                        // 분리된 문자열이 4개가 아닌 경우 예외 처리
                        System.out.println("errSs3");
                    }
                    if(notification.equals("채팅")){
                        alarmAsChat(currentStatus,reserveStatus);
                        System.out.println("알람 보내기-채팅");
                    }
                    else{
                        alarmAsEmail(currentStatus,reserveStatus);
                        System.out.println("알람 보내기-이메일");
                    }
                    break; // 루프 끝내고 스레드 닫기
                }
                System.out.println("알람 안보내고 30초마다 검사");
                try {
                    Thread.sleep(30000); // 30초에 한 번씩 크롤링해서 알람 조건에 맞는지 검사
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break; // 루프 끝내고 스레드 닫기
                }
            }
        });

        executionThread.start(); // 스레드 시작
    }

    //"상승&&&상승가격" 혹은 "하락&&하락가격"-->reserve 조건에 일치하면 true 리턴
    private boolean returnReserveResult(String currentStatus, String reserveStatus){
        String[] partsCurrent = currentStatus.split("&&&");
        String[] partsReserve = reserveStatus.split("&&&");

        String trendCurrent = null;
        String priceOfTrendC;
        String trendReserve = null;
        String priceOfTrendR;

        if (partsCurrent.length == 2) {
            trendCurrent = partsCurrent[0];
            priceOfTrendC = partsCurrent[1];
        } else {
            // 분리된 문자열이 2개가 아닌 경우 예외 처리
            priceOfTrendC = "0";
            System.out.println("errSs1");
        }
        if (partsReserve.length == 4) {
            trendReserve = partsReserve[1];
            priceOfTrendR = partsReserve[2];
            //partsCurrent[0]는 주식이름, partsCurrent[3]은 알림 방식
        } else {
            // 분리된 문자열이 4개가 아닌 경우 예외 처리
            priceOfTrendR = "0";
            System.out.println("errSs2");
        }
        if(Objects.equals(trendCurrent, trendReserve)){
            double trendCurrentDouble = Double.parseDouble(priceOfTrendC);
            double trendReserveDouble = Double.parseDouble(priceOfTrendR);
            System.out.println("currentDouble:" + trendCurrentDouble);
            System.out.println("reserveDouble:" + trendReserveDouble);
            return (trendReserveDouble <= trendCurrentDouble);
        }
        return false;
    }

    private void alarmAsChat(String currentStatus, String reserveStatus) {

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String[] partsCurrent = currentStatus.split("&&&");
                String[] partsReserve = reserveStatus.split("&&&");

                String trendCurrent = null;
                String priceOfTrendC = null;
                String trendReserve = null;
                String priceOfTrendR = null;
                String stockName = null;
                String notification = null;

                if (partsCurrent.length == 2) {
                    trendCurrent = partsCurrent[0];
                    priceOfTrendC = partsCurrent[1];
                } else {
                    // 분리된 문자열이 2개가 아닌 경우 예외 처리
                    System.out.println("errSs1");
                }
                if (partsReserve.length == 4) {
                    stockName = partsReserve[0];
                    trendReserve = partsReserve[1];
                    priceOfTrendR = partsReserve[2];
                    notification = partsReserve[3];
                } else {
                    // 분리된 문자열이 4개가 아닌 경우 예외 처리
                    System.out.println("errSs2");
                }
                try {
                    String message = "예약 알림!! " + stockName + "이(가) 예약하신 가격인 " + priceOfTrendR + "원 이상  "+
                            trendReserve + "했습니다. \n이것은 " +  notification + " 방식 알림입니다. \n현재 "
                            + trendCurrent +"한 가격은 " + priceOfTrendC + "원입니다.";
                    output.writeObject(message);
                    output.flush();

                    showMessage("\n주식 예약 알림_채팅: " + stockName);

                } catch (IOException e) {
                    showMessage("\n주식 예약 알림 중 오류_채팅: " + e.getMessage());
                }
            }
        });

        thread.start();
    }

    private void alarmAsEmail(String currentStatus, String reserveStatus){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String[] partsCurrent = currentStatus.split("&&&");
                String[] partsReserve = reserveStatus.split("&&&");

                String trendCurrent = null;
                String priceOfTrendC = null;
                String trendReserve = null;
                String priceOfTrendR = null;
                String stockName = null;
                String notification = null;
                String EmailAddress = null;

                if (partsCurrent.length == 2) {
                    trendCurrent = partsCurrent[0];
                    priceOfTrendC = partsCurrent[1];
                } else {
                    // 분리된 문자열이 2개가 아닌 경우 예외 처리
                    System.out.println("errSs11");
                }
                if (partsReserve.length == 4) {
                    stockName = partsReserve[0];
                    trendReserve = partsReserve[1];
                    priceOfTrendR = partsReserve[2];
                    notification = partsReserve[3];
                    EmailAddress = notification.replace("이메일","");
                } else {
                    // 분리된 문자열이 4개가 아닌 경우 예외 처리
                    System.out.println("errSs22");
                }
                String message = "예약 알림!! " + stockName + "이(가) 예약하신 가격인 " + priceOfTrendR + "원 이상 "+
                        trendReserve + "했습니다. \n이것은 " +  notification + " 방식 알림입니다. \n현재 "
                        + trendCurrent +"한 가격은 " + priceOfTrendC + "원입니다.";
                // TODO: senderEmail-naver 입력하세요
                final String senderEmail = "";
                // TODO: senderPassword-naver 입력하세요
                final String senderPassword = "";
                // TODO: 네이버 메일에서 POP3/SMTP 설정을 사용함으로 변경하세요
                final String smtpHost = "smtp.naver.com";
                final String smtpPort = "465";

                try {
                    Properties props = new Properties();
                    props.put("mail.smtp.host", smtpHost);
                    props.put("mail.smtp.auth", "true");
                    props.put("mail.smtp.user", senderEmail);
                    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                    props.put("mail.smtp.socketFactory.fallback", "false");
                    props.put("mail.smtp.socketFactory.port", smtpPort);
                    props.put("mail.debug", "true");
                    Session session = Session.getDefaultInstance(props);
                    MimeMessage msg = new MimeMessage(session);
                    msg.setFrom(new InternetAddress(senderEmail));
                    InternetAddress addressTo = null;
                    addressTo = new InternetAddress(EmailAddress);
                    msg.setRecipient(javax.mail.Message.RecipientType.TO, addressTo);
                    msg.setSubject("예약 알림");
                    System.out.println(message+"!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
//                    msg.setContent(message, "text/html; charset=iso-8859-9");
                    msg.setContent(message, "text/html; charset=UTF-8");

                    Transport t = session.getTransport("smtp");
                    t.connect(senderEmail, senderPassword);
                    t.sendMessage(msg, msg.getAllRecipients());
                    t.close();
                } catch (Exception exc) {
                    exc.printStackTrace();
                }

                showMessage("\n주식 예약 알림_이메일: " + stockName); //서버에 나타나는 메시지임
            }
        });

        thread.start();
    }

    private void whileChatting() throws IOException {
        String message = "연결됨";
        sendMessage(message);
        ableToType(true);
        while(true) {
            try {
                message = (String) input.readObject();
                showMessage("\n" + message);

                if (message.startsWith("파일 전송")) {
                    receiveFile();
                    continue;
                }
                if (message.startsWith("stockName")) {
                    String stockName = message.replace("stockName: ", "");
                    sendInfo(stockName);
                    continue;
                }
                if (message.startsWith("StockReserve")) {
                    String stockReserveInfo = message.replace("StockReserve: ", "");
                    System.out.println(getInfoReserve(stockReserveInfo));
                    continue;
                }
                if (message.startsWith("country")) {
                    String country = message.replace("country: ", "");
                    sendInfoRate(country);
                    continue;
                }
                if (message.equals("클라이언트 - quit")) {
                    System.out.println(message);
                    break;
                }

            } catch (ClassNotFoundException classNotFoundException) {
                showMessage("\n알 수 없는 객체를 받았습니다.");
            }
        }

        closeConnection(); // 연결 종료
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

    // 서버 실행
    public void startRunning() {
        try {
            server = new ServerSocket(12345, 100);

            while (true) {
                try {
                    waitForConnection();
                    setupStreams();
                    whileChatting();
                } catch (EOFException e) {
                    showMessage("\n클라이언트가 연결을 종료했습니다.");
                } finally {
                    closeConnection();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 클라이언트와의 연결을 기다림
    private void waitForConnection() throws IOException {
        showMessage("연결을 기다리는 중...\n");
        connection = server.accept();
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
            output.writeObject("서버 - " + message);
            output.flush();
            showMessage("\n서버 - " + message);
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
