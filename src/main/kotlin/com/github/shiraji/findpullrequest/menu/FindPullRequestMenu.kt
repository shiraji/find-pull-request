package com.github.shiraji.findpullrequest.menu

import com.github.shiraji.findpullrequest.config.getHosting
import com.github.shiraji.findpullrequest.config.getProtocol
import com.github.shiraji.findpullrequest.config.isDisable
import com.github.shiraji.findpullrequest.config.isJumpToFile
import com.github.shiraji.findpullrequest.config.isPopupAfterCopy
import com.github.shiraji.findpullrequest.config.setDisable
import com.github.shiraji.findpullrequest.config.setHosting
import com.github.shiraji.findpullrequest.config.setJumpToFile
import com.github.shiraji.findpullrequest.config.setPopupAfterCopy
import com.github.shiraji.findpullrequest.config.setProtocol
import com.github.shiraji.findpullrequest.domain.HostingService
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.Nls
import javax.swing.DefaultComboBoxModel
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel

class FindPullRequestMenu internal constructor(project: Project) : Configurable {
    private var disable: JCheckBox? = null
    private var jumpToFile: JCheckBox? = null
    private var protocols: JComboBox<Protocol>? = null
    private var root: JPanel? = null
    private var copyPopup: JCheckBox? = null
    private var hostingService: JComboBox<HostingService>? = null
    private var config: PropertiesComponent = PropertiesComponent.getInstance(project)

    private val isModifiedProtocol: Boolean
        get() {
            val selectedItem = protocols!!.selectedItem
            return selectedItem is Protocol && config.getProtocol() != selectedItem.text
        }

    private val isModifiedHostingService: Boolean
        get() {
            val selectedItem = hostingService!!.selectedItem
            return selectedItem is HostingService && config.getHosting() != selectedItem.name
        }

    private enum class Protocol constructor(val text: String) {
        https("https://"), http("http://");

        companion object {

            fun findProtocolBy(name: String): Protocol? {
                return when (name) {
                    "https://" -> https
                    "http://" -> http
                    else -> null
                }
            }
        }
    }

    init {
        protocols!!.model = DefaultComboBoxModel(Protocol.values())
        hostingService!!.model = DefaultComboBoxModel(HostingService.values())
    }

    @Nls
    override fun getDisplayName(): String {
        return "Find Pull Request Plugin"
    }

    override fun getHelpTopic(): String? {
        return null
    }

    override fun createComponent(): JComponent? {
        return root
    }

    override fun isModified(): Boolean {
        return (isModifiedProtocol ||
            isModifiedHostingService ||
            config.isJumpToFile() != jumpToFile!!.isSelected ||
            config.isDisable() != disable!!.isSelected ||
            config.isPopupAfterCopy() != copyPopup!!.isSelected)
    }

    @Throws(ConfigurationException::class)
    override fun apply() {
        config.setDisable(disable!!.isSelected)
        config.setJumpToFile(jumpToFile!!.isSelected)
        config.setPopupAfterCopy(copyPopup!!.isSelected)
        applyProtocol()
        applyHostingService()
    }

    private fun applyProtocol() {
        val selectedItem = protocols!!.selectedItem
        if (selectedItem is Protocol) {
            config.setProtocol(selectedItem.text)
        }
    }

    private fun applyHostingService() {
        val selectedItem = hostingService!!.selectedItem
        if (selectedItem is HostingService) {
            config.setHosting(selectedItem)
        }
    }

    override fun reset() {
        resetProtocols()
        resetHostingServices()
        disable!!.isSelected = config.isDisable()
        jumpToFile!!.isSelected = config.isJumpToFile()
        copyPopup!!.isSelected = config.isPopupAfterCopy()
    }

    private fun resetProtocols() {
        val protocol = config.getProtocol()
        val protocolBy = Protocol.findProtocolBy(protocol)
        var selectedIndex = 0
        if (protocolBy != null) {
            selectedIndex = protocolBy.ordinal
        }
        protocols!!.selectedIndex = selectedIndex
    }

    private fun resetHostingServices() {
        val hostingServiceConf = config.getHosting()
        val hostingServices = HostingService.findBy(hostingServiceConf)
        val selectedIndex = hostingServices.ordinal
        hostingService!!.selectedIndex = selectedIndex
    }

    override fun disposeUIResources() {
    }
}
