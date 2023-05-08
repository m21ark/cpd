package game.logic.structures;

import java.io.Serial;
import java.io.Serializable;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class MyConcurrentMap<K, S> implements Serializable {

    private Map<K, S> map;

    private final ReadWriteLock lock = new ReentrantReadWriteLock(true);

    public MyConcurrentMap() {
        this.map = new HashMap<>();
    }

    public S get(K key) {
        lock.readLock().lock();
        try {
            return this.map.get(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void remove(K key) {
        lock.writeLock().lock();
        try {
            this.map.remove(key);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void clear() {
        lock.writeLock().lock();
        try {
            this.map.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void put(K key, S value) {
        lock.writeLock().lock();
        try {
            this.map.put(key, value);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean containsKey(K key) {
        lock.writeLock().lock();
        try {
            return this.map.containsKey(key);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean containsValue(S value) {
        lock.writeLock().lock();
        try {
            return this.map.containsValue(value);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int size() {
        lock.readLock().lock();
        try {
            return this.map.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean isEmpty() {
        lock.readLock().lock();
        try {
            return this.map.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }

    public Map<K, S> getMap() {
        lock.readLock().lock();
        try {
            return this.map;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Serial
    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
        out.writeInt(map.size());
        for (Map.Entry<K, S> entry : map.entrySet()) {
            out.writeObject(entry.getKey());
            if (entry.getValue() instanceof Socket) {
                out.writeObject(null);
            } else {
                out.writeObject(entry.getValue());
            }
        }
    }

    @Serial
    @SuppressWarnings("unchecked")
    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        map = new HashMap<>();
        int size = in.readInt();
        for (int i = 0; i < size; i++) {
            K key = (K) in.readObject();
            S value = null;
            try {
                value = (S) in.readObject();
            } catch (Exception e) {
                // e.printStackTrace();
            }
            map.put(key, value);
        }
    }

}

