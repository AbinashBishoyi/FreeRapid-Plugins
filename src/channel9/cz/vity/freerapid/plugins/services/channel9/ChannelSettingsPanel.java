package cz.vity.freerapid.plugins.services.channel9;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Abinash Bishoyi
 */
public class ChannelSettingsPanel extends JPanel {
    private Channel9SettingsConfig config;

    public ChannelSettingsPanel(Channel9ServiceImpl service) throws Exception {
        super();
        config = service.getConfig();
        initPanel();
    }

    private void initPanel() {
        final String[] qualityStrings = {"MP3 (Audio only)", "MP4 (iPod, Zune HD)", "Mid Quality WMV (Lo-band, Mobile)", "High Quality MP4 (iPad, PC)", "Mid Quality MP4 (WP7, HTML5)", "High Quality WMV (PC, Xbox, MCE)"};
        final int[] qualityIndexMap = {0, 1, 2, 3, 4, 5};

        final JLabel qualityLabel = new JLabel("Preferred quality level:");
        final JComboBox qualityList = new JComboBox(qualityStrings);
        qualityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        qualityList.setAlignmentX(Component.LEFT_ALIGNMENT);
        int qs = config.getQualitySetting();
        for (int i = 0; i < qualityIndexMap.length; i++) {
            if (qualityIndexMap[i] == qs) {
                qualityList.setSelectedIndex(i);
                break;
            }
        }

        qualityList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setQualitySetting(qualityIndexMap[qualityList.getSelectedIndex()]);
            }
        });

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(qualityLabel);
        add(qualityList);
        add(Box.createRigidArea(new Dimension(0, 15)));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    }

}