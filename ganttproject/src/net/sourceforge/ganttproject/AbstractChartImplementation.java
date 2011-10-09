/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject team

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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;

import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.ChartModel;
import net.sourceforge.ganttproject.chart.ChartModelBase;
import net.sourceforge.ganttproject.chart.ChartRendererBase;
import net.sourceforge.ganttproject.chart.ChartSelection;
import net.sourceforge.ganttproject.chart.ChartSelectionListener;
import net.sourceforge.ganttproject.chart.ChartUIConfiguration;
import net.sourceforge.ganttproject.chart.TimelineChart;
import net.sourceforge.ganttproject.chart.export.ChartDimensions;
import net.sourceforge.ganttproject.chart.export.ChartImageBuilder;
import net.sourceforge.ganttproject.chart.export.ChartImageVisitor;
import net.sourceforge.ganttproject.chart.export.RenderedChartImage;
import net.sourceforge.ganttproject.chart.mouse.MouseInteraction;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.gui.zoom.ZoomEvent;
import net.sourceforge.ganttproject.gui.zoom.ZoomListener;
import net.sourceforge.ganttproject.task.TaskLength;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.time.TimeFrame;
import net.sourceforge.ganttproject.time.TimeUnit;
import net.sourceforge.ganttproject.time.TimeUnitStack;

import org.eclipse.core.runtime.IStatus;

public class AbstractChartImplementation implements TimelineChart, ZoomListener {
    public static final ImageIcon LOGO = new ImageIcon(AbstractChartImplementation.class.getResource("/icons/big.png"));
    private static final int HEADER_OFFSET = LOGO.getIconHeight();

    private final ChartModelBase myChartModel;
    private final IGanttProject myProject;
    private Set<ChartSelectionListener> mySelectionListeners= new LinkedHashSet<ChartSelectionListener>();
    private final ChartComponentBase myChartComponent;

    public AbstractChartImplementation(IGanttProject project, ChartModelBase chartModel, ChartComponentBase chartComponent) {
        assert chartModel != null;
        myChartModel = chartModel;
        myProject = project;
        myChartComponent = chartComponent;
    }

    public IGanttProject getProject() {
        return myProject;
    }

    public void beginScrollViewInteraction(MouseEvent e) {
        setActiveInteraction(myChartComponent.newScrollViewInteraction(e));
    }

    public MouseInteraction finishInteraction() {
        try {
            if (getActiveInteraction() != null) {
                getActiveInteraction().finish();
            }
            return getActiveInteraction();
        } finally {
            setActiveInteraction(null);
        }
    }

    protected void setActiveInteraction(MouseInteraction myActiveInteraction) {
        this.myActiveInteraction = myActiveInteraction;
    }

    public MouseInteraction getActiveInteraction() {
        return myActiveInteraction;
    }

    public void zoomChanged(ZoomEvent e) {
        myChartComponent.invalidate();
        myChartComponent.repaint();
    }


    public void paintChart(Graphics g) {
        getChartModel().paint(g);
    }
    private MouseInteraction myActiveInteraction;
    private TimeFrame myFirstTimeFrame;

    private ChartModelBase getChartModel() {
        return myChartModel;
    }

    /////////////////////////////////////////////////////////////
    // interface Chart
    @Override
    public void buildImage(GanttExportSettings settings, ChartImageVisitor imageVisitor) {
        ChartModelBase modelCopy = getChartModel().createCopy();
        modelCopy.setBounds(myChartComponent.getSize());
        if (settings.getStartDate() == null) {
            settings.setStartDate(modelCopy.getStartDate());
        }
        if (settings.getEndDate() == null) {
            settings.setEndDate(modelCopy.getEndDate());
        }
        ChartImageBuilder builder = new ChartImageBuilder(settings, modelCopy, myChartComponent.getTreeTable());
        builder.buildImage(imageVisitor);
    }

    @Override
    public RenderedImage getRenderedImage(GanttExportSettings settings) {
        class ChartImageVisitorImpl implements ChartImageVisitor {
            private RenderedChartImage myRenderedImage;
            private Graphics2D myGraphics;
            private BufferedImage myTreeImage;

            @Override
            public void acceptLogo(ChartDimensions d, Image logo) {
                Graphics2D g = getGraphics(d);
                g.setBackground(Color.WHITE);
                g.clearRect(0, 0, d.getTreeWidth(), d.getLogoHeight());
                g.drawImage(logo, 0, 0, null);
            }

            @Override
            public void acceptTable(ChartDimensions d, Component header, Component table) {
                Graphics2D g = getGraphics(d);
                g.translate(0, d.getLogoHeight());
                header.paintAll(g);

                g.translate(0, d.getTableHeaderHeight());
                table.printAll(g);
            }

            @Override
            public void acceptChart(ChartDimensions d, ChartModel model) {
                myRenderedImage = new RenderedChartImage(
                        model,
                        myTreeImage,
                        d.getChartWidth(),
                        d.getChartHeight());
            }

            private Graphics2D getGraphics(ChartDimensions d) {
                if (myGraphics == null) {
                    myTreeImage  = new BufferedImage(d.getTreeWidth(), d.getChartHeight(), BufferedImage.TYPE_INT_RGB);
                    myGraphics = myTreeImage.createGraphics();
                }
                return myGraphics;
            }
        };
        ChartImageVisitorImpl visitor = new ChartImageVisitorImpl();
        buildImage(settings, visitor);
        return visitor.myRenderedImage;
    }

