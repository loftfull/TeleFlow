import subprocess
import tempfile
import unittest
from pathlib import Path


class SmartFoldersBatchCoordinatorTests(unittest.TestCase):
    def test_rolls_back_prior_operations_in_reverse_on_failure(self):
        root = Path(__file__).resolve().parents[1]
        folders = root / "overlay/java/org/telegram/teleflow/folders"
        source = folders / "SmartFoldersBatchCoordinator.java"
        self.assertTrue(source.is_file(), source)
        harness = Path(tempfile.mkdtemp()) / "BatchHarness.java"
        harness.write_text(r'''
import java.util.*;
import org.telegram.teleflow.folders.SmartFoldersBatchCoordinator;

public class BatchHarness {
    static class Op implements SmartFoldersBatchCoordinator.Operation {
        final String key; final boolean fail; final List<String> log;
        Op(String key, boolean fail, List<String> log) { this.key=key; this.fail=fail; this.log=log; }
        public String key() { return key; }
        public void apply(SmartFoldersBatchCoordinator.StepCallback cb) {
            log.add("apply:"+key);
            cb.onResult(!fail, fail ? "boom" : null);
        }
        public void rollback(SmartFoldersBatchCoordinator.StepCallback cb) {
            log.add("rollback:"+key);
            cb.onResult(true, null);
        }
    }
    public static void main(String[] args) {
        List<String> log = new ArrayList<>();
        List<SmartFoldersBatchCoordinator.Operation> ops = Arrays.asList(
            new Op("a", false, log), new Op("b", false, log), new Op("c", true, log)
        );
        final SmartFoldersBatchCoordinator.Result[] result = new SmartFoldersBatchCoordinator.Result[1];
        SmartFoldersBatchCoordinator.apply(ops, r -> result[0] = r);
        List<String> expected = Arrays.asList("apply:a","apply:b","apply:c","rollback:b","rollback:a");
        if (!expected.equals(log)) throw new AssertionError(log.toString());
        if (result[0] == null || result[0].isSuccess()) throw new AssertionError("must fail");
        if (!"apply-failed-rolled-back".equals(result[0].getCode())) throw new AssertionError(result[0].getCode());
        SmartFoldersBatchCoordinator.Result preflight = SmartFoldersBatchCoordinator.Result.fail("folder-limit", "review", "review");
        if (preflight.isSuccess() || !"folder-limit".equals(preflight.getCode())) throw new AssertionError("preflight factory");
    }
}
''', encoding='utf-8')
        classes = Path(tempfile.mkdtemp())
        subprocess.run(["javac", "-d", str(classes), str(source), str(harness)], check=True)
        subprocess.run(["java", "-cp", str(classes), "BatchHarness"], check=True)


if __name__ == '__main__':
    unittest.main()
