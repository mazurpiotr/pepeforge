package pepin.pepeforge;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class RefactorPackages {
    public static void main(String[] args) throws IOException {
        String baseStr = "/home/piotr/Dokumenty/PepeForge-Workspace/pepeforge/src/main/java/pepin/pepeforge";
        Path base = Paths.get(baseStr);
        
        Map<String, String> fileToNewPackage = new HashMap<>();
        fileToNewPackage.put("ActionBarHelper.java", "util/ui");
        fileToNewPackage.put("AuraManager.java", "util/aura");
        fileToNewPackage.put("CombatUtils.java", "util/combat");
        fileToNewPackage.put("ProtectionUtil.java", "util/protection");
        fileToNewPackage.put("CooldownManager.java", "util/cooldown");
        fileToNewPackage.put("SchedulerCompat.java", "util/scheduler");
        fileToNewPackage.put("ScheduledTaskCompat.java", "util/scheduler");
        fileToNewPackage.put("ServerEnv.java", "util/env");
        fileToNewPackage.put("ItemMetaManager.java", "util/itemmeta");
        fileToNewPackage.put("ItemMetaCompat.java", "util/itemmeta");
        fileToNewPackage.put("PaperItemMetaAdapter.java", "util/itemmeta");
        fileToNewPackage.put("PaperDataComponentAdapter.java", "util/itemmeta");

        Map<String, String> importReplacements = new HashMap<>();
        for (Map.Entry<String, String> entry : fileToNewPackage.entrySet()) {
            String className = entry.getKey().replace(".java", "");
            String newSubPackage = entry.getValue().replace("/", ".");
            String oldImport = "import pepin.pepeforge.util." + className + ";";
            String newImport = "import pepin.pepeforge." + newSubPackage + "." + className + ";";
            importReplacements.put(oldImport, newImport);
        }

        // 1. Move files and update their package declarations
        for (Map.Entry<String, String> entry : fileToNewPackage.entrySet()) {
            Path source = base.resolve("util").resolve(entry.getKey());
            Path targetDir = base.resolve(entry.getValue());
            Path target = targetDir.resolve(entry.getKey());

            if (Files.exists(source)) {
                Files.createDirectories(targetDir);
                Files.move(source, target);
                
                String content = new String(Files.readAllBytes(target));
                String newPackage = "package pepin.pepeforge." + entry.getValue().replace("/", ".") + ";";
                content = content.replaceFirst("package pepin\\.pepeforge\\.util;", newPackage);
                
                // Update internal references that might use fully qualified names (rare but possible)
                for (Map.Entry<String, String> impEntry : importReplacements.entrySet()) {
                    String oldClass = impEntry.getKey().replace("import ", "").replace(";", "");
                    String newClass = impEntry.getValue().replace("import ", "").replace(";", "");
                    content = content.replace(oldClass, newClass);
                }

                Files.write(target, content.getBytes());
                System.out.println("Moved and updated package for " + entry.getKey());
            }
        }

        // 2. Search all .java files and replace imports
        Files.walk(Paths.get("/home/piotr/Dokumenty/PepeForge-Workspace/pepeforge/src"))
             .filter(Files::isRegularFile)
             .filter(p -> p.toString().endsWith(".java"))
             .forEach(p -> {
                 try {
                     String content = new String(Files.readAllBytes(p));
                     boolean changed = false;
                     for (Map.Entry<String, String> impEntry : importReplacements.entrySet()) {
                         if (content.contains(impEntry.getKey())) {
                             content = content.replace(impEntry.getKey(), impEntry.getValue());
                             changed = true;
                         }
                         
                         // also replace fully qualified names in code
                         String oldClass = impEntry.getKey().replace("import ", "").replace(";", "");
                         String newClass = impEntry.getValue().replace("import ", "").replace(";", "");
                         if (content.contains(oldClass)) {
                             content = content.replace(oldClass, newClass);
                             changed = true;
                         }
                     }
                     if (changed) {
                         Files.write(p, content.getBytes());
                         System.out.println("Updated imports in " + p.getFileName());
                     }
                 } catch (IOException e) {
                     e.printStackTrace();
                 }
             });
    }
}
