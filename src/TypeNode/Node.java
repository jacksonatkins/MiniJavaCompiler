package TypeNode;

public class Node {

    private NodeType type;
    public String idType;

    public Node(NodeType type) {
        this.type = type;
    }

    public NodeType getType() {
        return this.type;
    }

    public String idType() {
        return this.idType;
    }
}
