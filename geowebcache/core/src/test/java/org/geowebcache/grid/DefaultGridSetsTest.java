/**
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * <p>You should have received a copy of the GNU Lesser General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 *
 * <p>Copyright 2018
 */
package org.geowebcache.grid;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import cn.hutool.core.lang.Console;
import java.io.IOException;
import java.util.*;
import org.geowebcache.config.DefaultGridsets;
import org.geowebcache.io.XMLBuilder;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

public class DefaultGridSetsTest {

    @Test
    public void testDefaultMercatorGridSet() throws Exception {
        // setup GridSet defaults that use legacy names (i.e. use EPSG:######)
        final DefaultGridsets defaultGridSets = new DefaultGridsets(false, true);
        // create a GirdSetBroker with the defaults
        final GridSetBroker broker = new GridSetBroker(Arrays.asList(defaultGridSets));
        // make sure EPSG:3857 is available and is the mercator default
        GridSet epsg3857GridSet = broker.get("EPSG:3857");
        assertNotNull("GridSetBroker missing EPSG:3857 GridSet", epsg3857GridSet);
        // make sure GoogleMapsCompatible is NOT in the defaults
        GridSet googleMapsCompatible = broker.get("GoogleMapsCompatible");
        assertNull("Unexpected GoogleMapsCompatible GridSet found", googleMapsCompatible);
        // make sure EPSG:900913 is NOT in the defaults
        GridSet epsg900913GridSet = broker.get("EPSG:900913");
        assertNull("Unexpected EPSG:900913 GridSet found", epsg900913GridSet);
        // get the default mercator gridset and make sure it matches the EPSG:3857 gridset
        GridSet defaultMercatorGridSet = broker.getWorldEpsg3857();
        assertNotNull("GridSetBroker missing default mercator GridSet", defaultMercatorGridSet);
        assertEquals(
                "Unexpected default mercator GridSet", epsg3857GridSet, defaultMercatorGridSet);
    }

    @Test
    public void testXMLGridSet() throws IOException {

        StringBuilder str = new StringBuilder();
        XMLBuilder xml = new XMLBuilder(str);

        final DefaultGridsets defaultGridSets = new DefaultGridsets(false, true);
        // create a GirdSetBroker with the defaults
        final GridSetBroker broker = new GridSetBroker(Arrays.asList(defaultGridSets));
        Set<GridSet> usedGridsets = new HashSet<>();

        List<GridSet> capabilitiesGridsets = new ArrayList<>(broker.getGridSets());
        capabilitiesGridsets.retainAll(usedGridsets);
        // sorting makes it easier to find a gridset now that levels do not repeat the gridset name
        capabilitiesGridsets.sort(Comparator.comparing(GridSet::getName));
        for (GridSet gset : capabilitiesGridsets) {
            tileMatrixSet(xml, gset);
        }
        Console.log(xml.toString());
    }

    private void tileMatrixSet(XMLBuilder xml, GridSet gridSet) throws IOException {
        xml.indentElement("TileMatrixSet");
        xml.simpleElement("ows:Identifier", gridSet.getName(), true);
        // If the following is not good enough, please get in touch and we will try to fix it :)
        xml.simpleElement(
                "ows:SupportedCRS", "urn:ogc:def:crs:EPSG::" + gridSet.getSrs().getNumber(), true);
        // TODO detect these str.append("
        // <WellKnownScaleSet>urn:ogc:def:wkss:GlobalCRS84Pixel</WellKnownScaleSet>\n");
        for (int i = 0; i < gridSet.getNumLevels(); i++) {
            double[] tlCoordinates = gridSet.getOrderedTopLeftCorner(i);
            tileMatrix(
                    xml,
                    gridSet.getGrid(i),
                    tlCoordinates,
                    gridSet.getTileWidth(),
                    gridSet.getTileHeight(),
                    gridSet.isScaleWarning());
        }
        xml.endElement("TileMatrixSet");
    }

    private void tileMatrix(
            XMLBuilder xml,
            Grid grid,
            double[] tlCoordinates,
            int tileWidth,
            int tileHeight,
            boolean scaleWarning)
            throws IOException {
        xml.indentElement("TileMatrix");
        if (scaleWarning) {
            xml.simpleElement(
                    "ows:Abstract",
                    "The grid was not well-defined, the scale therefore assumes 1m per map unit.",
                    true);
        }
        xml.simpleElement("ows:Identifier", grid.getName(), true);
        xml.simpleElement("ScaleDenominator", Double.toString(grid.getScaleDenominator()), true);
        xml.indentElement("TopLeftCorner")
                .text(Double.toString(tlCoordinates[0]))
                .text(" ")
                .text(Double.toString(tlCoordinates[1]))
                .endElement();
        xml.simpleElement("TileWidth", Integer.toString(tileWidth), true);
        xml.simpleElement("TileHeight", Integer.toString(tileHeight), true);
        xml.simpleElement("MatrixWidth", Long.toString(grid.getNumTilesWide()), true);
        xml.simpleElement("MatrixHeight", Long.toString(grid.getNumTilesHigh()), true);
        xml.endElement("TileMatrix");
    }

