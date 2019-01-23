import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class ClientManager {
    public void run(String serverAddress,
                         short serverPort,
                         int architectureType,
                         int queryPerClient,
                         int arraySize,
                         int clientsNum,
                         int delta,
                         int[] res) {
        final Random rand = new Random();
        try (Socket sc = new Socket(serverAddress, 8082)) {//serverPort
            OutputStream os = sc.getOutputStream();
            os.write(architectureType);
            os.flush();
            os.write(queryPerClient);
            os.write(clientsNum);
            os.flush();
            os.close();
        } catch (IOException e) {
            System.out.println("Could not connect to host");
//            e.printStackTrace();
        }
//        System.out.println("Addres: " + serverAddress + "\nPort: " + serverPort + "\nArchType: " + architectureType +
//                "\nQueryPerCli: " + queryPerClient + "\nArraySize: " + arraySize + "\nClientNum: " + clientsNum + "\nDelta: " + delta);
        Thread[] th = new Thread[clientsNum];
        AtomicLong avgSortTime = new AtomicLong(0);
        AtomicLong avgReadToWrite = new AtomicLong(0);
        AtomicLong avgClientTime = new AtomicLong(0);
        if (architectureType != 2) {
            for (int i = 0; i < clientsNum; i++) {
                th[i] = new Thread(() -> {
                    long startClient = System.currentTimeMillis();
                    try (Socket sc = new Socket(serverAddress, serverPort)) {
                        try (InputStream is = sc.getInputStream(); OutputStream os = sc.getOutputStream()) {
                            int[] array = new int[arraySize];
                            for (int j = 0; j < queryPerClient; j++) {
                                Utils.randomArray(array);
                                MessageProtoc.Arr.Builder messageBuilder = MessageProtoc.Arr.newBuilder()
                                        .setNum(arraySize);
                                for (int c = 0; c < arraySize; c++) {
                                    messageBuilder.addData(array[c]);
                                }
                                MessageProtoc.Arr requestMessage = messageBuilder.build();
                                os.write(ByteBuffer.allocate(4).putInt(requestMessage.getSerializedSize()).array());
                                os.flush();
                                requestMessage.writeTo(os);
                                os.flush();
                                byte[] ansSz = new byte[4];
                                is.read(ansSz);
                                int answerMessageSize = ByteBuffer.wrap(ansSz).getInt();
                                byte[] answerBytes = new byte[answerMessageSize];
                                int bytesRead = is.read(answerBytes);
                                while (bytesRead < answerMessageSize) bytesRead += is.read(answerBytes, bytesRead, answerMessageSize - bytesRead);
                                MessageProtoc.Arr answerMessage = MessageProtoc.Arr.parseFrom(answerBytes);
                                Thread.sleep(delta);

                            }
                            byte[] buffInt = new byte[4];
                            is.read(buffInt);
                            int sortInc = ByteBuffer.wrap(buffInt).getInt();
                            is.read(buffInt);
                            int readWriteInc = ByteBuffer.wrap(buffInt).getInt();

                            long curSort = avgSortTime.get();
                            while (!avgSortTime.compareAndSet(curSort, curSort + sortInc))
                                curSort = avgSortTime.get();

                            long curWrite = avgReadToWrite.get();
                            while (!avgReadToWrite.compareAndSet(curWrite, curWrite + readWriteInc))
                                curWrite = avgReadToWrite.get();

                        } catch (InterruptedException e) {
                            System.out.println("waiting till next message were interrupted");
//                            e.printStackTrace();
                        }
                    } catch (IOException e) {
                        System.out.println("Connection to server problem");
                        e.printStackTrace();
                    }
                    long endClient = System.currentTimeMillis();
                    long increment = (endClient - startClient) / queryPerClient;
                    long curClient = avgClientTime.get();
                    while (!avgClientTime.compareAndSet(curClient, curClient + increment)) curClient = avgClientTime.get();
                });
                th[i].start();
            }
        } else {
            for (int i = 0; i < clientsNum; i++) {
                th[i] = new Thread(() -> {
                    try (SocketChannel sc = SocketChannel.open()) {
                        sc.connect(new InetSocketAddress(serverAddress, serverPort));
                        sc.configureBlocking(false);
                        try {
                            int[] array = new int[arraySize];
                            for (int j = 0; j < queryPerClient; j++) {
                                Utils.randomArray(array);
                                MessageProtoc.Arr.Builder messageBuilder = MessageProtoc.Arr.newBuilder()
                                        .setNum(arraySize);
                                for (int c = 0; c < arraySize; c++) {
                                    messageBuilder.addData(array[c]);
                                }
                                MessageProtoc.Arr requestMessage = messageBuilder.build();
                                ByteBuffer size = ByteBuffer.allocate(4);
                                size.putInt(requestMessage.getSerializedSize());
                                size.flip();
                                sc.write(size);
                                while (size.hasRemaining()) sc.write(size);
                                size.clear();
                                ByteBuffer buff = ByteBuffer.allocate(requestMessage.getSerializedSize());
                                buff.put(requestMessage.toByteArray());
                                buff.flip();
                                while (buff.hasRemaining()) {
                                    sc.write(buff);
                                }
                                buff.clear();
                                System.out.println("SENDED");
                                sc.read(size);
                                while (size.hasRemaining()) {
                                    sc.read(size);
                                }
                                size.flip();
                                int sz = size.getInt();
                                size.clear();
                                System.out.println("GET SIZE");
                                buff = ByteBuffer.allocate(sz);//if (buff.limit() != sz)
                                sc.read(buff);
                                while (buff.hasRemaining()) {
                                    sc.read(buff);
                                }
                                System.out.println("GET BUFF");
                                buff.flip();
//                                ByteString strBuff = ByteString.copyFrom(buff);
                                MessageProtoc.Arr answerMessage = MessageProtoc.Arr.parseFrom(buff.array());
                                buff.clear();
                                Thread.sleep(delta);

                            }
                        } catch (InterruptedException e) {
                            System.out.println("Waiting till next message were interrupted");
                            e.printStackTrace();
                        }
                    } catch (IOException e) {
                        System.out.println("Connection to server problem");
                    e.printStackTrace();
                    }
                });
                th[i].start();

            }
        }

        try {
            for (int i = 0; i < clientsNum; i++) {
                th[i].join();
            }
        } catch (InterruptedException e) {
            System.out.println("join failure");
            e.printStackTrace();
        }

        int avgSort = (int)(avgSortTime.get() / clientsNum);
        int avgWrite = (int)(avgReadToWrite.get() / clientsNum);
        int avgClient = (int)(avgClientTime.get() / clientsNum);
        res[0] = avgSort;
        res[1] = avgWrite;
        res[2] = avgClient;

    }
}
