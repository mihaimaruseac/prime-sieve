/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ro.lazcatluc.lambda.streams;

import ro.lazcatluc.lambda.streams.IStream;
import ro.lazcatluc.lambda.streams.Eratostene;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 * @author Catalin
 */
public class IStreamTest {
    
    private IStream<Long> str;
    
    @Before
    public void before() {
        str = Eratostene.nat();
    }
    
    @Test
    public void testMap() {
        List<Long> expectedResult = Arrays.asList(2l,4l,6l,8l,10l);
        
        List<Long> result = str.map(i -> 2*i).take(5);
        
        assertEquals(expectedResult, result);
    }
    
    @Test
    public void testFilter() {
        List<Long> expectedResult = Arrays.asList(3l,6l,9l,12l,15l);
        
        List<Long> result = str.filter(i -> i%3==0).take(5);
        
        assertEquals(expectedResult, result);
    }
    
    @Test
    public void testEvery() {
        List<Long> expectedResult = Arrays.asList(1l,5l,9l,13l,17l);
        
        List<Long> result = str.every(4).take(5);
                
        assertEquals(expectedResult, result);
    }

    @Test
    public void testSkip() {
        List<Long> expectedResult = Arrays.asList(5l,6l,7l,8l,9l);
        
        List<Long> result = str.skip(4).take(5);
        
        assertEquals(expectedResult, result);
    }

    @Test
    public void testSplitter() {
        List<Long> expectedResult = Arrays.asList(1l,2l,3l,4l,5l,1l,2l,3l,4l,5l);
        
        List<IStream<Long>> split = str.splitter();
        List<Long> result = split.get(0).take(5);
        result.addAll(split.get(1).take(5));
        
        assertEquals(expectedResult, result);
    }

}
