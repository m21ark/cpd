package game.logic.structures;

import java.io.Serializable;

public record Pair<K, V>(K key, V value) implements Serializable {
    public Pair {
        if (key == null || value == null) {
            throw new IllegalArgumentException("Key and value cannot be null");
        }
    }

    public K getFirst() { // POR ENQUANTO N E PRECISO LOCKS
        return this.key;
    }

    public V getSecond() {
        return value;
    }

}
