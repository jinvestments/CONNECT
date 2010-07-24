package gov.hhs.fha.nhinc.docretrieve.adapter.proxy;

import gov.hhs.fha.nhinc.common.nhinccommon.AssertionType;
import ihe.iti.xds_b._2007.RetrieveDocumentSetRequestType;
import ihe.iti.xds_b._2007.RetrieveDocumentSetResponseType;
import gov.hhs.fha.nhinc.adapterdocretrievesecured.AdapterDocRetrieveSecured;
import gov.hhs.fha.nhinc.adapterdocretrievesecured.AdapterDocRetrieveSecuredPortType;
import gov.hhs.fha.nhinc.connectmgr.ConnectionManagerCache;
import gov.hhs.fha.nhinc.nhinclib.NhincConstants;
import gov.hhs.fha.nhinc.saml.extraction.SamlTokenCreator;
import java.util.Map;
import javax.xml.ws.BindingProvider;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryError;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryErrorList;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryResponseType;

/**
 *
 *
 * @author Neil Webb
 */
public class AdapterDocRetrieveWebServiceSecuredProxy implements AdapterDocRetrieveProxy
{
    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(AdapterDocRetrieveWebServiceSecuredProxy.class);
    private static AdapterDocRetrieveSecured service = new AdapterDocRetrieveSecured();

    public RetrieveDocumentSetResponseType retrieveDocumentSet(RetrieveDocumentSetRequestType request, AssertionType assertion)
    {
        RetrieveDocumentSetResponseType response = null;
        log.debug("Begin AdapterDocRetrieveWebServiceSecuredProxy.retrieveDocumentSet(...)");

        try
        {
            log.debug("Retrieving adapter doc retrieve secured url");
            String url = ConnectionManagerCache.getLocalEndpointURLByServiceName(NhincConstants.ADAPTER_DOC_RETRIEVE_SECURED_SERVICE_NAME);
            AdapterDocRetrieveSecuredPortType port = getPort(url);

            log.debug("Creating SAML token");
            SamlTokenCreator tokenCreator = new SamlTokenCreator();
            Map requestContext = tokenCreator.CreateRequestContext(assertion, url, NhincConstants.DOC_RETRIEVE_ACTION);
            ((BindingProvider) port).getRequestContext().putAll(requestContext);

            log.debug("Sending secured adapter doc query message");
            response = port.respondingGatewayCrossGatewayRetrieve(request);
        }
        catch (Exception ex)
        {
            log.error("Error calling adapter secured doc retrieve service: " + ex.getMessage(), ex);
            response = createErrorResponse("Sending doc query to adapter");
        }

        log.debug("End AdapterDocRetrieveWebServiceSecuredProxy.retrieveDocumentSet(...)");
        return response;
    }

    private AdapterDocRetrieveSecuredPortType getPort(String url)
    {
        log.debug("Creating secured adapter doc retrieve port");
        AdapterDocRetrieveSecuredPortType port = service.getAdapterDocRetrieveSecuredPortSoap();
        log.info("Setting endpoint address to Adapter doc retrieve secured to " + url);
        ((BindingProvider) port).getRequestContext().put(javax.xml.ws.BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);
        return port;
    }
    
    private RetrieveDocumentSetResponseType createErrorResponse(String codeContext)
    {
        RetrieveDocumentSetResponseType response = new RetrieveDocumentSetResponseType();
        RegistryResponseType responseType = new RegistryResponseType();
        response.setRegistryResponse(responseType);
        responseType.setStatus("urn:oasis:names:tc:ebxml-regrep:ResponseStatusType:Failure");
        RegistryErrorList regErrList = new RegistryErrorList();
        responseType.setRegistryErrorList(regErrList);
        RegistryError regErr = new RegistryError();
        regErrList.getRegistryError().add(regErr);
        regErr.setCodeContext(codeContext);
        regErr.setErrorCode("XDSRepositoryError");
        regErr.setSeverity("Error");
        return response;
    }
}