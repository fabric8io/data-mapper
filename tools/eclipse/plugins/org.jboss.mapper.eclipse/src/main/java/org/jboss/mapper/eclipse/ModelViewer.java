package org.jboss.mapper.eclipse;

import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.jboss.mapper.MapperConfiguration;
import org.jboss.mapper.eclipse.viewers.ModelViewerUsedFieldsFilter;
import org.jboss.mapper.model.Model;

public class ModelViewer extends Composite {

    Model model;
    final TreeViewer treeViewer;
    boolean showFieldTypesInLabel = true;
    boolean showMappedFields = true;
    MapperConfiguration mapperConfig;
    ModelViewerUsedFieldsFilter usedFieldsFilter;
    String modelType;

    public ModelViewer(final Composite parent, final Model model) {
        super(parent, SWT.NONE);
        this.model = model;
        setBackground(parent.getBackground());
        setLayout(GridLayoutFactory.fillDefaults().create());
        ToolBar toolBar = new ToolBar(this, SWT.NONE);
        ToolItem collapseAllButton = new ToolItem(toolBar, SWT.PUSH);
        collapseAllButton.setImage(Activator.imageDescriptor("collapseall16.gif").createImage());
        treeViewer = new TreeViewer(this);
        ToolItem filterTypesButton = new ToolItem(toolBar, SWT.CHECK);
        filterTypesButton.setImage(Activator.imageDescriptor("filter16.gif").createImage());
        filterTypesButton.setToolTipText("Show/hide Types");
        ToolItem filterMappedFieldsButton = new ToolItem(toolBar, SWT.CHECK);
        filterMappedFieldsButton.setImage(Activator.imageDescriptor("filter16.gif").createImage());
        filterMappedFieldsButton.setToolTipText("Show/hide Mapped Fields");
        treeViewer.setComparator(new ViewerComparator() {

            @Override
            public int compare(Viewer viewer, Object model1, Object model2) {
                return ((Model) model1).getName().compareTo(((Model) model2).getName());
            }
        });
        treeViewer.setLabelProvider(new MyLabelProvider());

        final Tree tree = treeViewer.getTree();
        tree.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());

        treeViewer.setContentProvider(new ITreeContentProvider() {

            @Override
            public void dispose() {
            }

            @Override
            public Object[] getChildren(final Object parentElement) {
                final Model model = (Model) parentElement;
                return model.getChildren().toArray();
            }

            @Override
            public Object[] getElements(final Object inputElement) {
                return getChildren(inputElement);
            }

            @Override
            public Object getParent(final Object element) {
                return ((Model) element).getParent();
            }

            @Override
            public boolean hasChildren(final Object element) {
                return getChildren(element).length > 0;
            }

            @Override
            public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
            }
        });

        collapseAllButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                treeViewer.collapseAll();
            }
        });
        filterTypesButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent event) {
                ToolItem item = (ToolItem) event.widget;
                showFieldTypesInLabel = !item.getSelection();
                treeViewer.refresh(true);
            }
        });

        usedFieldsFilter = new ModelViewerUsedFieldsFilter(mapperConfig, modelType);
        filterMappedFieldsButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent event) {
                ToolItem item = (ToolItem) event.widget;
                showMappedFields = !item.getSelection();
                usedFieldsFilter.setShowMappedFields(showMappedFields);
                treeViewer.refresh(true);
            }
        });
        treeViewer.setInput(model);
        treeViewer.addFilter(usedFieldsFilter);
    }

    public void setInput(Object object) {
        treeViewer.setInput(object);
    }

    void setMapperConfiguration(MapperConfiguration config) {
        this.mapperConfig = config;
        if (usedFieldsFilter != null) {
            usedFieldsFilter.setMapperConfiguration(config);
        }
    }

    void setModelType(String inType) {
        this.modelType = inType;
        if (usedFieldsFilter != null) {
            usedFieldsFilter.setViewerType(inType);
        }
    }

    class MyLabelDecorator implements ILabelDecorator {

        @Override
        public void addListener(ILabelProviderListener arg0) {
        }

        @Override
        public void dispose() {
        }

        @Override
        public boolean isLabelProperty(Object arg0, String arg1) {
            return false;
        }

        @Override
        public void removeListener(ILabelProviderListener arg0) {
        }

        @Override
        public Image decorateImage(Image arg0, Object arg1) {
            return null;
        }

        @Override
        public String decorateText(String arg0, Object element) {
            final Model model = (Model) element;
            if (!showFieldTypesInLabel) {
                return model.getName();
            }
            return model.getName() + ": " + model.getType();
        }
    }

    class MyLabelProvider extends DecoratingLabelProvider {

        public MyLabelProvider() {
            super(new MyTreeLabelProvider(), new MyLabelDecorator());
        }
    }

    class MyTreeLabelProvider extends LabelProvider {

        @Override
        public String getText(final Object element) {
            final Model model = (Model) element;
            return model.getName();
        }

        @Override
        public Image getImage(Object element) {
            final Model model = (Model) element;
            ISharedImages images = JavaUI.getSharedImages();
            if (model.isCollection()) {
                return images.getImage(ISharedImages.IMG_FIELD_DEFAULT);
            } else if ((model.getChildren() != null && model.getChildren().size() > 0)) {
                return images.getImage(ISharedImages.IMG_OBJS_CLASS);
            } else {
                return images.getImage(ISharedImages.IMG_FIELD_PUBLIC);
            }
        }
    }
}
