# TTTalk
Things related to TTTalk

有关文件:链接：https://pan.baidu.com/s/1KfR8zK3fHZXoeR7mue-buw?pwd=xxic 
提取码：xxic 

# 1.先用此rom脱壳: https://www.cnblogs.com/r0ysue/p/16791596.html 

链接: https://pan.baidu.com/s/1A0Hn1kUWVTfnBnHV0i3alg?pwd=y5fj 提取码: y5fj 复制这段内容后打开百度网盘手机App，操作更方便哦 

 

# 1.1.然后用定制版的jadx修复每个dex 

链接: https://pan.baidu.com/s/1rEdYxvDveTD6KGdkNpcNYg?pwd=9qyj 提取码: 9qyj 复制这段内容后打开百度网盘手机App，操作更方便哦 

 

Tt语音有老版本的5.5没有加壳的，但是它进入房间的协议是老的不能进入新版本的房间了 

 

# 2.过反调试的检测，自己编译过反调试的内核即可

# 3.关于过frida的检测：下面的脚本可以过附加的检测，但是不能过spawn的检测。 


 

# 4.下面的脚本可以过frida检测，frida用的是 https://github.com/hzzheyang/strongR-frida-android/releases/tag/16.0.1 

使用方法：先断网，然后启动TT语音，稍等一会用注入脚本 frida -U TT语音 -l hook6.10.js，然后联网，即可打印数据包 

hook6.10.js
 

# 5.调用libprotocol.so 

 需要patch detect_emulator,否则调用native_init会失败。 

 

# 6.关于DeviceID的问题, 发送文本消息的时候会带上一个DeivceID,其实是ishumei的一个设备ID，用于风控。 

 

# 7.关于aosp10安装magisk无法生效问题，请用magisk22.0版本 