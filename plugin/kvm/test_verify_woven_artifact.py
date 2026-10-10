import io
import unittest
import zipfile
from verify_woven_artifact import ANNOTATION, CLEANUP, CRITICAL, verify


def archive(classes):
    data = io.BytesIO()
    with zipfile.ZipFile(data, 'w') as bundle:
        for name, content in classes.items():
            bundle.writestr(name, content)
    return data.getvalue()


class WovenArtifactTest(unittest.TestCase):
    def test_rejects_missing_critical_class(self):
        with self.assertRaisesRegex(ValueError, 'production lock class missing'):
            verify(archive({}))

    def test_rejects_unwoven_production_cleanup(self):
        with self.assertRaisesRegex(ValueError, 'Missing compiled deferred'):
            verify(archive({CRITICAL: ANNOTATION + b'Defer.defer'}))

    def test_checks_other_deferred_classes_too(self):
        with self.assertRaisesRegex(ValueError, 'Other.class'):
            verify(archive({CRITICAL: ANNOTATION + CLEANUP,
                            'Other.class': ANNOTATION}))

    def test_accepts_cleanup_and_ignores_unrelated_classes(self):
        result = verify(archive({CRITICAL: ANNOTATION + CLEANUP,
                                 'Plain.class': b'ordinary class'}))
        self.assertEqual(result['deferred_classes_checked'], 1)


if __name__ == '__main__':
    unittest.main()
