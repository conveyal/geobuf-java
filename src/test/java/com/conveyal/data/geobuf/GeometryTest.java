package com.conveyal.data.geobuf;

import geobuf.Geobuf;
import junit.framework.TestCase;
import org.geotools.geojson.feature.FeatureJSON;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.simple.SimpleFeature;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class GeometryTest extends TestCase {
    private final GeometryFactory gf = new GeometryFactory();

    @Test
    public void testPoint () throws Exception {
        String geojson = "{\"type\":\"Feature\",\n" +
                "    \"properties\":{},\n" +
                "    \"geometry\":{\n" +
                "        \"type\":\"Point\",\n" +
                "        \"coordinates\":[105.380859375,31.57853542647338]\n" +
                "    }\n" +
                "}";
        mainFunction(geojson);
    }

    @Test
    public void testMultiPoint() throws Exception {
        String geojson = "{\"type\":\"Feature\",\n" +
                "    \"properties\":{},\n" +
                "    \"geometry\":{\n" +
                "        \"type\":\"MultiPoint\",\n" +
                "        \"coordinates\":[\n" +
                "            [105.380859375,31.57853542647338],\n" +
                "            [105.580859375,31.52853542647338]\n" +
                "        ]\n" +
                "    }\n" +
                "}";
        mainFunction(geojson);
    }

    @Test
    public void testLineString() throws Exception {
        String geojson = "{\"type\":\"Feature\",\n" +
                "    \"properties\":{},\n" +
                "    \"geometry\":{\n" +
                "        \"type\":\"LineString\",\n" +
                "        \"coordinates\":[\n" +
                "            [105.6005859375,30.65681556429287],\n" +
                "            [107.95166015624999,31.98944183792288],\n" +
                "            [109.3798828125,30.031055426540206],\n" +
                "            [107.7978515625,29.935895213372444]\n" +
                "        ]\n" +
                "    }\n" +
                "}";
        mainFunction(geojson);
    }

    @Test
    public void testMultiLineString() throws Exception {
        String geojson = "{\"type\":\"Feature\",\n" +
                "    \"properties\":{},\n" +
                "    \"geometry\":{\n" +
                "        \"type\":\"MultiLineString\",\n" +
                "        \"coordinates\":\n" +
                "        [\n" +
                "            [\n" +
                "                [105.6005859375,30.65681556429287],\n" +
                "                [107.95166015624999,31.98944183792288],\n" +
                "                [109.3798828125,30.031055426540206],\n" +
                "                [107.7978515625,29.935895213372444]\n" +
                "            ],\n" +
                "            [\n" +
                "                [109.3798828125,30.031055426540206],\n" +
                "                [107.1978515625,31.235895213372444]\n" +
                "            ]\n" +
                "        ]\n" +
                "    }\n" +
                "}\n";
        mainFunction(geojson);
    }

    @Test
    public void testPolygon() throws Exception {
        String geojson = "{\"type\":\"Feature\",\n" +
                "    \"properties\":{},\n" +
                "    \"geometry\":{\n" +
                "        \"type\":\"Polygon\",\n" +
                "        \"coordinates\":[\n" +
                "            [\n" +
                "                [106.10595703125,33.33970700424026],\n" +
                "                [106.32568359375,32.41706632846282],\n" +
                "                [108.03955078125,32.2313896627376],\n" +
                "                [108.25927734375,33.15594830078649],\n" +
                "                [106.10595703125,33.33970700424026]\n" +
                "            ]\n" +
                "        ]\n" +
                "    }\n" +
                "}";
        mainFunction(geojson);
    }

    @Test
    public void testMultiPolygon() throws Exception {
        String geojson = "{\n" +
                "    \"type\": \"Feature\",\n" +
                "    \"properties\": {},\n" +
                "    \"geometry\": {\n" +
                "    \"type\": \"MultiPolygon\",\n" +
                "    \"coordinates\":\n" +
                "      [ \n" +
                "          [\n" +
                "              [\n" +
                "                  [109.2041015625,30.088107753367257],\n" +
                "                  [115.02685546875,30.088107753367257],\n" +
                "                  [115.02685546875,32.7872745269555],\n" +
                "                  [109.2041015625,32.7872745269555],\n" +
                "                  [109.2041015625,30.088107753367257]\n" +
                "            \n" +
                "            \n" +
                "              ]\n" +
                "          ],\n" +
                "          [\n" +
                "              [\n" +
                "                  [112.9833984375,26.82407078047018],\n" +
                "                  [116.69677734375,26.82407078047018],\n" +
                "                  [116.69677734375,29.036960648558267],\n" +
                "                  [112.9833984375,29.036960648558267],\n" +
                "                  [112.9833984375,26.82407078047018]\n" +
                "              ]\n" +
                "          ]\n" +
                "      ]\n" +
                "    }\n" +
                "}";
        mainFunction(geojson);
    }

    public void mainFunction(String geojson) throws Exception {
        FeatureJSON featureJSON = new FeatureJSON();
        SimpleFeature simpleFeature = featureJSON.readFeature(geojson);

        /**
         Encode simplefeature
         */
        GeobufFeature geobufFeature = new GeobufFeature(simpleFeature);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        GeobufEncoder encoder = new GeobufEncoder(output, 9);
        encoder.writeFeatureCollection(List.of(geobufFeature));

        /**
         Decode geobuf
         */
        ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
        GeobufDecoder decoder = new GeobufDecoder(input);
        GeobufFeature geoBufFeat = decoder.next();
        Geometry geometry = geoBufFeat.geometry;
        String geoWkt = geometry.toText();

        System.out.println(geoWkt);
    }
}
