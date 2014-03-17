package cz.vity.freerapid.plugins.services.tunescoop;

/**
 * @author ntoskrnl
 */
public class TuneScoopSettingsConfig {
    private boolean isCustom;
    private String customName;

    public TuneScoopSettingsConfig() {
        setDefault();
    }

    public void setDefault() {
        setIsCustom(false);
        setCustomName("%ARTIST% - %SONG%");
    }

    public void setIsCustom(final boolean isCustom) {
        this.isCustom = isCustom;
    }

    public boolean getIsCustom() {
        return isCustom;
    }

    public void setCustomName(final String customName) {
        this.customName = customName;
    }

    public String getCustomName() {
        return customName;
    }

}