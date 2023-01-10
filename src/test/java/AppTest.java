import com.bingli.MyTest;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;


public class AppTest
{

    @Test
    public void testLoadAfterGC()
    {
        System.out.println("\n\ntestLoadAfterGC");
        assertTrue( "the result of Class.forName and ClassLoader.loadClass is not the same class", MyTest.test(true));
    }

    @Test
    public void testLoadWithNoGC()
    {
        System.out.println("\n\ntestLoadWithNoGC");
        assertFalse( "the result of Class.forName and ClassLoader.loadClass is the same class", MyTest.test(false));
    }
}
