import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class OneReadThPoolSTPE implements Server {
    private int messageNum;
    private int clientsCount;
    private static ServerSocket servSc;

    public OneReadThPoolSTPE(int messageNum, int clientsCount) {
        this.messageNum = messageNum;
        this.clientsCount = clientsCount;
        try {
            servSc = new ServerSocket(8081);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void worker() {
        try {
            final ExecutorService thp = Executors.newFixedThreadPool(12);
            Thread[] thRead = new Thread[clientsCount];
            final Socket[] sc = new Socket[clientsCount];
            for (int k = 0; k < clientsCount; k++) {
                final int curNum = k;
                sc[curNum] = servSc.accept();
                final ExecutorService ansSTPE = Executors.newSingleThreadExecutor();
                thRead[k] = new Thread(() -> {
                    final AtomicLong avgSortTime = new AtomicLong(0);
                    final AtomicLong avgReadToWrite = new AtomicLong(0);
                    try (InputStream is = sc[curNum].getInputStream(); OutputStream os = sc[curNum].getOutputStream()) {
                        byte[] buffInt = new byte[4];

                        AtomicInteger anwerCounter = new AtomicInteger(0);
                        for (int c = 0; c < messageNum; c++) {
                            is.read(buffInt);
                            final long beforeRead = System.currentTimeMillis();
                            int messageLength = ByteBuffer.wrap(buffInt).getInt();
                            byte[] messBytes = new byte[messageLength];
                            int bytesRead = is.read(messBytes);
                            while (bytesRead < messageLength) {
                                bytesRead += is.read(messBytes, bytesRead, messageLength - bytesRead);
                            }
                            MessageProtoc.Arr message = MessageProtoc.Arr.parseFrom(messBytes);
                            int arrSize = message.getNum();
                            final int[] array = new int[arrSize];
                            for (int i = 0; i < arrSize; i++) {
                                array[i] = message.getData(i);
                            }
                            thp.execute(() -> {
                                long startSort = System.currentTimeMillis();
                                Utils.sort(array);
                                long finishSort = System.currentTimeMillis();
                                long curSort = avgSortTime.get();
                                while (!avgSortTime.compareAndSet(curSort, curSort + finishSort - startSort))
                                    curSort = avgSortTime.get();
                                ansSTPE.execute(() -> {
                                    anwerCounter.getAndIncrement();
                                    MessageProtoc.Arr.Builder ansMessageBuilder = MessageProtoc.Arr.newBuilder()
                                            .setNum(arrSize);
                                    for (int i = 0; i < arrSize; i++) {
                                        ansMessageBuilder.addData(array[i]);
                                    }
                                    MessageProtoc.Arr ansMessage = ansMessageBuilder.build();
                                    try {
                                        os.write(ByteBuffer.allocate(4).putInt(ansMessage.getSerializedSize()).array());
                                        os.flush();
                                        ansMessage.writeTo(os);
                                        os.flush();
                                    } catch (IOException e) {
                                        System.out.println("Error in writing answer to client");
                                        e.printStackTrace();
                                    }
                                    long afterWrite = System.currentTimeMillis();
                                    long curRead = avgReadToWrite.get();
                                    while (!avgReadToWrite.compareAndSet(curRead, curRead + afterWrite - beforeRead))
                                        curRead = avgReadToWrite.get();
                                });
                            });
                        }
                        while (anwerCounter.get() != messageNum);
                        ansSTPE.shutdown();
                        while (!ansSTPE.awaitTermination(200, TimeUnit.MILLISECONDS));
                        int avgSortRes = (int)(avgSortTime.get() / messageNum);
                        int avgReadWriteRes = (int)(avgReadToWrite.get() / messageNum);
                        os.write(ByteBuffer.allocate(4).putInt(avgSortRes).array());
                        os.write(ByteBuffer.allocate(4).putInt(avgReadWriteRes).array());
                        os.flush();

                    } catch (IOException e) {
                        System.out.println("Error in connection with client");
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        System.out.println("Waiting for send answer fails");
                        e.printStackTrace();
                    }
                });
                thRead[k].start();
            }
            for (int k = 0; k < clientsCount; k++) {
                thRead[k].join();
            }
            servSc.close();

        } catch (IOException e) {
            System.out.println("Server socket fail");
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.out.println("Server were interrupted");
            e.printStackTrace();
        }
    }
}
