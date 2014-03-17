package cz.vity.freerapid.plugins.container;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Subclasses must provide an initializer which takes one
 * {@link ContainerPlugin} as parameter.
 * The initializer should not throw exceptions.
 * Additionally, the must also provide a static method
 * {@code getSupportedFiles()} which returns {@code String[]}.
 *
 * @author ntoskrnl
 */
public interface ContainerFormat {

    /**
     * Reads links from a stream.
     *
     * @param is Source stream; guaranteed to be buffered
     *           and closed afterwards
     * @return List of files read from the file
     * @throws Exception If something goes wrong
     */
    public List<FileInfo> read(InputStream is) throws Exception;

    /**
     * Writes links to a stream.
     *
     * @param files Links to export
     * @param os    Destination stream; guaranteed to be buffered
     *              and closed afterwards (although it's a good idea
     *              to flush any wrapper streams before returning)
     * @throws Exception If something goes wrong
     */
    public void write(List<FileInfo> files, OutputStream os) throws Exception;

}
