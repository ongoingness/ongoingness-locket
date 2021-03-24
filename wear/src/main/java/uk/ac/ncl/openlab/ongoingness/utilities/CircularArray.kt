package uk.ac.ncl.openlab.ongoingness.utilities

import java.util.concurrent.atomic.AtomicInteger

/**
 * Circular array.
 *
 * @author Kyle Montague on 21/11/2018.
 */
class CircularArray<T> : Iterable<T>, Cloneable {

    /**
     * Creates a new instance of the array with the given size.
     */
    constructor(bufferSize: Int) {
        this.bufferSize = bufferSize
        this.arr = arrayOfNulls(bufferSize)
        this.tail = -1
    }

    /**
     * Creates a new instance of the array as a copy.
     */
    constructor(circularArray: CircularArray<T>) {
        this.arr = circularArray.arr.copyOf()
        this._size = circularArray._size
        this.tail = circularArray.tail
    }

    /**
     * List where elements are stored.
     */
    private var arr: Array<Any?>

    /**
     * Size of the list.
     */
    private var _size: Int = 0

    /**
     * Index of the last elements.
     */
    private var tail: Int

    /**
     * Number of initial spaces allocated to the array.
     */
    private var bufferSize: Int = 10

    /**
     * Calculates the first index of the array.
     * @return fist index of the array.
     */
    private val head: Int
        get() = if (_size == arr.size) (tail + 1) % _size else 0

    /**
     * Number of elements currently stored in the array.
     * @return size of the array.
     */
    val size: Int
        get() = _size

    /**
     * Add an element to the array.
     *
     * @param item element to be added to the array.
     */
    fun add(item: T) {
        tail = (tail + 1) % arr.size
        arr[tail] = item
        if (_size < arr.size) _size++
    }

    /**
     * Clear the array.
     */
    fun reset(){
        this.arr = arrayOfNulls(bufferSize)
        this._size = 0
        this.tail = -1
    }

    /**
     * Get an element from the array.
     *
     * @param index index of the element to be get.
     * @return the element at index position.
     */
    @Suppress("UNCHECKED_CAST")
    operator fun get(index: Int): T =
            when {
                _size == 0 || index > _size || index < 0 -> throw IndexOutOfBoundsException("$index")
                _size == arr.size -> arr[((head + index) % arr.size)]
                else -> arr[index]
            } as T

    public override fun clone(): CircularArray<T> = CircularArray(this)

    override fun iterator(): Iterator<T> = object : Iterator<T> {
        private val index: AtomicInteger = AtomicInteger(0)

        override fun hasNext(): Boolean = index.get() < size

        override fun next(): T = get(index.getAndIncrement())
    }

}