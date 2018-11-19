import java.util.Queue;
import java.util.function.Function;
import java.util.function.Supplier;

public class LightFutureImpl<T> implements LightFuture<T> {

    public LightFutureImpl(Supplier<T> supplier) {
        this.supplier = supplier;
        isCalulated = false;
    }

    public LightFutureImpl(Supplier<T> supplier, Queue queue) {
        this.supplier = supplier;
        isCalulated = false;
        this.queue = queue;
    }

    @Override
    public boolean isReady() {
        return isCalulated;
    }

    @Override
    public T get() throws LightExecutionException, InterruptedException {
        if (exception != null) throw new LightExecutionException();
        synchronized (this) {
            while (!isCalulated && exception == null) this.wait();
            if (exception != null) throw new LightExecutionException();
            return result;
        }
    }

    public void run() {
        synchronized (this) {
            try {
                T res = supplier.get();
                result = res;
                isCalulated = true;
            } catch (Exception e) {
                exception = e;
                this.notify();
            }
        }
    }

    @Override
    public <R> LightFuture<R> thenApply(Function<? super T, R> fun) {
        final Exception[] exp = new Exception[1];
        LightFutureImpl<R> fut = new LightFutureImpl<>(() -> {
            T res = null;
            try {
                res = get();
            } catch (LightExecutionException | InterruptedException e) {
                exp[0] = e;
            }
            return fun.apply(res);
        }, queue);
        synchronized (queue) {
            queue.add(fut);
        }
        if (exp[0] != null) {
            fut.exception = exp[0];
        }
        return fut;
    }

    private T result;
    private Supplier<T> supplier;
    private Exception exception;
    private boolean isCalulated;
    private Queue queue;
}
