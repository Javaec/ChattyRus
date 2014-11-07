
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
public class IrcTest {
    
    Irc irc;
    
    public IrcTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
        irc = new Irc() {

            @Override
            public void debug(String line) {
                System.err.println(line);
            }
            
            @Override
            void onChannelMessage (String channel, String nick, String from, String text) {
                System.out.println("Channel Message: "+channel+" "+nick+" "+from+" "+text);
            }
            
            @Override
            void onChannelAction (String channel, String nick, String from, String text) {
                System.out.println("Action Message: "+channel+" "+nick+" "+from+" "+text);
            }
        };
    }
    
    @After
    public void tearDown() {
    }

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}
    
    /**
     * This doesn't test if the messages are parsed correctly, but at least if
     * an unhandled exception occurs, which would stop the connection thread
     * completely.
     */
    @Test
    public void received() {
        // MODE
        try {
            irc.received(":jtv MODE #lotsofs +o da00");
            irc.received(":jtv MODE #lotsofs +o");
            irc.received("jtv MODE #lotsofs +o da00");
            irc.received("MODE #lotsofs +o da00");
            irc.received(": MODE #lotsofs +o da00");
        } catch (Exception ex) {
            fail("Exception: "+ex.toString());
        }
        // PRIVMSG
        try {
            irc.received(":jtv!jtv@jtv.tmi.twitch.tv PRIVMSG #lotsofs :EMOTESET tduva [130,793,33]");
            irc.received(":jtv!jtv@jtv.tmi.twitch.tv PRIVMSG #lotsofs :");
            irc.received(":jtv!jtv@jtv.tmi.twitch.tv PRIVMSG #lotsofs");
            irc.received(":jtv!jtvjtv.tmi.twitch.tv PRIVMSG #lotsofs :");
            irc.received(":jtv!jtvjtv.tmi.twitch.tv PRIVMSG #lotsofs :test");
            irc.received(":jtv!jtv@jtv.tmi.twitch.tv #lotsofs :test");
            irc.received(":jtv!jtv@jtv.tmi.twitch.tv PRIVMSG #lotsofs :"+(char)1+"ACTION action");
            irc.received(":jtv!jtv@jtv.tmi.twitch.tv PRIVMSG #lotsofs :"+(char)1+"ACTION");
        } catch (Exception ex) {
            fail("Exception: "+ex.toString());
        }
        // Short stuff
        try {
            irc.received(":");
            irc.received("");
            irc.received(" ");
        } catch (Exception ex) {
            fail("Exception: "+ex.toString());
        }
        // JOIN/PART
        try {
            irc.received(":tduva!tduva@tduva.tmi.twitch.tv JOIN #lotsofs");
            irc.received("tduva!tduva@tduva.tmi.twitch.tv JOIN #lotsofs");
            irc.received(":tduva!tduva@tduva.tmi.twitch.tv JOIN");
            irc.received(":tduvatduva@tduva.tmi.twitch.tv JOIN");
        } catch (Exception ex) {
            fail("Exception: "+ex.toString());
        }
        // PING
        try {
            irc.received("PING :tmi.twitch.tv");
            irc.received("PING :");
            irc.received("PING");
        } catch (Exception ex) {
            fail("Exception: "+ex.toString());
        }
        // NAMES
        try {
            irc.received(":tduva.tmi.twitch.tv 353 tduva = #lotsofs :krazyrasmus deenglow r4m80 adoms");
            irc.received(":tduva.tmi.twitch.tv 353 tduva #lotsofs :krazyrasmus deenglow r4m80 adoms");
            irc.received(":tduva.tmi.twitch.tv 353 tduva = #lotsofs :     ");
            irc.received(":tduva.tmi.twitch.tv 353 tduva = #lotsofs");
            irc.received(":tduva.tmi.twitch.tv 353 tduva #lotsofs :");
            irc.received(":tduva.tmi.twitch.tv 353 tduva #lotsofs");
            irc.received(":tduva.tmi.twitch.tv 366 tduva #lotsofs :End of /NAMES list");
            irc.received(":tduva.tmi.twitch.tv 366 tduva :End of /NAMES list");
            irc.received(":tduva.tmi.twitch.tv 366 tduva #lotsofs :");
            irc.received(":tduva.tmi.twitch.tv 366 tduva #lotsofs");
        } catch (Exception ex) {
            fail("Exception: "+ex.toString());
        }
    }
}
