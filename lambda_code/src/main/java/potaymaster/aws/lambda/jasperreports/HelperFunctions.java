package potaymaster.aws.lambda.jasperreports;

import java.util.Map;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.amazonaws.services.lambda.runtime.LambdaLogger;

public class HelperFunctions {
	
	private LambdaLogger logger;
	
	public HelperFunctions(LambdaLogger logger) {
    	this.logger = logger;
    }
	
	/*
     * Decode xml
     */
    public boolean extractXml(Node node, String name, Map<String, Object> parameters) {

        if (node == null) return false;

        String nodeName = name != null ? name + "-" + node.getNodeName() : node.getNodeName();

        NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            switch (nodeList.item(i).getNodeType()) {
                case Node.ELEMENT_NODE:
                    if (nodeList.item(i).hasChildNodes()) {
                        extractXml(nodeList.item(i), nodeName, parameters);
                    }
                    break;
                case Node.TEXT_NODE:
                    String value = nodeList.item(i).getNodeValue().trim();
                    if (!value.isEmpty()) {
                        parameters.put(nodeName, value);
                        logger.log("extractXml : Add to map: " + nodeName + " | " + value);
                    }
                    break;
                default:
                    break;
            }
        }
        return true;
    }
}
