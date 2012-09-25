package org.teleal.cling.binding.xml;

import static org.teleal.cling.model.XMLUtil.appendNewElement;
import static org.teleal.cling.model.XMLUtil.appendNewElementIfNotNull;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.teleal.cling.binding.xml.Descriptor.Device.ELEMENT;
import org.teleal.cling.model.Namespace;
import org.teleal.cling.model.ValidationException;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.DeviceDetails;
import org.teleal.cling.model.meta.LocalService;
import org.teleal.cling.model.meta.RemoteService;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.profile.ControlPointInfo;
import org.teleal.cling.model.types.DLNADoc;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class RecoverUDA10DeviceDescriptorBinderSAXImpl extends UDA10DeviceDescriptorBinderSAXImpl
{

  private static Logger log = Logger.getLogger("RecoverUDA10DevDescBindSAXImpl");

  private String fixGarbageTrailingChars(String descriptorXml, Exception e) {

    // (V13) NPE
    if (null == descriptorXml || descriptorXml.trim().equals("")) {
      log.warning("detected null or empty descriptor");
      return null;
    }
    int index = descriptorXml.indexOf("</root>");
    if (index == -1) return null;
    
    if (descriptorXml.length() != index + "</root>".length()) {
      log.warning("detected garbage characters after <root> node");
      return descriptorXml.substring(0, index) + "</root>";
    }
    return null;
  }

  private String fixMissingNamespace(String descriptorXml, Exception e) {

    // Windows: org.fourthline.cling.binding.xml.DescriptorBindingException: Could not parse device descriptor: org.seamless.xml.ParserException: org.xml.sax.SAXParseException: The prefix "dlna" for element "dlna:X_DLNADOC" is not bound.
    // Android: org.xmlpull.v1.XmlPullParserException: undefined prefix: dlna (position:START_TAG <{null}dlna:X_DLNADOC>@19:17 in java.io.StringReader@406dff48)

    Throwable cause = e.getCause();

    if (!(cause instanceof org.teleal.common.xml.ParserException))
      return null;

    String message = cause.getMessage();
    if (message == null)
      return null;

    Pattern pattern = Pattern.compile("The prefix \"(.*)\" for element"); // on Windows
    Matcher matcher = pattern.matcher(message);
    if (!matcher.find() || matcher.groupCount() != 1) {
      pattern = Pattern.compile("undefined prefix: ([^ ]*)"); // on Android
      matcher = pattern.matcher(message);
      if (!matcher.find() || matcher.groupCount() != 1)
        return null;
    }

    String missingNS = matcher.group(1);
    log.warning("detected missing namespace declaration: " + missingNS);

    // extract <root> attrbiutes
    pattern = Pattern.compile("<root([^>]*)");
    matcher = pattern.matcher(descriptorXml);
    if (!matcher.find() || matcher.groupCount() != 1)
      return null;

    String rootAttributes = matcher.group(1);

    // extract <root> body
    pattern = Pattern.compile("<root[^>]*>(.*)</root>", Pattern.DOTALL);
    matcher = pattern.matcher(descriptorXml);
    if (!matcher.find() || matcher.groupCount() != 1)
      return null;

    String rootBody = matcher.group(1);

    // add missing ns. It only matters that it is defined, not that it is correct
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" + "<root " + String.format("xmlns:%s=\"urn:schemas-dlna-org:device-1-0\"", missingNS) + rootAttributes + ">" + rootBody + "</root>";

  }

  @Override
  public <D extends Device> D describe(D undescribedDevice, String descriptorXml) throws DescriptorBindingException, ValidationException {

    DescriptorBindingException firstException = null;

    try {

      for (int retryCount = 0; retryCount < 5; retryCount++) {

        try {
          return super.describe(undescribedDevice, descriptorXml);
        } catch (DescriptorBindingException e) {

          // UShare fixes
          if (firstException == null) {
            firstException = e;
          }

          String fixedXml;

          fixedXml = fixMissingNamespace(descriptorXml, e);
          if (fixedXml != null) {
            descriptorXml = fixedXml;
            continue;
          }

          fixedXml = fixGarbageTrailingChars(descriptorXml, e);
          if (fixedXml != null) {
            descriptorXml = fixedXml;
            continue;
          }

          throw e;

        }
      }

      throw firstException;

    } catch (DescriptorBindingException e) {
      onInvalidXML(descriptorXml, firstException);
      throw e;
    } catch (ValidationException e) {
      onInvalidXML(descriptorXml, e);
      throw e;
    }
  }

  // override in subclass if you want to log non-parsing XML
  protected void onInvalidXML(String xml, Exception e) {
  }

  @Override
  protected void generateDevice(Namespace namespace, Device deviceModel, Document descriptor, Element rootElement, ControlPointInfo info) {

    Element deviceElement = appendNewElement(descriptor, rootElement, ELEMENT.device);

    appendNewElementIfNotNull(descriptor, deviceElement, ELEMENT.deviceType, deviceModel.getType());

    DeviceDetails deviceModelDetails = deviceModel.getDetails(info);
    appendNewElementIfNotNull(descriptor, deviceElement, ELEMENT.friendlyName, deviceModelDetails.getFriendlyName());
    if (deviceModelDetails.getManufacturerDetails() != null) {
      appendNewElementIfNotNull(descriptor, deviceElement, ELEMENT.manufacturer, deviceModelDetails.getManufacturerDetails().getManufacturer());
      appendNewElementIfNotNull(descriptor, deviceElement, ELEMENT.manufacturerURL, deviceModelDetails.getManufacturerDetails().getManufacturerURI());
    }
    if (deviceModelDetails.getModelDetails() != null) {
      appendNewElementIfNotNull(descriptor, deviceElement, ELEMENT.modelDescription, deviceModelDetails.getModelDetails().getModelDescription());
      appendNewElementIfNotNull(descriptor, deviceElement, ELEMENT.modelName, deviceModelDetails.getModelDetails().getModelName());
      appendNewElementIfNotNull(descriptor, deviceElement, ELEMENT.modelNumber, deviceModelDetails.getModelDetails().getModelNumber());
      appendNewElementIfNotNull(descriptor, deviceElement, ELEMENT.modelURL, deviceModelDetails.getModelDetails().getModelURI());
    }
    appendNewElementIfNotNull(descriptor, deviceElement, ELEMENT.serialNumber, deviceModelDetails.getSerialNumber());
    // changed location
    appendNewElementIfNotNull(descriptor, deviceElement, ELEMENT.UDN, deviceModel.getIdentity().getUdn());
    appendNewElementIfNotNull(descriptor, deviceElement, ELEMENT.presentationURL, deviceModelDetails.getPresentationURI());
    appendNewElementIfNotNull(descriptor, deviceElement, ELEMENT.UPC, deviceModelDetails.getUpc());

    if (deviceModelDetails.getDlnaDocs() != null) {
      for (DLNADoc dlnaDoc : deviceModelDetails.getDlnaDocs()) {
        appendNewElementIfNotNull(descriptor, deviceElement, Descriptor.Device.DLNA_PREFIX + ":" + ELEMENT.X_DLNADOC, dlnaDoc, Descriptor.Device.DLNA_NAMESPACE_URI);
      }
    }
    appendNewElementIfNotNull(descriptor, deviceElement, Descriptor.Device.DLNA_PREFIX + ":" + ELEMENT.X_DLNACAP, deviceModelDetails.getDlnaCaps(), Descriptor.Device.DLNA_NAMESPACE_URI);

    generateIconList(namespace, deviceModel, descriptor, deviceElement);
    generateServiceList(namespace, deviceModel, descriptor, deviceElement);
    generateDeviceList(namespace, deviceModel, descriptor, deviceElement, info);
  }

  @Override
  protected void generateServiceList(Namespace namespace, Device deviceModel, Document descriptor, Element deviceElement) {
    if (!deviceModel.hasServices())
      return;

    Element serviceListElement = appendNewElement(descriptor, deviceElement, ELEMENT.serviceList);

    for (Service service : deviceModel.getServices()) {
      Element serviceElement = appendNewElement(descriptor, serviceListElement, ELEMENT.service);

      appendNewElementIfNotNull(descriptor, serviceElement, ELEMENT.serviceType, service.getServiceType());
      appendNewElementIfNotNull(descriptor, serviceElement, ELEMENT.serviceId, service.getServiceId());
      if (service instanceof RemoteService) {
        RemoteService rs = (RemoteService) service;
        // Changed order
        appendNewElementIfNotNull(descriptor, serviceElement, ELEMENT.SCPDURL, rs.getDescriptorURI());
        appendNewElementIfNotNull(descriptor, serviceElement, ELEMENT.controlURL, rs.getControlURI());
        appendNewElementIfNotNull(descriptor, serviceElement, ELEMENT.eventSubURL, rs.getEventSubscriptionURI());
      } else if (service instanceof LocalService) {
        LocalService ls = (LocalService) service;
        // Changed order
        appendNewElementIfNotNull(descriptor, serviceElement, ELEMENT.SCPDURL, namespace.getDescriptorPath(ls));
        appendNewElementIfNotNull(descriptor, serviceElement, ELEMENT.controlURL, namespace.getControlPath(ls));
        appendNewElementIfNotNull(descriptor, serviceElement, ELEMENT.eventSubURL, namespace.getEventSubscriptionPath(ls));
      }
    }
  }

}