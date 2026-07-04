package bf.anptic.geoportail.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mock-netxms")
public class MockNetXmsController {

    @GetMapping("/objects/{nodeId}")
    public Map<String, Object> getObject(@PathVariable int nodeId) {
        Map<String, Object> fakeNode = new HashMap<>();
        fakeNode.put("id", nodeId);

        if (nodeId == 9999) {
            fakeNode.put("status", "CRITICAL");
            fakeNode.put("lastStatusChange", "2026-06-29T22:14:00Z");
            fakeNode.put("inboundUtilizationMbps", null);
            fakeNode.put("outboundUtilizationMbps", null);
            fakeNode.put("pingLastValueMs", null);
            fakeNode.put("availability30d", 91.2);
            fakeNode.put("linkType", "Fibre optique");
            fakeNode.put("signalQuality", null);
        } else {
            fakeNode.put("status", "NORMAL");
            fakeNode.put("lastStatusChange", null);
            fakeNode.put("inboundUtilizationMbps", 130.0);
            fakeNode.put("outboundUtilizationMbps", 65.0);
            fakeNode.put("pingLastValueMs", 8.0);
            fakeNode.put("availability30d", 99.9);
            fakeNode.put("linkType", "Fibre optique");
            fakeNode.put("signalQuality", "Très bonne");
        }

        return fakeNode;
    }

    @GetMapping("/objects/{parentId}/children")
    public List<Map<String, Object>> getChildren(@PathVariable int parentId) {
        List<Map<String, Object>> children = new ArrayList<>();

        if (parentId == 1042) {
            children.add(childStatus(10421, "NORMAL"));
            children.add(childStatus(10422, "CRITICAL"));
            children.add(childStatus(10423, "NORMAL"));
        } else if (parentId == 9999) {
            children.add(childStatus(30011, "NORMAL"));
            children.add(childStatus(30012, "NORMAL"));
        } else {
            children.add(childStatus(10011, "NORMAL"));
            children.add(childStatus(10012, "NORMAL"));
        }

        return children;
    }

    private Map<String, Object> childStatus(int objectId, String severity) {
        Map<String, Object> child = new HashMap<>();
        child.put("id", objectId);
        child.put("status", severity);
        return child;
    }
}