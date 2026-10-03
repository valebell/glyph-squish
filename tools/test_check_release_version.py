import unittest

from check_release_version import check_version


class ReleaseVersionTest(unittest.TestCase):
    def setUp(self):
        self.metadata = {
            "applicationId": "com.valebell.glyphsquish",
            "elements": [{"versionName": "1.0.2", "versionCode": 3, "filters": []}],
        }

    def test_patch_release_matches_minor_branch(self):
        self.assertEqual("1.0.2", check_version("release/1.0", self.metadata))

    def test_other_branch_or_version_family_is_rejected(self):
        for branch in ("dev", "release/1.1", "release/01.0", "release/1.0;echo bad"):
            with self.subTest(branch=branch), self.assertRaises(ValueError):
                check_version(branch, self.metadata)

    def test_wrong_package_is_rejected(self):
        self.metadata["applicationId"] = "another.app"
        with self.assertRaises(ValueError):
            check_version("release/1.0", self.metadata)

    def test_split_apk_is_rejected(self):
        self.metadata["elements"][0]["filters"] = [{"filterType": "ABI", "value": "arm64-v8a"}]
        with self.assertRaises(ValueError):
            check_version("release/1.0", self.metadata)

    def test_invalid_version_or_code_is_rejected(self):
        for version in ("1.0", "1.0.01", "1.0.0\nother=bad", "1.0.0-dev"):
            with self.subTest(version=version), self.assertRaises(ValueError):
                self.metadata["elements"][0]["versionName"] = version
                check_version("release/1.0", self.metadata)
        self.metadata["elements"][0]["versionName"] = "1.0.2"
        for code in (0, -1, 2_100_000_001, True, "3"):
            with self.subTest(code=code), self.assertRaises(ValueError):
                self.metadata["elements"][0]["versionCode"] = code
                check_version("release/1.0", self.metadata)


if __name__ == "__main__":
    unittest.main()
