/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ro.lazcatluc.lambda.streams;

import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Catalin
 */
public class EratosteneTest {
    
    public EratosteneTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of diff method, of class Eratostene.
     */
    @Test
    public void testDiff() {
        IStream<Long> source = Eratostene.nat();
        IStream<Long> even = new IStream<Long>() {
            long root = 2l;
            @Override
            public Long root() {
                return root;
            }

            @Override
            public Long next() {
                root+=2;
                return root;
            }
        };
        List<Long> expResult = Arrays.asList(1l,3l,5l,7l,9l);
        
        List<Long> result = Eratostene.diff(source, even).take(5);
        
        assertEquals(expResult, result);
    }

    /**
     * Test of eratostene method, of class Eratostene.
     */
    @Test
    public void firstTenPrimes() {
        List<Long> expResult = Arrays.asList(2l,3l,5l,7l,11l,13l,17l,19l,23l,29l);
        
        List<Long> result = Eratostene.eratostene().take(10);
        
        assertEquals(expResult, result);
    }

    /**
     * Test of nat method, of class Eratostene.
     */
    @Test
    public void firstFiveNaturalNumbers() {
        List<Long> expResult = Arrays.asList(1l,2l,3l,4l,5l);
        
        List<Long> result = Eratostene.nat().take(5);
        
        assertEquals(expResult, result);
    }

}