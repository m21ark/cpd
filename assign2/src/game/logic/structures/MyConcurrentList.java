package game.logic.structures;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;

public class MyConcurrentList<E> implements Iterable<E>, java.io.Serializable {
    private final List<E> elements;
    private final ReadWriteLock lock = new ReentrantReadWriteLock(true);

    public MyConcurrentList() {
        elements = new ArrayList<>();
    }

    public void add(E element) {
        lock.writeLock().lock();
        try {
            elements.add(element);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void remove(E element) {
        lock.writeLock().lock();
        try {
            elements.remove(element);
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

    public boolean contains(E element) {
        lock.readLock().lock();
        try {
            return elements.contains(element);
        } finally {
            lock.readLock().unlock();
        }
    }

    public E front() {
        lock.readLock().lock();
        try {
            return elements.get(0);
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

    public void removeWhere(Predicate<E> predicate) {
        lock.writeLock().lock();
        try {
            elements.removeIf(predicate);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Iterator<E> iterator() {
        lock.readLock().lock();
        try {
            return elements.iterator();
        } finally {
            lock.readLock().unlock();
        }
    }

    public E poll() {
        lock.writeLock().lock();
        try {
            if (elements.isEmpty()) return null;
            E element = elements.get(0);
            elements.remove(0);
            return element;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
