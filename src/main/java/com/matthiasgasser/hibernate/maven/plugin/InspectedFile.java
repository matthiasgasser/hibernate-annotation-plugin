package com.matthiasgasser.hibernate.maven.plugin;

import java.util.List;

/**
 * @author matthias
 * holds meta information of a parsed java source file
 * (annotations, name)
 */
public class InspectedFile {
	
	

	private List<String> annotations;

	private String canonicalName;

	public InspectedFile() {
	}
	
	public InspectedFile(List<String> annotations, String canonicalName) {
		this.annotations = annotations;
		this.canonicalName = canonicalName;
	}

	public List<String> getAnnotations() {
		return annotations;
	}

	public void setAnnotations(List<String> annotations) {
		this.annotations = annotations;
	}

	public String getCanonicalName() {
		return canonicalName;
	}

	public void setCanonicalName(String canonicalName) {
		this.canonicalName = canonicalName;
	}

	public boolean isEntityClass() {
		if(annotations.contains(HibernateAnnotationPlugin.ENTITY_ANNOTATION))
			return true;
		return false;
	}

}
