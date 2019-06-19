package org.csstudio.logbook.olog.property.fault;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.csstudio.logbook.olog.property.fault.FaultConfiguration.Group;
import org.csstudio.logbook.olog.property.fault.FaultConfigurationFactory;
import org.junit.Test;

public class FaultConfigurationParsingTest {

    @Test
    public void parserTest() throws JAXBException {
        final ClassLoader orig = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(JAXBContext.class.getClassLoader());

        try {
	        JAXBContext context = JAXBContext.newInstance(FaultConfiguration.class.getPackageName(),
	                FaultConfigurationFactory.class.getClassLoader());
	        Unmarshaller um = context.createUnmarshaller();
	        JAXBElement<FaultConfiguration> fc2 = um.unmarshal(new StreamSource(new File("resources/default_fault_config.xml")),
	                FaultConfiguration.class);
	        FaultConfiguration fc = fc2.getValue();
	        assertEquals("areas not equal", fc.getAreas(), Arrays.asList("Global", "Linac", "BR", "SR"));
	        assertEquals("subsystems not equal", fc.getSubsystems(), Arrays.asList("Diagnostics", "EPS", "RF", "Controls"));
	        assertEquals("devices not equal", fc.getDevices(), Arrays.asList("M01", "M02", "M03"));
	        List<Group> testGroups = Arrays.asList(new Group("Controls", "Kunal Shroff", "shroffk@bnl.gov"),
	                new Group("Operations", "Reid Smith", "smithr@bnl.gov"));
	        assertTrue("groups not equal", fc.getGroups().containsAll(testGroups));
        } finally {
            Thread.currentThread().setContextClassLoader(orig);
        }
    }

}
