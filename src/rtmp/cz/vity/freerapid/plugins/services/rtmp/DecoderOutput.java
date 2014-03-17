package cz.vity.freerapid.plugins.services.rtmp;

/**
 * this interface is for being able to re-use the decode routine
 * in different contexts e.g. reading a network stream or bytes from
 * a file or just for testing
 *
 * @author Peter Thomas
 */
interface DecoderOutput {

    public void write(Object packet);

    public void disconnect();

}