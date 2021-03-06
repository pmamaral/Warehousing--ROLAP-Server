//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.07.06 at 07:08:07 PM WEST 
//


package xml.client;

import javax.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the xml.client package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {


    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: xml.client
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Filter }
     * 
     */
    public Filter createFilter() {
        return new Filter();
    }

    /**
     * Create an instance of {@link Slice.SliceCollumns.Collumn }
     * 
     */
    public Slice.SliceCollumns.Collumn createSliceSliceCollumnsCollumn() {
        return new Slice.SliceCollumns.Collumn();
    }

    /**
     * Create an instance of {@link SelectedFacts.Fact }
     * 
     */
    public SelectedFacts.Fact createSelectedFactsFact() {
        return new SelectedFacts.Fact();
    }

    /**
     * Create an instance of {@link Slice.SliceCollumns }
     * 
     */
    public Slice.SliceCollumns createSliceSliceCollumns() {
        return new Slice.SliceCollumns();
    }

    /**
     * Create an instance of {@link Dimension }
     * 
     */
    public Dimension createDimension() {
        return new Dimension();
    }

    /**
     * Create an instance of {@link Slice.SliceCollumns.Collumn.ByValues }
     * 
     */
    public Slice.SliceCollumns.Collumn.ByValues createSliceSliceCollumnsCollumnByValues() {
        return new Slice.SliceCollumns.Collumn.ByValues();
    }

    /**
     * Create an instance of {@link Query.Cube }
     * 
     */
    public Query.Cube createQueryCube() {
        return new Query.Cube();
    }

    /**
     * Create an instance of {@link Query }
     * 
     */
    public Query createQuery() {
        return new Query();
    }

    /**
     * Create an instance of {@link SelectedFacts }
     * 
     */
    public SelectedFacts createSelectedFacts() {
        return new SelectedFacts();
    }

    /**
     * Create an instance of {@link Dimension.SelectedCollumns }
     * 
     */
    public Dimension.SelectedCollumns createDimensionSelectedCollumns() {
        return new Dimension.SelectedCollumns();
    }

    /**
     * Create an instance of {@link Slice }
     * 
     */
    public Slice createSlice() {
        return new Slice();
    }

    /**
     * Create an instance of {@link Query.Dimensions }
     * 
     */
    public Query.Dimensions createQueryDimensions() {
        return new Query.Dimensions();
    }

}
