package cz.vity.freerapid.plugins.services.rtmp;

/**
 * this interface is so that users can easily customize how the
 * client responds to the response of the server
 *
 * @author Peter Thomas
 */
public interface InvokeResultHandler {

    void handle(Invoke invoke, RtmpSession session);

}