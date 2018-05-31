public interface Function2<A, B, T> {
    T apply(A fstArg, B sndArg);

    default <R> Function2<A, B, R> compose(Function1<? super T, ? extends R> g) {
        return (fstArg, sndArg) -> g.apply(apply(fstArg, sndArg));
    }

    default Function1<B, T> bind1(A fstArg) {
        return sndArg -> apply(fstArg, sndArg);
    }

    default Function1<A, T> bind2(B sndArg) {
        return fstArg -> apply(fstArg, sndArg);
    }

    default Function1<A, Function1<B, T>> curry() {
        return fstArg -> (sndArg -> apply(fstArg, sndArg));
    }


}
