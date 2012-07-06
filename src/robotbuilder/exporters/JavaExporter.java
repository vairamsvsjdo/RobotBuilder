/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package robotbuilder.exporters;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import robotbuilder.MainFrame;
import robotbuilder.RobotTree;
import robotbuilder.data.RobotComponent;
import robotbuilder.data.RobotWalker;

/**
 *
 * @author Alex Henning
 */
public class JavaExporter extends AbstractExporter {
    private static String DESCRIPTION_PATH = "export/java/ExportDescription.json";
    private static String[] DESCRIPTION_PROPERTIES = {"Export", "Import", "Declaration",
        "Construction", "Extra", "ClassName", "Subsystem Export"};
    private static String ROBOT_MAP_TEMPLATE = "export/java/RobotMap.java";
    private static String OI_TEMPLATE = "export/java/OI.java";
    private static String SUBSYSTEM_TEMPLATE = "export/java/Subsystem.java";

    @Override
    public void export(RobotTree robot) throws IOException {
        System.out.println("Loading export description for java");
        loadExportDescription(DESCRIPTION_PATH, DESCRIPTION_PROPERTIES);
        getPath(robot.getRoot()); // TODO: Ugly hack

        // The RobotMap
        System.out.println("Loading template "+ROBOT_MAP_TEMPLATE);
        System.out.println("Exporting RobotMap");
        String template = loadTemplate(ROBOT_MAP_TEMPLATE);
        template = substitute(template, "package", robot.getRoot().getProperty("Java Package"));
        template = substitute(template, "imports", generateImports(robot.getRoot(), "RobotMap"));
        template = substitute(template, "declarations", generateDeclarations(robot, "RobotMap"));
        template = substitute(template, "constructions", generateConstructions(robot, "RobotMap"));
        
        System.out.println("Writing RobotMap file");
        FileWriter out = new FileWriter(getPath(robot.getRoot())+"RobotMap.java");
        out.write(template);
        out.close();
        
        // The OI
        System.out.println("Exporting OI.");
        if (oiExists()) {
            System.out.println("Exists, modifying.");
            template = loadTemplate(getPath(robot.getRoot())+"/OI.java");
            template = updateAutogeneratedCode("declarations", template, generateDeclarations(robot, "OI"));
            template = updateAutogeneratedCode("constructors", template, generateConstructions(robot, "OI"));
        } else {
            System.out.println("Doesn't exist, creating.");
            template = loadTemplate(OI_TEMPLATE);
            template = substitute(template, "package", robot.getRoot().getProperty("Java Package"));
            template = substitute(template, "imports", generateImports(robot.getRoot(), "OI"));
            template = substitute(template, "OI Declarations", generateDeclarations(robot, "OI"));
            template = substitute(template, "OI Constructors", generateConstructions(robot, "OI"));
        }
        
        System.out.println("Writing RobotMap file");
        FileWriter oiOut = new FileWriter(getPath(robot.getRoot())+"OI.java");
        oiOut.write(template);
        oiOut.close();
        
        // The Subsystems
        System.out.println("Exporting Subsystems");
        for (RobotComponent subsystem : robot.getSubsystems()) {
            System.out.println("Exporting Subsystem: "+subsystem);
            if (subsystemExists(subsystem, robot.getRoot())) {
                System.out.println("Exists, modifying.");
                template = loadTemplate(getPath(robot.getRoot())+"/subsystems/"+getFullName(subsystem)+".java");
                template = updateAutogeneratedCode("subsystem", template, generateSubsystemExport(subsystem));
            } else {
                System.out.println("Doesn't exist, creating.");
                template = loadTemplate(SUBSYSTEM_TEMPLATE);
                template = substitute(template, "package", robot.getRoot().getProperty("Java Package"));
                template = substitute(template, "imports", generateImports(subsystem, "RobotMap"));
                template = substitute(template, "Subsystem Name", getFullName(subsystem));
                template = substitute(template, "Subsystem Export", generateSubsystemExport(subsystem));
            }
            System.out.println("Writing "+getShortName(subsystem)+".java file");
            FileWriter subsystemOut = new FileWriter(getPath(robot.getRoot())+"/subsystems/"+getFullName(subsystem)+".java");
            subsystemOut.write(template);
            subsystemOut.close();
        }
        
        System.out.println("Done");
    }
    
    /**
     * Generate the import statements for the exported robot map file.
     * @param robot
     * @return The String of import statements
     */
    private String generateImports(RobotComponent robot, final String export) {
        final Set<String> imports = new TreeSet<String>();
        robot.walk(new RobotWalker() {
            @Override
            public void handleRobotComponent(RobotComponent self) {
                final Map<String, String> instructions = componentInstructions.get(self.getBase().getName());
                if (export.equals(instructions.get("Export"))) {
                    String instruction = instructions.get("Import");
                    String className = instructions.get("ClassName");
                    imports.add(substitute(instruction, self, className));
                }
            }
        });
        
        String out = "";
        for (String imp : imports) {
            if (!"".equals(imp)) out += imp + "\n";
        }
        return out;
    }
    
