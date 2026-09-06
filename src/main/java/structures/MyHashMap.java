package structures;

/**
 * A custom hash map implementation using separate chaining for collision resolution.
 * Provides O(1) average-case time complexity for get, put, containsKey, and remove operations.
 * Automatically resizes when the load factor exceeds 0.75.
 *
 * @param <K> The type of keys maintained by this map
 * @param <V> The type of mapped values
 */
public class MyHashMap<K, V> {

    /**
     * Internal node class for the linked list chains at each bucket.
     * Each node stores a key-value pair and a reference to the next node in the chain.
     */
    private static class Node<K, V> {
        K key;
        V value;
        Node<K, V> next;

        Node(K key, V value, Node<K, V> next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }

    private Node<K, V>[] buckets;  // Array of linked list heads
    private int size;              // Number of key-value pairs currently stored
    private int capacity;          // Number of buckets
    private static final double LOAD_FACTOR = 0.75; // Threshold for resizing

    /**
     * Constructs an empty hash map with a default initial capacity of 64.
     */
    public MyHashMap() {
        this(64);
    }

    /**
     * Constructs an empty hash map with the specified initial capacity.
     *
     * @param initialCapacity The initial number of buckets
     */
    @SuppressWarnings("unchecked")
    public MyHashMap(int initialCapacity) {
        this.capacity = initialCapacity;
        this.buckets = new Node[capacity];
        this.size = 0;
    }

    /**
     * Computes the bucket index for a given key using its hashCode.
     * Uses bitwise AND with (capacity - 1) for fast modulo when capacity is a power of 2.
     * Applies a secondary hash spread to reduce clustering.
     *
     * @param key The key to hash
     * @return The bucket index (0 to capacity-1)
     */
    private int hash(K key) {
        int h = key.hashCode();
        // Spread the higher bits downward to reduce collisions in lower bits
        h ^= (h >>> 16);
        return (h & 0x7FFFFFFF) % capacity;
    }

    /**
     * Associates the specified value with the specified key.
     * If the key already exists, the old value is replaced.
     *
     * @param key   The key with which the value is to be associated
     * @param value The value to be associated with the key
     * @return The previous value associated with key, or null if there was none
     */
    public V put(K key, V value) {
        // Resize if load factor exceeded
        if ((double) size / capacity > LOAD_FACTOR) {
            resize();
        }

        int index = hash(key);
        Node<K, V> current = buckets[index];

        // Check if key already exists in this bucket's chain
        while (current != null) {
            if (current.key.equals(key)) {
                V oldValue = current.value;
                current.value = value;
                return oldValue; // Key existed, return old value
            }
            current = current.next;
        }

        // Key not found — insert at the head of the chain
        buckets[index] = new Node<>(key, value, buckets[index]);
        size++;
        return null;
    }

    /**
     * Returns the value associated with the specified key.
     *
     * @param key The key whose associated value is to be returned
     * @return The value associated with the key, or null if the key is not found
     */
    public V get(K key) {
        int index = hash(key);
        Node<K, V> current = buckets[index];

        while (current != null) {
            if (current.key.equals(key)) {
                return current.value;
            }
            current = current.next;
        }
        return null; // Key not found
    }

    /**
     * Checks whether the map contains the specified key.
     *
     * @param key The key to check for
     * @return true if the map contains the key, false otherwise
     */
    public boolean containsKey(K key) {
        int index = hash(key);
        Node<K, V> current = buckets[index];

        while (current != null) {
            if (current.key.equals(key)) {
                return true;
            }
            current = current.next;
        }
        return false;
    }

    /**
     * Removes the mapping for the specified key if present.
     *
     * @param key The key whose mapping is to be removed
     * @return The previous value associated with key, or null if there was none
     */
    public V remove(K key) {
        int index = hash(key);
        Node<K, V> current = buckets[index];
        Node<K, V> prev = null;

        while (current != null) {
            if (current.key.equals(key)) {
                if (prev == null) {
                    // Removing the head of the chain
                    buckets[index] = current.next;
                } else {
                    // Removing a node in the middle or end
                    prev.next = current.next;
                }
                size--;
                return current.value;
            }
            prev = current;
            current = current.next;
        }
        return null; // Key not found
    }

    /**
     * Returns the number of key-value pairs in this map.
     *
     * @return The number of entries in the map
     */
    public int size() {
        return size;
    }

    /**
     * Checks whether the map is empty.
     *
     * @return true if the map contains no entries, false otherwise
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns all keys in the map as an array of Objects.
     * The caller should cast elements to the appropriate key type.
     *
     * @return An Object array containing all keys in the map
     */
    public Object[] getKeys() {
        Object[] keys = new Object[size];
        int idx = 0;
        for (int i = 0; i < capacity; i++) {
            Node<K, V> current = buckets[i];
            while (current != null) {
                keys[idx++] = current.key;
                current = current.next;
            }
        }
        return keys;
    }

    /**
     * Returns all values in the map as an array of Objects.
     * The caller should cast elements to the appropriate value type.
     *
     * @return An Object array containing all values in the map
     */
    public Object[] getValues() {
        Object[] values = new Object[size];
        int idx = 0;
        for (int i = 0; i < capacity; i++) {
            Node<K, V> current = buckets[i];
            while (current != null) {
                values[idx++] = current.value;
                current = current.next;
            }
        }
        return values;
    }

    /**
     * Doubles the capacity of the bucket array and rehashes all entries.
     * Called automatically when the load factor exceeds 0.75.
     */
    @SuppressWarnings("unchecked")
    private void resize() {
        int newCapacity = capacity * 2;
        Node<K, V>[] newBuckets = new Node[newCapacity];

        // Rehash all existing entries into the new bucket array
        for (int i = 0; i < capacity; i++) {
            Node<K, V> current = buckets[i];
            while (current != null) {
                Node<K, V> next = current.next; // Save next before we overwrite it

                // Compute new bucket index with the new capacity
                int h = current.key.hashCode();
                h ^= (h >>> 16);
                int newIndex = (h & 0x7FFFFFFF) % newCapacity;

                // Insert at head of new chain
                current.next = newBuckets[newIndex];
                newBuckets[newIndex] = current;

                current = next;
            }
        }

        this.buckets = newBuckets;
        this.capacity = newCapacity;
    }
}
