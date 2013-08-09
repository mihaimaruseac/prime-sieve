/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ro.lazcatluc.lambda.streams;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Demonstrates the use of lambda expressions
 * in computing the stream of prime numbers.
 *
 * @author Catalin
 */
public class Eratostene {

    /**
     * Stream set-like difference.
     * <p/>
     * The second stream numbers are eliminated from the first
     * stream. The streams must be ordered.
     *
     * @param <T>    the comparable type of the streams
     * @param source the source stream
     * @param elim   the stream of numbers to be eliminated
     * @return the set-difference stream containing the
     *         elements in source which are not in elim
     */
    public static <T extends Comparable<T>> IStream<T> diff(IStream<? extends T> source, IStream<? extends T> elim) {
        return new IStreamAggregator<>(source, elim,
                (stream1, stream2) -> {
                    T root1 = stream1.root();
                    T root2 = stream2.root();
                    while (true) {
                        /**
                         * If the element of the first stream is
                         * higher, we need to push the second stream
                         * to make sure we don't have to eliminate it
                         * later.
                         */
                        if (root1.compareTo(root2) > 0) {
                            root2 = stream2.next();
                        }
                        /**
                         * Remove the head of the first stream,
                         * since we found it in the second stream
                         * as well.
                         */
                        else if (root1.compareTo(root2) == 0) {
                            root1 = stream1.next();
                            root2 = stream2.next();
                        }
                        /**
                         * For sure the head of the first stream is
                         * not in the second stream because we already
                         * found a higher element and the stream
                         * is ordered; so we take it and push the
                         * first stream.
                         */
                        else {
                            T ret = root1;
                            stream1.next();
                            return ret;
                        }
                    }
                });
    }
    
    public static LazyListStream<Long> eratosteneWithSave() {
        IStream<Long> naturals = nat();
        return new LazyListStream<>(list -> {
            Long head = naturals.next();
            for (Long prime : list) {
                if (head % prime == 0) {
                    return;
                }
                if (prime * prime > head) {
                    list.add(head);
                    return;
                }
            }
            list.add(head);
        });
    }
    
    public static IStream<Long> eratosteneWithoutSaveSingleThread(LazyListStream<Long> savedPrimes, Long first) {
        return new IStream<Long>() {

            private final IStream<Long> naturals = nat(first).skip(1);

            @Override
            public Long root() {
                return naturals.root();
            }

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Long next() {
                iterateNaturals:
                while(true) {
                    Long head = naturals.next();
                    for (Long prime : savedPrimes) {
                        if (head % prime == 0) {
                            continue iterateNaturals;
                        }
                        assert prime * prime > 0;
                        if (prime * prime > head) {
                            return head;
                        }
                    }
                }
            }
        };
    }

