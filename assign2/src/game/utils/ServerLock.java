package game.utils;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ServerLock {

    private static final ReadWriteLock lockFile = new ReentrantReadWriteLock(true);
    private static final ReadWriteLock lockHeap = new ReentrantReadWriteLock(true);

    public static void lockReadFile() {
        lockFile.readLock().lock();
    }

    public static void unlockReadFile() {
        lockFile.readLock().unlock();
    }

    public static void lockWriteFile() {
        lockFile.writeLock().lock();
    }

    public static void unlockWriteFile() {
        lockFile.writeLock().unlock();
    }




























}

