package cz.vity.freerapid.plugins.services.itv;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author tong2shot
 */
class SettingsPanel extends JPanel {

    private SettingsConfig config;

    public SettingsPanel(ItvServiceImpl service) throws Exception {
        super();
        config = service.getConfig();
        initPanel();
    }

    private void initPanel() {
        final JCheckBox checkSubtitles = new JCheckBox("Download subtitles", config.isDownloadSubtitles());

        checkSubtitles.setAlignmentX(Component.LEFT_ALIGNMENT);

        checkSubtitles.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                config.setDownloadSubtitles(checkSubtitles.isSelected());
            }
        });

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(checkSubtitles);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    }

}
