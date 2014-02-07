package eu.scape_project.dataconnetor.doms;

import dk.statsbiblioteket.doms.central.connectors.BackendInvalidCredsException;
import dk.statsbiblioteket.doms.central.connectors.BackendInvalidResourceException;
import dk.statsbiblioteket.doms.central.connectors.BackendMethodFailedException;
import dk.statsbiblioteket.doms.central.connectors.EnhancedFedora;
import dk.statsbiblioteket.doms.central.connectors.fedora.pidGenerator.PIDGeneratorException;
import dk.statsbiblioteket.doms.central.connectors.fedora.structures.DatastreamProfile;
import dk.statsbiblioteket.doms.central.connectors.fedora.structures.ObjectProfile;
import dk.statsbiblioteket.util.Bytes;
import dk.statsbiblioteket.util.Checksums;
import eu.scape_project.dataconnetor.doms.exceptions.AlreadyExistsException;
import eu.scape_project.dataconnetor.doms.exceptions.CommunicationException;
import eu.scape_project.dataconnetor.doms.exceptions.NotFoundException;
import eu.scape_project.dataconnetor.doms.exceptions.ParsingException;
import eu.scape_project.dataconnetor.doms.exceptions.UnauthorizedException;
import eu.scape_project.model.File;
import eu.scape_project.model.Identifier;
import eu.scape_project.model.IntellectualEntity;
import eu.scape_project.model.LifecycleState;
import eu.scape_project.model.Representation;
import eu.scape_project.model.TechnicalMetadata;
import eu.scape_project.model.TechnicalMetadataList;

import javax.xml.bind.JAXBException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

public class EntityManipulator {


    public static final String HASMODEL = "info:fedora/fedora-system:def/model#hasModel";
    private List<String> collections;
    private EnhancedFedora enhancedFedora;
    private String scape_content_model;


    public EntityManipulator(List<String> collections, EnhancedFedora enhancedFedora, String scape_content_model) {
        this.collections = collections;
        this.enhancedFedora = enhancedFedora;
        this.scape_content_model = scape_content_model;
    }

    /**
     * This method reads an object as an intellectual entity and returns this
     *
     * @param pid        the pid of the object
     * @param references
     *
     * @return the parsed entity
     * @throws BackendInvalidResourceException
     * @throws BackendInvalidCredsException
     * @throws BackendMethodFailedException
     * @throws eu.scape_project.dataconnetor.doms.exceptions.ParsingException
     * @throws JAXBException
     * @throws PIDGeneratorException
     * @throws MalformedURLException
     */
    IntellectualEntity read(String pid, boolean references) throws
                                                            CommunicationException,
                                                            UnauthorizedException,
                                                            NotFoundException,
                                                            ParsingException {
        EnhancedFedora fedora = getEnhancedFedora();

        List<String> identifiers = null;
        try {
            identifiers = TypeUtils.getDCIdentifiers(fedora, pid, "scape");

            ObjectProfile profile = fedora.getObjectProfile(pid, null);
            DSCompositeModel model = getDsCompositeModel(fedora, profile.getContentModels());

            //Build the entity
            IntellectualEntity.Builder builder = new IntellectualEntity.Builder();
            builder.identifier(new Identifier(TypeUtils.pickEntityIdentifier(identifiers)));
            builder.descriptive(getIfExists(pid,fedora,profile,model.getDescriptive()));
            builder.lifecycleState((LifecycleState) getIfExists(pid,fedora,profile,model.getLifeCycle()));

            //TODO version number


            //Build the representation
            Representation.Builder rep_builder = new Representation.Builder();
            rep_builder.identifier(new Identifier(TypeUtils.pickRepresentationIdentifier(identifiers)));
            for (String repTechDatastream : model.getRepresentationTechnical()) {
                rep_builder.technical(repTechDatastream, getIfExists(pid, fedora, profile, repTechDatastream));
            }

            rep_builder.rights(getIfExists(pid, fedora, profile, model.getRights()));
            rep_builder.source(getIfExists(pid, fedora, profile, model.getSource()));
            rep_builder.provenance(getIfExists(pid, fedora, profile, model.getProvenance()));
            rep_builder.title(profile.getLabel());

            //Build the File
            File.Builder file_builder = new File.Builder();
            file_builder.identifier(new Identifier(TypeUtils.pickFileIdentifier(identifiers)));
            for (String fileTechDatastream : model.getFileTechnical()) {
                file_builder.technical(fileTechDatastream, getIfExists(pid, fedora, profile, fileTechDatastream));
            }

            String contentDatastreamName = model.getFileContent();
            for (DatastreamProfile datastreamProfile : profile.getDatastreams()) {
                if (datastreamProfile.getID().equals(contentDatastreamName)) {
                    file_builder.filename(datastreamProfile.getLabel());
                    file_builder.mimetype(datastreamProfile.getMimeType());
                    file_builder.uri(URI.create(datastreamProfile.getUrl()));
                }
            }
            //Build the bitstreams
            rep_builder.files(Arrays.asList(file_builder.build()));
            builder.representations(Arrays.asList(rep_builder.build()));
            return builder.build();
        } catch (BackendMethodFailedException e) {
            throw new CommunicationException(e);
        } catch (BackendInvalidResourceException e) {
            throw new NotFoundException(e);
        } catch (BackendInvalidCredsException e) {
            throw new UnauthorizedException(e);
        }
    }

