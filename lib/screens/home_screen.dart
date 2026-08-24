import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../theme/app_theme.dart';
import '../services/api_service.dart';
import 'keyboard_management.dart';
import 'skin_store.dart';
import 'account_screen.dart';
import 'settings_screen.dart';
import 'dictionary_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _currentIndex = 0;

  final List<Widget> _screens = [
    const _HomeTab(),
    const KeyboardManagementScreen(),
    const SkinStoreScreen(),
    const AccountScreen(),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: _screens[_currentIndex],
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _currentIndex,
        onTap: (i) => setState(() => _currentIndex = i),
        items: const [
          BottomNavigationBarItem(icon: Icon(Icons.grid_view_rounded), label: '首页'),
          BottomNavigationBarItem(icon: Icon(Icons.keyboard), label: '键盘'),
          BottomNavigationBarItem(icon: Icon(Icons.palette_outlined), label: '皮肤'),
          BottomNavigationBarItem(icon: Icon(Icons.person_outline), label: '我的'),
        ],
      ),
    );
  }
}

class _HomeTab extends StatefulWidget {
  const _HomeTab();

  @override
  State<_HomeTab> createState() => _HomeTabState();
}

class _HomeTabState extends State<_HomeTab> {
  Map<String, dynamic> _stats = {'total_chars': 0, 'total_inputs': 0, 'accuracy': 100};

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _loadStats());
  }

  Future<void> _loadStats() async {
    final api = context.read<ApiService>();
    if (api.isLoggedIn) {
      await api.fetchUserStats();
      if (mounted) setState(() => _stats = api.userStats);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('初眠输入法')),
      body: ListView(
        children: [
          GroupCard(
            title: '输入统计',
            children: [
              Padding(
                padding: const EdgeInsets.all(16),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceAround,
                  children: [
                    _StatItem(value: '${_stats['total_chars'] ?? 0}', label: '总字数'),
                    _StatItem(value: '${_stats['total_inputs'] ?? 0}', label: '总次数'),
                    _StatItem(value: '${_stats['accuracy'] ?? 100}%', label: '准确率'),
                  ],
                ),
              ),
            ],
          ),
          GroupCard(
            title: '常用功能',
            children: [
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 16),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceAround,
                  children: [
                    FeatureIcon(icon: Icons.keyboard, label: '键盘管理', color: const Color(0xFF007AFF), onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const KeyboardManagementScreen()))),
                    FeatureIcon(icon: Icons.palette_outlined, label: '皮肤商店', color: const Color(0xFFFF9500), onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const SkinStoreScreen()))),
                    FeatureIcon(icon: Icons.menu_book, label: '词库下载', color: const Color(0xFF34C759), onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const DictionaryScreen()))),
                    FeatureIcon(icon: Icons.settings, label: '设置', color: const Color(0xFF8E8E93), onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const SettingsScreen()))),
                  ],
                ),
              ),
            ],
          ),
          GroupCard(
            title: '输入方式',
            children: [
              SimpleTile(icon: Icons.mic, title: '语音输入', subtitle: '在线/本地语音识别', iconBg: const Color(0xFFFF3B30).withOpacity(0.12), iconColor: const Color(0xFFFF3B30)),
              const Divider(height: 1, indent: 58),
              SimpleTile(icon: Icons.gesture, title: '手写输入', subtitle: '半屏/全屏手写', iconBg: const Color(0xFF007AFF).withOpacity(0.12), iconColor: const Color(0xFF007AFF)),
              const Divider(height: 1, indent: 58),
              SimpleTile(icon: Icons.emoji_emotions_outlined, title: '表情输入', subtitle: 'Emoji/颜文字/表情包', iconBg: const Color(0xFFFF9500).withOpacity(0.12), iconColor: const Color(0xFFFF9500)),
              const Divider(height: 1, indent: 58),
              SimpleTile(icon: Icons.code, title: '摩斯电码', subtitle: '点划输入解码', iconBg: const Color(0xFF5856D6).withOpacity(0.12), iconColor: const Color(0xFF5856D6)),
            ],
          ),
          GroupCard(
            title: '工具',
            children: [
              SimpleTile(icon: Icons.contacts, title: '联系人快输', subtitle: '授权后快速输入联系人', iconBg: const Color(0xFF34C759).withOpacity(0.12), iconColor: const Color(0xFF34C759)),
              const Divider(height: 1, indent: 58),
              SimpleTile(icon: Icons.content_paste, title: '剪贴板', subtitle: '历史剪贴记录', iconBg: const Color(0xFFFF9500).withOpacity(0.12), iconColor: const Color(0xFFFF9500)),
              const Divider(height: 1, indent: 58),
              SimpleTile(icon: Icons.translate, title: '常用语', subtitle: '快捷短语管理', iconBg: const Color(0xFF007AFF).withOpacity(0.12), iconColor: const Color(0xFF007AFF)),
              const Divider(height: 1, indent: 58),
              SimpleTile(icon: Icons.text_fields, title: '文字编辑', subtitle: '光标控制/选择/复制', iconBg: const Color(0xFF8E8E93).withOpacity(0.12), iconColor: const Color(0xFF8E8E93)),
            ],
          ),
          const SizedBox(height: 20),
        ],
      ),
    );
  }
}

class _StatItem extends StatelessWidget {
  final String value;
  final String label;
  const _StatItem({required this.value, required this.label});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Text(value, style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w600, color: AppTheme.primary)),
        const SizedBox(height: 4),
        Text(label, style: const TextStyle(fontSize: 12, color: AppTheme.secondary)),
      ],
    );
  }
}
