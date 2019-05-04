package cn.pgyyd.mcg.singleton;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

//TODO: 重写此类，更改为 时间+机器ID+random 组合而成的ID
public class JobIDGenerator {

    private static long nodeId;

    public static void init(int id) {
        nodeId = id % 100;
    }

    private static AtomicLong atomicLong = new AtomicLong(0);

    private static JobIDGenerator ourInstance = new JobIDGenerator();

    public static JobIDGenerator getInstance() {
        return ourInstance;
    }

    private JobIDGenerator() {
    }

    public long generate() {
        long rand = atomicLong.incrementAndGet() % 10000;
        return System.currentTimeMillis() * 1000000 + nodeId * 10000 + rand;
    }
}
