package cn.pgyyd.mcg.singleton;

import java.util.concurrent.atomic.AtomicInteger;

public class JobIDGenerator {

    private static AtomicInteger atomicInteger = new AtomicInteger(0);

    private static JobIDGenerator ourInstance = new JobIDGenerator();

    public static JobIDGenerator getInstance() {
        return ourInstance;
    }

    private JobIDGenerator() {
    }

    public int generate() {
        return atomicInteger.incrementAndGet();
    }
}
