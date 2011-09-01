/*
GanttProject is an opensource project management tool.
Copyright (C) 2002-2011 Alexandre Thomas, Dmitry Barashev, GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.sourceforge.ganttproject;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import net.sourceforge.ganttproject.action.EditMenu;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.action.ImportResources;
import net.sourceforge.ganttproject.action.NewArtefactAction;
import net.sourceforge.ganttproject.action.NewHumanAction;
import net.sourceforge.ganttproject.action.NewTaskAction;
import net.sourceforge.ganttproject.action.ResourceActionSet;
import net.sourceforge.ganttproject.action.SwitchViewAction;
import net.sourceforge.ganttproject.action.project.ProjectMenu;
import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.calendar.WeekendCalendarImpl;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.GanttChart;
import net.sourceforge.ganttproject.chart.ToggleChartAction;
import net.sourceforge.ganttproject.delay.DelayManager;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.document.DocumentsMRU;
import net.sourceforge.ganttproject.document.HttpDocument;
import net.sourceforge.ganttproject.document.OpenDocumentAction;
import net.sourceforge.ganttproject.document.Document.DocumentException;
import net.sourceforge.ganttproject.export.CommandLineExportApplication;
import net.sourceforge.ganttproject.gui.GanttDialogInfo;
import net.sourceforge.ganttproject.gui.GanttDialogPerson;
import net.sourceforge.ganttproject.gui.NotificationManagerImpl;
import net.sourceforge.ganttproject.gui.ResourceTreeUIFacade;
import net.sourceforge.ganttproject.gui.TaskTreeUIFacade;
import net.sourceforge.ganttproject.gui.TestGanttRolloverButton;
import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.about.AboutDialog;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.gui.scrolling.ScrollingManager;
import net.sourceforge.ganttproject.importer.Importer;
import net.sourceforge.ganttproject.io.GPSaver;
import net.sourceforge.ganttproject.io.GanttXMLOpen;
import net.sourceforge.ganttproject.io.GanttXMLSaver;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.parser.GPParser;
import net.sourceforge.ganttproject.parser.ParserFactory;
import net.sourceforge.ganttproject.plugins.PluginManager;
import net.sourceforge.ganttproject.print.PrintManager;
import net.sourceforge.ganttproject.print.PrintPreview;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.resource.ResourceEvent;
import net.sourceforge.ganttproject.resource.ResourceView;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.task.CustomColumnsStorage;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskManagerConfig;
import net.sourceforge.ganttproject.task.TaskNode;
import net.sourceforge.ganttproject.task.algorithm.AdjustTaskBoundsAlgorithm;
import net.sourceforge.ganttproject.task.algorithm.RecalculateTaskCompletionPercentageAlgorithm;
import net.sourceforge.ganttproject.time.TimeUnitStack;
import net.sourceforge.ganttproject.util.BrowserControl;

/**
 * Main frame of the project
 */
