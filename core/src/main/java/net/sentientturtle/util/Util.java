package net.sentientturtle.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Class for assorted utility functions
 */
public class Util {
    @SuppressWarnings("unchecked")
    public static <T, E extends Throwable> T sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }

    public static Thread[] getAllThreads() {
        ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
        while (threadGroup.getParent() != null) {
            threadGroup = threadGroup.getParent();
        }

        Thread[] threads = new Thread[threadGroup.activeCount()];
        while (threadGroup.enumerate(threads, true) == threads.length) {
            threads = new Thread[threads.length * 2];
        }

        return threads;
    }

    public static <T, R> List<R> mapList(List<T> source, Function<T, R> mapper) {
        final ArrayList<R> list = new ArrayList<>(source.size());
        for (T t : source) {
            list.add(mapper.apply(t));
        }
        return list;
    }
}
