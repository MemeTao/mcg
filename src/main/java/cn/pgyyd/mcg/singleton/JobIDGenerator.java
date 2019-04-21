package cn.pgyyd.mcg.singleton;

import java.util.concurrent.atomic.AtomicInteger;

//TODO: 重写此类，更改为 时间+机器ID+random 组合而成的ID
public class JobIDGenerator {

    private static AtomicInteger atomicInteger = new AtomicInteger(0);

    private static JobIDGenerator ourInstance = new JobIDGenerator();

    public static JobIDGenerator getInstance() {
        return ourInstance;
    }

    private JobIDGenerator() {
    }

    public long generate() {
        return atomicInteger.incrementAndGet();
    }
}
