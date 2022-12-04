import os
import json
import xml.etree.ElementTree as ET
# Press the green button in the gutter to run the script.
if __name__ == '__main__':
    try:

        # pull files from an Android device in which TT Talk is installed
        # notice: TT Talk must be logged in that device
        os.system('adb shell su 0 "cp /data/data/com.yiyou.ga/shared_prefs/auth.xml /sdcard/auth.xml"')
        os.system('adb shell su 0 "cp /data/data/com.yiyou.ga/shared_prefs/pref_outside_share_info.xml /sdcard/pref_outside_share_info.xml"')
        os.system('adb shell su 0 "cp /data/data/com.yiyou.ga/shared_prefs/BUGLY_COMMON_VALUES.xml /sdcard/BUGLY_COMMON_VALUES.xml"')
        os.system(
            'adb shell su 0 "cp /data/data/com.yiyou.ga/shared_prefs/73c8e35220bc684412f1bd86b04785ca.xml /sdcard/73c8e35220bc684412f1bd86b04785ca.xml"')

        # if preference_new_deivce_id.xml is not showing up under 'shared_prefs' directory
        # you must execute 'pm clear com.yiyou.ga' and login you account again.
        os.system(
            'adb shell su 0 "cp /data/data/com.yiyou.ga/shared_prefs/preference_new_deivce_id.xml /sdcard/preference_new_deivce_id.xml"')
        os.system('adb pull /sdcard/auth.xml .')
        os.system('adb pull /sdcard/pref_outside_share_info.xml .')
        os.system('adb pull /sdcard/BUGLY_COMMON_VALUES.xml .')
        os.system('adb pull /sdcard/73c8e35220bc684412f1bd86b04785ca.xml .')
        os.system('adb pull /sdcard/preference_new_deivce_id.xml .')

        # read the information that is used in 'client' project
        acc = None
        uid = None
        acc_type = None
        pwd = None
        deviceID = None
        androidid = None
        key_web_ua = None
        deviceIdV2 = None

        tree = ET.parse('auth.xml')
        root = tree.getroot()
        for child in root:
            attr = child.attrib
            if 'acc' == attr['name']:
                acc = child.text
            if 'uid' == attr['name']:
                uid = attr['value']
            if 'acc_type' == attr['name']:
                acc_type = attr['value']
            if 'pwd' == attr['name']:
                pwd = child.text

        tree = ET.parse('73c8e35220bc684412f1bd86b04785ca.xml')
        root = tree.getroot()
        for child in root:
            attr = child.attrib
            if '73c8e35220bc684412f1bd86b04785ca' == attr['name']:
                # add a prefix character 'B'
                # I don't know why, but it's written in TT Talk app
                # this field is given from the response of
                # http://fp-it.fengkongcloud.com/deviceprofile/v4
                # by ishumei
                deviceID = 'B' + child.text

        tree = ET.parse('BUGLY_COMMON_VALUES.xml')
        root = tree.getroot()
        for child in root:
            attr = child.attrib
            if 'androidid' == attr['name']:
                androidid = child.text

        tree = ET.parse('pref_outside_share_info.xml')
        root = tree.getroot()
        for child in root:
            attr = child.attrib
            if 'key_web_ua' == attr['name']:
                key_web_ua = child.text

        tree = ET.parse('preference_new_deivce_id.xml')
        root = tree.getroot()
        for child in root:
            attr = child.attrib
            if 'preference_key_deivce_id_v2' == attr['name']:
                deviceIdV2 = child.text

        # writes everything into a json formatted file
        if acc is None or uid is None or acc_type is None or pwd is None or deviceID is None or androidid is None or key_web_ua is None:
            print(u'信息读取失败.')
        else:
            jsonObj = {}
            jsonObj['acc'] = acc
            jsonObj['uid'] = uid
            jsonObj['acc_type'] = acc_type
            jsonObj['pwd'] = pwd
            jsonObj['deviceID'] = deviceID
            jsonObj['deviceIdV2'] = deviceIdV2
            jsonObj['androidid'] = androidid
            jsonObj['key_web_ua'] = key_web_ua

            jsonstr = json.dumps(jsonObj)
            with open(acc + '.json', 'w', encoding='utf-8') as f:
                f.write(jsonstr)
                f.close()
    except Exception as e:
        print(u'失败:' + str(e))

