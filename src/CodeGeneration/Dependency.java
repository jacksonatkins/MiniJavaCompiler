package CodeGeneration;

import java.util.ArrayList;
import java.util.List;

public class Dependency {
    public String identifier;
    public String dependencies;
    public List<String> fields;
    public List<String> methods;

    public Dependency(String identifier) {
        this.identifier = identifier;
        this.dependencies = null;
        this.fields = new ArrayList<>();
        this.methods = new ArrayList<>();
    }
}
