/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ro.lazcatluc.lambda.streams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 *
 * @author Catalin
 */
public final class LazyListStream<T> implements IStream<T>, Iterable<T> {

    /**
     * The list backing this stream
     */
    private final List<T> streamQueue;
    /**
     * The current position in the stream
     */
    
    private int currentPosition = 0;
    
    /**
     * The contract of this function is that
     * it will always be called from a synchronized context
     * of the list.
     */
    private final Consumer<List<T>> computeNext;

    public LazyListStream(Consumer<List<T>> computeNext) {
        streamQueue = Collections.synchronizedList(new ArrayList<>());
        this.computeNext = computeNext;
    }

    private LazyListStream(List<T> queue, Consumer<List<T>> computeNext) {
        this.streamQueue = queue;
        this.computeNext = computeNext;
    }

    public LazyListStream newInstance() {
        return new LazyListStream(streamQueue, computeNext);
    }

    @Override
    public T root() {
        
            while (currentPosition > streamQueue.size() - 1) {
                computeNext.accept(streamQueue);
            }
        
        return streamQueue.get(currentPosition);
    }

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public T next() {
        T root = root();
        currentPosition++;
        return root;
    }

    @Override
    public void remove() {
        currentPosition++;
    }

    void reset() {
        currentPosition = 0;
    }

    @Override
    public Iterator<T> iterator() {
        return streamQueue.listIterator();
    }
}
