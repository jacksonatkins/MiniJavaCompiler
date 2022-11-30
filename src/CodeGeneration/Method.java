package CodeGeneration;

public class Method {
    public String identifier;
    public String implementorIdentifier;
    public int offset;

    public Method(String identifier, String implementorIdentifier) {
        this.identifier = identifier;
        this.implementorIdentifier = implementorIdentifier;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }


}
