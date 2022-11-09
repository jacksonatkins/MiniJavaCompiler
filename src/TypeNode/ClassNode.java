package TypeNode;
import java.util.*;

public class ClassNode extends Node {

    private Map<String, Node> fields;
    private Map<String, MethodNode> methods;
    public String name;

    public ClassNode(String name) {
        super(NodeType.CLASS);
        this.name = name;
        this.fields = new HashMap<>();
        this.methods = new HashMap<>();
    }

    public String getName() {
        return this.name;
    }

    public Map<String, Node> getFields() {
        return this.fields;
    }

    public Map<String, MethodNode> getMethods() {
        return this.methods;
    }

}
