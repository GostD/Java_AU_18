import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SelectorsThPool implements Server {
    private int messageNum;
    private int clientsCount;

    public SelectorsThPool() {
        new SingleThreadForEvery(1, 1);
    }

    public SelectorsThPool(int messageNum, int clientsCount) {
        this.messageNum = messageNum;
        this.clientsCount = clientsCount;
    }

    public void worker(ServerSocket servSc) {
        ServerSocketChannel servScCh = null;
        try {
            servSc.close();
            servScCh = ServerSocketChannel.open().bind(new InetSocketAddress(8081));
        } catch (IOException e) {
            e.printStackTrace();
        }
        Map<SocketChannel, ByteBuffer> sizeBuffs = new HashMap<>();
        Map<SocketChannel, ByteBuffer> dataBuffs = new HashMap<>();
        try {
            Selector readSelector = Selector.open();
            Selector writeSelector = Selector.open();
            Map<SocketChannel, ByteBuffer> channelToByteBuffer = new HashMap<>();
            ExecutorService thP = Executors.newFixedThreadPool(4);

            Thread th1 = new Thread(() -> {
//                ByteBuffer size = ByteBuffer.allocate(4);
//                ByteBuffer array = ByteBuffer.allocate(1);
                while (true) {//!Thread.interrupted();
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
                            ByteBuffer size = sizeBuffs.get(ch);
                            ByteBuffer array = dataBuffs.get(ch);
                            boolean noSize = size.hasRemaining();
                            try {
                                if (size.hasRemaining()) {
                                    ch.read(size);
                                } else if (array.hasRemaining()) {
                                    ch.read(array);
                                }
                            } catch (IOException e) {
                            }
                            if (!size.hasRemaining() && noSize) {
                                size.flip();
                                int arraySize = size.getInt();
                                array = ByteBuffer.allocate(arraySize);
                            }
                            if (!array.hasRemaining()) {
                                array.flip();
                                try {
                                    final MessageProtoc.Arr message = MessageProtoc.Arr.parseFrom(array.array());
                                    int arrSize = message.getNum();
                                    final int[] arr = new int[arrSize];
                                    for (int i = 0; i < arrSize; i++) {
                                        arr[i] = message.getData(i);
                                    }
                                    thP.execute(() -> {
                                        Utils.sort(arr);
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
                                        channelToByteBuffer.put(ch, ansBuff);
                                        try {
                                            ch.register(writeSelector, SelectionKey.OP_WRITE);
                                        } catch (IOException e) {
                                            System.out.println("Register channel to write failed");
//                                            e.printStackTrace();
                                        }

                                    });
                                } catch (InvalidProtocolBufferException e) {
                                    System.out.println("Could not read message");
//                                    e.printStackTrace();
                                }
                                size.clear();
                                array.clear();
                            }
                        }
                        keyIterator.remove();
                    }
                }
            });
            Thread th2 = new Thread(() -> {
                while (true) {
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
                            ByteBuffer buff = channelToByteBuffer.get(ch);
                            try {
                                ch.write(buff);
                                if (!buff.hasRemaining()) {
                                    key.cancel();
                                    channelToByteBuffer.remove(ch);
                                }
                            } catch (IOException e) {
                                System.out.println("Write to buffer fails");
//                            e.printStackTrace();
                            }
                        }
                        keyIterator.remove();
                    }
                }
            });

            try {//(ServerSocketChannel servScCh = servSc.getChannel())
                for (int k = 0; k < clientsCount; k++) {
                    SocketChannel sc = servScCh.accept();
                    sc.configureBlocking(false);
                    sizeBuffs.put(sc, ByteBuffer.allocate(4));
                    dataBuffs.put(sc, ByteBuffer.allocate(1));
                    sc.register(readSelector, SelectionKey.OP_READ);
                }
                th1.start();
                th2.start();
                th1.join();
                th2.join();

            } catch (IOException e) {
                System.out.println("Server socket fail");
    //            e.printStackTrace();
            } catch (InterruptedException e) {
                System.out.println("Could not join selector thread");
//                e.printStackTrace();
            }
        } catch (IOException e) {
            System.out.println("Selector open fails");
//            e.printStackTrace();
        }
    }
}