    private static final int PRIMES_TO_GET_IN_PARALLEL = 10001;
    public static IStream<Long> eratostene() {
        LazyListStream<Long> savedPrimes = eratosteneWithSave();
        final NavigableSet<Long> sortedComputedResults = new ConcurrentSkipListSet<>(Arrays.asList(new Long[]{2l, 3l, 5l}));
        final int parallelism = 5;
        final List<IStream<Long>> streams = new ArrayList<>(parallelism);
        for (int i = 0; i < parallelism-1; i++) {
            final IStream<Long> partialPrimeStream = partialPrimeStream(i, parallelism, savedPrimes);
            streams.add(partialPrimeStream);
        }
        return new IStream<Long>() {
            
            private Long root;
            {
                next();
            }
            
            @Override
            public Long root() {
                return root;
            }

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Long next() {
                if (sortedComputedResults.isEmpty()) {                   
                    streams.stream().parallel().forEach(stream -> {
                        sortedComputedResults.addAll(stream.take(PRIMES_TO_GET_IN_PARALLEL));
                    });
                }
                root = sortedComputedResults.pollFirst();
                return root();
            }
            
        };
    }

    
    protected static IStream<Long> partialPrimeStream(final int skip, final int parallelism, LazyListStream<Long> savedPrimes) {
        return new IStream<Long>() {
            
            private final IStream<Long> naturals = nat().skip(skip).every(parallelism);
            {
                next();
            }
            public String toString() {
                return "Natural.skip("+skip+").every("+parallelism+")";
            }
            @Override
            public Long root() {
                return naturals.root();
            }
            
            @Override
            public boolean hasNext() {
                return true;
            }
            
            @Override
            public Long next() {
                iterateNaturals:
                while (true) {
                    Long head = naturals.next();
                    
                    for (Long prime : savedPrimes) {
                        if (head % prime == 0) {
                            continue iterateNaturals;
                        }
                        assert prime * prime > 0;
                        if (prime * prime > head) {
                            return head;
                        }
                    }
                }
            }
        };
    }
    /**
     * Computes the stream of prime numbers in the following way:
     * 1. Start with the natural numbers at 2.
     * 2. The head of the stream is always a prime number so we take it as it is.
     * 3. Remove the stream of multiples of the head from the original stream
     * and push the stream.
     * 4. Go to 2.
     *
     * @return the stream of prime numbers
     */
    public static IStream<Long> eratosteneMultiples() {

        return new IStream<Long>() {
            Long root = 2l;
            /**
             * Split the source stream so we can use it twice,
             * for computing multiples and for the difference.
             */
            List<IStream<Long>> splitMultiplesFromDiffs = nat().skip(1).splitter();
            /**
             * Keep this stream for the source of "immediate" primes
             * (i.e. numbers that must be prime since they have not
             * been eliminated by previous iterations).
             */
            IStream<Long> next = nat().skip(1);

            @Override
            public Long root() {
                return root;
            }

            @Override
            public Long next() {
                final Long nextRootThatNeedsDiff = splitMultiplesFromDiffs.get(0).root();

                root = next.next();

                /**
                 * We don't need to do a diff if the root is
                 * below the square of the prime number that is
                 * on the head of the `splitMultiplesFromDiffs` stream. It is
                 * surely prime and will get its turn to be
                 * multiplied at a later date through the `splitMultiplesFromDiffs`
                 * stream.
                 */
                if (root.equals(nextRootThatNeedsDiff*nextRootThatNeedsDiff)) {

                    /**
                     * We don't need all the natural multiples of the
                     * root element, just the ones we haven't eliminated
                     * yet (i.e. the multiples obtained using the
                     * current stream)
                     *
                     */
                    List<IStream<Long>> multiply = splitMultiplesFromDiffs.get(0).map(i -> i * nextRootThatNeedsDiff).splitter();

                    /**
                     * Updating our copy of `splitMultiplesFromDiffs` ensures we don't try
                     * to uselessly eliminate multiples of multiples of primes
                     * (i.e. 4 = 2^2) at a later date.
                     */
                    splitMultiplesFromDiffs = diff(splitMultiplesFromDiffs.get(1).skip(1), multiply.get(0)).splitter();

                    /**
                     * Eliminate the multiples of the root element from
                     * the current source stream.
                     */
                    next = diff(next, multiply.get(1));
                    root = next.root();
                }

                return root;
            }
        };
    }

    public static IStream<Long> nat() {
        return nat(1l);
    }
    
    /**
     * A stream of numbers built using a Peano-style
     * successor function.
     *
     * @param first the first number of the naturals
     * @return the stream of natural numbers
     */
    public static IStream<Long> nat(final Long first) {
        return new IStream<Long>() {
            private Long root = first;

            @Override
            public Long root() {
                return root;
            }

            @Override
            public Long next() {
                root += 1;
                return root;
            }
        };
    }

    static void analyzeEratostene() {
        long start = System.currentTimeMillis();
        final int step = 10000;
        final int withSaveStep = 99;
        final int totalSteps = 400000;
        IStream<Long> eratostene = eratosteneWithSave().every(step);
        for (long i = 1; i <= withSaveStep; i++) {
            eratostene.next();
            markSignificantPrime(i, step, eratostene, start);
        }
        LazyListStream<Long> eratosteneWithSave = (LazyListStream<Long>)(eratostene.original());
        Long first = eratosteneWithSave.root();
        eratosteneWithSave.reset();
        System.out.println("Switching to eratosteneWithoutSaveSingleThread");
        IStream<Long> eratosteneWithoutSave = eratosteneWithoutSaveSingleThread(eratosteneWithSave,first).skip(step-1).every(step);
        for (long i = withSaveStep+1; i < totalSteps; i++) {
            markSignificantPrime(i, step, eratosteneWithoutSave, start);
            eratosteneWithoutSave.next();
        }
    }

    protected static void markSignificantPrime(long i, final int step, IStream<Long> eratostene, long start) {
        System.out.println("The " + (i * step) + "th prime number is " +
                eratostene.root() + " obtained in " +
                ((System.currentTimeMillis() - start) / 1000) + "s");
    }

    public static void main(String[] args) {
        analyzeEratostene();
    }
}
