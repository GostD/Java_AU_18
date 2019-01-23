import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

public class SingleThreadForEvery implements Server {
    private int messageNum;
    private int clientsCount;

    public SingleThreadForEvery() {
        new SingleThreadForEvery(1, 1);
    }

    public SingleThreadForEvery(int messageNum, int clientsCount) {
        this.messageNum = messageNum;
        this.clientsCount = clientsCount;
    }

    public void worker(ServerSocket servSc) {
        try {
            Thread[] th = new Thread[clientsCount];
            for (int k = 0; k < clientsCount; k++) {
                final Socket sc = servSc.accept();
                final int curNum = k;
                th[k] = new Thread(() -> {
                    try (InputStream is = sc.getInputStream(); OutputStream os = sc.getOutputStream()) {
                        long avgSortTime = 0;
                        long avgReadToWrite = 0;
                        for (int c = 0; c < messageNum; c++) {
                            long timeBeforeRead = System.currentTimeMillis();
                            byte[] messLen = new byte[4];
                            is.read(messLen);
                            int messageLength = ByteBuffer.wrap(messLen).getInt();
                            byte[] msgBytes = new byte[messageLength];
                            int bytesRead = is.read(msgBytes);
                            while (bytesRead < messageLength) {
                                bytesRead += is.read(msgBytes, bytesRead, messageLength - bytesRead);
                            }
                            MessageProtoc.Arr message = MessageProtoc.Arr.parseFrom(msgBytes);
                            int arrSize = message.getNum();
                            int[] array = new int[arrSize];
                            for (int i = 0; i < arrSize; i++) {
                                array[i] = message.getData(i);
                            }
                            long sortStart = System.currentTimeMillis();
                            Utils.sort(array);
                            long sortFinish = System.currentTimeMillis();
                            avgSortTime += sortFinish - sortStart;
                            MessageProtoc.Arr.Builder ansMessageBuilder = MessageProtoc.Arr.newBuilder()
                                    .setNum(arrSize);
                            for (int i = 0; i < arrSize; i++) {
                                ansMessageBuilder.addData(array[i]);
                            }
                            MessageProtoc.Arr ansMessage = ansMessageBuilder.build();
                            os.write(ByteBuffer.allocate(4).putInt(ansMessage.getSerializedSize()).array());
                            os.flush();
                            ansMessage.writeTo(os);
                            os.flush();
                            long timeAfterWrite = System.currentTimeMillis();
                            avgReadToWrite += timeAfterWrite - timeBeforeRead;
                        }
                        int avgSortRes = (int)(avgSortTime / clientsCount);
                        int avgReadWriteRes = (int)(avgReadToWrite / clientsCount);
                        os.write(ByteBuffer.allocate(4).putInt(avgSortRes).array());
                        os.write(ByteBuffer.allocate(4).putInt(avgReadWriteRes).array());
                        os.flush();
                    } catch (IOException e) {
                        System.out.println("Error in connection with client");
                        e.printStackTrace();
                    }
                });
                th[k].start();
            }
            for (int k = 0; k < clientsCount; k++) {
                th[k].join();
            }

        } catch (IOException e) {
            System.out.println("Server socket fail");
//            e.printStackTrace();
        } catch (InterruptedException e) {
            System.out.println("Server were interrupted");
//            e.printStackTrace();
        }

    }
}
