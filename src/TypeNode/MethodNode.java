package TypeNode;

import java.util.*;

public class MethodNode extends Node {

    private Node returnType;
    public Map<String, Node> parameters;
    public Map<String, Node> localVars;

    public List<Node> parameterTypes;


    public MethodNode(Node returnType) {
        super(NodeType.METHOD);
        this.returnType = returnType;
        this.parameters = new HashMap<>();
        this.localVars = new HashMap<>();
        this.parameterTypes = new ArrayList<>();
    }

    public Node getReturnType() {
        return this.returnType;
    }

    public Map<String, Node> getParameters() {
        return this.parameters;
    }

    public Map<String, Node> getLocalVars() {
        return this.localVars;
    }

    public List<Node> getParameterTypes() { return this.parameterTypes; }
}
