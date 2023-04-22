package game.logic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MyConcurrentList<E> implements Iterable<E>{
    private final List<E> elements;
    private final ReadWriteLock lock = new ReentrantReadWriteLock(true);

    public MyConcurrentList() {
        elements = new ArrayList<>();
    }

    public void add(E player) {
        lock.writeLock().lock();
        try {
            elements.add(player);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void remove(E player) {
        lock.writeLock().lock();
        try {
            elements.remove(player);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public E get(int index) {
        lock.readLock().lock();
        try {
            return elements.get(index);
        } finally {
            lock.readLock().unlock();
        }
    }

    public int size() {
        lock.readLock().lock();
        try {
            return elements.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean isEmpty() {
        lock.readLock().lock();
        try {
            return elements.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean contains(E player) {
        lock.readLock().lock();
        try {
            return elements.contains(player);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void clear() {
        lock.writeLock().lock();
        try {
            elements.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Iterator<E> iterator() {
        return elements.iterator();
    }
}
