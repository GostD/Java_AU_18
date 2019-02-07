import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class SelectorsThPool implements Server {
    private int messageNum;
    private int clientsCount;
    private static ServerSocketChannel servScCh;

    public SelectorsThPool(int messageNum, int clientsCount) {
        this.messageNum = messageNum;
        this.clientsCount = clientsCount;
        try {
            servScCh = ServerSocketChannel.open().bind(new InetSocketAddress(8081));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void worker() {
        Map<SocketChannel, ByteBuffer> sizeBuffs = new HashMap<>();
        Map<SocketChannel, ByteBuffer> dataBuffs = new HashMap<>();
        Map<SocketChannel, Long> beforeReadTime = new HashMap<>();
        try {
            AtomicLong avgSortTime = new AtomicLong(0);
            AtomicLong avgReadToWrite = new AtomicLong(0);
            Selector readSelector = Selector.open();
            Selector writeSelector = Selector.open();
            Map<SocketChannel, ByteBuffer> channelToByteBuffer = new HashMap<>();
            ExecutorService thP = Executors.newFixedThreadPool(12);

            Thread th1 = new Thread(() -> {
                int count = 0;
                while (count < clientsCount * messageNum) {
                    try {
                        if (readSelector.select() == 0) continue;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Set<SelectionKey> selectionKeys = readSelector.selectedKeys();
                    Iterator<SelectionKey> keyIterator = selectionKeys.iterator();
                    while (keyIterator.hasNext()) {
                        SelectionKey key = keyIterator.next();
                        if (key.isReadable()) {
                            final SocketChannel ch = (SocketChannel) key.channel();
                            ByteBuffer size = sizeBuffs.getOrDefault(ch, null);
                            ByteBuffer array = dataBuffs.get(ch);
                            boolean noSize = size != null && size.hasRemaining();
                            try {
                                if (size != null && size.hasRemaining()) {
                                    ch.read(size);
                                } else if (array.hasRemaining()) {
                                    ch.read(array);
                                }
                            } catch (IOException e) {
                            }
                            if (size != null && !size.hasRemaining() && noSize) {
                                size.flip();
                                int arraySize = size.getInt();
                                sizeBuffs.remove(ch);
                                dataBuffs.remove(ch);
                                dataBuffs.put(ch, ByteBuffer.allocate(arraySize));
                            }
                            if (!array.hasRemaining()) {
                                synchronized (beforeReadTime) {
                                    if (!beforeReadTime.containsKey(ch))
                                        beforeReadTime.put(ch, System.currentTimeMillis());
                                }
                                array.flip();
                                try {
                                    final MessageProtoc.Arr message = MessageProtoc.Arr.parseFrom(array.array());
                                    int arrSize = message.getNum();
                                    final int[] arr = new int[arrSize];
                                    for (int i = 0; i < arrSize; i++) {
                                        arr[i] = message.getData(i);
                                    }

                                    thP.execute(() -> {
                                        long startSort = System.currentTimeMillis();
                                        Utils.sort(arr);
                                        long finishSort = System.currentTimeMillis();
                                        long curSort = avgSortTime.get();
                                        while (!avgSortTime.compareAndSet(curSort, curSort + finishSort - startSort))
                                            curSort = avgSortTime.get();

                                        MessageProtoc.Arr.Builder ansMessageBuilder = MessageProtoc.Arr.newBuilder()
                                                .setNum(arrSize);
                                        for (int i = 0; i < arrSize; i++) {
                                            ansMessageBuilder.addData(arr[i]);
                                        }
                                        MessageProtoc.Arr ansMessage = ansMessageBuilder.build();
                                        ByteBuffer sz = ByteBuffer.allocate(4);
                                        sz.putInt(ansMessage.getSerializedSize());
                                        sz.flip();
                                        try {
                                            ch.write(sz);
                                            while (sz.hasRemaining()) ch.write(sz);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        ByteBuffer ansBuff = ByteBuffer.allocate(ansMessage.getSerializedSize());
                                        ansBuff.put(ansMessage.toByteArray());
                                        ansBuff.flip();
                                        synchronized (channelToByteBuffer) {
                                            channelToByteBuffer.put(ch, ansBuff);
                                        }

                                    });
                                } catch (InvalidProtocolBufferException e) {
                                    System.out.println("Could not read message");
                                    e.printStackTrace();
                                }
                                if (size == null) sizeBuffs.put(ch, ByteBuffer.allocate(4));
                                array.clear();
                                count++;

                            }
                        }
                        keyIterator.remove();
                    }
                }
            });
            Thread th2 = new Thread(() -> {
                int count = 0;
                while (count < clientsCount * messageNum) {
                    try {
                        if (writeSelector.select() == 0) continue;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Set<SelectionKey> selectionKeys = writeSelector.selectedKeys();
                    Iterator<SelectionKey> keyIterator = selectionKeys.iterator();
                    while (keyIterator.hasNext()) {
                        SelectionKey key = keyIterator.next();
                        if (key.isWritable()) {
                            SocketChannel ch = (SocketChannel) key.channel();
                            boolean contains = false;
                            ByteBuffer buff = ByteBuffer.allocate(1);
                            synchronized (channelToByteBuffer) {
                                contains = channelToByteBuffer.containsKey(ch);
                                if (contains) buff = channelToByteBuffer.get(ch);
                            }
                            if (!contains) {
                                keyIterator.remove();
                                continue;
                            }

                            try {
                                ch.write(buff);
                                if (!buff.hasRemaining()) {
                                    long afterWrite = System.currentTimeMillis();
                                    long curRead = avgReadToWrite.get();
                                    synchronized (beforeReadTime) {
                                        while (!avgReadToWrite.compareAndSet(curRead, curRead + afterWrite - beforeReadTime.get(ch)))
                                            curRead = avgReadToWrite.get();
                                    }
                                    beforeReadTime.remove(ch);
                                    count++;
                                    synchronized (channelToByteBuffer) {
                                        channelToByteBuffer.remove(ch);
                                    }
                                }

                            } catch (IOException e) {
                                System.out.println("Write to buffer fails");
                            }
                        }
                        keyIterator.remove();
                    }
                }
            });

            List<SocketChannel> sCh = new ArrayList<>();

            try {
                for (int k = 0; k < clientsCount; k++) {
                    SocketChannel sc = servScCh.accept();
                    sc.configureBlocking(false);
                    sizeBuffs.put(sc, ByteBuffer.allocate(4));
                    dataBuffs.put(sc, ByteBuffer.allocate(1));
                    sCh.add(sc);
                    sc.register(readSelector, SelectionKey.OP_READ);
                    sc.register(writeSelector, SelectionKey.OP_WRITE);
                }
                th1.start();
                th2.start();
                th1.join();
                th2.join();
                int avgSortRes = (int)(avgSortTime.get() / (clientsCount * messageNum));
                int avgReadWriteRes = (int)(avgReadToWrite.get() / (clientsCount * messageNum));
                for (SocketChannel sc : sCh) {
                    ByteBuffer avgS = ByteBuffer.allocate(4).putInt(avgSortRes);
                    avgS.flip();
                    while (avgS.hasRemaining()) sc.write(avgS);
                    ByteBuffer avgReadWrite = ByteBuffer.allocate(4).putInt(avgReadWriteRes);
                    avgReadWrite.flip();
                    while (avgReadWrite.hasRemaining()) sc.write(avgReadWrite);
                }

            } catch (IOException e) {
                System.out.println("Server socket fail");
                e.printStackTrace();
            } catch (InterruptedException e) {
                System.out.println("Could not join selector thread");
                e.printStackTrace();
            }
            servScCh.close();
        } catch (IOException e) {
            System.out.println("Selector open fails");
            e.printStackTrace();
        }
    }
}
