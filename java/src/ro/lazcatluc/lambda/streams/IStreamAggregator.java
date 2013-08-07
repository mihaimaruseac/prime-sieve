/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ro.lazcatluc.lambda.streams;

import java.util.function.BiFunction;

/**
 * Build a stream based on two other 
 * streams together with an operator that acts on 
 * the streams and calls root() and next() on them
 * accordingly.
 * 
 * The contract of this class is that it never calls
 * next() on the backing streams, instead leaving it to
 * the operator.
 * 
 * @author Catalin
 */
public class IStreamAggregator<T> implements IStream<T> {

	private final IStream<? extends T> first;
	private final IStream<? extends T> second;
	private final BiFunction<IStream<? extends T>,IStream<? extends T>,T> aggregator;
    
	private T root;
	
	IStreamAggregator(IStream<? extends T> first, IStream<? extends T> second, 
					 BiFunction<IStream<? extends T>,IStream<? extends T>,T> aggregator) {
		this.first = first;
		this.second = second;
		this.aggregator = aggregator;
	}
	
	@Override
	public T next() {
		root = aggregator.apply(first, second);
		return root;
	}
	
	@Override
	public T root() {
		if (root == null) {
			next();
		}
		return root;
	}		
}
