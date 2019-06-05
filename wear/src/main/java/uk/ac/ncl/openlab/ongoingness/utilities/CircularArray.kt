package uk.ac.ncl.openlab.ongoingness.utilities

/**
 * Created by Kyle Montague on 21/11/2018.
 */

import java.util.concurrent.atomic.AtomicInteger

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

    private var arr: Array<Any?>
    private var _size: Int = 0
    private var tail: Int
    private var bufferSize: Int = 10

    private val head: Int
        get() = if (_size == arr.size) (tail + 1) % _size else 0

    /**
     * Number of elements currently stored in the array.
     */
    val size: Int
        get() = _size

    /**
     * Add an element to the array.
     */
    fun add(item: T) {
        tail = (tail + 1) % arr.size
        arr[tail] = item
        if (_size < arr.size) _size++
    }

    fun reset(){
        this.arr = arrayOfNulls(bufferSize)
        this._size = 0
        this.tail = -1
    }

    /**
     * Get an element from the array.
     */
    @Suppress("UNCHECKED_CAST")
    operator fun get(index: Int): T =
            when {
                _size == 0 || index > _size || index < 0 -> throw IndexOutOfBoundsException("$index")
                _size == arr.size -> arr[((head + index) % arr.size)]
                else -> arr[index]
            } as T

    /**
     * This array as a list.
     */
    @Suppress("UNCHECKED_CAST")
    fun toList(): List<T> = iterator().asSequence().toList()

    public override fun clone(): CircularArray<T> = CircularArray(this)

    override fun iterator(): Iterator<T> = object : Iterator<T> {
        private val index: AtomicInteger = AtomicInteger(0)

        override fun hasNext(): Boolean = index.get() < size

        override fun next(): T = get(index.getAndIncrement())
    }

}