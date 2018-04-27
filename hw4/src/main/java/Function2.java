public interface Function2<A, B, T> {
    T apply(A fstArg, B sndArg);

    default <R> Function2<A, B, R> compose(Function1<? super T, ? extends R> g) {
        return (A fstArg, B sndArg) -> g.apply(this.apply(fstArg, sndArg));
    }

    default Function1<B, T> bind1(A fstArg) {
        return (B sndArg) -> this.apply(fstArg, sndArg);
    }

    default Function1<A, T> bind2(B sndArg) {
        return (A fstArg) -> this.apply(fstArg, sndArg);
    }

    default Function1<A, Function1<B, T>> curry() {
        return (A fstArg) -> ((B sndArg) -> this.apply(fstArg, sndArg));
    }


}
