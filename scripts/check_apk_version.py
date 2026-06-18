import apkutils
import sys
import xml.etree.ElementTree as ET

apk_path = sys.argv[1] if len(sys.argv) > 1 else 'app-debug.apk'
apk = apkutils.APK.from_file(apk_path)
manifest_xml = apk.get_manifest()
root = ET.fromstring(manifest_xml)
# Android namespace
ns = {'android': 'http://schemas.android.com/apk/res/android'}
vc = root.attrib.get('{http://schemas.android.com/apk/res/android}versionCode')
vn = root.attrib.get('{http://schemas.android.com/apk/res/android}versionName')
print(f"versionCode: {vc}")
print(f"versionName: {vn}")
