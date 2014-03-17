package cz.vity.freerapid.plugins.services.youtube;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Kajda, JPEXS, ntoskrnl
 */
public class YouTubeSettingsPanel extends JPanel {
    private YouTubeSettingsConfig config;

    public YouTubeSettingsPanel(YouTubeServiceImpl service) throws Exception {
        super();
        config = service.getConfig();
        initPanel();
    }

    private void initPanel() {
        final String[] qualityStrings = {"Highest available", "1080p (HD)", "720p (HD)", "480p", "360p", "240p", "Lowest available"};
        final int[] qualityIndexMap = {4, 6, 5, 3, 2, 1, 0}; //Due to quality settings in older versions, 4 is Highest available
        final String[] containerStrings = {"Any container", "MP4", "FLV", "WebM", "3GP"};

        final JLabel qualityLabel = new JLabel("Preferred quality level:");
        final JComboBox qualityList = new JComboBox(qualityStrings);
        final JLabel containerLabel = new JLabel("Preferred container:");
        final JComboBox containerList = new JComboBox(containerStrings);
        final JCheckBox orderCheckBox = new JCheckBox("Sort by newest first when adding links from user pages");
        final JCheckBox subtitlesCheckBox = new JCheckBox("Download subtitles whenever available");
        qualityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        qualityList.setAlignmentX(Component.LEFT_ALIGNMENT);
        containerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        containerList.setAlignmentX(Component.LEFT_ALIGNMENT);
        orderCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitlesCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        int qs = config.getQualitySetting();
        for (int i = 0; i < qualityIndexMap.length; i++) {
            if (qualityIndexMap[i] == qs) {
                qualityList.setSelectedIndex(i);
                break;
            }
        }
        containerList.setSelectedIndex(config.getContainer());
        orderCheckBox.setSelected(config.isReversePlaylistOrder());
        subtitlesCheckBox.setSelected(config.isDownloadSubtitles());

        qualityList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setQualitySetting(qualityIndexMap[qualityList.getSelectedIndex()]);
            }
        });
        containerList.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setContainer(containerList.getSelectedIndex());
            }
        });
        orderCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setReversePlaylistOrder(orderCheckBox.isSelected());
            }
        });
        subtitlesCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setDownloadSubtitles(subtitlesCheckBox.isSelected());
            }
        });
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(qualityLabel);
        add(qualityList);
        add(containerLabel);
        add(containerList);
        add(Box.createRigidArea(new Dimension(0, 15)));
        add(orderCheckBox);
        add(subtitlesCheckBox);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    }

}