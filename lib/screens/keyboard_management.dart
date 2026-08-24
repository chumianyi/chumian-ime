import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../theme/app_theme.dart';
import '../services/api_service.dart';

class KeyboardManagementScreen extends StatefulWidget {
  const KeyboardManagementScreen({super.key});

  @override
  State<KeyboardManagementScreen> createState() => _KeyboardManagementScreenState();
}

class _KeyboardManagementScreenState extends State<KeyboardManagementScreen> {
  // 默认启用的键盘
  final Map<String, bool> _keyboards = {
    '拼音输入': true,
    '英语输入': true,
    '手写输入': true,
    '摩斯电码输入': false,
    'Emoji趣味键盘': false,
    '随机键盘': false,
    '繁体字输入': false,
    '生僻字键盘': false,
  };

  final Map<String, String> _keyboardDesc = {
    '拼音输入': '智能拼音，支持联想和预测',
    '英语输入': '英文输入，支持自动纠错',
    '手写输入': '手写识别，支持简体和繁体',
    '摩斯电码输入': '用点和划输入文字',
    'Emoji趣味键盘': '海量表情符号',
    '随机键盘': '每次打开键位随机',
    '繁体字输入': '繁体中文输入',
    '生僻字键盘': '生僻字快速输入',
  };

  final Map<String, IconData> _keyboardIcons = {
    '拼音输入': Icons.text_fields,
    '英语输入': Icons.language,
    '手写输入': Icons.edit,
    '摩斯电码输入': Icons.bluetooth_audio,
    'Emoji趣味键盘': Icons.emoji_emotions,
    '随机键盘': Icons.shuffle,
    '繁体字输入': Icons.translate,
    '生僻字键盘': Icons.local_library,
  };

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('键盘管理', style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          Text('选择要使用的键盘，默认启用拼音、英语和手写', style: TextStyle(color: Colors.grey[600])),
          const SizedBox(height: 16),
          ..._keyboards.entries.map((entry) {
            return GlassContainer(
              margin: const EdgeInsets.only(bottom: 12),
              child: Row(
                children: [
                  Container(
                    width: 48,
                    height: 48,
                    decoration: BoxDecoration(
                      gradient: const LinearGradient(
                        colors: [AppTheme.primaryColor, AppTheme.secondaryColor],
                      ),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Icon(_keyboardIcons[entry.key], color: Colors.white),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(entry.key, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600)),
                        const SizedBox(height: 2),
                        Text(_keyboardDesc[entry.key] ?? '', style: TextStyle(color: Colors.grey[600], fontSize: 12)),
                      ],
                    ),
                  ),
                  Switch(
                    value: entry.value,
                    onChanged: (value) {
                      setState(() {
                        _keyboards[entry.key] = value;
                      });
                    },
                    activeColor: AppTheme.primaryColor,
                  ),
                ],
              ),
            );
          }).toList(),
          const SizedBox(height: 16),
          // 输入设置
          const Text('输入设置', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          const SizedBox(height: 12),
          GlassContainer(
            child: Column(
              children: [
                _SettingTile(icon: Icons.auto_awesome, title: '输入预测', subtitle: '智能预测下一个字'),
                _SettingTile(icon: Icons.correct, title: '自动纠错', subtitle: '自动修正输入错误'),
                _SettingTile(icon: Icons.volume_up, title: '按键音效', subtitle: '打字时播放音效'),
                _SettingTile(icon: Icons.vibration, title: '按键震动', subtitle: '打字时震动反馈'),
                _SettingTile(icon: Icons.contacts, title: '联系人词库', subtitle: '快速输入联系人姓名'),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _SettingTile extends StatefulWidget {
  final IconData icon;
  final String title;
  final String subtitle;
  const _SettingTile({required this.icon, required this.title, required this.subtitle});

  @override
  State<_SettingTile> createState() => _SettingTileState();
}

class _SettingTileState extends State<_SettingTile> {
  bool _enabled = true;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      leading: Icon(widget.icon, color: AppTheme.primaryColor),
      title: Text(widget.title),
      subtitle: Text(widget.subtitle, style: const TextStyle(fontSize: 12)),
      trailing: Switch(
        value: _enabled,
        onChanged: (v) => setState(() => _enabled = v),
        activeColor: AppTheme.primaryColor,
      ),
    );
  }
}
