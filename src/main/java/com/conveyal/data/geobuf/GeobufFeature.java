package com.conveyal.data.geobuf;

import com.vividsolutions.jts.geom.Geometry;
import org.geotools.feature.type.GeometryTypeImpl;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;

import java.util.HashMap;
import java.util.Map;

/**
 * A representation of a GeoBuf feature.
 */
public class GeobufFeature implements Cloneable {
    public Geometry geometry;
    public Map<String, Object> properties;
    public String id;
    public long numericId;

    public GeobufFeature(SimpleFeature simpleFeature) {
        this.geometry = (Geometry) simpleFeature.getDefaultGeometry();
        this.properties = new HashMap<>();
        this.id = simpleFeature.getID();

        // copy over attributes
        for (Property p :simpleFeature.getProperties()) {
            if (p.getType() instanceof GeometryTypeImpl)
                continue;

            this.properties.put(p.getName().toString(), p.getValue());
        }
    }

    public GeobufFeature () {}

    /** return a copy of this object (also makes a defensive copy of properties, but not of the geometry as the geometry is considered immutable) */
    public GeobufFeature clone () {
        GeobufFeature ret;
        try {
            ret = (GeobufFeature) super.clone();
        } catch (CloneNotSupportedException e) {
            // contact spock immediately
            throw new RuntimeException(e);
        }

        ret.properties = new HashMap<>();
        ret.properties.putAll(this.properties);

        // no need to clone geometry as it's immutable

        return ret;
    }
}
