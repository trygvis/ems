package no.java.ems.server;

import fj.*;
import fj.data.*;

public class EmsServerUtil {
    public static <T> F<T, Option<T>> fromNull_() {
        return new F<T, Option<T>>() {
            public Option<T> f(T t) {
                return t != null ? Option.some(t) : Option.<T>none();
            }
        };
    }
}
