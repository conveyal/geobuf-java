package com.conveyal.data.geobuf;

import org.locationtech.jts.geom.*;
import geobuf.Geobuf;
import org.geotools.feature.type.GeometryTypeImpl;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A representation of a GeoBuf feature.
 */
public class GeobufFeature implements Cloneable, Serializable {
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(GeobufFeature.class);

    public Geometry geometry;
    public Map<String, Object> properties;
    public String id;
    public long numericId;

    private static final GeometryFactory geometryFactory = new GeometryFactory();

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

    /** decode a feature from GeoBuf, passing in the keys in the file and the precision divison (e.g. 1e6 for precision 6) */
    public GeobufFeature(Geobuf.Data.Feature feature, List<String> keys, double precisionDivisor) {
        // easy part: parse out the properties
        this.properties = new HashMap<>();

        for (int i = 0; i < feature.getPropertiesCount(); i += 2) {
            Geobuf.Data.Value val = feature.getValues(feature.getProperties(i + 1));

            Object valObj = null;

            if (val.hasBoolValue())
                valObj = val.getBoolValue();
            else if (val.hasDoubleValue())
                valObj = val.getDoubleValue();
            else if (val.hasNegIntValue())
                valObj = val.getNegIntValue();
            else if (val.hasPosIntValue())
                valObj = val.getPosIntValue();
            else if (val.hasStringValue())
                valObj = val.getStringValue();

            this.properties.put(keys.get(feature.getProperties(i)), valObj);
        }

        // parse geometry
        Geobuf.Data.Geometry gbgeom = feature.getGeometry();

        if (Geobuf.Data.Geometry.Type.POINT.equals(gbgeom.getType()))
            this.geometry = decodePoint(gbgeom, precisionDivisor);
        else if (Geobuf.Data.Geometry.Type.MULTIPOINT.equals(gbgeom.getType()))
            this.geometry = decodeMultiPoint(gbgeom, precisionDivisor);
        else if (Geobuf.Data.Geometry.Type.LINESTRING.equals(gbgeom.getType()))
            this.geometry = decodeLineString(gbgeom, precisionDivisor);
        else if (Geobuf.Data.Geometry.Type.MULTILINESTRING.equals(gbgeom.getType()))
            this.geometry = decodeMultiLineString(gbgeom, precisionDivisor);
        else if (Geobuf.Data.Geometry.Type.POLYGON.equals(gbgeom.getType()))
            this.geometry = decodePolygon(gbgeom, precisionDivisor);
        else if (Geobuf.Data.Geometry.Type.MULTIPOLYGON.equals(gbgeom.getType()))
            this.geometry = decodeMultipolygon(gbgeom, precisionDivisor);
        else
            LOG.warn("Unsupported geometry type {}", gbgeom.getType());

        // parse ID
        if (feature.hasIntId())
            this.numericId = feature.getIntId();
        else
            this.id = feature.getId();
    }

    private Geometry decodePoint(Geobuf.Data.Geometry gbgeom, double precisionDivisor) {
        return geometryFactory.createPoint(new Coordinate(gbgeom.getCoords(0) / precisionDivisor, gbgeom.getCoords(1) / precisionDivisor));
    }

    private Geometry decodeMultiPoint(Geobuf.Data.Geometry gbgeom, double precisionDivisor) {
        int npoint = gbgeom.getLengthsCount();

        Point[] points = new Point[npoint];
        int coordGlobalIdx = 0;

        if (npoint < 1) {
            points = new Point[1];

            long x = gbgeom.getCoords(coordGlobalIdx++);
            long y = gbgeom.getCoords(coordGlobalIdx++);

            points[0] = geometryFactory.createPoint(new Coordinate(x / precisionDivisor, y / precisionDivisor));

        } else {
            npoint = gbgeom.getLengths(0);
            points = new Point[npoint];
            for (int point = 0; point < npoint; point++) {

                long x = gbgeom.getCoords(coordGlobalIdx++);
                long y = gbgeom.getCoords(coordGlobalIdx++);

                points[point] = geometryFactory.createPoint(new Coordinate(x / precisionDivisor, y / precisionDivisor));
            }
        }
        return geometryFactory.createMultiPoint(points);
    }

    private Geometry decodeLineString(Geobuf.Data.Geometry gbgeom, double precisionDivisor) {
        int ncoord = gbgeom.getCoordsCount() / 2;
        if (ncoord < 2) {
            LOG.error("Geometry cannot be empty!");
            return geometryFactory.createLineString();
        }
        else {
            Coordinate[] coords = new Coordinate[ncoord];

            long prevx = 0, prevy = 0;
            int coordGlobalIdx = 0;

            for (int coordIdx = 0; coordIdx < ncoord; coordIdx++) {
                long x = gbgeom.getCoords(coordGlobalIdx++) + prevx;
                long y = gbgeom.getCoords(coordGlobalIdx++) + prevy;

                coords[coordIdx] = new Coordinate(x / precisionDivisor, y / precisionDivisor);

                prevx = x;
                prevy = y;
            }
            return geometryFactory.createLineString(coords);
        }
    }