    private Object getIfExists(String pid, EnhancedFedora fedora, ObjectProfile profile, String datastream) throws
                                                                                                            BackendInvalidCredsException,
                                                                                                            BackendMethodFailedException,
                                                                                                            ParsingException {
        try {
            boolean get = false;
            for (DatastreamProfile datastreamProfile : profile.getDatastreams()) {
                if (datastreamProfile.getID().equals(datastream)) {
                    get = true;
                    break;
                }
            }
            if (get) {
                String rights = fedora.getXMLDatastreamContents(pid, datastream, null);
                return XmlUtils.toObject((rights));
            } else {
                return null;
            }
        } catch (BackendInvalidResourceException e) {
            return null;
        }
    }


    /**
     * Create a new entity in doms from the given entity
     *
     * @param entity the entity to persist
     *
     * @return the pid of the new entity
     * @throws JAXBException
     * @throws PIDGeneratorException
     * @throws MalformedURLException
     * @throws eu.scape_project.dataconnetor.doms.exceptions.AlreadyExistsException
     * @throws ParsingException
     * @throws eu.scape_project.dataconnetor.doms.exceptions.UnauthorizedException
     * @throws eu.scape_project.dataconnetor.doms.exceptions.CommunicationException
     */
    public String createNew(IntellectualEntity entity) throws
                                                       CommunicationException,
                                                       AlreadyExistsException,
                                                       ParsingException,
                                                       UnauthorizedException {
        try {
            try {
                getPids(entity.getIdentifier().getValue());
                throw new AlreadyExistsException("An entity with id '"+entity.getIdentifier().getValue()+"' already exists");
            } catch (NotFoundException e){
                return createOrUpdate(null, entity);
            }
        } catch (NotFoundException e) {
            throw new CommunicationException(e);//This should not be possible, so it means something else is broken
        }
    }

    private Object findContents(String representationTechnicalDatastream, TechnicalMetadataList technical) {
        for (TechnicalMetadata technicalMetadata : technical.getContent()) {
            if (technicalMetadata.getId().equals(representationTechnicalDatastream)) {
                return technicalMetadata.getContents();
            }
        }
        return null;
    }


    /**
     * Update an entity already persisted in doms
     *
     * @param pid
     * @param entity
     */
    private String createOrUpdate(String pid, IntellectualEntity entity) throws
                                                                 CommunicationException,
                                                                 NotFoundException,
                                                                 ParsingException,
                                                                 UnauthorizedException {
        EnhancedFedora fedora = getEnhancedFedora();

        try {
            String logmessage = "logmessage";

            List<String> scapeIdentifiers = TypeUtils.formatIdentifiers(entity);

            ObjectProfile profile;
            DSCompositeModel model;
            if (pid == null) {
                //We have a new object here, with the identifiers
                pid = fedora.newEmptyObject(scapeIdentifiers, getCollections(), logmessage);
                //add the content models
                String contentModel = "info:fedora/" + scape_content_model;
                fedora.addRelation(
                        pid, "info:fedora/" + pid, HASMODEL, contentModel, false, logmessage);

                profile = null;
                model = getDsCompositeModel(fedora, Arrays.asList(contentModel));
            } else {
                profile = fedora.getObjectProfile(pid, null);
                setIdentifiers(pid, scapeIdentifiers, fedora);
                model = getDsCompositeModel(fedora, profile.getContentModels());
            }



            //TODO version number

            changeIfNeeded(pid, fedora, logmessage, profile, model.getLifeCycle(), entity.getLifecycleState());
            changeIfNeeded(pid, fedora, logmessage, profile, model.getDescriptive(), entity.getDescriptive());

            for (Representation representation : entity.getRepresentations()) {
                changeIfNeeded(pid, fedora, logmessage, profile, model.getProvenance(), representation.getProvenance());
                changeIfNeeded(pid, fedora, logmessage, profile, model.getRights(), representation.getRights());
                changeIfNeeded(pid, fedora, logmessage, profile, model.getSource(), representation.getSource());

                for (String representationTechnicalDatastream : model.getRepresentationTechnical()) {
                    Object contents = findContents(representationTechnicalDatastream, representation.getTechnical());
                    changeIfNeeded(pid, fedora, logmessage, profile, representationTechnicalDatastream, contents);

                }
                if (representation.getTitle() != null) {
                    fedora.modifyObjectLabel(pid, representation.getTitle(), logmessage);
                } else {
                    //TODO remove label
                }
                for (File file : representation.getFiles()) {
                    for (String fileTechnicalMetadata : model.getFileTechnical()) {
                        Object contents = findContents(fileTechnicalMetadata, file.getTechnical());
                        changeIfNeeded(pid, fedora, logmessage, profile, fileTechnicalMetadata, contents);
                    }
                    if (file.getFilename() != null && file.getUri() != null && file.getMimetype() != null) {
                        fedora.addExternalDatastream(
                                pid,
                                model.getFileContent(),
                                file.getFilename(),
                                file.getUri().toString(),
                                "unknown",
                                file.getMimetype(),
                                null,
                                logmessage);
                    }

                }

            }
            return pid;
        } catch (BackendInvalidCredsException e) {
            throw new UnauthorizedException(e);

        } catch (BackendInvalidResourceException | BackendMethodFailedException | PIDGeneratorException e) {
            throw new CommunicationException(e);
        }

    }

