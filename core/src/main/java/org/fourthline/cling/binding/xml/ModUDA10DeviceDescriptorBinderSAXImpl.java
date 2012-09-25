package org.teleal.cling.binding.xml;

import static org.teleal.cling.model.XMLUtil.appendNewElement;
import static org.teleal.cling.model.XMLUtil.appendNewElementIfNotNull;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
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
import org.teleal.cling.transport.impl.XmlPullParserUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ModUDA10DeviceDescriptorBinderSAXImpl extends UDA10DeviceDescriptorBinderSAXImpl
{
  private static Logger log = Logger.getLogger("ModUDA10DevDescBinderSAXImpl");
  @Override
  public <D extends Device> D describe(D undescribedDevice, String descriptorXml) throws DescriptorBindingException, ValidationException {
  
    // copied from base since we don't wan't to catch this
    if (descriptorXml == null || descriptorXml.length() == 0) {
      throw new DescriptorBindingException("Null or empty descriptor");
    }
    
    try {
      return super.describe(undescribedDevice, descriptorXml);
  
    } catch (DescriptorBindingException ex) {
    
      log.warning("Broken descriptor xml, try fixing");
      String fixedXml = fixXml(descriptorXml);
      
      if (fixedXml.length() == descriptorXml.length()) {
        log.warning("Nothing was fixed:" + descriptorXml );
        throw ex;  
      }
      
      try {
        return super.describe(undescribedDevice, fixedXml); 
      
      } catch (DescriptorBindingException e) {
        log.warning("Failed to fix xml: " + fixedXml);
        throw e;
      }
    }
  }
  
  @Override
  protected void generateDevice(Namespace namespace, Device deviceModel, Document descriptor, Element rootElement, ControlPointInfo info) {

    Element deviceElement = appendNewElement(descriptor, rootElement, ELEMENT.device);

    appendNewElementIfNotNull(descriptor, deviceElement, ELEMENT.deviceType, deviceModel.getType());
    
    DeviceDetails deviceModelDetails = deviceModel.getDetails(info);
    appendNewElementIfNotNull(
            descriptor, deviceElement, ELEMENT.friendlyName,
            deviceModelDetails.getFriendlyName()
    );
    if (deviceModelDetails.getManufacturerDetails() != null) {
        appendNewElementIfNotNull(
                descriptor, deviceElement, ELEMENT.manufacturer,
                deviceModelDetails.getManufacturerDetails().getManufacturer()
        );
        appendNewElementIfNotNull(
                descriptor, deviceElement, ELEMENT.manufacturerURL,
                deviceModelDetails.getManufacturerDetails().getManufacturerURI()
        );
    }
    if (deviceModelDetails.getModelDetails() != null) {
        appendNewElementIfNotNull(
                descriptor, deviceElement, ELEMENT.modelDescription,
                deviceModelDetails.getModelDetails().getModelDescription()
        );
        appendNewElementIfNotNull(
                descriptor, deviceElement, ELEMENT.modelName,
                deviceModelDetails.getModelDetails().getModelName()
        );
        appendNewElementIfNotNull(
                descriptor, deviceElement, ELEMENT.modelNumber,
                deviceModelDetails.getModelDetails().getModelNumber()
        );
        appendNewElementIfNotNull(
                descriptor, deviceElement, ELEMENT.modelURL,
                deviceModelDetails.getModelDetails().getModelURI()
        );
    }
    appendNewElementIfNotNull(
            descriptor, deviceElement, ELEMENT.serialNumber,
            deviceModelDetails.getSerialNumber()
    );
    // changed location
    appendNewElementIfNotNull(descriptor, deviceElement, ELEMENT.UDN, deviceModel.getIdentity().getUdn());
    appendNewElementIfNotNull(
            descriptor, deviceElement, ELEMENT.presentationURL,
            deviceModelDetails.getPresentationURI()
    );
    appendNewElementIfNotNull(
            descriptor, deviceElement, ELEMENT.UPC,
            deviceModelDetails.getUpc()
    );

    if (deviceModelDetails.getDlnaDocs() != null) {
        for (DLNADoc dlnaDoc : deviceModelDetails.getDlnaDocs()) {
            appendNewElementIfNotNull(
                    descriptor, deviceElement, Descriptor.Device.DLNA_PREFIX + ":" + ELEMENT.X_DLNADOC,
                    dlnaDoc, Descriptor.Device.DLNA_NAMESPACE_URI
            );
        }
    }
    appendNewElementIfNotNull(
            descriptor, deviceElement, Descriptor.Device.DLNA_PREFIX + ":" + ELEMENT.X_DLNACAP,
            deviceModelDetails.getDlnaCaps(), Descriptor.Device.DLNA_NAMESPACE_URI
    );

    generateIconList(namespace, deviceModel, descriptor, deviceElement);
    generateServiceList(namespace, deviceModel, descriptor, deviceElement);
    generateDeviceList(namespace, deviceModel, descriptor, deviceElement, info);
  }

  @Override
  protected void generateServiceList(Namespace namespace, Device deviceModel, Document descriptor, Element deviceElement) {
    if (!deviceModel.hasServices()) return;

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
        appendNewElementIfNotNull(descriptor, serviceElement, ELEMENT.eventSubURL,
            namespace.getEventSubscriptionPath(ls));
      }
    }
  }
  
  /**
   * Ushare Media Server comes with a <dlna: prefix on X_DLNADOC that blows off so try fix for that.
   */
  private String fixXml(String xml) {
    String searchString = "<dlna:X_DLNADOC>(.*)</dlna:X_DLNADOC>";
    Pattern pattern = Pattern.compile(searchString, Pattern.DOTALL);
    Matcher matcher = pattern.matcher(xml);
    StringBuffer sb = new StringBuffer(xml.length());

    if (matcher.find() && matcher.groupCount() == 1) {

      String replacedString;
      String xmlEncodedLastChange = matcher.group(1);

      if (XmlPullParserUtils.isNullOrEmpty(xmlEncodedLastChange)) {
        replacedString = "<X_DLNADOC>DMS-1.0</X_DLNA_DOC>";
      } else {

        xmlEncodedLastChange = xmlEncodedLastChange.trim();
        replacedString = "<X_DLNADOC>" + StringUtils.replaceChars(xmlEncodedLastChange, "<>", null) + "</X_DLNADOC>";
      }

      matcher.appendReplacement(sb, Matcher.quoteReplacement(replacedString));
      matcher.appendTail(sb);
      return sb.toString();

    }
    log.warning("Nothing to fix in xml");
    return xml;
  }

}
