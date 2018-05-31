public interface Predicate<A> {

    Predicate<Object> ALWAYS_TRUE = arg -> true;
    Predicate<Object> ALWAYS_FALSE = arg -> false;

    boolean apply(A arg);

    default Predicate<A> or(Predicate<? super A> pred) {
        return arg -> apply(arg) || pred.apply(arg);
    }

    default Predicate<A> and(Predicate<? super A> pred) {
        return arg -> apply(arg) && pred.apply(arg);
    }

    default Predicate<A> not() {
        return arg -> !apply(arg);
    }


}
