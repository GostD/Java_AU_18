import java.util.function.Function;
import java.util.function.Supplier;

public interface LightFuture<T> {
    boolean isReady();
    T get() throws LightExecutionException, InterruptedException;
    <R> LightFuture<R> thenApply(Function<? super T, R> fun);
}
