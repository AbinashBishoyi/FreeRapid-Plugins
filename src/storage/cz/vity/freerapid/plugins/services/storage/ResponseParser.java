package cz.vity.freerapid.plugins.services.storage;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

/**
 * @author Vity
 */
class ResponseParser {
    private final String input;

    public ResponseParser(String input) {
        this.input = input;
    }

    public String getString(String key) throws PluginImplementationException {
        return PlugUtils.getStringBetween(input, "'" + key + "' : '", "'");
    }

    public int getInt(String key) throws PluginImplementationException {
        return PlugUtils.getNumberBetween(input, "'" + key + "' : ", "");
    }
}
