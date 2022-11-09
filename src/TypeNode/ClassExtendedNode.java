package TypeNode;

public class ClassExtendedNode extends ClassNode {

    public String className;
    public String extendsName;

    public ClassExtendedNode(String name, String extendsName) {
        super(name);
        this.className = name;
        this.extendsName = extendsName;
    }

    public String getExtendsName() { return this.extendsName; }


}
