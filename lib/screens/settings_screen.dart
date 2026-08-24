import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../theme/app_theme.dart';

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  bool _darkMode = false;
  bool _soundEnabled = true;
  bool _vibrationEnabled = true;
  bool _autoCorrect = true;
  bool _prediction = true;
  double _keyboardHeight = 0.5;

  @override
  Widget build(BuildContext context) {
    final themeProvider = Provider.of<ThemeProvider>(context);

    return Scaffold(
      appBar: AppBar(title: const Text('设置')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // 外观
          const _SectionTitle(title: '外观'),
          GlassContainer(
            child: Column(
              children: [
                SwitchListTile(
                  title: const Text('深色模式'),
                  subtitle: const Text('跟随系统或手动切换'),
                  value: _darkMode,
                  onChanged: (v) {
                    setState(() => _darkMode = v);
                    themeProvider.setThemeMode(v ? ThemeMode.dark : ThemeMode.light);
                  },
                ),
                const Divider(),
                ListTile(
                  title: const Text('键盘高度'),
                  subtitle: Slider(
                    value: _keyboardHeight,
                    onChanged: (v) => setState(() => _keyboardHeight = v),
                    activeColor: AppTheme.primaryColor,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),

          // 输入
          const _SectionTitle(title: '输入设置'),
          GlassContainer(
            child: Column(
              children: [
                SwitchListTile(
                  title: const Text('输入预测'),
                  subtitle: const Text('智能预测下一个字'),
                  value: _prediction,
                  onChanged: (v) => setState(() => _prediction = v),
                ),
                const Divider(),
                SwitchListTile(
                  title: const Text('自动纠错'),
                  subtitle: const Text('自动修正输入错误'),
                  value: _autoCorrect,
                  onChanged: (v) => setState(() => _autoCorrect = v),
                ),
                const Divider(),
                SwitchListTile(
                  title: const Text('按键音效'),
                  subtitle: const Text('打字时播放音效'),
                  value: _soundEnabled,
                  onChanged: (v) => setState(() => _soundEnabled = v),
                ),
                const Divider(),
                SwitchListTile(
                  title: const Text('按键震动'),
                  subtitle: const Text('打字时震动反馈'),
                  value: _vibrationEnabled,
                  onChanged: (v) => setState(() => _vibrationEnabled = v),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),

          // 语音
          const _SectionTitle(title: '语音输入'),
          GlassContainer(
            child: Column(
              children: const [
                ListTile(
                  leading: Icon(Icons.mic, color: AppTheme.primaryColor),
                  title: Text('在线语音识别'),
                  subtitle: Text('使用免费API进行语音转文字'),
                  trailing: Icon(Icons.chevron_right),
                ),
                Divider(),
                ListTile(
                  leading: Icon(Icons.mic_off, color: AppTheme.primaryColor),
                  title: Text('本地语音识别'),
                  subtitle: Text('离线语音识别模型'),
                  trailing: Icon(Icons.chevron_right),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),

          // 关于
          const _SectionTitle(title: '关于'),
          GlassContainer(
            child: Column(
              children: [
                const ListTile(
                  leading: Icon(Icons.info, color: AppTheme.primaryColor),
                  title: Text('版本'),
                  subtitle: Text('初眠输入法 v1.0.0'),
                ),
                const Divider(),
                ListTile(
                  leading: const Icon(Icons.update, color: AppTheme.primaryColor),
                  title: const Text('检查更新'),
                  onTap: () {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('已是最新版本')),
                    );
                  },
                ),
                const Divider(),
                const ListTile(
                  leading: Icon(Icons.privacy_tip, color: AppTheme.primaryColor),
                  title: Text('隐私政策'),
                  trailing: Icon(Icons.chevron_right),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _SectionTitle extends StatelessWidget {
  final String title;
  const _SectionTitle({required this.title});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(left: 4, bottom: 8),
      child: Text(title, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
    );
  }
}