public class GanttProject extends GanttProjectBase implements ActionListener,
        ResourceView, KeyListener {

    /** The current version of ganttproject */
    public static final String version = GPVersion.V2_0_X;

    /** The language use */
    private GanttLanguage language = GanttLanguage.getInstance();

    /** The JTree part. */
    private GanttTree2 tree;

    /** GanttGraphicArea for the calendar with Gantt */
    private GanttGraphicArea area;

    /** GanttPeoplePanel to edit person that work on the project */
    private GanttResourcePanel resp;

    /** Menu */
    private JMenu mProject, mMRU, /*mEdit,*/ mTask, mHuman, mHelp, mServer,
            mCalendar;

    // public JMenu mView;

    /** Menuitem */
    private JMenuItem miPreview,/* miCut, miCopy, miPaste, miOptions,*/
            miDeleteTask, /* miUp, miDown, */miDelHuman, miSendMailHuman,
            miPrjCal, miWebPage, miAbout, miChartOptions;

    private static final int maxSizeMRU = 5;

    private DocumentsMRU documentsMRU = new DocumentsMRU(maxSizeMRU);

    /** Toolbar button */
    private TestGanttRolloverButton bSave, bCopy, bCut, bPaste, bNewTask, bDelete,
            bProperties;

    private TestGanttRolloverButton bUndo, bRedo;

    /** The project filename */
    public Document projectDocument = null;

    /** Informations for the current project. */
    public PrjInfos prjInfos = new PrjInfos();

    /** Boolean to know if the file has been modify */
    public boolean askForSave = false;

    /** Is the application only for viewer. */
    public boolean isOnlyViewer;

    private ResourceActionSet myResourceActions;

    private final TaskManager myTaskManager;

    private final FacadeInvalidator myFacadeInvalidator;

    private UIConfiguration myUIConfiguration;

    private final GanttOptions options;

    private JMenuBar bar;

    private JToolBar toolBar;

    private Action myTaskPropertiesAction;

    private NewTaskAction myNewTaskAction;

    private NewHumanAction myNewHumanAction;

    private NewArtefactAction myNewArtefactAction;

    private Action myDeleteHumanAction;

    private TaskContainmentHierarchyFacadeImpl myCachedFacade;

    private ArrayList<GanttPreviousState> myPreviousStates = new ArrayList<GanttPreviousState>();

    private MouseListener myStopEditingMouseListener = null;

    private DelayManager myDelayManager;

    private ProjectMenu myProjectMenu;

    private GanttChartTabContentPanel myGanttChartTabContent;

    private ResourceChartTabContentPanel myResourceChartTabContent;

    private RowHeightAligner myRowHeightAligner;

    private final EditMenu myEditMenu;

    @Override
    public TaskContainmentHierarchyFacade getTaskContainment() {
        if (myFacadeInvalidator == null) {
            return TaskContainmentHierarchyFacade.STUB;
        }
        if (!myFacadeInvalidator.isValid() || myCachedFacade == null) {
            myCachedFacade = new TaskContainmentHierarchyFacadeImpl(tree);
            myFacadeInvalidator.reset();
        }
        return myCachedFacade;
    }

    private void initOptions() {
        // Color color = GanttGraphicArea.taskDefaultColor;
        // myApplicationConfig.register(options);
        options.setUIConfiguration(myUIConfiguration);
        options.setDocumentsMRU(documentsMRU);
        if (options.load()) {
            GanttGraphicArea.taskDefaultColor = options.getDefaultColor();

            HttpDocument.setLockDAVMinutes(options.getLockDAVMinutes());
        }

        myUIConfiguration = options.getUIConfiguration();
    }

    public GanttProject(boolean isOnlyViewer, boolean isApplet) {
        System.err.println("Creating main frame...");
        ToolTipManager.sharedInstance().setInitialDelay(200);
        ToolTipManager.sharedInstance().setDismissDelay(60000);

        Mediator.registerTaskSelectionManager(getTaskSelectionManager());
        /*
         * [bbaranne] I add a Mediator object so that we can get the
         * GanttProject singleton where ever we are in the source code. Perhaps
         * some of you don't like this, but I believe that it is practical...
         */
        Mediator.registerGanttProject(this);

        this.isOnlyViewer = isOnlyViewer;
        if (!isOnlyViewer) {
            setTitle(language.getText("appliTitle"));
        } else {
            setTitle("GanttViewer");
        }
        setFocusable(true);
        System.err.println("1. loading look'n'feels");
        options = new GanttOptions(getRoleManager(), getDocumentManager(),
                isOnlyViewer);
        myUIConfiguration = options.getUIConfiguration();
        class TaskManagerConfigImpl implements TaskManagerConfig {
            public Color getDefaultColor() {
                return myUIConfiguration.getTaskColor();
            }

            public GPCalendar getCalendar() {
                return GanttProject.this.getActiveCalendar();
            }

            public TimeUnitStack getTimeUnitStack() {
                return GanttProject.this.getTimeUnitStack();
            }

            public HumanResourceManager getResourceManager() {
                return GanttProject.this.getHumanResourceManager();
            }

            public URL getProjectDocumentURL() {
                try {
                    return getDocument().getURI().toURL();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    return null;
                }
            }

        }
        TaskManagerConfig taskConfig = new TaskManagerConfigImpl();
        myTaskManager = TaskManager.Access.newInstance(
                new TaskContainmentHierarchyFacade.Factory() {
                    public TaskContainmentHierarchyFacade createFacede() {
                        return GanttProject.this.getTaskContainment();
                    }
                }, taskConfig, getCustomColumnsStorage());
        ImageIcon icon = new ImageIcon(getClass().getResource(
                "/icons/ganttproject.png"));
        setIconImage(icon.getImage());

        // Create each objects
        myFacadeInvalidator = new FacadeInvalidator(getTree().getJTree()
                .getModel());
        getProject().addProjectEventListener(myFacadeInvalidator);
        area = new GanttGraphicArea(this, getTree(), getTaskManager(),
                getZoomManager(), getUndoManager());
        options.addOptionGroups(new GPOptionGroup[] {getUIFacade().getOptions()});
        options.addOptionGroups(getUIFacade().getGanttChart().getOptionGroups());
        options.addOptionGroups(getUIFacade().getResourceChart().getOptionGroups());
        options.addOptionGroups(new GPOptionGroup[] { getProjectUIFacade().getOptionGroup() });
        options.addOptionGroups(getDocumentManager().getNetworkOptionGroups());
        options.addOptions(getRssFeedChecker().getOptions());
        myRowHeightAligner = new RowHeightAligner(tree, area.getMyChartModel());
        area.getMyChartModel().addOptionChangeListener(myRowHeightAligner);
        System.err.println("2. loading options");
        initOptions();
        area.setUIConfiguration(myUIConfiguration);
        getTree().setGraphicArea(area);

        miChartOptions = new JMenuItem(area.getOptionsDialogAction());

        getZoomManager().addZoomListener(area.getZoomListener());


        System.err.println("3. creating menu...");

        bar = new JMenuBar();
        setJMenuBar(bar);
        // Allocation of the menus
        mProject = new JMenu();
        mMRU = new JMenu();
        mMRU.setIcon(new ImageIcon(getClass().getResource(
                "/icons/recent_16.gif")));
        // mView = new JMenu ();
        mTask = new JMenu();
        mHuman = new JMenu();
        mHelp = new JMenu();
        mCalendar = new JMenu();

        createProjectMenu();
        bar.add(mProject);
        myEditMenu = new EditMenu(getProject(), getUIFacade(), getViewManager());
        bar.add(myEditMenu.create());

        myNewTaskAction = new NewTaskAction(getProject(), getUndoManager());
        mTask.add(myNewTaskAction);
        miDeleteTask = createNewItem("/icons/delete_16.gif");
        mTask.add(miDeleteTask);
        myTaskPropertiesAction = getTree().getTaskPropertiesAction();
        mTask.add(myTaskPropertiesAction);
        getResourcePanel().setTaskPropertiesAction(myTaskPropertiesAction);

        myNewHumanAction = new NewHumanAction(getHumanResourceManager(), this) {
            @Override
            public void actionPerformed(ActionEvent event) {
                super.actionPerformed(event);
                getTabs().setSelectedIndex(UIFacade.RESOURCES_INDEX);
            }
        };

        mHuman.add(myNewHumanAction);
        myDeleteHumanAction = getResourceActions().getDeleteHumanAction();
        miDelHuman = new JMenuItem(myDeleteHumanAction);
        mHuman.add(miDelHuman);
        // miPropHuman = createNewItem("/icons/properties_16.gif");
        // mHuman.add(miPropHuman);
        mHuman.add(getResourcePanel().getResourcePropertiesAction());
        miSendMailHuman = createNewItem("/icons/send_mail_16.gif");
        mHuman.add(miSendMailHuman);

        mHuman.add(new ImportResources(getHumanResourceManager(),
                getTaskManager(), getRoleManager(), this));

        miPrjCal = createNewItem("/icons/default_calendar_16.gif");
        mCalendar.add(miPrjCal);
        miWebPage = createNewItem("/icons/home_16.gif");
        mHelp.add(miWebPage);
        miAbout = createNewItem("/icons/manual_16.gif");
        mHelp.add(miAbout);
        JMenu viewMenu = createViewMenu();
        if (viewMenu != null)
            bar.add(viewMenu);
        // bar.add (mView);
        bar.add(mTask);
        bar.add(mHuman);
        // bar.add(mCalendar);
        bar.add(mHelp);
        setMnemonic();
        // to create a default project
        // createDefaultTree(tree);
        System.err.println("4. creating views...");
        myGanttChartTabContent = new GanttChartTabContentPanel(
            getProject(), getUIFacade(), getTaskTree(), area, getUIConfiguration());
        GPView ganttView = getViewManager().createView(myGanttChartTabContent,
                new ImageIcon(getClass().getResource("/icons/tasks_16.gif")));
        ganttView.setVisible(true);
        myResourceChartTabContent = new ResourceChartTabContentPanel(
                getProject(), getUIFacade(), getResourcePanel(),
                getResourcePanel().area);
        GPView resourceView = getViewManager().createView(
                myResourceChartTabContent,
                new ImageIcon(getClass().getResource("/icons/res_16.gif")));
        resourceView.setVisible(true);
        getTabs().setSelectedIndex(0);

        // pert area
        // getTabs().setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        getTabs().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                bNewTask.setEnabled(getTabs().getSelectedIndex() == UIFacade.GANTT_INDEX
                                || getTabs().getSelectedIndex() == UIFacade.RESOURCES_INDEX);

                bDelete.setEnabled(getTabs().getSelectedIndex() == UIFacade.GANTT_INDEX
                                || getTabs().getSelectedIndex() == UIFacade.RESOURCES_INDEX);

                bProperties.setEnabled(getTabs().getSelectedIndex() == UIFacade.GANTT_INDEX
                                || getTabs().getSelectedIndex() == UIFacade.RESOURCES_INDEX);

                if (getTabs().getSelectedIndex() == UIFacade.GANTT_INDEX) {
                    // Gantt Chart
                    bNewTask.setToolTipText(getToolTip(language.getCorrectedLabel("createTask")));
                    bDelete.setToolTipText(getToolTip(language.getCorrectedLabel("deleteTask")));
                    bProperties.setToolTipText(getToolTip(language.getCorrectedLabel("propertiesTask")));

                    if (options.getButtonShow() != GanttOptions.ICONS) {
                        bNewTask.setText(language.getCorrectedLabel("createTask"));
                        bDelete.setText(language.getCorrectedLabel("deleteTask"));
                        bProperties.setText(language.getCorrectedLabel("propertiesTask"));
                    }

                } else if (getTabs().getSelectedIndex() == UIFacade.RESOURCES_INDEX) {
                    // Resources Chart
                    bNewTask.setToolTipText(getToolTip(language.getCorrectedLabel("newHuman")));
                    bDelete.setToolTipText(getToolTip(language.getCorrectedLabel("deleteHuman")));
                    bProperties.setToolTipText(getToolTip(language.getCorrectedLabel("resource.properties")));

                    if (options.getButtonShow() != GanttOptions.ICONS) {
                        bNewTask.setText(language.getCorrectedLabel("newHuman"));
                        bDelete.setText(language.getCorrectedLabel("deleteHuman"));
                        bProperties.setText(language.getCorrectedLabel("resource.properties"));
                    }
                }
            }
        });
        // Add tab pane on the content pane
        getContentPane().add(getTabs(), BorderLayout.CENTER);
        // Add toolbar
        toolBar = new JToolBar();
        toolBar.addComponentListener(new ComponentListener() {

            public void componentResized(ComponentEvent arg0) {
                setHiddens();
                refresh();
            }

            public void componentMoved(ComponentEvent arg0) {
            }

            public void componentShown(ComponentEvent arg0) {
            }

            public void componentHidden(ComponentEvent arg0) {
            }
        });
        this.addButtons(toolBar);
        getContentPane()
                .add(
                        toolBar,
                        (toolBar.getOrientation() == JToolBar.HORIZONTAL) ? BorderLayout.NORTH
                                : BorderLayout.WEST);

        // add the status bar
        if (!isOnlyViewer)
            getContentPane().add(getStatusBar(), BorderLayout.SOUTH);
        getStatusBar().setVisible(options.getShowStatusBar());

        // add a keyboard listener
        addKeyListener(this);

        SwitchViewAction switchAction = new SwitchViewAction(this);
        JMenuItem invisibleItem = new JMenuItem(switchAction);
        invisibleItem.setVisible(false);
        bar.add(invisibleItem);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent evt) {
                exitForm();
            }

            @Override
            public void windowOpened(WindowEvent e) {
                myRowHeightAligner.optionsChanged();
                ((NotificationManagerImpl)getNotificationManager()).showPending();
                getRssFeedChecker().run();

            }

        });

        System.err.println("5. calculating size and packing...");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension windowSize = getPreferredSize();
        // Put the frame at the middle of the screen
        setLocation(screenSize.width / 2 - (windowSize.width / 2),
                screenSize.height / 2 - (windowSize.height / 2));
        this.pack();

        System.err.println("6. changing language ...");
        changeLanguage();

        // changeUndoNumber ();
        System.err.println("7. changing look'n'feel ...");
        getUIFacade().setLookAndFeel(getUIFacade().getLookAndFeel());
        if (options.isLoaded()) {
            setBounds(options.getX(), options.getY(), options.getWidth(),
                    options.getHeight());
        }
        System.err.println("8. finalizing...");
