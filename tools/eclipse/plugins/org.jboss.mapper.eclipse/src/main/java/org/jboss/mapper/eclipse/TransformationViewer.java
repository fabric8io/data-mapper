package org.jboss.mapper.eclipse;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.jboss.mapper.FieldMapping;
import org.jboss.mapper.Literal;
import org.jboss.mapper.LiteralMapping;
import org.jboss.mapper.MapperConfiguration;
import org.jboss.mapper.MappingOperation;
import org.jboss.mapper.MappingType;
import org.jboss.mapper.camel.CamelConfigBuilder;
import org.jboss.mapper.camel.EndpointHelper;
import org.jboss.mapper.camel.config.CamelEndpointFactoryBean;
import org.jboss.mapper.model.Model;

class TransformationViewer extends Composite {

    private static final String MAIN_PATH = "src/main/";
    private static final String RESOURCES_PATH = MAIN_PATH + "resources/";
    private static final String CAMEL_CONFIG_PATH = RESOURCES_PATH + "META-INF/spring/camel-context.xml";

    final IFile configFile;
    final MapperConfiguration mapperConfig;
    final TableViewer viewer;
    Stack<Model> sourcehistory = null;
    Stack<Model> targethistory = null;
    String endpointID = null;

    TransformationViewer(final Composite parent, final IFile configFile, final MapperConfiguration mapperConfig) {
        super(parent, SWT.NONE);

        this.configFile = configFile;
        this.mapperConfig = mapperConfig;

        setLayout(GridLayoutFactory.fillDefaults().create());
        setBackground(parent.getBackground());

        // Create tool bar
        final ToolBar toolBar = new ToolBar(this, SWT.NONE);
        final ToolItem deleteButton = new ToolItem(toolBar, SWT.PUSH);
        deleteButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ETOOL_DELETE));
        deleteButton.setToolTipText("Delete the selected operation(s)");
        deleteButton.setEnabled(false);

        viewer = new TableViewer(this);
        final Table table = viewer.getTable();
        table.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
        table.setHeaderVisible(true);
        final TableViewerColumn sourceColumn = new TableViewerColumn(viewer, SWT.LEFT);
        sourceColumn.getColumn().setText("Source Item");
        sourceColumn.setLabelProvider(new ColumnLabelProvider() {

            @Override
            public String getText(final Object element) {
                MappingOperation<?, ?> mapping = (MappingOperation<?, ?>) element;
                if (MappingType.LITERAL == mapping.getType())
                    return "\"" + ((LiteralMapping) mapping).getSource().getValue() + "\"";
                return super.getText(((FieldMapping) mapping).getSource().getName());
            }
        });
        final TableViewerColumn operationColumn = new TableViewerColumn(viewer, SWT.CENTER);
        operationColumn.getColumn().setText("Operation");
        operationColumn.setLabelProvider(new ColumnLabelProvider() {

            @Override
            public String getText(final Object element) {
                return "=>";
            }
        });
        final TableViewerColumn targetColumn = new TableViewerColumn(viewer, SWT.LEFT);
        targetColumn.getColumn().setText("Target Item");
        targetColumn.setLabelProvider(new ColumnLabelProvider() {

            @Override
            public String getText(final Object element) {
                Model target = (Model) ((MappingOperation<?, ?>) element).getTarget();
                return super.getText(target.getName());
            }
        });
        viewer.setContentProvider(new IStructuredContentProvider() {

            @Override
            public void dispose() {
            }

            @Override
            public Object[] getElements(final Object inputElement) {
                final List<Object> fieldMappings = new ArrayList<>();
                for (final MappingOperation<?, ?> mapping : mapperConfig.getMappings()) {
                    fieldMappings.add(mapping);
                    table.setData(mapping.toString(), mapping);
                }
                return fieldMappings.toArray();
            }

            @Override
            public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
            }
        });
        viewer.addSelectionChangedListener(new ISelectionChangedListener() {

            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                deleteButton.setEnabled(!event.getSelection().isEmpty());
            }
        });
        deleteButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                for (final Iterator<?> iter = ((IStructuredSelection) viewer.getSelection()).iterator(); iter.hasNext();) {
                    MappingOperation<?, ?> mapping = (MappingOperation<?, ?>) iter.next();
                    mapperConfig.removeMapping(mapping);
                    try {
                        save();
                        viewer.refresh();
                    } catch (final Exception e) {
                        Activator.error(getShell(), e);
                    }
                }
            }
        });
        viewer.setInput(mapperConfig.getMappings());
        operationColumn.getColumn().pack();
        table.addControlListener(new ControlAdapter() {

            @Override
            public void controlResized(final ControlEvent event) {
                final int width = (table.getSize().x - operationColumn.getColumn().getWidth()) / 2 - 1;
                sourceColumn.getColumn().setWidth(width);
                targetColumn.getColumn().setWidth(width);
            }
        });
    }

    void map(Literal literal, Model targetModel) {
        mapperConfig.map(literal, targetModel);
        save();
    }

    void map(Model sourceModel, Model targetModel) {
        mapperConfig.map(sourceModel, targetModel);
        save();
    }

    void save() {
        try (FileOutputStream stream = new FileOutputStream(new File(configFile.getLocationURI()))) {
            mapperConfig.saveConfig(stream);
            if (sourcehistory != null && sourcehistory.size() > 0 || targethistory != null
                            && targethistory.size() > 0) {
                Model initialSource = null;
                Model lastSource = null;
                if (sourcehistory != null && sourcehistory.size() > 0) {
                    initialSource = sourcehistory.firstElement();
                    lastSource = sourcehistory.lastElement();
                }
                Model initialTarget = null;
                Model lastTarget = null;
                if (targethistory != null && targethistory.size() > 0) {
                    initialTarget = targethistory.firstElement();
                    lastTarget = targethistory.lastElement();
                }

                boolean needCamelUpdate = false;
                if (!modelsEqual(initialSource,lastSource)) {
                    // update the sourceModel
                    System.out.println("Updated endpoint uri sourceModel to " + lastSource.getName());
                    needCamelUpdate = true;
                }                    
                if (!modelsEqual(initialTarget,lastTarget)) {
                    // update the targetModel
                    System.out.println("Updated endpoint uri targetModel to " + lastTarget.getName());
                    needCamelUpdate = true;
                }
                if (endpointID == null && needCamelUpdate) {
                    InputDialog dialog = new InputDialog(getShell(), "Camel Endpoint ID?", 
                            "Please specify the ID of the endpoint to update in the Camel Context file.", null, null);
                    if (dialog.open() == Window.OK) {
                        endpointID = dialog.getValue();
                    }
                }

                if (endpointID != null && needCamelUpdate) {

                    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
                    IFile file = root.getFileForLocation(configFile.getLocation());
                    IProject project = file.getProject();
                    File camelFile = new File(project.getFile(CAMEL_CONFIG_PATH).getLocationURI());
                    CamelConfigBuilder builder = CamelConfigBuilder.loadConfig(camelFile);
                    CamelEndpointFactoryBean theEndpoint = builder.getEndpoint(endpointID);

                    if (theEndpoint != null) {
                        System.out.println("Found endpoint: " + endpointID);
                        // update the endpoint's URI with the source and target
                        if (!modelsEqual(initialSource,lastSource)) {
                            // update the sourceModel
                            EndpointHelper.setSourceModel(theEndpoint, lastSource.getType());
                            System.out.println("Updated endpoint uri sourceModel to " + lastSource.getType());
                        }                    
                        if (!modelsEqual(initialTarget,lastTarget)) {
                            // update the targetModel
                            EndpointHelper.setTargetModel(theEndpoint, lastTarget.getType());
                            System.out.println("Updated endpoint uri targetModel to " + lastTarget.getType());
                        }

                        System.out.println("Now saving the camel file...");
                        try (FileOutputStream camelConfigStream = new FileOutputStream(camelFile)) {
                            builder.saveConfig(camelConfigStream);
                        } catch (final Exception e) {
                            Activator.error(getShell(), e);
                        }
                    } else {
                        MessageDialog.openError(getShell(), 
                                "Camel Endpoint Not Found",
                                "No endpoint named '" + endpointID + "' was found in the Camel context.");
                    }
                }

                // now reset the history
                if (!modelsEqual(initialSource,lastSource)) {
                    System.out.println("Resetting source history");
                    sourcehistory.clear();
                    sourcehistory.push(lastSource);
                }
                if (!modelsEqual(initialTarget,lastTarget)) {
                    System.out.println("Resetting target history");
                    targethistory.clear();
                    targethistory.push(lastSource);
                }
            }
            configFile.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
            viewer.refresh();
        } catch (final Exception e) {
            Activator.error(getShell(), e);
        }
    }
    
    private boolean modelsEqual(Model left, Model right) {
        if (left == null && right == null) {
            return true;
        } 
        if (left == null || right == null) {
            return false;
        }
        if (left.getName().equals(right.getName())) {
            if (left.getType().equals(right.getType())) {
                return true;
            }
        }
        return false;
    }

}
