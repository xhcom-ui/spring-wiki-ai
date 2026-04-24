package com.example.flowlong.service;

import com.example.flowlong.entity.ProcessDefinitionEntity;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProcessBusinessConfigService {

    private final ProcessDefinitionService processDefinitionService;

    public ProcessBusinessConfigService(ProcessDefinitionService processDefinitionService) {
        this.processDefinitionService = processDefinitionService;
    }

    public Map<String, Object> getActiveProcessBusinessConfig(String processKey) {
        ProcessDefinitionEntity definition = processDefinitionService.getActiveProcessDefinitionByKey(processKey);
        if (definition == null) {
            throw new RuntimeException("未找到激活流程定义: " + processKey);
        }
        return buildBusinessConfig(definition);
    }

    public Map<String, Object> findNodeConfig(String processKey, String nodeId) {
        if (nodeId == null || nodeId.isBlank()) {
            return null;
        }
        Map<String, Object> config = getActiveProcessBusinessConfig(processKey);
        Object tasksRaw = config.get("tasks");
        if (tasksRaw instanceof List<?> tasks) {
            for (Object item : tasks) {
                if (item instanceof Map<?, ?> task && nodeId.equals(task.get("nodeId"))) {
                    return castMap(task);
                }
            }
        }
        Object startNode = config.get("startNode");
        if (startNode instanceof Map<?, ?> start && nodeId.equals(start.get("nodeId"))) {
            return castMap(start);
        }
        return null;
    }

    public Map<String, Object> buildBusinessConfig(ProcessDefinitionEntity definition) {
        try {
            Document document = parseXml(definition.getBpmnXml());
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("processKey", definition.getProcessKey());
            payload.put("processName", definition.getProcessName());
            payload.put("designerType", definition.getDesignerType());
            payload.put("version", definition.getVersion());
            payload.put("startNode", parseFirstNode(document, "startEvent", "START"));
            payload.put("tasks", parseTaskNodes(document));
            return payload;
        } catch (Exception ex) {
            throw new RuntimeException("解析流程业务配置失败: " + ex.getMessage(), ex);
        }
    }

    private Map<String, Object> parseFirstNode(Document document, String elementName, String scope) {
        NodeList nodes = document.getElementsByTagNameNS("*", elementName);
        if (nodes.getLength() == 0) {
            return null;
        }
        return toNodeConfig((Element) nodes.item(0), scope);
    }

    private List<Map<String, Object>> parseTaskNodes(Document document) {
        NodeList nodes = document.getElementsByTagNameNS("*", "userTask");
        List<Map<String, Object>> tasks = new ArrayList<>();
        for (int index = 0; index < nodes.getLength(); index += 1) {
            tasks.add(toNodeConfig((Element) nodes.item(index), "TASK"));
        }
        return tasks;
    }

    private Map<String, Object> toNodeConfig(Element element, String scope) {
        String[] labels = parseDocumentation(readChildText(element, "documentation"));
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("scope", scope);
        node.put("nodeId", element.getAttribute("id"));
        node.put("nodeName", element.getAttribute("name"));
        node.put("formKey", readAttribute(element, "formKey"));
        node.put("pageLabel", labels[0]);
        node.put("formLabel", labels[1]);
        return node;
    }

    private String[] parseDocumentation(String documentation) {
        if (documentation == null || documentation.isBlank()) {
            return new String[] {"", ""};
        }
        String[] pieces = documentation.split("/");
        if (pieces.length < 2) {
            return new String[] {documentation.trim(), ""};
        }
        return new String[] {pieces[0].trim(), pieces[1].trim()};
    }

    private String readAttribute(Element element, String attributeName) {
        NamedNodeMap attributes = element.getAttributes();
        for (int index = 0; index < attributes.getLength(); index += 1) {
            Node item = attributes.item(index);
            String nodeName = item.getNodeName();
            if (attributeName.equals(item.getLocalName())
                    || attributeName.equals(nodeName)
                    || nodeName.endsWith(":" + attributeName)) {
                return item.getNodeValue();
            }
        }
        return "";
    }

    private String readChildText(Element element, String localName) {
        NodeList children = element.getChildNodes();
        for (int index = 0; index < children.getLength(); index += 1) {
            Node child = children.item(index);
            if (child instanceof Element childElement) {
                String nodeName = childElement.getNodeName();
                if (localName.equals(childElement.getLocalName())
                        || localName.equals(nodeName)
                        || nodeName.endsWith(":" + localName)) {
                    return childElement.getTextContent();
                }
            }
        }
        return "";
    }

    private Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Map<?, ?> source) {
        return (Map<String, Object>) source;
    }
}
