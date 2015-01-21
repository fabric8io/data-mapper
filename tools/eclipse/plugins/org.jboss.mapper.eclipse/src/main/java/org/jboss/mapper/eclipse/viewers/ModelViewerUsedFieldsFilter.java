package org.jboss.mapper.eclipse.viewers;

import java.util.List;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.jboss.mapper.MapperConfiguration;
import org.jboss.mapper.MappingOperation;
import org.jboss.mapper.model.Model;

public class ModelViewerUsedFieldsFilter extends ViewerFilter {
	
	MapperConfiguration mapConfig;
	String viewerType;
	boolean showMappedFields = true;

	public ModelViewerUsedFieldsFilter(MapperConfiguration config, String viewerType) {
		this.mapConfig = config;
		this.viewerType = viewerType;
	}
	
	public void setShowMappedFields(boolean flag) {
		showMappedFields = flag;
	}
	
	public void setMapperConfiguration(MapperConfiguration config) {
		mapConfig = config;
	}
	
	public void setViewerType(String viewType) {
		viewerType = viewType;
	}
	
	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (mapConfig != null) {
			List<MappingOperation<?, ?>> mappings = null; 
			if (viewerType.equalsIgnoreCase("Source")) {
				mappings = mapConfig.getMappingsForSource((Model) element);
			} else if (viewerType.equalsIgnoreCase("Target")) {
				mappings = mapConfig.getMappingsForTarget((Model) element);
			}
			if (mappings != null && !mappings.isEmpty()) {
				return showMappedFields;
			}
		}
		return true;
	}

}
