package org.jboss.mapper.eclipse;

import java.util.List;

import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
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
import org.jboss.mapper.MappingOperation;
import org.jboss.mapper.eclipse.viewers.ModelViewerUsedFieldsFilter;
import org.jboss.mapper.model.Model;

public class ModelViewer extends Composite {

    Model model;
    final TreeViewer treeViewer;
    boolean showFieldTypesInLabel = false;
    boolean showMappedFields = true;
    MapperConfiguration mapperConfig;
    ModelViewerUsedFieldsFilter usedFieldsFilter;
    String modelType;
    private final RootWrapper mRootWrapper = new RootWrapper();

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
                if (model1 instanceof Model && model2 instanceof Model) {
                    return ((Model) model1).getName().compareTo(((Model) model2).getName());
                }
                return 0;
            }
        });
        treeViewer.setLabelProvider(new MyStyledLabelProvider());

        final Tree tree = treeViewer.getTree();
        tree.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());

        treeViewer.setComparer(new IElementComparer() {
            public int hashCode(Object element) {
                if (element instanceof Model) {
                    Model key = ((Model) element);
                    if (key != null) {
                        return key.hashCode();
                    }
                }
                if (element != null) {
                    return element.hashCode();
                }
                return 0;
            }

            public boolean equals(Object a, Object b) {
                if (a instanceof Model && b instanceof Model) {
                    Model keyA = ((Model) a);
                    Model keyB = ((Model) b);
                    if (keyA != null) {
                        return keyA.equals(keyB);
                    }
                }
                if (a != null && a instanceof Model && b instanceof Model) {
                    return a.equals(b);
                }
                if (a instanceof RootWrapper && b instanceof RootWrapper) {
                    return true;
                }
                return false;
            }
        });

        treeViewer.setContentProvider(new ITreeContentProvider() {

            @Override
            public void dispose() {
            }

            @Override
            public Object[] getChildren(final Object parentElement) {
                if (parentElement instanceof RootWrapper) {
                    Model root = ((RootWrapper)parentElement).getRoot();
                    if (root != null) {
                        return new Object[] { root };
                    }
                }    
                if (parentElement instanceof Model) {
                    final Model model = (Model) parentElement;
                    return model.getChildren().toArray();
                }
                return new Object[0];
            }

            @Override
            public Object[] getElements(final Object inputElement) {
                return getChildren(inputElement);
            }

            @Override
            public Object getParent(final Object element) {
                if (element instanceof Model) {
                    return ((Model) element).getParent();
                }
                return null;
            }

            @Override
            public boolean hasChildren(final Object element) {
                if (element instanceof Model) {
                    return getChildren(element).length > 0;
                }
                return false;
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
                showFieldTypesInLabel = item.getSelection();
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
        treeViewer.setInput(mRootWrapper);
        treeViewer.addFilter(usedFieldsFilter);
    }

    public void setInput(Object object) {
        mRootWrapper.setRoot((Model) object);
        treeViewer.setInput(mRootWrapper);
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

    class MyStyledLabelProvider extends StyledCellLabelProvider {
        
        private static final String LIST_OF = "List of ";
        
        @Override
        public void update(ViewerCell cell) {
          Object element = cell.getElement();
          StyledString text = new StyledString();
          Model modelForLabel = (Model) element;
          if (isMapped(element)) {
              text.append("*", StyledString.DECORATIONS_STYLER);
          }
          text.append(modelForLabel.getName());
          if (showFieldTypesInLabel) {
              String type = modelForLabel.getType(); 
              if ( type.startsWith( "[" ) ) { 
                  text.append(":", StyledString.DECORATIONS_STYLER);
                  text.append(" " + LIST_OF, StyledString.COUNTER_STYLER);
                  text.append(type.substring( 1, type.length() - 1 ), 
                          StyledString.DECORATIONS_STYLER);
              } else {
                  text.append(": " + type, StyledString.DECORATIONS_STYLER);
              }
          }
          Image image = getImage(element);
          cell.setImage(image);
          cell.setText(text.toString());
          cell.setStyleRanges(text.getStyleRanges());
          super.update(cell);

        }

        private boolean isMapped(Object element) {
            if (mapperConfig != null && element instanceof Model) {
                List<MappingOperation<?, ?>> mappings = null; 
                if (modelType.equalsIgnoreCase("Source")) {
                    mappings = mapperConfig.getMappingsForSource((Model) element);
                } else if (modelType.equalsIgnoreCase("Target")) {
                    mappings = mapperConfig.getMappingsForTarget((Model) element);
                }
                if (mappings != null && !mappings.isEmpty()) {
                    return true;
                }
            }
            return false;
        }
        
        private Image getImage(Object element) {
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
    
    private static class RootWrapper {
        private Model mRoot;
        
        public void setRoot(Model model) {
            mRoot = model;
        }
        
        public Model getRoot() {
            return mRoot;
        }
    }
    
    public void refresh(){
        this.treeViewer.refresh(true);
    }
}