//        applyComponentOrientation(GanttLanguage.getInstance()
//                .getComponentOrientation());
        myTaskManager.addTaskListener(new TaskModelModificationListener(this));
        if (ourWindowListener != null) {
            addWindowListener(ourWindowListener);
        }
        addMouseListenerToAllContainer(this.getComponents());
        myDelayManager = new DelayManager(myTaskManager, getUndoManager(), tree);
        Mediator.registerDelayManager(myDelayManager);
        myDelayManager.addObserver(tree);

        getRootPane()
                .getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "refresh");
        getRootPane().getActionMap().put("refresh", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                getActiveChart().reset();
                repaint();
            }
        });
        this.setModified(false);
    }

    private void addMouseListenerToAllContainer(Component[] cont) {
        for (int i = 0; i < cont.length; i++) {
            cont[i].addMouseListener(getStopEditingMouseListener());
            if (cont[i] instanceof Container)
                addMouseListenerToAllContainer(((Container) cont[i])
                        .getComponents());
        }
    }

    /**
     * @return A mouseListener that stop the edition in the ganttTreeTable.
     */
    private MouseListener getStopEditingMouseListener() {
        if (myStopEditingMouseListener == null)
            myStopEditingMouseListener = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getSource() != bNewTask && e.getClickCount() == 1)
                        tree.stopEditing();
                    if (e.getButton() == MouseEvent.BUTTON1
                            && !(e.getSource() instanceof JTable)
                            && !(e.getSource() instanceof AbstractButton)) {
                        Task taskUnderPointer = area.new MouseSupport()
                                .findTaskUnderMousePointer(e.getX(), e.getY());
                        if (taskUnderPointer == null) {
                            getTaskSelectionManager().clear();
                        }
                    }
                }
            };
        return myStopEditingMouseListener;
    }

    private void createProjectMenu() {
        mServer = new JMenu();
        mServer.setIcon(new ImageIcon(getClass().getResource(
                "/icons/server_16.gif")));
        myProjectMenu = new ProjectMenu(this);
        mProject.add(myProjectMenu.getProjectSettingsAction());
        mProject.add(myProjectMenu.getNewProjectAction());
        mProject.add(myProjectMenu.getOpenProjectAction());
        mProject.add(mMRU);
        updateMenuMRU();
        mProject.addSeparator();
        mProject.add(myProjectMenu.getSaveProjectAction());
        mProject.add(myProjectMenu.getSaveProjectAsAction());
        mProject.addSeparator();

        mProject.add(myProjectMenu.getImportFileAction());
        mProject.add(myProjectMenu.getExportFileAction());
        mProject.addSeparator();

        mServer.add(myProjectMenu.getOpenURLAction());
        mServer.add(myProjectMenu.getSaveURLAction());
        mProject.add(mServer);
        mProject.addSeparator();
        mProject.add(myProjectMenu.getPrintAction());
        miPreview = createNewItem("/icons/preview_16.gif");
        mProject.add(miPreview);
        mProject.addSeparator();
        mProject.add(myProjectMenu.getExitAction());
    }

    private JMenu createViewMenu() {
        JMenu result = changeMenuLabel(new JMenu(), language.getText("view"));
        result.add(miChartOptions);
        List<Chart> charts = PluginManager.getCharts();

        if (!charts.isEmpty()) {
            result.addSeparator();
        }
        for (Chart chart : charts) {
            result.add(new JCheckBoxMenuItem(new ToggleChartAction(chart, getViewManager())));
        }
        return result;
    }

    public GanttProject(boolean isOnlyViewer) {
        this(isOnlyViewer, false);
    }

    /**
     * Updates the last open file menu items.
     */
    private void updateMenuMRU() {
        mMRU.removeAll();
        int index = 0;
        Iterator<Document> iterator = documentsMRU.iterator();
        while (iterator.hasNext()) {
            index++;
            Document document = iterator.next();
            JMenuItem mi = new JMenuItem(new OpenDocumentAction(index,
                    document, this));
            mMRU.add(mi);
        }
    }

    public String getXslDir() {
        return options.getXslDir();
    }

    /** @return the options of ganttproject. */
    public GanttOptions getGanttOptions() {
        return options;
    }

    public void restoreOptions() {
        options.initDefault();
        myUIConfiguration = options.getUIConfiguration();
        GanttGraphicArea.taskDefaultColor = new Color(140, 182, 206);
        area.repaint();
    }

    // /** @return the status Bar of the main frame. */
    // public GanttStatusBar getStatusBar() {
    // return statusBar;
    // }

    public String getXslFo() {
        return options.getXslFo();
    }

    /** Create mnemonic for keyboard */
    public void setMnemonic() {
        int MENU_MASK = GPAction.MENU_MASK;

        miDeleteTask.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, MENU_MASK));
    }

    /** Create an item with a label */
    public JMenuItem createNewItemText(String label) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(this);
        return item;
    }

    /** Create an item with an icon */
    public JMenuItem createNewItem(String icon) {
        URL url = getClass().getResource(icon);
        JMenuItem item = url == null ? new JMenuItem() : new JMenuItem(
                new ImageIcon(url));
        item.addActionListener(this);
        return item;
    }

    /** Create an item with a label and an icon */
    public JMenuItem createNewItem(String label, String icon) {
        JMenuItem item = new JMenuItem(label, new ImageIcon(getClass()
                .getResource(icon)));
        item.addActionListener(this);
        return item;
    }

    /** Function to change language of the project */
    public void changeLanguage() {
        applyComponentOrientation(language.getComponentOrientation());
        changeLanguageOfMenu();
        area.repaint();
        getResourcePanel().area.repaint();
        getResourcePanel().refresh(language);

        this.tree.changeLanguage(language);
        CustomColumnsStorage.changeLanguage(language);

        applyComponentOrientation(language.getComponentOrientation());
    }

    /**
     * Change the label for menu, in fact check in the label contains a mnemonic
     */
    public JMenu changeMenuLabel(JMenu menu, String label) {
        int index = label.indexOf('$');
        if (index != -1 && label.length() - index > 1) {
            menu.setText(label.substring(0, index).concat(
                    label.substring(++index)));
            menu.setMnemonic(Character.toLowerCase(label.charAt(index)));
        } else {
            menu.setText(label);
            // menu.setMnemonic('');
        }
        return menu;
    }

    /**
     * Change the label for menuItem, in fact check in the label contains a
     * mnemonic
     */
    public JMenuItem changeMenuLabel(JMenuItem menu, String label) {
        int index = label.indexOf('$');
        if (index != -1 && label.length() - index > 1) {
            menu.setText(label.substring(0, index).concat(
                    label.substring(++index)));
            menu.setMnemonic(Character.toLowerCase(label.charAt(index)));
        } else {
            menu.setText(label);
            // menu.setMnemonic('');
        }
        return menu;
    }

    /**
     * Change the label for JCheckBoxmenuItem, in fact check in the label
     * contains a mnemonic
     */
    public JCheckBoxMenuItem changeMenuLabel(JCheckBoxMenuItem menu,
            String label) {
        int index = label.indexOf('$');
        if (index != -1 && label.length() - index > 1) {
            menu.setText(label.substring(0, index).concat(
                    label.substring(++index)));
            menu.setMnemonic(Character.toLowerCase(label.charAt(index)));
        } else {
            menu.setText(label);
            // menu.setMnemonic('');
        }
        return menu;
    }

    /** Set the menus language after the user select a different language */
    private void changeLanguageOfMenu() {
        mProject = changeMenuLabel(mProject, language.getText("project"));
        mTask = changeMenuLabel(mTask, language.getText("task"));
        mHuman = changeMenuLabel(mHuman, language.getText("human"));
        mHelp = changeMenuLabel(mHelp, language.getText("help"));
        mCalendar = changeMenuLabel(mCalendar, language.getText("calendars"));
        mMRU = changeMenuLabel(mMRU, language.getText("lastOpen"));

        mServer = changeMenuLabel(mServer, language.getText("webServer"));
        miPreview = changeMenuLabel(miPreview, language.getText("preview"));
        // miNewTask = changeMenuLabel(miNewTask,
        // language.getText("createTask"));
        miDeleteTask = changeMenuLabel(miDeleteTask, language.getText("deleteTask"));
        mHuman.insert(changeMenuLabel(mHuman.getItem(0), language.getText("newHuman")), 0);
        miDelHuman = changeMenuLabel(miDelHuman, language.getText("deleteHuman"));
        mHuman.insert(changeMenuLabel(mHuman.getItem(4), language.getText("importResources")), 4);
        miSendMailHuman = changeMenuLabel(miSendMailHuman, language.getText("sendMail"));

        miPrjCal = changeMenuLabel(miPrjCal, language.getText("projectCalendar"));

        miWebPage = changeMenuLabel(miWebPage, language.getText("webPage"));
        miAbout = changeMenuLabel(miAbout, language.getText("about"));
        miChartOptions = changeMenuLabel(miChartOptions, language.getText("chartOptions"));

        bNewTask.setToolTipText(getToolTip(language.getCorrectedLabel("createTask")));
        // bCut.setToolTipText(getToolTip(language.getCorrectedLabel("cut")));
        // bCopy.setToolTipText(getToolTip(language.getCorrectedLabel("copy")));
        // bPaste.setToolTipText(getToolTip(language.getCorrectedLabel("paste")));
        bDelete.setToolTipText(getToolTip(language.getCorrectedLabel("deleteTask")));
        bProperties.setToolTipText(getToolTip(language.getCorrectedLabel("propertiesTask")));
        bUndo.setToolTipText(getToolTip(language.getCorrectedLabel("undo")));
        bRedo.setToolTipText(getToolTip(language.getCorrectedLabel("redo")));
        getTabs().setTitleAt(1, language.getCorrectedLabel("human"));
    }

    /** Invoked when a key has been pressed. */
    public void keyPressed(KeyEvent e) {
        // Consume the event to prevent it to go farther.
        int code = e.getKeyCode();
        if (code == KeyEvent.KEY_LOCATION_UNKNOWN) {
            e.consume();
        }

        switch (code) {
        case KeyEvent.VK_DELETE:
            e.consume();
            if (!isOnlyViewer) {
                if (getViewIndex() == UIFacade.GANTT_INDEX)
                    deleteTasks(true);
                else if (getViewIndex() == UIFacade.RESOURCES_INDEX) {
                    deleteResources();
                }
            }
            break;
        case KeyEvent.VK_ENTER:
            break;
        case KeyEvent.VK_F5:
            e.consume();
            getActiveChart().reset();
            repaint();
            break;
        }
    }

    /** Invoked when a key has been released. */
    public void keyReleased(KeyEvent e) {
    }

    /** Invoked when a key has been typed. */
    public void keyTyped(KeyEvent e) {
    }

    /** Return the ToolTip in HTML (with gray bgcolor) */
    public static String getToolTip(String msg) {
        return "<html><body bgcolor=#EAEAEA>" + msg + "</body></html>";
    }


    /** Create the button on toolbar */
    private void addButtons(JToolBar toolBar) {
        bSave = new TestGanttRolloverButton(myProjectMenu.getSaveProjectAction());
        bCut = new TestGanttRolloverButton(getCutAction());
        bCopy = new TestGanttRolloverButton(getCopyAction());
        bPaste = new TestGanttRolloverButton(getPasteAction());

        myNewArtefactAction = new NewArtefactAction(
                new NewArtefactAction.ActiveActionProvider() {
                    public AbstractAction getActiveAction() {
                        return getTabs().getSelectedIndex() == UIFacade.GANTT_INDEX ? (AbstractAction) myNewTaskAction
                                : (AbstractAction) myNewHumanAction;

                    }
                }, options.getIconSize());
        bNewTask = new TestGanttRolloverButton(myNewArtefactAction);
        bDelete = new TestGanttRolloverButton(
                new ImageIcon(getClass().getResource(
                        "/icons/delete_" + options.getIconSize() + ".gif")));
        bDelete.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (getTabs().getSelectedIndex() == UIFacade.GANTT_INDEX) {// Gantt
                    deleteTasks(true);
                } else if (getTabs().getSelectedIndex() == UIFacade.RESOURCES_INDEX) { // Resource
                    // chart
                    final HumanResource[] context = getResourcePanel()
                            .getContext().getResources();
                    if (context.length > 0) {
                        Choice choice = getUIFacade().showConfirmationDialog(
                                getLanguage().getText("msg6")
                                        + getDisplayName(context) + "?",
                                getLanguage().getText("question"));
                        if (choice == Choice.YES) {
                            getUndoManager().undoableEdit("Delete Human OK",
                                    new Runnable() {
                                        public void run() {
                                            for (int i = 0; i < context.length; i++) {
                                                context[i].delete();
                                            }
                                        }
                                    });
                            repaint2();
                            refreshProjectInfos();
                        }
                    }
                }
            }
        });

        bProperties = new TestGanttRolloverButton(new ImageIcon(getClass()
                .getResource(
                        "/icons/properties_" + options.getIconSize() + ".gif")));
        bProperties.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (getTabs().getSelectedIndex() == UIFacade.GANTT_INDEX) {// Gantt
                    // Chart
                    propertiesTask();
                } else if (getTabs().getSelectedIndex() == UIFacade.RESOURCES_INDEX) { // Resource
                    // chart
                    getResourcePanel().getResourcePropertiesAction()
                            .actionPerformed(null);
                }
            }
        });

        ScrollingManager scrollingManager = getScrollingManager();
        scrollingManager.addScrollingListener(area.getViewState());
        scrollingManager.addScrollingListener(getResourcePanel().area
                .getViewState());
        bUndo = new TestGanttRolloverButton(myEditMenu.getUndoAction());
        bRedo = new TestGanttRolloverButton(myEditMenu.getRedoAction());

        toolBar.add(bSave);
        toolBar.add(bUndo);
        toolBar.add(bRedo);
        toolBar.addSeparator();
        toolBar.add(bCut);
        toolBar.add(bCopy);
        toolBar.add(bPaste);
        toolBar.addSeparator();
        toolBar.add(bNewTask);
        toolBar.add(bDelete);
        toolBar.add(bProperties);
    }

    public List<GanttPreviousState> getBaselines() {
        return myPreviousStates;
    }

    private void aboutDialog() {
        AboutDialog agp = new AboutDialog(this);
        agp.setVisible(true);
    }

    private String getDisplayName(Object[] objs) {
        if (objs.length == 1) {
            return objs[0].toString();
        }
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < objs.length; i++) {
            result.append(objs[i].toString());
            if (i < objs.length - 1) {
                result.append(", ");
            }
        }
        return result.toString();
    }

    /** Exit the Application */
    private void exitForm() {
        quitApplication();
    }

    /** A menu has been activate */
    public void actionPerformed(ActionEvent evt) {
        Object source = evt.getSource();
        if (source instanceof JMenuItem) {
            if (source == miPreview) {
                previewPrint();
            } else if (source == miDeleteTask) {
                deleteTasks(true);
            } else if (source == miPrjCal) {
                System.out.println("Project calendar");
            } else if (source == miWebPage) {
                    openWebPage();
            } else if (source == miAbout) {
                aboutDialog();
            } else if (source == miSendMailHuman) {
                getTabs().setSelectedIndex(1);
                getResourcePanel().sendMail(this);
            }
        } else if (source instanceof Document) {
            if (getProjectUIFacade().ensureProjectSaved(getProject())) {
                openStartupDocument((Document) source);
            }
        }
    }

    public HumanResource newHumanResource() {
        final HumanResource people = getHumanResourceManager().newHumanResource();
        people.setRole(getRoleManager().getDefaultRole());
        GanttDialogPerson dp = new GanttDialogPerson(getUIFacade(),
                getLanguage(), people);
        dp.setVisible(true);
        if (dp.result()) {

            getUndoManager().undoableEdit("new Resource", new Runnable() {
                public void run() {
                    getHumanResourceManager().add(people);
                }
            });
        }
        return people;
    }

    /** Create a new task */
    @Override
    public Task newTask() {

        getTabs().setSelectedIndex(UIFacade.GANTT_INDEX);

        int index = -1;
        MutableTreeNode selectedNode = getTree().getSelectedNode();
        if (selectedNode != null) {
            DefaultMutableTreeNode parent1 = (DefaultMutableTreeNode) selectedNode
                    .getParent();
            index = parent1.getIndex(selectedNode) + 1;
            tree.getTreeTable().getTree().setSelectionPath(
                    new TreePath(parent1.getPath()));
            tree.getTreeTable().getTreeTable().editingStopped(
                    new ChangeEvent(tree.getTreeTable().getTreeTable()));
        }

        GanttCalendar cal = new GanttCalendar(area.getStartDate());

        DefaultMutableTreeNode node = tree.getSelectedNode();
        String nameOfTask = getTaskManager().getTaskNamePrefixOption().getValue();
        GanttTask task = getTaskManager().createTask();
        task.setStart(cal);
        task.setDuration(getTaskManager().createLength(1));
        getTaskManager().registerTask(task);
        task.setName(nameOfTask + "_" + task.getTaskID());
        task.setColor(area.getTaskColor());
        tree.addObject(task, node, index);

        /*
         * this will add new custom columns to the newly created task.
         */

        AdjustTaskBoundsAlgorithm alg = getTaskManager()
                .getAlgorithmCollection().getAdjustTaskBoundsAlgorithm();
        alg.run(task);
        RecalculateTaskCompletionPercentageAlgorithm alg2 = getTaskManager()
                .getAlgorithmCollection()
                .getRecalculateTaskCompletionPercentageAlgorithm();
        alg2.run(task);
        area.repaint();
        setAskForSave(true);
        getUIFacade().setStatusText(language.getText("createNewTask"));
        // setQuickSave(true);
        tree.setEditingTask(task);
        if (options.getAutomatic()) {
            propertiesTask();
        }
        repaint2();
        return task;
    }

    public void deleteResources() {
        myDeleteHumanAction.actionPerformed(null);
    }

    /**
     * Delete the current task
     *
     * @param confirmation
     *            Unused at the moment, but might be used to show confirmation
     *            when true
     */
    public void deleteTasks(boolean confirmation) {
        getTree().getDeleteTasksAction().actionPerformed(null);
    }

    /** Edit task parameters */
    public void propertiesTask() {
        myTaskPropertiesAction.actionPerformed(null);
    }

    /** Refresh the informations of the project on the status bar. */
    public void refreshProjectInfos() {
        if (getTaskManager().getTaskCount() == 0 && resp.nbPeople() == 0)
            getStatusBar().setSecondText("");
        else
            getStatusBar().setSecondText(
                    language.getCorrectedLabel("task") + " : "
                            + getTaskManager().getTaskCount() + "  "
                            + language.getCorrectedLabel("resources") + " : "
                            + resp.nbPeople());
    }

    /** Print the project */
    public void printProject() {
        Chart chart = getUIFacade().getActiveChart();

        if (chart == null) {
            getUIFacade()
                    .showErrorDialog(
                            "Failed to find active chart.\nPlease report this problem to GanttProject development team");
            return;
        }
        try {
            PrintManager.printChart(chart, options.getExportSettings());
        } catch (OutOfMemoryError e) {
            getUIFacade().showErrorDialog(GanttLanguage.getInstance().getText("printing.out_of_memory"));
        }
    }

    public void previewPrint() {

        Date startDate, endDate;
        Chart chart = getUIFacade().getActiveChart();

        if (chart == null) {
            getUIFacade()
                    .showErrorDialog(
                            "Failed to find active chart.\nPlease report this problem to GanttProject development team");
            return;
        }

        try {
            startDate = chart.getStartDate();
            endDate = chart.getEndDate();
        } catch (UnsupportedOperationException e) {
            startDate = null;
            endDate = null;
        }

        if (getTabs().getSelectedIndex() == UIFacade.GANTT_INDEX) {
            startDate = area.getChartModel().getStartDate();
            endDate = area.getChartModel().getEndDate();
        } else if (getTabs().getSelectedIndex() == UIFacade.RESOURCES_INDEX) {
            startDate = getResourcePanel().area.getChartModel().getStartDate();
            endDate = getResourcePanel().area.getChartModel().getEndDate();
        }
        try {
            PrintPreview preview = new PrintPreview(getProject(),
                    getUIFacade(), chart, startDate, endDate);
            preview.setVisible(true);
        } catch (OutOfMemoryError e) {
            getUIFacade().showErrorDialog(
                    GanttLanguage.getInstance().getText(
                            "printing.out_of_memory"));
            return;
        }
    }

    /** Create a new project */
    public void newProject() {
        getProjectUIFacade().createProject(getProject());
        fireProjectCreated();
    }

    /** Open a local project file with dialog box (JFileChooser) */
    public void openFile() throws IOException, DocumentException {
        getProjectUIFacade().openProject(this);
    }

    /** Open a remote project file with dialog box (GanttURLChooser) */
    public void openURL() {
        try {
            getProjectUIFacade().openRemoteProject(getProject());
        } catch (DocumentException e) {
            getUIFacade().showErrorDialog(e);
        } catch (IOException e) {
            getUIFacade().showErrorDialog(e);
        }
    }

    public void open(Document document) throws IOException, DocumentException {
        openDocument(document);
        if (document.getPortfolio() != null) {
            Document defaultDocument = document.getPortfolio()
                    .getDefaultDocument();
            openDocument(defaultDocument);
        }
    }

    private void openDocument(Document document) throws IOException, DocumentException {
        if (document.getFileName().toLowerCase().endsWith(".xml") == false
                && document.getFileName().toLowerCase().endsWith(".gan") == false) {
            // Unknown file extension
            String errorMessage = language.getText("msg2") + "\n"
                    + document.getFileName();
            throw new IOException(errorMessage);
        }

        boolean locked = document.acquireLock();
        if (!locked) {
            getUIFacade().logErrorMessage(
                    new Exception(language.getText("msg13")));
        }
        document.read();
        if (documentsMRU.add(document)) {
            updateMenuMRU();
        }
        if (locked) {
            projectDocument = document;
        }
        setTitle(language.getText("appliTitle") + " ["
                + document.getFileName() + "]");
        for (Chart chart : PluginManager.getCharts()) {
            chart.setTaskManager(myTaskManager);
            chart.reset();
        }

        // myDelayManager.fireDelayObservation(); // it is done in repaint2
        addMouseListenerToAllContainer(this.getComponents());

        getTaskManager().projectOpened();
        fireProjectOpened();
        // As we just have opened a new file it is still unmodified, so mark it as such
        setModified(false);
    }

    public void openStartupDocument(String path) {
        if (path != null) {
            final Document document = getDocumentManager().getDocument(path);
            // openStartupDocument(document);
            getUndoManager().undoableEdit("OpenFile", new Runnable() {
                public void run() {
                    try {
                        getProjectUIFacade()
                                .openProject(document, getProject());
                    } catch (DocumentException e) {
                        if (!tryImportDocument(document)) {
                            getUIFacade().showErrorDialog(e);
                        }
                    } catch (IOException e) {
                        if (!tryImportDocument(document)) {
                            getUIFacade().showErrorDialog(e);
                        }
                    }
                }
            });
        }
    }

    private boolean tryImportDocument(Document document) {
        boolean success = false;
        List<Importer> importers = PluginManager.getExtensions(Importer.EXTENSION_POINT_ID, Importer.class);
        for (Importer importer : importers) {
            if (Pattern.matches(".*(" + importer.getFileNamePattern()
                    + ")$", document.getFilePath())) {
                try {
                    importer.setContext(getProject(), getUIFacade(),
                            getGanttOptions().getPluginPreferences());
                    importer.run(new File(document.getFilePath()));
                    success = true;
                    break;
                } catch (Throwable e) {
                    if (!GPLogger.log(e)) {
                        e.printStackTrace(System.err);
                    }
                }
            }
        }
        return success;
    }

    private void openStartupDocument(Document document) {
        try {
            getProjectUIFacade().openProject(document, getProject());
        } catch (DocumentException e) {
            getUIFacade().showErrorDialog(e);
        } catch (IOException e) {
            getUIFacade().showErrorDialog(e);
        }
    }

    /** Save the project as (with a dialog file chooser) */
    public boolean saveAsProject() {
        getProjectUIFacade().saveProjectAs(getProject());
        return true;
    }

    /** Save the project on a server (with a GanttURLChooser) */
    public boolean saveAsURLProject() {
        getProjectUIFacade().saveProjectRemotely(getProject());
        return true;
    }

    /** Save the project on a file */
    public void saveProject() {
        getProjectUIFacade().saveProject(getProject());
    }

    public void changeWorkingDirectory(String newWorkDir) {
        if (null != newWorkDir)
            options.setWorkingDirectory(newWorkDir);
    }

    /** @return the UIConfiguration. */
    @Override
    public UIConfiguration getUIConfiguration() {
        return myUIConfiguration;
    }

    /** Quit the application */
    public void quitApplication() {
        options.setWindowPosition(getX(), getY());
        options.setWindowSize(getWidth(), getHeight());
        options.setUIConfiguration(myUIConfiguration);
        options.setDocumentsMRU(documentsMRU);
        options.setToolBarPosition(toolBar.getOrientation());
        options.save();
        if (getProjectUIFacade().ensureProjectSaved(getProject())) {
            getProject().close();
            setVisible(false);
            dispose();
            System.exit(0);
        } else {
            setVisible(true);
        }
    }

    /** Open the web page */
    public void openWebPage() {
        if (!BrowserControl.displayURL("http://ganttproject.biz/")) {
            GanttDialogInfo gdi = new GanttDialogInfo(this,
                    GanttDialogInfo.ERROR, GanttDialogInfo.YES_OPTION, language
                            .getText("msg4"), language.getText("error"));
            gdi.setVisible(true);
            return;
        }
        getUIFacade().setStatusText(
                GanttLanguage.getInstance().getText("opening")
                        + " www.ganttproject.biz");
    }

    public void setAskForSave(boolean afs) {
        if (isOnlyViewer)
            return;
        fireProjectModified(afs);
        String title = getTitle();
        // String last = title.substring(title.length() - 11, title.length());
        askForSave = afs;
        try {
            if (System.getProperty("mrj.version") != null) {
                rootPane.putClientProperty("windowModified", Boolean
                        .valueOf(afs));
                // see http://developer.apple.com/qa/qa2001/qa1146.html
            } else {
                if (askForSave) {
                    if (!title.endsWith(" *")) {
                        setTitle(title + " *");
                    }
                }

            }
        } catch (AccessControlException e) {
            // This can happen when running in a sandbox (Java WebStart)
            System.err.println(e + ": " + e.getMessage());
        }
    }

    public GanttResourcePanel getResourcePanel() {
        if (this.resp == null) {
            this.resp = new GanttResourcePanel(this, getTree(), getUIFacade());
            this.resp.setResourceActions(getResourceActions()); // TODO pass
            getHumanResourceManager().addView(this.resp);
        }
        return this.resp;
    }

    public GanttLanguage getLanguage() {
        return this.language;
    }

    public GanttGraphicArea getArea() {
        return this.area;
    }

    public GanttTree2 getTree() {
        if (tree == null) {
            tree = new GanttTree2(this, getTaskManager(), getTaskSelectionManager(), getUIFacade());
        }
        return tree;
    }

    public Action getCopyAction() {
        return getViewManager().getCopyAction();
    }

    public Action getCutAction() {
        return getViewManager().getCutAction();
    }

    public Action getPasteAction() {
        return getViewManager().getPasteAction();
    }

    private ResourceActionSet getResourceActions() {
        if (myResourceActions == null) {
            myResourceActions = new ResourceActionSet(this,
                    getResourcePanel(), this, getUIFacade());
        }
        return myResourceActions;
    }

    public static class Args {
        @Parameter(names = "-log", description = "Enable logging")
        public boolean log = true;

        @Parameter(names = "-log_file", description = "Log file name")
        public String logFile = "";

        @Parameter(names = {"-h", "-help"}, description = "Print usage")
        public boolean help = false;

        @Parameter(description = "Input file name")
        public List<String> file = null;
    }

    /** The main */
    public static boolean main(String[] arg) {
        URL logConfig = GanttProject.class.getResource("/logging.properties");
        if (logConfig != null) {
            try {
                GPLogger.readConfiguration(logConfig);
            } catch (IOException e) {
                System.err.println("Failed to setup logging: " + e.getMessage());
                e.printStackTrace();
            }
        }

        CommandLineExportApplication cmdlineApplication = new CommandLineExportApplication();
        Args mainArgs = new Args();
        try {
            JCommander cmdLineParser = new JCommander(new Object[] {mainArgs, cmdlineApplication.getArguments()}, arg);
            if (mainArgs.help) {
                cmdLineParser.usage();
                System.exit(0);
            }
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
        if (mainArgs.log && !mainArgs.logFile.isEmpty()) {
            try {
                GPLogger.setLogFile(mainArgs.logFile);
                File logFile = new File(mainArgs.logFile);
                System.setErr(new PrintStream(new FileOutputStream(logFile)));
                System.out.println("Writing log to " + logFile.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("Failed to write log to file: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Check if an export was requested from the command line
        if (cmdlineApplication.export(mainArgs)) {
            // Export succeeded so exit applciation
            return false;
        }

        GanttSplash splash = new GanttSplash();
        try {
            splash.setVisible(true);
            GanttProject ganttFrame = new GanttProject(false);
            System.err.println("Main frame created");
            if (mainArgs.file != null && !mainArgs.file.isEmpty()) {
                ganttFrame.openStartupDocument(mainArgs.file.get(0));
            } else {
                ganttFrame.fireProjectCreated();
            }
            ganttFrame.setVisible(true);
            if (System.getProperty("os.name").toLowerCase().startsWith("mac os x")) {
                OSXAdapter.registerMacOSXApplication(ganttFrame);
            }
            ganttFrame.getActiveChart().reset();
            return true;
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        } finally {
            splash.close();
            System.err.println("Splash closed");
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    Thread.currentThread().setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                        @Override
                        public void uncaughtException(Thread t, Throwable e) {
                            GPLogger.log(e);
                        }
                    });
                }
            });
        }
    }

    public static final String HUMAN_RESOURCE_MANAGER_ID = "HUMAN_RESOURCE";

    public static final String ROLE_MANAGER_ID = "ROLE_MANAGER";

    private GPCalendar myFakeCalendar = new WeekendCalendarImpl();

    // private GPCalendar myFakeCalendar = new AlwaysWorkingTimeCalendarImpl();

    private ParserFactory myParserFactory;

    private HumanResourceManager myHumanResourceManager;

    private RoleManager myRoleManager;

    private static WindowListener ourWindowListener;

    /////////////////////////////////////////////////////////
    // IGanttProject implementation
    @Override
    public String getProjectName() {
        return prjInfos.getName();
    }

    @Override
    public void setProjectName(String projectName) {
        prjInfos.setName(projectName);
        setAskForSave(true);
    }

    @Override
    public String getDescription() {
        return prjInfos.getDescription();
    }

    @Override
    public void setDescription(String description) {
        prjInfos.setDescription(description);
        setAskForSave(true);
    }

    @Override
    public String getOrganization() {
        return prjInfos.getOrganization();
    }

    @Override
    public void setOrganization(String organization) {
        prjInfos.setOrganization(organization);
        setAskForSave(true);
    }

    @Override
    public String getWebLink() {
        return prjInfos.getWebLink();
    }

    @Override
    public void setWebLink(String webLink) {
        prjInfos.setWebLink(webLink);
        setAskForSave(true);
    }

    @Override
    public HumanResourceManager getHumanResourceManager() {
        if (myHumanResourceManager == null) {
            myHumanResourceManager = new HumanResourceManager(getRoleManager().getDefaultRole(), getResourceCustomPropertyManager());
            myHumanResourceManager.addView(this);
        }
        return myHumanResourceManager;
    }

    @Override
    public TaskManager getTaskManager() {
        return myTaskManager;
    }

    @Override
    public RoleManager getRoleManager() {
        if (myRoleManager == null) {
            myRoleManager = RoleManager.Access.getInstance();
        }
        return myRoleManager;
    }

    @Override
    public Document getDocument() {
        return projectDocument;
    }

    public void setDocument(Document document) {
        projectDocument = document;
    }

    @Override
    public GanttLanguage getI18n() {
        return getLanguage();
    }

    @Override
    public GPCalendar getActiveCalendar() {
        return myFakeCalendar;
    }

    @Override
    public void setModified() {
        setAskForSave(true);
    }

    public void setModified(boolean modified) {
        setAskForSave(modified);

        String title = getTitle();
        if(modified == false && title.endsWith(" *")) {
            // Remove * from title
            setTitle(title.substring(0, title.length() - 2));
        }
    }

    public boolean isModified() {
        return askForSave;
    }

    @Override
    public void close() {
        fireProjectClosed();
        prjInfos = new PrjInfos();
        RoleManager.Access.getInstance().clear();
        if (null != projectDocument) {
            projectDocument.releaseLock();
        }
        projectDocument = null;
        getTaskManager().projectClosed();
        getCustomColumnsStorage().reset();

        for (int i = 0; i < myPreviousStates.size(); i++) {
            myPreviousStates.get(i).remove();
        }
        myPreviousStates = new ArrayList<GanttPreviousState>();
        getTaskManager().getCalendar().clearPublicHolidays();
        setModified(false);
    }

    @Override
    protected ParserFactory getParserFactory() {
        if (myParserFactory == null) {
            myParserFactory = new ParserFactoryImpl();
        }
        return myParserFactory;
    }

    /////////////////////////////////////////////////////////////////
    // ResourceView implementation
    public void resourceAdded(ResourceEvent event) {
        if (getStatusBar() != null) {
            // tabpane.setSelectedIndex(1);
            getUIFacade().setStatusText(
                    GanttLanguage.getInstance().correctLabel(
                            GanttLanguage.getInstance().getText("newHuman")));
            setAskForSave(true);
            refreshProjectInfos();
        }
    }

    public void resourcesRemoved(ResourceEvent event) {
        refreshProjectInfos();
        setAskForSave(true);
    }

    public void resourceChanged(ResourceEvent e) {
        setAskForSave(true);
    }

    public void resourceAssignmentsChanged(ResourceEvent e) {
        setAskForSave(true);
    }

    /////////////////////////////////////////////////////////////////
    // UIFacade

    public GanttChart getGanttChart() {
        return getArea();
    }

    public Chart getResourceChart() {
        return getResourcePanel().area;
    }

    public int getGanttDividerLocation() {
        // return mySplitPane.getDividerLocation();
        return myGanttChartTabContent.getDividerLocation();
    }

    public void setGanttDividerLocation(int location) {
        myGanttChartTabContent.setDividerLocation(location);
    }

    public int getResourceDividerLocation() {
        return myResourceChartTabContent.getDividerLocation();
        // return getResourcePanel().getDividerLocation();
    }

    public void setResourceDividerLocation(int location) {
        myResourceChartTabContent.setDividerLocation(location);
    }

    public TaskTreeUIFacade getTaskTree() {
        return getTree();
    }

    public ResourceTreeUIFacade getResourceTree() {
        return getResourcePanel();
    }

    private class ParserFactoryImpl implements ParserFactory {
        public GPParser newParser() {
            return new GanttXMLOpen(prjInfos, getUIConfiguration(),
                    getTaskManager(), getUIFacade());
        }

        public GPSaver newSaver() {
            return new GanttXMLSaver(GanttProject.this, getTree(),
                    getResourcePanel(), getArea(), getUIFacade());
        }
    }

    public void setRowHeight(int value) {
        tree.getTreeTable().getTable().setRowHeight(value);
    }


    public void repaint2() {
        getResourcePanel().getResourceTreeTableModel().updateResources();
        getResourcePanel().getResourceTreeTable().setRowHeight(20);
        if (myDelayManager != null) {
            myDelayManager.fireDelayObservation();
        }
        super.repaint();
    }

    public void recalculateCriticalPath() {
        if (myUIConfiguration.isCriticalPathOn()) {
            getTaskManager().processCriticalPath(
                    (Task) ((TaskNode) tree.getRoot()).getUserObject());
            ArrayList<TaskNode> projectTasks = tree.getProjectTasks();
            for (TaskNode projectTask : projectTasks) {
                getTaskManager().processCriticalPath(
                        (Task) projectTask.getUserObject());
            }
            repaint();
        }
    }

    public int getViewIndex() {
        if (getTabs() == null)
            return -1;
        return getTabs().getSelectedIndex();
    }

    public void setViewIndex(int viewIndex) {
        if (getTabs().getTabCount() > viewIndex) {
            getTabs().setSelectedIndex(viewIndex);
        }
    }

    public static void setWindowListener(WindowListener windowListener) {
        ourWindowListener = windowListener;
    }

    public void refresh() {
        getTaskManager().processCriticalPath(getTaskManager().getRootTask());
        getResourcePanel().getResourceTreeTableModel().updateResources();
        getResourcePanel().getResourceTreeTable().setRowHeight(20);
        if (myDelayManager != null)
            myDelayManager.fireDelayObservation();
        super.repaint();
    }

    public void setHiddens() {
    }
}