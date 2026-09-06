package structures;

/**
 * A lightweight dynamic array for accumulating elements.
 * Distinct from MyArrayList — provides a minimal API tailored for use
 * within the store classes (e.g., collecting production companies, film IDs).
 * Doubles capacity on overflow for amortised O(1) appends.
 *
 * @param <E> The type of elements stored in this list
 */
public class MyList<E> {

    private Object[] data;  // Internal storage array
    private int size;       // Number of elements currently stored

    /**
     * Constructs an empty list with an initial capacity of 16.
     */
    public MyList() {
        this(16);
    }

    /**
     * Constructs an empty list with the specified initial capacity.
     *
     * @param initialCapacity The initial capacity of the internal array
     */
    public MyList(int initialCapacity) {
        this.data = new Object[initialCapacity];
        this.size = 0;
    }

    /**
     * Appends an element to the end of the list.
     * Doubles the capacity if the internal array is full.
     *
     * @param element The element to add
     */
    public void add(E element) {
        if (size >= data.length) {
            Object[] newData = new Object[data.length * 2];
            for (int i = 0; i < size; i++) {
                newData[i] = data[i];
            }
            data = newData;
        }
        data[size++] = element;
    }

    /**
     * Returns the element at the specified index.
     *
     * @param index The index of the element to return
     * @return The element at the specified index
     */
    @SuppressWarnings("unchecked")
    public E get(int index) {
        return (E) data[index];
    }

    /**
     * Replaces the element at the specified index.
     *
     * @param index   The index of the element to replace
     * @param element The new element
     */
    public void set(int index, E element) {
        data[index] = element;
    }

    /**
     * Returns the number of elements in this list.
     *
     * @return The size of the list
     */
    public int size() {
        return size;
    }

    /**
     * Checks if the list contains no elements.
     *
     * @return true if the list is empty, false otherwise
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Removes the element at the specified index, shifting subsequent elements left.
     *
     * @param index The index of the element to remove
     * @return The removed element
     */
    @SuppressWarnings("unchecked")
    public E removeAt(int index) {
        E removed = (E) data[index];
        for (int i = index + 1; i < size; i++) {
            data[i - 1] = data[i];
        }
        data[--size] = null;
        return removed;
    }

    /**
     * Checks whether the list contains the specified element.
     *
     * @param element The element to search for
     * @return true if the element is found, false otherwise
     */
    public boolean contains(E element) {
        for (int i = 0; i < size; i++) {
            if (element.equals(data[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Converts this list to a plain Object array of exactly the right length.
     *
     * @return An Object array containing all elements
     */
    public Object[] toArray() {
        Object[] result = new Object[size];
        for (int i = 0; i < size; i++) {
            result[i] = data[i];
        }
        return result;
    }
}
