package com.github.shiraji.findpullrequest.menu

import com.github.shiraji.findpullrequest.model.*
import com.github.shiraji.findpullrequest.model.FindPullRequestHostingServices
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.Nls

import javax.swing.*

class FindPullRequestMenu internal constructor(project: Project) : Configurable {
    private var disable: JCheckBox? = null
    private var debugMode: JCheckBox? = null
    private var jumpToFile: JCheckBox? = null
    private var protocols: JComboBox<Protocol>? = null
    private var root: JPanel? = null
    private var copyPopup: JCheckBox? = null
    private var hostingService: JComboBox<FindPullRequestHostingServices>? = null
    private var config: PropertiesComponent = PropertiesComponent.getInstance(project)

    private val isModifiedProtocol: Boolean
        get() {
            val selectedItem = protocols!!.selectedItem
            return selectedItem is Protocol && config.getProtocol() != selectedItem.text
        }

    private val isModifiedHostingService: Boolean
        get() {
            val selectedItem = hostingService!!.selectedItem
            return selectedItem is FindPullRequestHostingServices && config.getHosting() != selectedItem.name
        }

    private enum class Protocol private constructor(val text: String) {
        https("https://"), http("http://");

        companion object {

            fun findProtocolBy(name: String): Protocol? {
                when (name) {
                    "https://" -> return https
                    "http://" -> return http
                    else -> return null
                }
            }
        }
    }

    init {
        protocols!!.model = DefaultComboBoxModel(Protocol.values())
        hostingService!!.model = DefaultComboBoxModel(FindPullRequestHostingServices.values())
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
        return (isModifiedProtocol
            || isModifiedHostingService
            || config.isDebugMode() != debugMode!!.isSelected
            || config.isJumpToFile() != jumpToFile!!.isSelected
            || config.isDisable() != disable!!.isSelected
            || config.isPopupAfterCopy() != copyPopup!!.isSelected)
    }

    @Throws(ConfigurationException::class)
    override fun apply() {
        config.setDisable(disable!!.isSelected)
        config.setDebugMode(debugMode!!.isSelected)
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
        if (selectedItem is FindPullRequestHostingServices) {
            config.setHosting(selectedItem)
        }
    }

    override fun reset() {
        resetProtocols()
        resetHostingServices()
        disable!!.isSelected = config.isDisable()
        debugMode!!.isSelected = config.isDebugMode()
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
        val hostingServices = FindPullRequestHostingServices.findBy(hostingServiceConf)
        val selectedIndex = hostingServices.ordinal
        hostingService!!.selectedIndex = selectedIndex
    }

    override fun disposeUIResources() {
    }

}