    private Geometry decodeMultiLineString(Geobuf.Data.Geometry gbgeom, double precisionDivisor) {
        int nline = gbgeom.getLengthsCount();

        LineString[] lines = new LineString[nline];
        int coordGlobalIdx = 0;

        if (nline < 1) {
            lines = new LineString[1];
            int ncoord = gbgeom.getCoordsCount() / 2;
            if (ncoord < 1) {
                LOG.error("Geometry cannot be empty!");
            }
            else {
                Coordinate[] coords = new Coordinate[ncoord];

                long prevx = 0, prevy = 0;

                for (int coordIdx = 0; coordIdx < ncoord; coordIdx++) {
                    long x = gbgeom.getCoords(coordGlobalIdx++) + prevx;
                    long y = gbgeom.getCoords(coordGlobalIdx++) + prevy;

                    coords[coordIdx] = new Coordinate(x / precisionDivisor, y / precisionDivisor);

                    prevx = x;
                    prevy = y;
                }
                lines[0] = geometryFactory.createLineString(coords);
            }
        } else {
            for (int line = 0; line < nline; line++) {
                int ncoord = gbgeom.getLengths(line);
                Coordinate[] coords = new Coordinate[ncoord];

                long prevx = 0, prevy = 0;

                for (int coordIdx = 0; coordIdx < ncoord; coordIdx++) {
                    long x = gbgeom.getCoords(coordGlobalIdx++) + prevx;
                    long y = gbgeom.getCoords(coordGlobalIdx++) + prevy;

                    coords[coordIdx] = new Coordinate(x / precisionDivisor, y / precisionDivisor);

                    prevx = x;
                    prevy = y;
                }

                lines[line] = geometryFactory.createLineString(coords);
            }
        }
        return geometryFactory.createMultiLineString(lines);
    }

    private Geometry decodePolygon(Geobuf.Data.Geometry gbgeom, double precisionDivisor) {
        int coordGlobalIdx = 0;
        int nring = gbgeom.getLengthsCount();

        LinearRing shell = null;
        LinearRing[] holes = null;

        if (nring != 0) {
            holes = new LinearRing[nring - 1];
        } else
            nring ++;

        for (int ring = 0; ring < nring; ring++) {
            int ncoord = gbgeom.getCoordsCount() / 2;
            if (nring != 1) {
                ncoord = gbgeom.getLengths(ring);
            }

            Coordinate[] coords = new Coordinate[ncoord + 1];

            long prevx = 0, prevy = 0;

            for (int coordRingIdx = 0; coordRingIdx < ncoord; coordRingIdx++) {
                long x = gbgeom.getCoords(coordGlobalIdx++) + prevx;
                long y = gbgeom.getCoords(coordGlobalIdx++) + prevy;

                coords[coordRingIdx] = new Coordinate(x / precisionDivisor, y / precisionDivisor);

                prevx = x;
                prevy = y;
            }

            coords[ncoord] = coords[0];

            LinearRing theRing = geometryFactory.createLinearRing(coords);

            if (ring == 0)
                shell = theRing;
            else
                holes[ring - 1] = theRing;
        }

        return geometryFactory.createPolygon(shell, holes);
    }

    private Geometry decodeMultipolygon(Geobuf.Data.Geometry gbgeom, double precisionDivisor) {
        // decode multipolygon one polygon at a time
        // first length is number of polygons, next is number of rigns, number of coordinates for each ring,
        // number of rings, number of coordinates for each ring . . .
        int len = 0, coordGlobalIdx = 0;
        int npoly = gbgeom.getLengths(len++);

        Polygon[] polygons = new Polygon[npoly];

        for (int poly = 0; poly < npoly; poly++) {
            int nring = gbgeom.getLengths(len++);

            if (nring < 1) {
                LOG.warn("Polygon has zero rings");
                continue;
            }

            // geobuf treats the exterior as ring 0, while JTS treats it as a separate entity
            LinearRing shell = null;
            LinearRing[] holes = new LinearRing[nring - 1];

            for (int ring = 0; ring < nring; ring++) {
                int ncoord = gbgeom.getLengths(len++);
                Coordinate[] coords = new Coordinate[ncoord + 1];

                long prevx = 0, prevy = 0;

                for (int coordRingIdx = 0; coordRingIdx < ncoord; coordRingIdx++) {
                    // TODO more than two dimensions
                    long x = gbgeom.getCoords(coordGlobalIdx++) + prevx;
                    long y = gbgeom.getCoords(coordGlobalIdx++) + prevy;

                    coords[coordRingIdx] = new Coordinate(x / precisionDivisor, y / precisionDivisor);

                    prevx = x;
                    prevy = y;
                }

                // JTS wants closed polygons
                coords[ncoord] = coords[0];

                LinearRing theRing = geometryFactory.createLinearRing(coords);

                if (ring == 0)
                    shell = theRing;
                else
                    holes[ring - 1] = theRing;
            }

            polygons[poly] = geometryFactory.createPolygon(shell, holes);
        }

        return geometryFactory.createMultiPolygon(polygons);
    }

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
