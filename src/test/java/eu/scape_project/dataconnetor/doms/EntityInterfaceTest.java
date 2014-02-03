package eu.scape_project.dataconnetor.doms;

import dk.statsbiblioteket.doms.central.connectors.BackendInvalidCredsException;
import dk.statsbiblioteket.doms.central.connectors.BackendInvalidResourceException;
import dk.statsbiblioteket.doms.central.connectors.BackendMethodFailedException;
import dk.statsbiblioteket.doms.central.connectors.EnhancedFedora;
import dk.statsbiblioteket.doms.central.connectors.fedora.structures.DatastreamProfile;
import dk.statsbiblioteket.doms.central.connectors.fedora.structures.ObjectProfile;
import eu.scape_project.dataconnetor.doms.exceptions.ParsingException;
import eu.scape_project.model.Identifier;
import eu.scape_project.model.IntellectualEntity;
import eu.scape_project.model.LifecycleState;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EntityInterfaceTest {
    @Test
    public void testRead() throws Exception {
        String pid = "uuid:testPid";
        String entityIdentifier = "entity-1";
        String representationIdentifier = "representation-1";
        String fileIdentifier = "file-1";
        String title = "entity 1 title";
        String fileName = "header_image";
        String mimeType = "image/png";
        String fileUrl ="http://www.scape-project.eu/wp-content/themes/medani/images/scape_logo.png";


        EnhancedFedora fedora = mock(EnhancedFedora.class);

        String scape_descriptive = "SCAPE_DESCRIPTIVE";
        String scape_lifecycle = "SCAPE_LIFECYCLE";

        String scape_rights = "SCAPE_RIGHTS";
        String scape_provenance = "SCAPE_PROVENANCE";
        String scape_source = "SCAPE_SOURCE";
        String scape_representation_technical = "SCAPE_REPRESENTATION_TECHNICAL";

        String scape_file_technical = "SCAPE_FILE_TECHNICAL";
        String scape_file_content = "SCAPE_FILE_CONTENT";


        setupContentModels(
                fedora,
                scape_descriptive,
                scape_lifecycle,
                scape_rights,
                scape_provenance,
                scape_source,
                scape_representation_technical,
                scape_file_technical,
                scape_file_content);


        setIdentifiers(pid, entityIdentifier, representationIdentifier, fileIdentifier, fedora);
        setObjectProfile(
                pid,
                title,
                fileName,
                mimeType,
                fileUrl,
                fedora,
                scape_descriptive,
                scape_lifecycle,
                scape_rights,
                scape_provenance,
                scape_source,
                scape_representation_technical,
                scape_file_technical,
                scape_file_content);


        addEntityDatastreams(pid, title, fedora, scape_descriptive, scape_lifecycle);


        addRepresentationDatastreams(
                pid,
                fedora,
                scape_rights,
                scape_provenance,
                scape_source,
                scape_representation_technical);
        addFileDatastreams(pid, fedora, scape_file_technical);
        EntityInterface entityInterface = new EntityInterface(fedora);

        IntellectualEntity entity = entityInterface.read(pid);

        Assert.assertEquals(XmlUtils.toString(entity.getDescriptive()), "descriptive");


    }

    private void setIdentifiers(String pid, String entityIdentifier, String representationIdentifier,
                                String fileIdentifier, EnhancedFedora fedora) throws
                                                                              BackendInvalidCredsException,
                                                                              BackendMethodFailedException,
                                                                              BackendInvalidResourceException {
        when(fedora.getXMLDatastreamContents(eq(pid), eq("DC"))).thenReturn(
                getDC(pid, entityIdentifier, representationIdentifier, fileIdentifier));
    }

    private void setObjectProfile(String pid, String title, String fileName, String mimeType, String fileUrl,
                                  EnhancedFedora fedora, String scape_descriptive, String scape_lifecycle,
                                  String scape_rights, String scape_provenance, String scape_source,
                                  String scape_representation_technical, String scape_file_technical,
                                  String scape_file_content) throws
                                                             BackendMethodFailedException,
                                                             BackendInvalidCredsException,
                                                             BackendInvalidResourceException {
        ObjectProfile objectProfile = new ObjectProfile();
        objectProfile.setLabel(title);
        objectProfile.setPid(pid);
        objectProfile.setContentModels(
                Arrays.asList(
                        TypeUtils.ENTITY_CONTENT_MODEL,
                        TypeUtils.REPRESENTATION_CONTENT_MODEL,
                        TypeUtils.FILE_CONTENT_MODEL));


        List<DatastreamProfile> datastreamProfiles = new ArrayList<>(); for (String name : Arrays.asList(
                scape_descriptive,
                scape_lifecycle,
                scape_rights,
                scape_provenance,
                scape_source,
                scape_representation_technical,
                scape_file_technical)) {
            DatastreamProfile managed = makeManagedDatastream(pid, name);
            datastreamProfiles.add(managed);
        }
        DatastreamProfile fileContent = new DatastreamProfile();
        fileContent.setID(scape_file_content);

        fileContent.setLabel(fileName);

        fileContent.setMimeType(mimeType);

        fileContent.setUrl(fileUrl);
        fileContent.setInternal(false);
        datastreamProfiles.add(fileContent);
        objectProfile.setDatastreams(datastreamProfiles);

        when(fedora.getObjectProfile(eq(pid), anyLong())).thenReturn(objectProfile);
    }

    private void addFileDatastreams(String pid, EnhancedFedora fedora, String scape_file_technical) throws
                                                                                                    BackendInvalidCredsException,
                                                                                                    BackendMethodFailedException,
                                                                                                    BackendInvalidResourceException {
        when(fedora.getXMLDatastreamContents(eq(pid), eq(scape_file_technical), anyLong())).thenReturn(
                getEmptyTextMD());
    }

    private void addRepresentationDatastreams(String pid, EnhancedFedora fedora, String scape_rights,
                                              String scape_provenance, String scape_source,
                                              String scape_representation_technical) throws
                                                                                     BackendInvalidCredsException,
                                                                                     BackendMethodFailedException,
                                                                                     BackendInvalidResourceException {
        when(fedora.getXMLDatastreamContents(eq(pid), eq(scape_provenance), anyLong())).thenReturn(
                getSimpleProvenance());

        when(fedora.getXMLDatastreamContents(eq(pid), eq(scape_rights), anyLong())).thenReturn(
                getSimpleRights());

        when(fedora.getXMLDatastreamContents(eq(pid), eq(scape_representation_technical), anyLong())).thenReturn(
                getEmptyTextMD());
        when(fedora.getXMLDatastreamContents(eq(pid), eq(scape_source), anyLong())).thenReturn(
                getSimpleSource());
    }

    private void addEntityDatastreams(String pid, String title, EnhancedFedora fedora, String scape_descriptive,
                                      String scape_lifecycle) throws
                                                              BackendInvalidCredsException,
                                                              BackendMethodFailedException,
                                                              BackendInvalidResourceException,
                                                              ParsingException {
        when(fedora.getXMLDatastreamContents(eq(pid), eq(scape_descriptive), anyLong())).thenReturn(
                getDescriptive(title));
        when(
                fedora.getXMLDatastreamContents(
                        eq(pid), eq(scape_lifecycle), anyLong())).thenReturn(
                getSimpleLifeCycle());
    }

    private void setupContentModels(EnhancedFedora fedora, String scape_descriptive, String scape_lifecycle,
                                    String scape_rights, String scape_provenance, String scape_source,
                                    String scape_representation_technical, String scape_file_technical,
                                    String scape_file_content) throws
                                                               BackendInvalidCredsException,
                                                               BackendMethodFailedException,
                                                               BackendInvalidResourceException {
        when(
                fedora.getXMLDatastreamContents(
                        eq(TypeUtils.ENTITY_CONTENT_MODEL), eq(DSCompositeModel.DS_COMPOSITE_MODEL))).thenReturn(
                getEntityDsComp(scape_descriptive, scape_lifecycle));

        when(
                fedora.getXMLDatastreamContents(
                        eq(TypeUtils.REPRESENTATION_CONTENT_MODEL),
                        eq(DSCompositeModel.DS_COMPOSITE_MODEL))).thenReturn(
                getRepresentationDsComp(scape_rights, scape_provenance, scape_source, scape_representation_technical));


        when(
                fedora.getXMLDatastreamContents(
                        eq(TypeUtils.FILE_CONTENT_MODEL), eq(DSCompositeModel.DS_COMPOSITE_MODEL))).thenReturn(
                getFileDsComp(scape_file_technical, scape_file_content));
    }

    private String getDC(String pid, String entityIdentifier, String representationIdentifier, String fileIdentifier) {
        return "<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n" +
        "  <dc:identifier>" + pid + "</dc:identifier>\n" +
        "  <dc:identifier>" + TypeUtils.formatEntityIdentifier(new Identifier(entityIdentifier)) + "</dc:identifier>\n" +
        "  <dc:identifier>" + TypeUtils.formatRepresentationIdentifier(new Identifier(representationIdentifier)) + "</dc:identifier>\n" +
        "  <dc:identifier>" + TypeUtils.formatFileIdentifier(new Identifier(fileIdentifier)) + "</dc:identifier>\n" +
        "</oai_dc:dc>";
    }

    private String getFileDsComp(String scape_file_technical, String scape_file_content) {
        return "<dsCompositeModel xmlns=\"info:fedora/fedora-system:def/dsCompositeModel#\">\n" +
        "    <dsTypeModel ID=\"" + scape_file_technical + "\">\n" +
        "        <form MIME=\"text/xml\"/>\n" +
        "        <extension name=\"SCAPE\">\n" +
        "            <mapsAs name=\"file_technical\"/>\n" +
        "        </extension>\n" +
        "    </dsTypeModel>\n" +
        "\n" +
        "    <dsTypeModel ID=\"" + scape_file_content + "\">\n" +
        "        <extension name=\"SCAPE\">\n" +
        "            <mapsAs name=\"file_content\"/>\n" +
        "        </extension>\n" +
        "    </dsTypeModel>\n" +
        "\n             " +
        " </dsCompositeModel>\n ";
    }

    private String getRepresentationDsComp(String scape_rights, String scape_provenance, String scape_source,
                                           String scape_representation_technical) {
        return "<dsCompositeModel xmlns=\"info:fedora/fedora-system:def/dsCompositeModel#\">\n" +
        "    <dsTypeModel ID=\"" + scape_rights + "\">\n" +
        "        <form MIME=\"text/xml\"/>\n" +
        "        <extension name=\"SCAPE\">\n" +
        "            <mapsAs name=\"rights\"/>\n" +
        "        </extension>\n" +
        "    </dsTypeModel>\n" +
        "\n" +
        "    <dsTypeModel ID=\"" + scape_provenance + "\">\n" +
        "        <form MIME=\"text/xml\"/>\n" +
        "        <extension name=\"SCAPE\">\n" +
        "            <mapsAs name=\"provenance\"/>\n" +
        "        </extension>\n" +
        "    </dsTypeModel>\n" +
        "\n" +
        "    <dsTypeModel ID=\"" + scape_source + "\">\n" +
        "        <form MIME=\"text/xml\"/>\n" +
        "        <extension name=\"SCAPE\">\n" +
        "            <mapsAs name=\"source\"/>\n" +
        "        </extension>\n" +
        "    </dsTypeModel>\n" +
        "\n" +
        "    <dsTypeModel ID=\"" + scape_representation_technical + "\">\n" +
        "        <form MIME=\"text/xml\"/>\n" +
        "        <extension name=\"SCAPE\">\n" +
        "            <mapsAs name=\"representation_technical\"/>\n" +
        "        </extension>\n" +
        "    </dsTypeModel>\n" +
        "</dsCompositeModel>\n";
    }

    private String getEntityDsComp(String scape_descriptive, String scape_lifecycle) {
        return "<dsCompositeModel xmlns=\"info:fedora/fedora-system:def/dsCompositeModel#\">\n" +
        "    <dsTypeModel ID=\"" + scape_descriptive + "\">\n" +
        "        <form MIME=\"text/xml\"/>\n" +
        "        <extension name=\"SCAPE\">\n" +
        "            <mapsAs name=\"descriptive\"/>\n" +
        "        </extension>\n" +
        "    </dsTypeModel>\n" +
        "    <dsTypeModel ID=\"" + scape_lifecycle + "\">\n" +
        "        <form MIME=\"text/xml\"/>\n" +
        "        <extension name=\"SCAPE\">\n" +
        "            <mapsAs name=\"lifecycle\"/>\n" +
        "        </extension>\n" +
        "    </dsTypeModel>\n" +
        "</dsCompositeModel>\n";
    }

    private DatastreamProfile makeManagedDatastream(String pid, String id) {
        DatastreamProfile managed = new DatastreamProfile();
        managed.setID(id);
        managed.setMimeType("text/xml");
        managed.setInternal(true);
        return managed;
    }

    private String getSimpleSource() {
        return " <dc:dublin-core xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n" +
               "                        <dc:title>Source object 1</dc:title>\n" +
               "                    </dc:dublin-core>";
    }

    private String getDescriptive(String title) {
        return "<dc:dublin-core xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n" +
               " <dc:title>" + title + "</dc:title>\n" +
               "</dc:dublin-core>\n";
    }

    private String getSimpleLifeCycle() throws ParsingException {
        return XmlUtils.toString(
                new LifecycleState(
                        "details", LifecycleState.State.INGESTED));
    }

    private String getSimpleProvenance() {
        return "                    <premis:premis version=\"2.2\" xmlns:premis=\"info:lc/xmlns/premis-v2\" xmlns:xlink=\"http://www.w3.org/1999/xlink\">\n" +
               "                        <premis:event>\n" +
               "                            <premis:eventType>INGEST</premis:eventType>\n" +
               "                            <premis:eventDetail>inital ingest</premis:eventDetail>\n" +
               "                            <premis:linkingAgentIdentifier xlink:role=\"CREATOR\" xlink:title=\"Testman Testrevicz\"/>\n" +
               "                        </premis:event>\n" +
               "                    </premis:premis>\n";
    }

    private String getSimpleRights() {
        return "                    <premis:rights xmlns:premis=\"info:lc/xmlns/premis-v2\">\n" +
               "                        <premis:rightsStatement>\n" +
               "                            <premis:copyrightInformation>\n" +
               "<premis:copyrightStatus>no copyright</premis:copyrightStatus>\n" +
               "<premis:copyrightJurisdiction>de</premis:copyrightJurisdiction>\n" +
               "                            </premis:copyrightInformation>\n" +
               "                        </premis:rightsStatement>\n" +
               "                    </premis:rights>\n";
    }

    private String getEmptyTextMD() {
        return "<textmd:textMD xmlns:textmd=\"info:lc/xmlns/textmd-v3\">\n" +
               "<textmd:encoding>\n" +
               "<textmd:encoding_platform linebreak=\"LF\"/>\n" +
               "</textmd:encoding>\n" +
               "</textmd:textMD>";
    }

    @Test
    public void testCreateNew() throws Exception {

    }

    @Test
    public void testUpdate() throws Exception {

    }
}