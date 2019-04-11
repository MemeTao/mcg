package cn.pgyyd.mcg.singleton;


import java.util.concurrent.atomic.AtomicInteger;

//不能像C++那样搞个模板参数。。。
public class JobCounter {

    private static AtomicInteger atomicInteger = new AtomicInteger(0);

    private static JobCounter instance = new JobCounter();

    private JobCounter() {}

    public static JobCounter getInstance() {
        return instance;
    }

    public int incrementAndGet() {
        return atomicInteger.incrementAndGet();
    }

    public int decrementAndGet() {
        return atomicInteger.decrementAndGet();
    }

    public int get() {
        return atomicInteger.get();
    }

}