    @Test
    public void testNonGwc11xDefaultGridSets() throws Exception {
        // setup GridSet defaults that use non-legacy names (i.e. use GlobalCRS84Geometric instead
        // of EPSG:4326)
        final DefaultGridsets defaultGridSets = new DefaultGridsets(false, false);
        // create a GirdSetBroker with the defaults
        final GridSetBroker broker = new GridSetBroker(Arrays.asList(defaultGridSets));
        // make sure GlobalCRS84Geometric is available and is the unprojected default
        GridSet globalCrs84GridSet = broker.get("GlobalCRS84Geometric");
        assertNotNull("GridSetBroker missing GlobalCRS84Geometric GridSet", globalCrs84GridSet);
        // make sure that EPSG:4326 is NOT in the defaults
        GridSet epsg4326GridSet = broker.get("EPSG:4326");
        assertNull("Unexpected EPSG:4326 GridSet found", epsg4326GridSet);
        // get the default unprojected gridset and make sure it matches the GlobalCRS84Geometric
        // gridset
        GridSet defaultUnprojectedGridSet = broker.getWorldEpsg4326();
        assertNotNull(
                "GridSetBroker missing default unprojected GridSet", defaultUnprojectedGridSet);
        assertEquals(
                "Unexpected default unprojected GridSet",
                globalCrs84GridSet,
                defaultUnprojectedGridSet);
        // make sure GoogleMapsCompatible is available and is the mercator default
        GridSet googleMapsCompatible = broker.get("GoogleMapsCompatible");
        assertNotNull("GridSetBroker missing GoogleMapsCompatible GridSet", googleMapsCompatible);
        // make sure EPSG:3857 is NOT in the defaults
        GridSet epsg3857GridSet = broker.get("EPSG:3857");
        assertNull("Unexpected EPSG:3857 GridSet found", epsg3857GridSet);
        // make sure EPSG:900913 is NOT in the defaults
        GridSet epsg900913GridSet = broker.get("EPSG:900913");
        assertNull("Unexpected EPSG:900913 GridSet found", epsg900913GridSet);
        // get the default mercator gridset and make sure it matches the GoogleMapsCompatible
        // gridset
        GridSet defaultMercatorGridSet = broker.getWorldEpsg3857();
        assertNotNull("GridSetBroker missing default mercator GridSet", defaultMercatorGridSet);
        assertEquals(
                "Unexpected default mercator GridSet",
                googleMapsCompatible,
                defaultMercatorGridSet);
    }

    @Test
    public void test4326x2() {
        DefaultGridsets gs = new DefaultGridsets(true, true);
        GridSet base = gs.worldEpsg4326();
        GridSet x2 = gs.worldEpsg4326x2();

        // description
        assertThat(base.getDescription(), CoreMatchers.containsString("256"));
        assertThat(x2.getDescription(), CoreMatchers.containsString("512"));

        // tile sizes
        assertEquals(256, base.getTileWidth());
        assertEquals(256, base.getTileHeight());
        assertEquals(512, x2.getTileWidth());
        assertEquals(512, x2.getTileHeight());

        // reference pixel size and scale denominators
        for (int level = 0; level < base.getNumLevels(); level++) {
            assertEquals(
                    base.getGrid(level).getScaleDenominator(),
                    x2.getGrid(level).getScaleDenominator() * 2,
                    1e-6);
        }
    }

    @Test
    public void test3857x2() {
        DefaultGridsets gs = new DefaultGridsets(true, true);
        GridSet base = gs.worldEpsg3857();
        GridSet x2 = gs.worldEpsg3857x2();

        // description
        assertThat(base.getDescription(), CoreMatchers.containsString("256x256"));
        assertThat(x2.getDescription(), CoreMatchers.containsString("512x512"));

        // tile sizes
        assertEquals(256, base.getTileWidth());
        assertEquals(256, base.getTileHeight());
        assertEquals(512, x2.getTileWidth());
        assertEquals(512, x2.getTileHeight());

        // reference pixel size and scale denominators
        for (int level = 0; level < base.getNumLevels(); level++) {
            assertEquals(
                    base.getGrid(level).getScaleDenominator(),
                    x2.getGrid(level).getScaleDenominator() * 2,
                    1e-6);
        }
    }
}
