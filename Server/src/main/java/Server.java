import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class Server {

    private volatile boolean stop;

    public static void main(String[] args) throws IOException {
        new Server().start();
    }

    private void start() throws IOException {
        Selector selector = Selector.open();
        ServerSocketChannel serverSocet = ServerSocketChannel.open();
        serverSocet.socket().bind(new InetSocketAddress("localhost", 9000));
        serverSocet.configureBlocking(false);
        serverSocet.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("Server Started. Port 9000");

        while (true) {
            selector.select();
            System.out.println("New selector event");
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey selectionKey = iterator.next();
                if (selectionKey.isAcceptable()) {
                    System.out.println("new selector acceptable event");
                    register(selector, serverSocet);
                }
                if (selectionKey.isReadable()) {
                    System.out.println("new selector readable event");
                    readMessage(selectionKey);
                }
                iterator.remove();
            }
        }
    }

    public void register(Selector selector, ServerSocketChannel serverSocket) throws IOException {
        SocketChannel client = serverSocket.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);
        System.out.println("Next client is connected");
    }

    public void readMessage(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(256);
        /*client.write(byteBuffer.flip());*/
        int readCount = client.read(byteBuffer);
        if (readCount > 0) {
            byteBuffer.flip();
            byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);
            String receiveMsg = new String(bytes, "UTF-8");
            if ("/n".equals(receiveMsg)) {
                stop();
                return;
            }
            String responseMessage = receiveMsg;
            System.out.println("New message: " + responseMessage + "Thread name: " + Thread.currentThread().getName());
            ByteBuffer responseBuffer = ByteBuffer.allocate(responseMessage.getBytes().length);
            responseBuffer.put(responseMessage.getBytes());
            responseBuffer.flip();
            client.write(responseBuffer);
        }
    }

    private void stop() {
        this.stop = true;
    }
}