    public Date getStartDate() {
        return getChartModel().getStartDate();
    }

    public void setStartDate(Date startDate) {
        getChartModel().setStartDate(startDate);
        myFirstTimeFrame = scrollTimeFrame(startDate);
        startDate = myFirstTimeFrame.getStartDate();
        getChartModel().setStartDate(startDate);
    }

    public void scrollBy(TaskLength duration) {
        setStartDate(getChartModel().getTaskManager().shift(getStartDate(), duration));
    }

    @Override
    public void setStartOffset(int pixels) {
        getChartModel().setHorizontalOffset(pixels);
    }

    private TimeFrame scrollTimeFrame(Date scrolledDate) {
        TimeFrame result = null;
        if (getTopTimeUnit().isConstructedFrom(getBottomTimeUnit())) {
            result = getTimeUnitStack().createTimeFrame(scrolledDate,
                    getTopTimeUnit(), getBottomTimeUnit());
        } else {
            result = getTimeUnitStack().createTimeFrame(scrolledDate,
                    getBottomTimeUnit(), getBottomTimeUnit());
        }
        return result;
    }

    private TimeUnit getTopTimeUnit() {
        return getChartModel().getTopUnit();
    }

    private TimeUnit getBottomTimeUnit() {
        return getChartModel().getBottomUnit();
    }

    private TimeUnitStack getTimeUnitStack() {
        return myProject.getTimeUnitStack();
    }

    public Date getEndDate() {
        return getChartModel().getEndDate();
    }

    public void setDimensions(int height, int width) {
        Dimension bounds = new Dimension(width, height);
        getChartModel().setBounds(bounds);
    }

    public void setBottomUnit(TimeUnit bottomUnit) {
        getChartModel().setBottomTimeUnit(bottomUnit);
    }

    public void setTopUnit(TimeUnit topUnit) {
        getChartModel().setTopTimeUnit(topUnit);
    }

    public void setBottomUnitWidth(int width) {
        getChartModel().setBottomUnitWidth(width);
    }

    public String getName() {
        return myChartComponent.getName();
    }

    public void setTaskManager(TaskManager taskManager) {
        throw new UnsupportedOperationException();
    }

    public void reset() {
        throw new UnsupportedOperationException();
    }

    public Icon getIcon() {
        return null;
    }

    public GPOptionGroup[] getOptionGroups() {
        return getChartModel().getChartOptionGroups();
    }

    public Chart createCopy() {
        return new AbstractChartImplementation(myProject, getChartModel().createCopy(), myChartComponent);
    }

    public Object getAdapter(Class arg0) {
        return null;
    }

    public ChartSelection getSelection() {
        throw new UnsupportedOperationException();
    }

    public IStatus canPaste(ChartSelection selection) {
        throw new UnsupportedOperationException();
    }

    public void paste(ChartSelection selection) {
        throw new UnsupportedOperationException();
    }

    public void addSelectionListener(ChartSelectionListener listener) {
        mySelectionListeners.add(listener);
    }

    public void removeSelectionListener(ChartSelectionListener listener) {
        mySelectionListeners.remove(listener);
    }

    protected void fireSelectionChanged() {
        for (Iterator<ChartSelectionListener> listeners = mySelectionListeners.iterator(); listeners.hasNext();) {
            ChartSelectionListener nextListener = listeners.next();
            nextListener.selectionChanged();
        }
    }

    public void addRenderer(ChartRendererBase renderer) {
        myChartModel.addRenderer(renderer);
    }

    public void resetRenderers() {
        myChartModel.resetRenderers();
    }

    public ChartModel getModel() {
        return myChartModel;
    }

    @Override
    public ChartUIConfiguration getStyle() {
        return myChartModel.getChartUIConfiguration();
    }

    public int getHeaderHeight(JComponent tableContainer, JTable table) {
        JTableHeader tableHeader = table.getTableHeader();
        Point headerLocation = tableHeader.getLocationOnScreen();
        Point treeLocation = tableContainer.getLocationOnScreen();
        return headerLocation.y - treeLocation.y + tableHeader.getHeight() + HEADER_OFFSET;

    }
}