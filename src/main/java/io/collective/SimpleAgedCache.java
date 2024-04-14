package io.collective;
import java.time.Clock;

// A simple cache implementation that supports expiration of entries.
public class SimpleAgedCache<E> {
    private final Clock clock;  // Clock instance to track time.
    private ExpirableEntry<E> head;  // Head of the linked list.
    private ExpirableEntry<E> tail;  // Tail of the linked list.
    private int size;  // Size of the cache.

    // Default constructor that uses the system UTC clock.
    public SimpleAgedCache() {
        this(Clock.systemUTC());
    }

    // Constructor that accepts a custom clock.
    public SimpleAgedCache(Clock clock) {
        this.clock = clock;
        this.head = null;
        this.tail = null;
        this.size = 0;
    }

    // Method to add an entry to the cache.
    public synchronized void put(E key, E value, int retentionInMillis) {
        if (key == null || value == null) {
            throw new NullPointerException("Neither key nor value can be null.");
        }

        // Create a new node.
        ExpirableEntry<E> newNode = new ExpirableEntry<>(key, value, retentionInMillis, clock);
        // Check if a node with the same key already exists.
        ExpirableEntry<E> existingNode = find(key);

        if (existingNode == null) {
            // If not, add the new node.
            addNewNode(newNode);
        } else {
            // If yes, update the existing node.
            updateExistingNode(existingNode, newNode);
        }
    }

    // Method to add a new node to the linked list.
    private void addNewNode(ExpirableEntry<E> node) {
        if (head == null) {
            head = node;
        } else {
            tail.next = node;
            node.prev = tail;
        }
        tail = node;
        size++;
    }

    // Method to update an existing node in the linked list.
    private void updateExistingNode(ExpirableEntry<E> existingNode, ExpirableEntry<E> newNode) {
        existingNode.setValue(newNode.getValue());
        existingNode.setExpiry(newNode.getExpiry());
    }

    // Method to check if the cache is empty.
    public boolean isEmpty() {
        return size() == 0;
    }

    // Method to get the size of the cache.
    public synchronized int size() {
        clean();
        return size;
    }

    // Method to find a node in the linked list.
    private ExpirableEntry<E> find(E key) {
        ExpirableEntry<E> current = head;
        while (current != null) {
            if (current.getKey().equals(key)) {
                return current;
            }
            current = current.next;
        }
        return null;
    }

    // Method to get a value from the cache.
    public synchronized E get(E key) {
        clean();
        ExpirableEntry<E> node = find(key);
        return (node != null && !node.isExpired(clock)) ? node.getValue() : null;
    }

    // Method to remove expired entries from the cache.
    private void clean() {
        ExpirableEntry<E> node = head;
        while (node != null) {
            if (node.isExpired(clock)) {
                remove(node);
            }
            node = node.next;
        }
    }

    // Method to remove a node from the linked list.
    private void remove(ExpirableEntry<E> node) {
        if (node.prev != null) {
            node.prev.next = node.next;
        } else {
            head = node.next;
        }
        if (node.next != null) {
            node.next.prev = node.prev;
        } else {
            tail = node.prev;
        }
        size--;
    }
}

// Class representing an entry in the cache.
class ExpirableEntry<E> {
    ExpirableEntry<E> prev;  // Previous node in the linked list.
    ExpirableEntry<E> next;  // Next node in the linked list.
    E key;  // Key of the entry.
    E value;  // Value of the entry.
    long expiry;  // Expiration time of the entry.

    // Constructor for the entry.
    public ExpirableEntry(E key, E value, int retentionInMillis, Clock clock) {
        this.key = key;
        this.value = value;
        this.expiry = clock.millis() + retentionInMillis;
    }

    // Method to check if the entry has expired.
    public boolean isExpired(Clock clock) {
        return clock.millis() > expiry;
    }

    // Getter for the key.
    public E getKey() {
        return key;
    }

    // Getter for the value.
    public E getValue() {
        return value;
    }

    // Setter for the value.
    public void setValue(E value) {
        this.value = value;
    }

    // Getter for the expiration time.
    public long getExpiry() {
        return expiry;
    }

    // Setter for the expiration time.
    public void setExpiry(long expiry) {
        this.expiry = expiry;
    }
}