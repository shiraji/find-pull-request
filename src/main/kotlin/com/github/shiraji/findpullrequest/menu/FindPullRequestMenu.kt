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
    private val disable: JCheckBox? = null
    private val debugMode: JCheckBox? = null
    private val jumpToFile: JCheckBox? = null
    private val protocols: JComboBox<Protocol>? = null
    private val root: JPanel? = null
    private val copyPopup: JCheckBox? = null
    private val hostingService: JComboBox<FindPullRequestHostingServices>? = null
    private val config: PropertiesComponent

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

        config = PropertiesComponent.getInstance(project)
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
