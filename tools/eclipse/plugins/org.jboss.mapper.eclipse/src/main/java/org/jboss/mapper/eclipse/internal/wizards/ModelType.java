package org.jboss.mapper.eclipse.internal.wizards;

import org.jboss.mapper.TransformType;

/**
 *
 */
public enum ModelType {

    /**
     *
     */
    CLASS( "Java Class", TransformType.JAVA ),

    /**
     *
     */
    JAVA( "Java Source", TransformType.JAVA ),

    /**
     *
     */
    JSON( "JSON", TransformType.JSON ),

    /**
     *
     */
    JSON_SCHEMA( "JSON Schema", TransformType.JSON ),

    /**
     *
     */
    XML( "XML", TransformType.XML ),

    /**
     *
     */
    XSD( "XSD", TransformType.XML );

    final String text;

    /**
     *
     */
    public final TransformType transformType;

    private ModelType( final String text,
                       final TransformType transformType ) {
        this.text = text;
        this.transformType = transformType;
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
        return text;
    }
}