    private void changeIfNeeded(String pid, EnhancedFedora fedora, String logmessage, ObjectProfile profile,
                                String datastream, Object content) throws
                                                                   ParsingException,
                                                                   BackendMethodFailedException,
                                                                   BackendInvalidResourceException,
                                                                   BackendInvalidCredsException {

        if (content != null) {

            String contentString = XmlUtils.toString(content);
            String md5sum = Bytes.toHex(Checksums.md5(contentString));

            boolean toWrite = true;
            if (profile != null) {
                for (DatastreamProfile datastreamProfile : profile.getDatastreams()) {
                    if (datastreamProfile.getID().equals(datastream)) {
                        if (md5sum.equalsIgnoreCase(datastreamProfile.getChecksum())) {
                            toWrite = false;
                            break;
                        }
                    }
                }
            }
            if (toWrite) {
                fedora.modifyDatastreamByValue(
                        pid, datastream, contentString, md5sum, null, logmessage);
            }
        } else {
            try {
                fedora.deleteDatastream(pid, datastream, logmessage);
            } catch (BackendInvalidResourceException e) {
                //ignore, the datastream does not exist
            }
        }
    }

    private DSCompositeModel getDsCompositeModel(EnhancedFedora fedora, List<String> contentModels) throws
                                                                                               BackendMethodFailedException,
                                                                                               BackendInvalidResourceException,
                                                                                               BackendInvalidCredsException {
        DSCompositeModel model = new DSCompositeModel();
        for (String contentModel : contentModels) {
            model.merge(new DSCompositeModel(contentModel.replace("info:fedora/", ""), fedora));
        }
        return model;
    }

    private void setIdentifiers(String pid, List<String> scapeIdentifiers, EnhancedFedora fedora) {
        //TODO
    }


    private List<String> getCollections() {
        return collections;
    }

    private EnhancedFedora getEnhancedFedora() {
        return enhancedFedora;
    }

    public IntellectualEntity readFromEntityID(String entityID, boolean references) throws
                                                                                    NotFoundException,
                                                                                    CommunicationException,
                                                                                    UnauthorizedException,
                                                                                    ParsingException {
        List<String> pids = null;
        pids = getPids(entityID);
        return read(pids.get(0), references);
    }

    public void updateFromEntityID(String entityID, IntellectualEntity entity) throws
                                                                               NotFoundException,
                                                                               CommunicationException,
                                                                               UnauthorizedException,
                                                                               ParsingException {
        List<String> pids = null;
        pids = getPids(entityID);
        createOrUpdate(pids.get(0), entity);
    }

    private List<String> getPids(String entityID) throws
                                                  UnauthorizedException,
                                                  CommunicationException,
                                                  NotFoundException {
        List<String> pids;
        try {
            pids = getEnhancedFedora().findObjectFromDCIdentifier(
                    TypeUtils.formatEntityIdentifier(
                            new Identifier(
                                    entityID)));
        } catch (BackendInvalidCredsException e) {
            throw new UnauthorizedException(e);
        } catch (BackendMethodFailedException e) {
            throw new CommunicationException(e);
        }
        if (pids.size() == 0) {
            throw new NotFoundException();
        }
        return pids;
    }

}