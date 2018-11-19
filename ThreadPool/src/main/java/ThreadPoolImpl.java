import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Supplier;

public class ThreadPoolImpl {

    ThreadPoolImpl(int n) {
        assert(n > 0);
        for(int i = 0; i < n; i++) {
            threads.add(new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        LightFutureImpl task;
                        synchronized (tasks) {
                            if (tasks.isEmpty()) {
                                tasks.wait();
                            }
                            if (Thread.currentThread().isInterrupted() || tasks.isEmpty()) continue;
                            task = tasks.poll();
                        }
                        synchronized (task) {
                            task.run();
                            task.notify();
                        }
                    } catch (InterruptedException e) {return;}//Thread.currentThread().interrupt();}
                }
            }));
        }
        for (Thread th : threads) th.start();
    }

    public <T> LightFuture<T> submit(Supplier<T> supplier) {
        LightFutureImpl future = new LightFutureImpl(supplier, tasks);
        synchronized (tasks) {
            tasks.add(future);
            tasks.notifyAll();
        }
        return future;
    }

    void shutdown() throws InterruptedException {
        for (Thread th : threads) {
            th.interrupt();
        }
    }


    List<Thread> threads = new ArrayList<>();
    Queue<LightFutureImpl> tasks = new LinkedList<>();
    
}
