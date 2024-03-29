
package chatty.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

/**
 * Writes a simple UTF-8 encoded textfile as a cache for Strings which can be
 * written/read easily. Saves a timestamp of when it was written into the file,
 * which is used to determine whether the cache expired when reading it.
 * 
 * @author tduva
 */
public class SimpleCache {
    
    private static final Logger LOGGER = Logger.getLogger(SimpleCache.class.getName());
    
    private static final Charset CHARSET = Charset.forName("UTF-8");
    
    private final String id;
    private final Path file;
    private final long expireTime;
    
    /**
     * Creates a new cache object for a single file.
     * 
     * @param id Used in debug messages to identify the cache contents
     * @param file The file to save into
     * @param expireTime The time in seconds that the cache should be valid for
     */
    public SimpleCache(String id, String file, long expireTime) {
        this.id = id;
        this.file = Paths.get(file);
        this.expireTime = expireTime;
    }
    
    /**
     * Saves the given text into the file specified for this cache.
     *
     * @param data The text to save
     */
    public void save(String data) {
        LOGGER.info("Cache: Trying to save "+id+"..");
        try (BufferedWriter writer = Files.newBufferedWriter(file,CHARSET)) {
            writer.write(new Long(System.currentTimeMillis() / 1000).toString()+"\n");
            writer.write(data);
            LOGGER.info("Cache: Saved "+id+".");
        }
        catch (IOException ex) {
            LOGGER.warning("Cache: Error saving "+id+" ["+ex+"]");
        }
    }
    
    /**
     * Load cached text from the file specified for this cache.
     *
     * @return The cached text or null if the file isn't recent enough or an
     * error occured
     */
    public String load() {
        LOGGER.info("Cache: Trying to load "+id+"..");
        try (BufferedReader reader = Files.newBufferedReader(file, CHARSET)) {
            long time = Long.parseLong(reader.readLine());
            long timePassed = (System.currentTimeMillis() / 1000) - time;
            if (timePassed > expireTime) {
                LOGGER.info("Cache: Did not load "+id+" (expired)");
                return null;
            }
            StringBuilder data = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                data.append(line);
                data.append("\n");
            }
            return data.toString();
        } catch (IOException | NumberFormatException ex) {
            LOGGER.warning("Cache: Error loading "+id+" ["+ex+"]");
            return null;
        }
    }
    
}
