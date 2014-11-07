
package chatty;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tduva
 */
public class HelperTest {
    
    public HelperTest() {
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

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}
    
    @Test
    public void validateChannelTest() {
        assertTrue(Helper.validateChannel("joshimuz"));
        assertTrue(Helper.validateChannel("51nn3r"));
        assertTrue(Helper.validateChannel("#joshimuz"));
        assertFalse(Helper.validateChannel("##joshimuz"));
        assertFalse(Helper.validateChannel(""));
        assertFalse(Helper.validateChannel(" "));
        assertFalse(Helper.validateChannel("abc$"));
    }
    
    @Test
    public void checkChannelTest() {
        assertEquals(Helper.checkChannel("abc"), "#abc");
        assertNull(Helper.checkChannel(""));
        assertNull(Helper.checkChannel("#"));
        assertNull(Helper.checkChannel(" 1"));
        assertEquals(Helper.checkChannel("#abc"), "#abc");
    }
    
    @Test
    public void removeDuplicateWhitespaceTest() {
        assertEquals(Helper.removeDuplicateWhitespace(" ")," ");
        assertEquals(Helper.removeDuplicateWhitespace(""), "");
        assertEquals(Helper.removeDuplicateWhitespace("abc"),"abc");
        assertEquals(Helper.removeDuplicateWhitespace("a  b"), "a b");
        assertEquals(Helper.removeDuplicateWhitespace("       "), " ");
        assertEquals(Helper.removeDuplicateWhitespace(" a  b  "), " a b ");
    }
}
