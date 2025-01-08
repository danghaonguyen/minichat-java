import java.io.*; // Thư viện xử lý I/O (đọc, ghi dữ liệu)
import java.net.*; // Thư viện hỗ trợ kết nối mạng qua socket
import java.util.*; // Thư viện cung cấp cấu trúc dữ liệu List

public class Server { 
    private ServerSocket serverSocket; // Socket để lắng nghe kết nối từ các client
    private static final List<ClientHandler> clients = new ArrayList<>(); // Danh sách quản lý các client đã kết nối

    public Server(ServerSocket serverSocket) { 
        this.serverSocket = serverSocket; // Gán đối tượng serverSocket được truyền vào
    }

    public void startServer() { // Phương thức để khởi động server
        System.out.println("Server is running..."); // Thông báo khi server bắt đầu chạy
        try {
            while (!serverSocket.isClosed()) { // Vòng lặp liên tục chờ kết nối mới từ client
                Socket socket = serverSocket.accept(); // Chấp nhận kết nối từ client
                System.out.println("A new client has connected!"); // Thông báo khi có client mới

                ClientHandler clientHandler = new ClientHandler(socket); // Tạo một ClientHandler để quản lý client
                clients.add(clientHandler); // Thêm client vào danh sách quản lý

                new Thread(clientHandler).start(); // Tạo luồng riêng để xử lý client
            }
        } catch (IOException e) { // Bắt lỗi khi server gặp vấn đề
            System.out.println("Server error: " + e.getMessage()); // In lỗi ra console
        }
    }

    public static void broadcastMessage(String message, ClientHandler sender) { // Gửi tin nhắn đến tất cả client
        for (ClientHandler client : clients) { // Lặp qua danh sách client
            if (client != sender) { // Bỏ qua client gửi tin
                client.sendMessage(message); // Gửi tin nhắn đến các client còn lại
            }
        }
    }

    public static void removeClient(ClientHandler clientHandler) { // Xóa client khỏi danh sách khi ngắt kết nối
        clients.remove(clientHandler); // Xóa client
        System.out.println(clientHandler.getUsername() + " has disconnected."); // Thông báo client đã ngắt kết nối
        broadcastMessage("[Thông báo] " + clientHandler.getUsername() + " đã rời phòng chat.", null); // Gửi thông báo đến các client khác
    }

    public static void main(String[] args) throws IOException { // Điểm bắt đầu chương trình
        Server server = new Server(new ServerSocket(1236)); // Tạo server với cổng 1236
        server.startServer(); // Khởi động server
    }
}

class ClientHandler implements Runnable { // Lớp xử lý từng client, mỗi client là một luồng riêng
    private final Socket socket; // Socket kết nối giữa server và client
    private final BufferedReader reader; // Luồng đọc dữ liệu từ client
    private final BufferedWriter writer; // Luồng ghi dữ liệu gửi tới client
    private final String username; // Tên người dùng của client

    public ClientHandler(Socket socket) { // Constructor khởi tạo ClientHandler
        this.socket = socket; // Gán socket kết nối
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream())); // Đọc dữ liệu từ socket
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())); // Ghi dữ liệu vào socket

            username = reader.readLine(); // Đọc tên người dùng do client gửi
            System.out.println("Username: " + username); // Hiển thị tên người dùng
            Server.broadcastMessage("[Thông báo] " + username + " đã tham gia phòng chat!", this); // Gửi thông báo đến các client khác
        } catch (IOException e) { // Bắt lỗi khi xảy ra vấn đề với client
            throw new RuntimeException("Failed to initialize client handler", e); // Ném ngoại lệ khi không thể khởi tạo
        }
    }

    @Override
    public void run() { // Phương thức chính của luồng để xử lý client
        try {
            String message;
            while ((message = reader.readLine()) != null) { // Đọc tin nhắn từ client
                System.out.println(username + ": " + message); // Hiển thị tin nhắn trong console của server
                Server.broadcastMessage(username + ": " + message, this); // Gửi tin nhắn đến các client khác
            }
        } catch (IOException e) { // Bắt lỗi khi mất kết nối với client
            closeEverything(); // Đóng các tài nguyên liên quan đến client
        }
    }

    public void sendMessage(String message) { // Phương thức gửi tin nhắn tới client
        try {
            writer.write(message); // Ghi tin nhắn vào luồng
            writer.newLine(); // Thêm ký tự xuống dòng
            writer.flush(); // Đẩy dữ liệu ngay lập tức
        } catch (IOException e) { // Bắt lỗi khi không thể gửi tin nhắn
            closeEverything(); // Đóng tài nguyên nếu lỗi xảy ra
        }
    }

    public String getUsername() { // Phương thức lấy tên người dùng
        return username;
    }

    private void closeEverything() { // Phương thức đóng tất cả tài nguyên liên quan đến client
        try {
            Server.removeClient(this); // Gỡ client khỏi danh sách quản lý
            if (reader != null) reader.close(); // Đóng luồng đọc
            if (writer != null) writer.close(); // Đóng luồng ghi
            if (socket != null) socket.close(); // Đóng socket
        } catch (IOException e) { // Kiểm tra lỗi khi đóng tài nguyên
            System.out.println("Error closing resources: " + e.getMessage()); // Hiển thị lỗi
        }
    }
}
