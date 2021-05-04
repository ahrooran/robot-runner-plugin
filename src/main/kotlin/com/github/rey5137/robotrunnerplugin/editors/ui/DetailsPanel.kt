package com.github.rey5137.robotrunnerplugin.editors.ui

import com.github.rey5137.robotrunnerplugin.editors.ui.argument.ArgumentModel
import com.github.rey5137.robotrunnerplugin.editors.ui.argument.ArgumentTable
import com.github.rey5137.robotrunnerplugin.editors.ui.assignment.AssignmentModel
import com.github.rey5137.robotrunnerplugin.editors.ui.assignment.AssignmentTable
import com.github.rey5137.robotrunnerplugin.editors.xml.*
import com.intellij.ui.JBSplitter
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.migLayout.createLayoutConstraints
import icons.MyIcons
import net.miginfocom.layout.CC
import net.miginfocom.swing.MigLayout
import javax.swing.JPanel
import javax.swing.JTable


class DetailsPanel(private val robotElement: RobotElement)
    : JPanel(MigLayout(createLayoutConstraints(10, 10))) {

    private val nameField = JBTextField()
    private val statusLabel = JBLabel()
    private val tagsField = JBTextField()
    private val tabPane = JBTabbedPane()
    private val argumentModel = ArgumentModel()
    private val argumentTable =  ArgumentTable(argumentModel).apply {
        cellSelectionEnabled = true
        autoResizeMode = JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS
    }
    private val assignmentModel = AssignmentModel()
    private val assignmentTable = AssignmentTable(assignmentModel).apply {
        cellSelectionEnabled = true
        autoResizeMode = JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS
    }
    private val argumentSplitter = JBSplitter(true, 0.7F)
    private val messagePanel = JPanel()

    init {
        add(statusLabel, CC().cell(0, 0).minWidth("32px"))
        add(nameField, CC().cell(0, 0).growX().pushX(1F))
        nameField.isEditable = false

        add(JBLabel("Tags"), CC().cell(0, 1).minWidth("32px"))
        add(tagsField, CC().cell(0, 1).growX().pushX(1F))
        tagsField.isEditable = false

        add(tabPane, CC().newline().grow().push(1F, 1F))

        argumentSplitter.firstComponent = ToolbarDecorator.createDecorator(argumentTable)
            .disableUpAction()
            .disableDownAction()
            .disableRemoveAction()
            .createPanel()

        argumentSplitter.secondComponent = ToolbarDecorator.createDecorator(assignmentTable)
            .disableUpAction()
            .disableDownAction()
            .disableRemoveAction()
            .createPanel()
    }

    fun showDetails(element: Element) {
        if (element is HasCommonField) {
            nameField.text = element.name
            nameField.select(0, 0)
            statusLabel.icon = if (element.status.isPassed) MyIcons.StatusPass else MyIcons.StatusFail
        }

        if(element is HasTagsField) {
            tagsField.text = element.tags.joinToString(separator = ", ")
        }
        else {
            tagsField.text = ""
        }

        tabPane.removeAll()
        if(element is KeywordElement) {
            tabPane.add("Argument / Assigment", argumentSplitter)
            argumentModel.populateModel(element)
            argumentTable.adjustColumn(ArgumentModel.INDEX_ARGUMENT)
            argumentTable.adjustColumn(ArgumentModel.INDEX_INPUT)
            assignmentModel.populateModel(element)
            assignmentTable.adjustColumn(AssignmentModel.INDEX_ASSIGNMENT)

            tabPane.add("Messages", this.messagePanel)
        }
    }

    private fun ArgumentModel.populateModel(element: KeywordElement) {
        val message = element.messages.asSequence()
            .filter { it.level == "TRACE"}
            .mapNotNull { robotElement.messageMap[it.valueIndex] }
            .find { it.isArgumentMessage() }
        if(message == null)
            setArguments(
                List(element.arguments.size) { ARGUMENT_EMPTY },
                element.arguments.map { listOf(InputArgument(value = it, rawInput = it)) }
            )
        else {
            try {
                val arguments = message.parseArguments()
                val inputArguments = arguments.parseArgumentInputs(element.arguments)
                setArguments(arguments, inputArguments)
            }
            catch (ex: Exception) {
                ex.printStackTrace()
                setArguments(emptyList(), emptyList())
            }
        }
    }

    private fun AssignmentModel.populateModel(element: KeywordElement) {
        val assigns = element.assigns

        if(assigns.isEmpty()) {
            setAssignments(emptyList())
        }
        else {
            val message = element.messages.asSequence()
                .filter { it.level == "TRACE" }
                .mapNotNull { robotElement.messageMap[it.valueIndex] }
                .find { it.isReturnMessage() }

            try {
                setAssignments(assigns.parseAssignments(message?.parseReturn()))
            }
            catch (ex: Exception) {
                ex.printStackTrace()
                setAssignments(assigns.parseAssignments(null))
            }
        }

    }

}