import subprocess
import tempfile
import unittest
from pathlib import Path


class FolderProvisioningPlannerTests(unittest.TestCase):
    def test_plans_only_non_empty_unmanaged_categories_with_limit(self):
        root = Path(__file__).resolve().parents[1]
        folders = root / "overlay/java/org/telegram/teleflow/folders"
        expected = [
            folders / "ManagedFolderSpec.java",
            folders / "ManagedFolderCatalog.java",
            folders / "FolderProvisioningPlan.java",
            folders / "FolderProvisioningPlanner.java",
        ]
        for path in expected:
            self.assertTrue(path.is_file(), path)

        harness = Path(tempfile.mkdtemp()) / "FolderProvisioningHarness.java"
        harness.write_text(r'''
import java.util.*;
import org.telegram.teleflow.folders.*;

public class FolderProvisioningHarness {
    public static void main(String[] args) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("AI & Tech", 8);
        counts.put("Финансы", 5);
        counts.put("Авто", 0);
        counts.put("Новости", 4);
        counts.put("Разобрать", 2);

        Set<String> managed = new LinkedHashSet<>();
        managed.add("finance");

        FolderProvisioningPlan plan = FolderProvisioningPlanner.plan(counts, managed, 8, 10);
        if (plan.getAvailableSlotsBefore() != 2) throw new AssertionError("slots");
        if (plan.getCreate().size() != 2) throw new AssertionError("create size " + plan.getCreate().size());
        if (!"ai-tech".equals(plan.getCreate().get(0).getLogicalKey())) throw new AssertionError("priority 1");
        if (!"news".equals(plan.getCreate().get(1).getLogicalKey())) throw new AssertionError("priority 2");
        if (plan.getBlockedByLimit().size() != 1 || !"review".equals(plan.getBlockedByLimit().get(0).getLogicalKey())) {
            throw new AssertionError("blocked");
        }
        for (ManagedFolderSpec spec : plan.getCreate()) {
            if (!spec.getServerName().startsWith("TF ")) throw new AssertionError("server prefix");
            if (spec.getServerName().codePointCount(0, spec.getServerName().length()) > 12) {
                throw new AssertionError("name too long: " + spec.getServerName());
            }
        }

        FolderProvisioningPlan none = FolderProvisioningPlanner.plan(counts, managed, 10, 10);
        if (!none.getCreate().isEmpty()) throw new AssertionError("must not exceed limit");
    }
}
''', encoding="utf-8")
        classes = Path(tempfile.mkdtemp())
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", str(classes), *map(str, expected), str(harness)], check=True)
        subprocess.run(["java", "-cp", str(classes), "FolderProvisioningHarness"], check=True)


if __name__ == "__main__":
    unittest.main()
