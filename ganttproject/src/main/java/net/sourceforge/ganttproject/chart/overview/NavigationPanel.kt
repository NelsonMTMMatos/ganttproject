/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2010-2011 Dmitry Barashev, GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.sourceforge.ganttproject.chart.overview

import biz.ganttproject.app.FXToolbarBuilder
import biz.ganttproject.core.option.GPOption
import biz.ganttproject.core.option.IntegerOption
import javafx.scene.control.Button
import javafx.scene.control.ContentDisplay
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.action.scroll.*
import net.sourceforge.ganttproject.chart.TimelineChart
import net.sourceforge.ganttproject.gui.UIFacade
import java.awt.Component
import javax.swing.AbstractAction

class NavigationPanel(private val myProject: IGanttProject, private val myChart: TimelineChart, uiFacade: UIFacade) {
    private val myScrollActions: Array<AbstractAction>
    private val myScrollBackAction: GPAction
    private val myScrollForwardAction: GPAction
    private val myDpiOption: IntegerOption
    private val myLafOption: GPOption<String>

    init {
        myScrollActions = arrayOf(ScrollToStartAction(myProject, myChart),
                ScrollToTodayAction(myChart), ScrollToEndAction(myProject, myChart),
                ScrollToSelectionAction(uiFacade, myChart))
        myScrollBackAction = ScrollTimeIntervalAction("scroll.back", -1, myProject.taskManager, myChart.model,
                uiFacade.scrollingManager).also {
          it.putValue(GPAction.TEXT_DISPLAY, ContentDisplay.TEXT_ONLY)
        }
        myScrollForwardAction = ScrollTimeIntervalAction("scroll.forward", 1, myProject.taskManager,
                myChart.model, uiFacade.scrollingManager).also {
          it.putValue(GPAction.TEXT_DISPLAY, ContentDisplay.TEXT_ONLY)
        }
        myDpiOption = uiFacade.dpiOption
        myLafOption = uiFacade.lafOption
    }

    val component: Component by lazy {
      FXToolbarBuilder()
        .addButton(myScrollBackAction)
        .addButton(myScrollForwardAction)
        .addNode(Button("foo"))
        .withClasses("toolbar-common", "toolbar-small")
        .withScene()
        .build().component
    }
//    val component: Component
//        get() = ToolbarBuilder()
//                .withDpiOption(myDpiOption)
//                .withLafOption(myLafOption) { s -> if (s!!.indexOf("nimbus") >= 0) 2f else 1f }
//                .withHeight(24)
//                .withGapFactory(ToolbarBuilder.Gaps.VDASH)
//                .withBackground(myChart.style.spanningHeaderBackgroundColor)
//                .addComboBox(myScrollActions, myScrollActions[1])
//                .button(myScrollBackAction).withAutoRepeat(200).add()
//                .button(myScrollForwardAction).withAutoRepeat(200).add()
//                .build()
//                .toolbar
}