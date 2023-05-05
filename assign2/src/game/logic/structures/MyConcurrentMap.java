package game.logic.structures;

import java.io.Serial;
import java.io.Serializable;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;


public class MyConcurrentMap<K, S> implements Serializable {

    private Map<K, S> map;

    public MyConcurrentMap() {
        this.map = new HashMap<>();
    }

    public S get(K key) {
        return this.map.get(key);
    }

    public void remove(K key) {
        this.map.remove(key);
    }

    public void clear() {
        this.map.clear();
    }

    public void put(K key, S value) {
        this.map.put(key, value);
    }

    public boolean containsKey(K key) {
        return this.map.containsKey(key);
    }

    public boolean containsValue(S value) {
        return this.map.containsValue(value);
    }

    public int size() {
        return this.map.size();
    }

    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    public Map<K, S> getMap() {
        return this.map;
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

