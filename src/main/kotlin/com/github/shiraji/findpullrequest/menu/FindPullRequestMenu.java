package com.github.shiraji.findpullrequest.menu;

import com.github.shiraji.findpullrequest.model.FindPullRequestConfig;
import com.github.shiraji.findpullrequest.model.FindPullRequestHostingServices;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class FindPullRequestMenu implements Configurable {
    private JCheckBox disable;
    private JCheckBox debugMode;
    private JCheckBox jumpToFile;
    private JComboBox<Protocol> protocols;
    private JPanel root;
    private JCheckBox copyPopup;
    private JComboBox<FindPullRequestHostingServices> hostingService;
    private PropertiesComponent config;

    private enum Protocol {
        https("https://"), http("http://");
        private String text;

        Protocol(String text) {
            this.text = text;
        }

        @NotNull
        public String getText() {
            return text;
        }

        @Nullable
        public static Protocol findProtocolBy(String name) {
            switch (name) {
                case "https://":
                    return https;
                case "http://":
                    return http;
                default:
                    return null;
            }
        }
    }

    FindPullRequestMenu(Project project) {
        super();

        config = PropertiesComponent.getInstance(project);
        protocols.setModel(new DefaultComboBoxModel<>(Protocol.values()));
        hostingService.setModel(new DefaultComboBoxModel<>(FindPullRequestHostingServices.values()));
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Find Pull Request Plugin";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return root;
    }

    @Override
    public boolean isModified() {
        return isModifiedProtocol()
                || isModifiedHostingService()
                || FindPullRequestConfig.isDebugMode(config) != debugMode.isSelected()
                || FindPullRequestConfig.isJumpToFile(config) != jumpToFile.isSelected()
                || FindPullRequestConfig.isDisable(config) != disable.isSelected()
                || FindPullRequestConfig.isPopupAfterCopy(config) != copyPopup.isSelected();
    }

    private boolean isModifiedProtocol() {
        Object selectedItem = protocols.getSelectedItem();
        return selectedItem instanceof Protocol && !FindPullRequestConfig.getProtocol(config).equals(
                ((Protocol)selectedItem).getText());
    }

    private boolean isModifiedHostingService() {
        Object selectedItem = hostingService.getSelectedItem();
        return selectedItem instanceof FindPullRequestHostingServices
                && !FindPullRequestConfig.getHosting(config).equals(((FindPullRequestHostingServices)selectedItem).name());
    }

    @Override
    public void apply() throws ConfigurationException {
        FindPullRequestConfig.setDisable(config, disable.isSelected());
        FindPullRequestConfig.setDebugMode(config, debugMode.isSelected());
        FindPullRequestConfig.setJumpToFile(config, jumpToFile.isSelected());
        FindPullRequestConfig.setPopupAfterCopy(config, copyPopup.isSelected());
        applyProtocol();
        applyHostingService();
    }

    private void applyProtocol() {
        Object selectedItem = protocols.getSelectedItem();
        if (selectedItem instanceof Protocol) {
            FindPullRequestConfig.setProtocol(config, ((Protocol)selectedItem).getText());
        }
    }

    private void applyHostingService() {
        Object selectedItem = hostingService.getSelectedItem();
        if (selectedItem instanceof FindPullRequestHostingServices) {
            FindPullRequestConfig.setHosting(config, (FindPullRequestHostingServices) selectedItem);
        }
    }

    @Override
    public void reset() {
        resetProtocols();
        resetHostingServices();
        disable.setSelected(FindPullRequestConfig.isDisable(config));
        debugMode.setSelected(FindPullRequestConfig.isDebugMode(config));
        jumpToFile.setSelected(FindPullRequestConfig.isJumpToFile(config));
        copyPopup.setSelected(FindPullRequestConfig.isPopupAfterCopy(config));
    }

    private void resetProtocols() {
        String protocol = FindPullRequestConfig.getProtocol(config);
        Protocol protocolBy = Protocol.findProtocolBy(protocol);
        int selectedIndex = 0;
        if (protocolBy != null) {
            selectedIndex = protocolBy.ordinal();
        }
        protocols.setSelectedIndex(selectedIndex);
    }

    private void resetHostingServices() {
        String hostingServiceConf = FindPullRequestConfig.getHosting(config);
        FindPullRequestHostingServices hostingServices = FindPullRequestHostingServices.findBy(hostingServiceConf);
        int selectedIndex = hostingServices.ordinal();
        hostingService.setSelectedIndex(selectedIndex);
    }

    @Override
    public void disposeUIResources() {

    }
}
