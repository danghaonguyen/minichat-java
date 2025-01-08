import javax.swing.*; // Thư viện để tạo giao diện người dùng
import javax.swing.border.EmptyBorder; // Để tạo khoảng cách giữa các thành phần
import java.awt.*; // Thư viện hỗ trợ thiết kế giao diện
import java.awt.event.ActionEvent; // Sự kiện hành động, dùng để lắng nghe nút bấm
import java.io.*; // Thư viện hỗ trợ xử lý I/O (đọc, ghi dữ liệu)
import java.net.Socket; // Thư viện hỗ trợ kết nối mạng qua socket

public class ChatUI extends JFrame { // Lớp chính kế thừa JFrame để tạo cửa sổ giao diện
    private JPanel chatPanel; // Khu vực hiển thị các tin nhắn
    private JTextField messageField; // Ô nhập tin nhắn
    private JButton sendButton; // Nút gửi tin nhắn
    private BufferedReader bufferedReader; // Đọc dữ liệu từ server
    private BufferedWriter bufferedWriter; // Gửi dữ liệu tới server
    private Socket socket; // Socket để kết nối tới server
    private String username; // Tên người dùng

    public ChatUI(String title, String username, String host, int port) { // Constructor, khởi tạo giao diện
        super(title); // Đặt tiêu đề cho cửa sổ
        this.username = username; // Lưu tên người dùng

        // Thiết lập giao diện chính
        setLayout(new BorderLayout()); // Sử dụng bố cục BorderLayout
        setSize(600, 700); // Đặt kích thước cửa sổ
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Đóng chương trình khi cửa sổ bị đóng

        // Tiêu đề hiển thị tên người dùng
        JLabel titleLabel = new JLabel("" + username, SwingConstants.CENTER); // Tạo nhãn hiển thị tên người dùng
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16)); // Đặt phông chữ
        add(titleLabel, BorderLayout.NORTH); // Đặt tiêu đề ở đầu cửa sổ

        // Khu vực hiển thị tin nhắn
        chatPanel = new JPanel(); // Tạo panel chứa các tin nhắn
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS)); // Sắp xếp các tin nhắn theo chiều dọc
        JScrollPane scrollPane = new JScrollPane(chatPanel); // Thêm thanh cuộn cho khu vực tin nhắn
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED); // Hiển thị thanh cuộn khi cần
        add(scrollPane, BorderLayout.CENTER); // Đặt khu vực này ở giữa cửa sổ

        // Khu vực nhập tin nhắn
        messageField = new JTextField(); // Tạo ô nhập tin nhắn
        sendButton = new JButton("Gửi"); // Tạo nút gửi tin nhắn
        JPanel inputPanel = new JPanel(new BorderLayout()); // Tạo panel chứa ô nhập và nút gửi
        inputPanel.add(messageField, BorderLayout.CENTER); // Đặt ô nhập ở giữa
        inputPanel.add(sendButton, BorderLayout.EAST); // Đặt nút gửi ở bên phải
        add(inputPanel, BorderLayout.SOUTH); // Đặt khu vực nhập ở cuối cửa sổ

        // Kết nối tới server
        try {
            socket = new Socket(host, port); // Kết nối tới server qua địa chỉ và cổng
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream())); // Khởi tạo để đọc dữ liệu từ server
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())); // Khởi tạo để gửi dữ liệu tới server

            // Gửi tên người dùng tới server
            bufferedWriter.write(username); // Gửi tên người dùng
            bufferedWriter.newLine(); // Kết thúc dòng
            bufferedWriter.flush(); // Đảm bảo dữ liệu được gửi đi ngay

            // Lắng nghe tin nhắn từ server
            listenForMessages(); // Gọi hàm lắng nghe tin nhắn
        } catch (IOException e) { // Xử lý nếu không kết nối được tới server
            JOptionPane.showMessageDialog(this, "Không thể kết nối đến server: " + e.getMessage()); // Hiển thị thông báo lỗi
            closeEverything(); // Đóng các tài nguyên
        }

        // Sự kiện gửi tin nhắn
        sendButton.addActionListener(this::sendMessage); // Gắn sự kiện khi nút gửi được bấm
        messageField.addActionListener(this::sendMessage); // Gửi tin nhắn khi nhấn Enter

        setVisible(true); // Hiển thị cửa sổ giao diện
    }

    private void sendMessage(ActionEvent e) { // Hàm gửi tin nhắn
        String message = messageField.getText().trim(); // Lấy nội dung từ ô nhập tin nhắn
        if (!message.isEmpty()) { // Nếu tin nhắn không rỗng
            try {
                bufferedWriter.write(message); // Gửi tin nhắn tới server
                bufferedWriter.newLine(); // Kết thúc dòng
                bufferedWriter.flush(); // Đảm bảo dữ liệu được gửi đi ngay
                addMessageToChat(username, message, true); // Hiển thị tin nhắn trên giao diện
                messageField.setText(""); // Xóa nội dung trong ô nhập
            } catch (IOException ex) { // Xử lý lỗi khi gửi tin nhắn
                JOptionPane.showMessageDialog(this, "Lỗi khi gửi tin nhắn: " + ex.getMessage()); // Hiển thị lỗi
                closeEverything(); // Đóng các tài nguyên
            }
        }
    }

    private void listenForMessages() { // Lắng nghe tin nhắn từ server
        new Thread(() -> { // Tạo luồng riêng để lắng nghe
            String message;
            try {
                while ((message = bufferedReader.readLine()) != null) { // Đọc từng dòng tin nhắn từ server
                    if (message.startsWith("[Thông báo]")) { // Nếu là thông báo
                        addNotificationToChat(message); // Hiển thị thông báo
                    } else { // Nếu là tin nhắn từ người dùng khác
                        String[] parts = message.split(": ", 2); // Tách tên người gửi và nội dung
                        if (parts.length == 2) { // Nếu tin nhắn hợp lệ
                            addMessageToChat(parts[0], parts[1], false); // Hiển thị tin nhắn
                        }
                    }
                }
            } catch (IOException e) { // Xử lý lỗi khi kết nối bị mất
                JOptionPane.showMessageDialog(this, "Kết nối đến server đã mất: " + e.getMessage()); // Hiển thị lỗi
                closeEverything(); // Đóng các tài nguyên
            }
        }).start(); // Bắt đầu luồng
    }

    private void addMessageToChat(String sender, String message, boolean isOwnMessage) { // Hiển thị tin nhắn
        JPanel messagePanel = new JPanel(new BorderLayout()); // Tạo panel cho tin nhắn
        messagePanel.setBorder(new EmptyBorder(5, 10, 5, 10)); // Tạo khoảng cách cho tin nhắn

        JLabel messageLabel = new JLabel("<html><b>" + sender + ":</b> " + message + "</html>"); // Tạo nhãn hiển thị tin nhắn
        messageLabel.setFont(new Font("Arial", Font.PLAIN, 14)); // Đặt phông chữ

        if (isOwnMessage) { // Nếu là tin nhắn của chính mình
            messagePanel.add(messageLabel, BorderLayout.EAST); // Đặt tin nhắn ở bên phải
        } else { // Nếu là tin nhắn từ người khác
            messagePanel.add(messageLabel, BorderLayout.WEST); // Đặt tin nhắn ở bên trái
        }

        chatPanel.add(messagePanel); // Thêm tin nhắn vào giao diện
        chatPanel.revalidate(); // Làm mới giao diện
        chatPanel.repaint(); // Vẽ lại giao diện
    }

    private void addNotificationToChat(String notification) { // Hiển thị thông báo
        JPanel notificationPanel = new JPanel(new BorderLayout()); // Tạo panel cho thông báo
        notificationPanel.setBorder(new EmptyBorder(10, 0, 10, 0)); // Tạo khoảng cách cho thông báo

        JLabel notificationLabel = new JLabel("<html><i>" + notification + "</i></html>"); // Tạo nhãn hiển thị thông báo
        notificationLabel.setHorizontalAlignment(SwingConstants.CENTER); // Canh giữa

        notificationPanel.add(notificationLabel, BorderLayout.CENTER); // Thêm thông báo vào panel
        chatPanel.add(notificationPanel); // Thêm panel vào giao diện
        chatPanel.revalidate(); // Làm mới giao diện
        chatPanel.repaint(); // Vẽ lại giao diện
    }

    private void closeEverything() { // Đóng tất cả tài nguyên
        try {
            if (bufferedReader != null) bufferedReader.close(); // Đóng luồng đọc
            if (bufferedWriter != null) bufferedWriter.close(); // Đóng luồng ghi
            if (socket != null) socket.close(); // Đóng socket
        } catch (IOException e) { // Xử lý lỗi khi đóng tài nguyên
            e.printStackTrace();
        } finally {
            System.exit(0); // Thoát chương trình
        }
    }
}