    private String generateDeclarations(RobotTree robot, final String export) {
        final LinkedList<String> declarations = new LinkedList<String>();
        robot.walk(new RobotWalker() {
            @Override
            public void handleRobotComponent(RobotComponent self) {
                final Map<String, String> instructions = componentInstructions.get(self.getBase().getName());
                if (export.equals(instructions.get("Export"))) {
                    String instruction = instructions.get("Declaration");
                    String className = instructions.get("ClassName");
                    declarations.add(substitute(instruction, self, className));
                }
            }
        });
        
        String out = "";
        for (String dec : declarations) {
            if (!"".equals(dec)) out += "    " + dec + "\n";
        }
        return out;
    }
    
    private String generateConstructions(RobotTree robot, final String export) {
        final LinkedList<String> constructions = new LinkedList<String>();
        robot.walk(new RobotWalker() {
            @Override
            public void handleRobotComponent(RobotComponent self) {
                final Map<String, String> instructions = componentInstructions.get(self.getBase().getName());
                if (export.equals(instructions.get("Export"))) {
                    String instruction = instructions.get("Construction");
                    String extraInstruction = instructions.get("Extra");
                    String className = instructions.get("ClassName");
                    constructions.add(substitute(instruction, self, className));
                    constructions.add(substitute(extraInstruction, self, className));
                }
            }
        });

        String out = "";
        for (String cons : constructions) {
            if (!"".equals(cons)) out += "        " + cons + "\n";
        }
        return out;
    }
    
    private boolean oiExists() {
        return false;
    }
    
    private String generateOIConstructors(RobotComponent robot) {
        return "<<Constructors>>";
    }
    
    private boolean subsystemExists(RobotComponent subsystem, RobotComponent robot) throws IOException {
        return (new File(getPath(robot)+"/subsystems/"+getFullName(subsystem)+".java")).exists();
    }
    
    private String generateSubsystemExport(RobotComponent subsystem) {
        final LinkedList<String> components = new LinkedList<String>();
        subsystem.walk(new RobotWalker() {
            @Override
            public void handleRobotComponent(RobotComponent self) {
                String instruction = componentInstructions.get(self.getBase().getName()).get("Subsystem Export");
                String className = componentInstructions.get(self.getBase().getName()).get("ClassName");
                System.out.println(self.getBase().getName()+": "+className+" -- "+instruction);
                components.add(substitute(instruction, self, className));
            }
        });

        String out = "";
        for (String comp : components) {
            if (!"".equals(comp)) out += "    " + comp + "\n";
        }
        return out;
    }
    
    private String getPath(RobotComponent robot) throws IOException {
        if ((robot.getProperty("Java Package")).equals("")) {
            String packageName = (String) JOptionPane.showInputDialog(MainFrame.getInstance().getFrame(), "Java Package", "Java Package", JOptionPane.PLAIN_MESSAGE, null, null, null);
            robot.setProperty("Java Package", packageName);
        }
        if ((robot.getProperty("Java Project")).equals("")) {
            String file = null;
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = fileChooser.showDialog(MainFrame.getInstance().getFrame(), "Export");
            if (result == JFileChooser.CANCEL_OPTION) {
                throw new IOException("No file selected.");
            } else if (result == JFileChooser.ERROR_OPTION) {
                throw new IOException("Error selecting file.");
            } else if (result == JFileChooser.APPROVE_OPTION) {
                file = fileChooser.getSelectedFile().getAbsolutePath();
            }
            robot.setProperty("Java Project", file);
        }
        System.out.println("Path: "+robot.getProperty("Java Project")+"/src/"+robot.getProperty("Java Package").replace(".", "/")+"/");
        return robot.getProperty("Java Project")+"/src/"+robot.getProperty("Java Package").replace(".", "/")+"/";
    }

    @Override
    public String getFullName(RobotComponent comp) {
        if (comp.getBase().getType().equals("Subsystem")) {
            return comp.getFullName().replace(" ", "_");
        } else {
            return getFullName(comp.getFullName());
        }
    }
    
    @Override
    public String getFullName(String s) {
        return s.toUpperCase().replace(" ", "_");
    }

    @Override
    public String getShortName(RobotComponent comp) {
        return getShortName(comp.getName());
    }

    @Override
    public String getShortName(String s) {
        return s.substring(0, 1).toLowerCase()+s.replace(" ", "").substring(1);
    }
    
    private String updateAutogeneratedCode(String id, String source, String update) {
        String beginning = "// BEGIN AUTOGENERATED CODE, SOURCE=ROBOTBUILDER ID="+id.toUpperCase();
        String end = "// END AUTOGENERATED CODE, SOURCE=ROBOTBUILDER ID="+id.toUpperCase();
        return source.replaceFirst(beginning+"([\\s\\S]*?)"+end,
                       beginning+"\n"+update+"\n"+end);
    }
}
