import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  bool _soundEnabled = true;
  bool _vibrationEnabled = true;
  bool _autoCorrect = true;
  bool _prediction = true;
  double _keyboardHeight = 0.5;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('设置')),
      body: ListView(
        children: [
          GroupCard(
            title: '外观',
            children: [
              SimpleTile(
                icon: Icons.dark_mode,
                title: '深色模式',
                subtitle: '跟随系统',
                iconBg: Colors.grey.withOpacity(0.12),
                iconColor: Colors.grey,
                trailing: Switch(value: false, onChanged: (_) {}),
                showChevron: false,
              ),
              const Divider(height: 1, indent: 58),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                child: Row(
                  children: [
                    Container(
                      width: 30, height: 30,
                      decoration: BoxDecoration(color: AppTheme.accent.withOpacity(0.12), borderRadius: BorderRadius.circular(8)),
                      child: const Icon(Icons.height, size: 16, color: AppTheme.accent),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text('键盘高度', style: TextStyle(fontSize: 15)),
                          Slider(
                            value: _keyboardHeight,
                            onChanged: (v) => setState(() => _keyboardHeight = v),
                            activeColor: AppTheme.accent,
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          GroupCard(
            title: '输入设置',
            children: [
              SimpleTile(
                icon: Icons.auto_awesome,
                title: '输入预测',
                iconBg: AppTheme.accent.withOpacity(0.12),
                iconColor: AppTheme.accent,
                trailing: Switch(value: _prediction, onChanged: (v) => setState(() => _prediction = v)),
                showChevron: false,
              ),
              const Divider(height: 1, indent: 58),
              SimpleTile(
                icon: Icons.check_circle_outline,
                title: '自动纠错',
                iconBg: Colors.green.withOpacity(0.12),
                iconColor: Colors.green,
                trailing: Switch(value: _autoCorrect, onChanged: (v) => setState(() => _autoCorrect = v)),
                showChevron: false,
              ),
              const Divider(height: 1, indent: 58),
              SimpleTile(
                icon: Icons.volume_up,
                title: '按键音效',
                iconBg: Colors.orange.withOpacity(0.12),
                iconColor: Colors.orange,
                trailing: Switch(value: _soundEnabled, onChanged: (v) => setState(() => _soundEnabled = v)),
                showChevron: false,
              ),
              const Divider(height: 1, indent: 58),
              SimpleTile(
                icon: Icons.vibration,
                title: '按键震动',
                iconBg: Colors.blue.withOpacity(0.12),
                iconColor: Colors.blue,
                trailing: Switch(value: _vibrationEnabled, onChanged: (v) => setState(() => _vibrationEnabled = v)),
                showChevron: false,
              ),
            ],
          ),
          GroupCard(
            title: '语音输入',
            children: [
              SimpleTile(icon: Icons.mic, title: '在线语音识别', subtitle: '免费API语音转文字', iconBg: const Color(0xFFFF3B30).withOpacity(0.12), iconColor: const Color(0xFFFF3B30)),
              const Divider(height: 1, indent: 58),
              SimpleTile(icon: Icons.mic_off, title: '本地语音识别', subtitle: '离线识别模型', iconBg: Colors.grey.withOpacity(0.12), iconColor: Colors.grey),
            ],
          ),
          GroupCard(
            title: '关于',
            children: [
              SimpleTile(icon: Icons.info, title: '版本', subtitle: '初眠输入法 v1.2.0', iconBg: Colors.grey.withOpacity(0.12), iconColor: Colors.grey, showChevron: false),
              const Divider(height: 1, indent: 58),
              SimpleTile(
                icon: Icons.update,
                title: '检查更新',
                iconBg: AppTheme.accent.withOpacity(0.12),
                iconColor: AppTheme.accent,
                onTap: () => ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('已是最新版本'))),
              ),
              const Divider(height: 1, indent: 58),
              SimpleTile(icon: Icons.privacy_tip, title: '隐私政策', iconBg: Colors.green.withOpacity(0.12), iconColor: Colors.green),
            ],
          ),
          const SizedBox(height: 20),
        ],
      ),
    );
  }
}
