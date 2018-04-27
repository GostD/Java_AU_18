import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Collections {
    public static <T, R> Iterable<R> map(Function1<? super T, ? extends R> fun, Iterable<? extends T> a) {
        List<R> list = new ArrayList<>();
        for (T elem : a)
            list.add(fun.apply(elem));
        return list;
    }

    public static <T> Iterable<T> filter(Predicate<? super T> pred, Iterable<? extends T> a) {
        List<T> list = new ArrayList<>();
        for (T elem : a) {
            if (pred.apply(elem))
                list.add(elem);
        }
        return list;
    }

    public static <T> Iterable<T> takeWhile(Predicate<? super T> pred, Iterable<? extends T> a) {
        List<T> list = new ArrayList<>();
        for (T elem : a) {
            if (!pred.apply(elem))
                break;
            list.add(elem);
        }
        return list;
    }

    public static <T> Iterable<T> takeUntil(Predicate<? super T> pred, Iterable<? extends T> a) {
        return takeWhile(pred.not(), a);
    }

    public static <A, T> T foldr(Function2<? super A, ? super T, ? extends T> fun, Iterable<? extends A> a, T ini) {
        return itFoldr(fun, a.iterator(), ini);
    }

    public static <A, T> T itFoldr(Function2<? super A, ? super T, ? extends T> fun, Iterator<? extends A> it, T ini) {
        A cur = it.next();
        if (it.hasNext()) {
            return fun.apply(cur, itFoldr(fun, it, ini));
        } else {
            return fun.apply(cur, ini);
        }
    }

    public static <A, T> T foldl(Function2<? super T, ? super A, ? extends T> fun, Iterable<? extends A> a, T ini) {
        T res = ini;
        for (A elem : a)
            res = fun.apply(res, elem);
        return res;
    }

}
