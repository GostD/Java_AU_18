public interface Function1<A, T> {
    T apply(A arg);

    default <R> Function1<A, R> compose(Function1<? super T, ? extends R> g) {
        return (A arg) -> g.apply(this.apply(arg));
    }
}
