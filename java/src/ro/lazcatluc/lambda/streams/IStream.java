/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ro.lazcatluc.lambda.streams;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Generic interface for infinite collections
 * keeping a local quasi-state via the root()
 * method.
 * 
 * @author Catalin
 */
public interface IStream<T> extends Iterator<T> {
    
    /**
     * The quasi-state of the stream.
     * The contract of this method is that it must
     * execute in O(1) time and be idempotent.
     * 
     * @return the head of the stream
     */
	T root();
	
    /**
     * Skips the next n elements from this stream.
     * They will not be processed by anything else.
     * 
     * @param n the number of elements to skip
     * 
     * @return the current stream
     */
	default IStream<T> skip(int n) {
		while (n > 0) {
			next();
			n--;
		}
		return this;
	}
	
    /**
     * Takes the next n elements from this stream.
     * This method mutates the state of the stream
     * just like the skip method.
     * 
     * @param n the number of elements to take
     * @return the list of elements taken from the stream
     */
	default List<T> take(int n) {
		List<T> ret = new ArrayList<>(n);
		while (n > 0) {
			ret.add(root());
			next();
			n--;
		}
		return ret;
	}
	
    /**
     * {@inheritDoc}
     */
	@Override
	default void remove() {
		skip(1);
	}	
	
    /**
     * {@inheritDoc}
     */
	@Override
	default boolean hasNext() {
        // Always true for infinite streams.
		return true;
	}
	
    /**
     * Takes an operator and wraps the output of the
     * stream with it. The state of the original stream 
     * changes as take / skip operations are executed
     * on the resulting stream.
     * 
     * @param func the operator that should wrap the output
     *  of the stream
     * 
     * @return the new stream backed by the output of the
     *  original stream
     */
	default IStream<T> map(final UnaryOperator<T> func) {
		final Supplier<T> origRoot = () -> root();
		final Supplier<T> origNext = () -> next();
		
		return new IStream<T>() {
			T root = origRoot.get();
			
			@Override
			public T root() {
				return func.apply(root);
			}
			
			@Override
			public T next() {
				root = origNext.get();
				return root();
			}
			
		};
	}
	
    /**
     * Removes all the elements from the original stream
     * if they do not pass the predicate test.
     * 
     * This call alters the state of the original stream.
     * 
     * @param pred the predicate that needs to be passed
     *  by the elements of the stream
     * 
     * @return the new stream backed by the output of the
     * original stream.
     */
	default IStream<T> filter(final Predicate<T> pred) {
		final Supplier<T> origRoot = () -> root();
		final Supplier<T> origNext = () -> next();
		
		return new IStream<T>() {
			T root = origRoot.get();
			{
				if (!pred.test(root)) {
					next();
				}
			}
			
			@Override
			public T root() {
				return root;
			}
			
			@Override
			public T next() {
				do {
					root = origNext.get();
				} while (!pred.test(root));
				return root();
			}
			
		};
	}
	
    /**
     * Takes 1 element, then skips n-1 elements.
     * 
     * @param step the number of steps to pass
     * 
     * @return the new stream backed by the original
     *  stream.
     */
	default IStream<T> every(final int step) {
		final Supplier<T> origRoot = () -> root();
		final Supplier<T> origNext = () -> next();
        final IStream<T> original = this;
		return new IStream<T>() {
			private T root = origRoot.get();
			
			@Override
			public T root() {
				return root;
			}
			
			@Override
			public T next() {
				for (int i=0;i<step;i++) {
					root = origNext.get();
				}
				return root();
			}
			
            @Override
            public IStream<T> original() {
                return original;
            }
		};
	}
    
    /**
     * 
     * @return the stream from which this stream has originated
     */
    default IStream<T> original() {
        return null;
    }
	
    /**
     * Clones the stream into two separate streams so that
     * it can be processed twice. Normally, map or filter
     * mutate the stream - use splitter in order to save
     * a copy of the original stream.
     * 
     * @return two copies of the original stream; note that
     *  the original stream itself is rendered unusable after
     *  this method
     */
	default List<IStream<T>> splitter() {
		final Supplier<T> origRoot = () -> root();
		final Supplier<T> origNext = () -> next();
		
        /**
         * Use a queue to cache the results of the stream
         * so that they can be read twice. If a stream is ahead
         * it will read from the original stream and offer the
         * element to the queue as well. If a stream is behind,
         * it will poll the queue directly.
         * 
         */
		final Queue<T> results = new LinkedList<>();
		List<IStream<T>> ret = new ArrayList<>(2);
		final AtomicBoolean firstIsAhead = new AtomicBoolean(false);
		
		for (int i=0;i<2;i++) {
			final int finali = i;
			ret.add(
				new IStream<T>() {
					T root = origRoot.get();
			
					@Override
					public T root() {
						return root;
					}
					
					@Override
					public T next() {
						if (results.isEmpty() || (finali == 0 && firstIsAhead.get()) || (finali == 1 && !firstIsAhead.get())) {
                            // This stream is ahead or the queue is empty
                            // Read from the original and offer to the queue
							root = origNext.get();
							if (results.isEmpty()) {
                                // This stream has just overtaken the other stream
								firstIsAhead.set(finali == 0);
							}
							results.offer(root);
						}
						else {
							root = results.poll();
						}
						return root();
					}
				}
			);
		}
		
		return ret;
	}
	
}