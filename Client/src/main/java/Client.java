import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class Client extends Thread {
    private Selector selector;
    private SocketChannel socketChannel;
    private volatile boolean stop;
    SocketChannel channel;

    public Client() throws IOException {
        selector = Selector.open();
        socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);

    }

    public static void main(String[] args) {
        Client client = null;
        try {
            client = new Client();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        client.start();
        System.out.println("Client exist");
    }

    public void run() {
        try {
            boolean connected = socketChannel.connect(new InetSocketAddress("localhost", 9000));
            if (connected) {
                socketChannel.register(selector, SelectionKey.OP_WRITE);
            } else {
                socketChannel.register(selector, SelectionKey.OP_CONNECT);
            }
            while (true) {
                selector.select();
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();
                    iterator.remove();
                    handler(selectionKey);
                }
            }
        } catch (ClosedChannelException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handler(SelectionKey key) throws IOException {
        if (key.isValid()) {
            channel = (SocketChannel) key.channel();
            if (key.isConnectable()) {
                if (channel.finishConnect()) {
                    channel.register(selector, SelectionKey.OP_WRITE);
                }
            }
            if (key.isReadable()) {
                ByteBuffer readBuffer = ByteBuffer.allocate(256);
                int readCount = channel.read(readBuffer);
                if (readCount > 0) {
                    readBuffer.flip();
                    byte[] bytes = new byte[readBuffer.remaining()];
                    readBuffer.get(bytes);
                    System.out.println("Receiving message: " + new String(bytes, "UTF-8"));
                } else {
                    key.cancel();
                    channel.close();
                }
                channel.register(selector, SelectionKey.OP_WRITE);
            }
            if (key.isWritable()) {
                BufferedReader localReader = new BufferedReader(new InputStreamReader(System.in));
                String msg = null;
                if (((msg = localReader.readLine()) != null)) {
                    ByteBuffer writeBuffer = ByteBuffer.allocate(msg.getBytes().length);
                    writeBuffer.put(msg.getBytes());
                    writeBuffer.flip();
                    socketChannel.write(writeBuffer);
                    socketChannel.register(selector, SelectionKey.OP_READ);
                }
            }
        }
    }

}