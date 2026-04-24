package com.example.flowlong.service;

import com.aizuda.bpm.engine.core.FlowLongContext;
import com.aizuda.bpm.engine.core.enums.TaskType;
import com.aizuda.bpm.engine.model.NodeAssignee;
import com.aizuda.bpm.engine.model.NodeModel;
import com.aizuda.bpm.engine.model.ProcessModel;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class BpmnFlowLongConverter {

    public String convertBpmnXmlToFlowLongJson(String bpmnXml, String fallbackName) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(bpmnXml)));

            Element processElement = firstElement(document, "process");
            if (processElement == null) {
                throw new RuntimeException("未找到 BPMN process 节点");
            }

            String processKey = processElement.getAttribute("id");
            String processName = processElement.getAttribute("name");
            if (processKey == null || processKey.isBlank()) {
                throw new RuntimeException("BPMN process id 不能为空");
            }
            if (processName == null || processName.isBlank()) {
                processName = fallbackName == null || fallbackName.isBlank() ? processKey : fallbackName;
            }

            Map<String, Element> nodeMap = collectProcessNodes(processElement);
            Map<String, String> outgoingMap = collectSingleOutgoingFlows(processElement);

            Element startEvent = firstChildElement(processElement, "startEvent");
            if (startEvent == null) {
                throw new RuntimeException("仅支持包含单个 startEvent 的 BPMN");
            }

            String currentId = outgoingMap.get(startEvent.getAttribute("id"));
            if (currentId == null) {
                throw new RuntimeException("startEvent 未连接到后续节点");
            }

            List<NodeModel> approvalNodes = new ArrayList<>();
            while (currentId != null) {
                Element current = nodeMap.get(currentId);
                if (current == null) {
                    throw new RuntimeException("流程节点缺失: " + currentId);
                }

                String localName = current.getLocalName();
                if ("userTask".equals(localName)) {
                    approvalNodes.add(toApprovalNode(current));
                    currentId = outgoingMap.get(currentId);
                    continue;
                }
                if ("endEvent".equals(localName)) {
                    break;
                }

                throw new RuntimeException("当前仅支持 startEvent -> userTask... -> endEvent 的顺序审批 BPMN，暂不支持节点类型: " + localName);
            }

            if (approvalNodes.isEmpty()) {
                throw new RuntimeException("当前仅支持至少包含一个 userTask 的顺序审批 BPMN");
            }

            NodeModel endNode = new NodeModel();
            endNode.setNodeName("流程结束");
            endNode.setNodeKey("End_Auto");
            endNode.setType(TaskType.end.getValue());

            for (int i = 0; i < approvalNodes.size(); i++) {
                NodeModel current = approvalNodes.get(i);
                current.setChildNode(i + 1 < approvalNodes.size() ? approvalNodes.get(i + 1) : endNode);
            }

            ProcessModel processModel = new ProcessModel();
            processModel.setKey(processKey);
            processModel.setName(processName);
            processModel.setNodeConfig(approvalNodes.get(0));
            processModel.setExtendConfig(new LinkedHashMap<>());

            return FlowLongContext.toJson(processModel);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("BPMN 转 FlowLong 模型失败: " + ex.getMessage(), ex);
        }
    }

    private NodeModel toApprovalNode(Element userTask) {
        NodeModel nodeModel = new NodeModel();
        nodeModel.setNodeKey(userTask.getAttribute("id"));
        nodeModel.setNodeName(attributeOrDefault(userTask, "name", "审批节点"));
        nodeModel.setType(TaskType.approval.getValue());
        nodeModel.setNodeAssigneeList(List.of(buildAssignee(userTask)));
        nodeModel.setExtendConfig(new LinkedHashMap<>());
        return nodeModel;
    }

    private NodeAssignee buildAssignee(Element userTask) {
        String assignee = firstNonBlank(
                userTask.getAttribute("flowable:assignee"),
                userTask.getAttribute("camunda:assignee"),
                userTask.getAttributeNS("http://flowable.org/bpmn", "assignee"),
                userTask.getAttributeNS("http://camunda.org/schema/1.0/bpmn", "assignee")
        );

        if (assignee == null || assignee.isBlank()) {
            assignee = userTask.getAttribute("id");
        }

        String normalized = assignee.trim();
        if (normalized.startsWith("${") && normalized.endsWith("}")) {
            normalized = normalized.substring(2, normalized.length() - 1);
        }

        NodeAssignee nodeAssignee = new NodeAssignee();
        nodeAssignee.setId(normalized);
        nodeAssignee.setName(normalized);
        nodeAssignee.setWeight(100);
        nodeAssignee.setExtendConfig(new LinkedHashMap<>());
        return nodeAssignee;
    }

    private Map<String, Element> collectProcessNodes(Element processElement) {
        Map<String, Element> nodeMap = new HashMap<>();
        String[] supported = {"startEvent", "userTask", "endEvent", "exclusiveGateway"};
        for (String localName : supported) {
            NodeList nodeList = processElement.getElementsByTagNameNS("*", localName);
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element element = (Element) nodeList.item(i);
                nodeMap.put(element.getAttribute("id"), element);
            }
        }
        return nodeMap;
    }

    private Map<String, String> collectSingleOutgoingFlows(Element processElement) {
        Map<String, List<String>> outgoing = new HashMap<>();
        NodeList sequenceFlows = processElement.getElementsByTagNameNS("*", "sequenceFlow");
        for (int i = 0; i < sequenceFlows.getLength(); i++) {
            Element sequenceFlow = (Element) sequenceFlows.item(i);
            String sourceRef = sequenceFlow.getAttribute("sourceRef");
            String targetRef = sequenceFlow.getAttribute("targetRef");
            outgoing.computeIfAbsent(sourceRef, ignored -> new ArrayList<>()).add(targetRef);
        }

        Map<String, String> flattened = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : outgoing.entrySet()) {
            if (entry.getValue().size() > 1) {
                throw new RuntimeException("当前仅支持单出口顺序流，节点 " + entry.getKey() + " 存在多条出口");
            }
            flattened.put(entry.getKey(), entry.getValue().get(0));
        }
        return flattened;
    }

    private Element firstElement(Document document, String localName) {
        NodeList nodeList = document.getElementsByTagNameNS("*", localName);
        return nodeList.getLength() > 0 ? (Element) nodeList.item(0) : null;
    }

    private Element firstChildElement(Element parent, String localName) {
        NodeList nodeList = parent.getElementsByTagNameNS("*", localName);
        return nodeList.getLength() > 0 ? (Element) nodeList.item(0) : null;
    }

    private String attributeOrDefault(Element element, String attributeName, String fallback) {
        String value = element.getAttribute(attributeName);
        return value == null || value.isBlank() ? fallback : value;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
