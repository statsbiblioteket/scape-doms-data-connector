package eu.scape_project.dataconnetor.doms;

import dk.statsbiblioteket.doms.central.connectors.EnhancedFedoraImpl;
import dk.statsbiblioteket.doms.central.connectors.fedora.pidGenerator.PIDGeneratorException;
import dk.statsbiblioteket.doms.webservices.authentication.Credentials;
import dk.statsbiblioteket.doms.webservices.configuration.ConfigCollection;
import eu.scape_project.dataconnetor.doms.exceptions.ConfigurationException;

import javax.xml.bind.JAXBException;
import java.net.MalformedURLException;
import java.util.ArrayList;

public class EntityInterfaceFactory {

    public static EntityManipulator getInstance(Credentials credentials) throws

                                                ConfigurationException {
        try {
            return new EntityManipulator(
                    new ArrayList<String>(), new EnhancedFedoraImpl(
                    credentials,
                    ConfigCollection.getProperties().getProperty("doms.url"),
                    ConfigCollection.getProperties().getProperty("pidgenerator.url"),
                    null), ConfigCollection.getProperties().getProperty("scape.contentModel"));
        } catch (JAXBException | PIDGeneratorException | MalformedURLException e) {
            throw new ConfigurationException();
        }

    }
}
