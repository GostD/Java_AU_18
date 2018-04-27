public interface Predicate<A> {

    Predicate<Object> ALWAYS_TRUE = arg -> true;
    Predicate<Object> ALWAYS_FALSE = arg -> false;

    Boolean apply(A arg);

    default Predicate<A> or(Predicate<? super A> pred) {
        return (A arg) -> this.apply(arg) || pred.apply(arg);
    }

    default Predicate<A> and(Predicate<? super A> pred) {
        return (A arg) -> this.apply(arg) && pred.apply(arg);
    }

    default Predicate<A> not() {
        return (A arg) -> !this.apply(arg);
    }


}
