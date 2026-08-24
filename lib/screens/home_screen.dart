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
    return AuroraBackground(
      child: Scaffold(
        backgroundColor: Colors.transparent,
        appBar: AppBar(
          title: const Text(
            '初眠输入法',
            style: TextStyle(fontWeight: FontWeight.bold, fontSize: 22),
          ),
          actions: [
            IconButton(
              icon: const Icon(Icons.settings),
              onPressed: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(builder: (_) => const SettingsScreen()),
                );
              },
            ),
          ],
        ),
        body: _screens[_currentIndex],
        bottomNavigationBar: GlassContainer(
          margin: const EdgeInsets.all(8),
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
          borderRadius: 24,
          child: BottomNavigationBar(
            currentIndex: _currentIndex,
            onTap: (index) => setState(() => _currentIndex = index),
            type: BottomNavigationBarType.fixed,
            backgroundColor: Colors.transparent,
            elevation: 0,
            selectedItemColor: AppTheme.primaryColor,
            unselectedItemColor: Colors.grey,
            items: const [
              BottomNavigationBarItem(icon: Icon(Icons.home), label: '首页'),
              BottomNavigationBarItem(icon: Icon(Icons.keyboard), label: '键盘'),
              BottomNavigationBarItem(icon: Icon(Icons.palette), label: '皮肤'),
              BottomNavigationBarItem(icon: Icon(Icons.person), label: '我的'),
            ],
          ),
        ),
      ),
    );
  }
}

class _HomeTab extends StatelessWidget {
  const _HomeTab();

  @override
  Widget build(BuildContext context) {
    final api = Provider.of<ApiService>(context);

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 欢迎卡片
          GlassContainer(
            child: Row(
              children: [
                Container(
                  width: 60,
                  height: 60,
                  decoration: BoxDecoration(
                    gradient: const LinearGradient(
                      colors: [AppTheme.primaryColor, AppTheme.secondaryColor],
                    ),
                    borderRadius: BorderRadius.circular(16),
                  ),
                  child: const Icon(Icons.keyboard_voice, color: Colors.white, size: 30),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        api.isLoggedIn ? '你好, ${api.username}' : '欢迎使用初眠输入法',
                        style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        api.isLoggedIn ? '喵喵币: ${api.meowCoins}' : '登录后享受更多功能',
                        style: TextStyle(color: Colors.grey[600], fontSize: 13),
                      ),
                    ],
                  ),
                ),
                if (api.isLoggedIn)
                  ElevatedButton(
                    onPressed: () async {
                      await api.signIn();
                      if (context.mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(content: Text('签到成功，获得1喵喵币')),
                        );
                      }
                    },
                    style: ElevatedButton.styleFrom(
                      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                    ),
                    child: const Text('签到'),
                  ),
              ],
            ),
          ),
          const SizedBox(height: 16),

          // 功能快捷入口
          const Text('功能中心', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          const SizedBox(height: 12),
          GridView.count(
            crossAxisCount: 4,
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            mainAxisSpacing: 12,
            crossAxisSpacing: 12,
            children: [
              _FeatureIcon(icon: Icons.keyboard, label: '键盘管理', onTap: () {}),
              _FeatureIcon(icon: Icons.palette, label: '皮肤商店', onTap: () {}),
              _FeatureIcon(icon: Icons.menu_book, label: '词库下载', onTap: () {
                Navigator.push(context, MaterialPageRoute(builder: (_) => const DictionaryScreen()));
              }),
              _FeatureIcon(icon: Icons.mic, label: '语音输入', onTap: () {}),
              _FeatureIcon(icon: Icons.edit, label: '手写输入', onTap: () {}),
              _FeatureIcon(icon: Icons.contacts, label: '联系人', onTap: () {}),
              _FeatureIcon(icon: Icons.analytics, label: '输入统计', onTap: () {}),
              _FeatureIcon(icon: Icons.update, label: '检查更新', onTap: () async {
                final result = await api.checkUpdate();
                if (context.mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: Text(result['has_update'] == true ? '发现新版本' : '已是最新版本')),
                  );
                }
              }),
            ],
          ),
          const SizedBox(height: 16),

          // 今日统计
          GlassContainer(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('今日输入统计', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                const SizedBox(height: 12),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceAround,
                  children: [
                    _StatItem(value: '1,234', label: '输入字数'),
                    _StatItem(value: '56', label: '输入次数'),
                    _StatItem(value: '98%', label: '准确率'),
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),

          // 推荐皮肤
          const Text('推荐皮肤', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          const SizedBox(height: 12),
          SizedBox(
            height: 120,
            child: ListView(
              scrollDirection: Axis.horizontal,
              children: [
                _SkinPreviewCard(name: '极光紫', gradient: [AppTheme.primaryColor, AppTheme.secondaryColor]),
                _SkinPreviewCard(name: '樱花粉', gradient: [Color(0xFFFF9A9E), Color(0xFFFAD0C4)]),
                _SkinPreviewCard(name: '海洋蓝', gradient: [Color(0xFF4FACFE), Color(0xFF00F2FE)]),
                _SkinPreviewCard(name: '森林绿', gradient: [Color(0xFF43E97B), Color(0xFF38F9D7)]),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _FeatureIcon extends StatelessWidget {
  final IconData icon;
  final String label;
  final VoidCallback onTap;
  const _FeatureIcon({required this.icon, required this.label, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: GlassContainer(
        padding: const EdgeInsets.all(8),
        borderRadius: 16,
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(icon, color: AppTheme.primaryColor, size: 24),
            const SizedBox(height: 4),
            Text(label, style: const TextStyle(fontSize: 10), textAlign: TextAlign.center),
          ],
        ),
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
        Text(value, style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: AppTheme.primaryColor)),
        const SizedBox(height: 4),
        Text(label, style: TextStyle(color: Colors.grey[600], fontSize: 12)),
      ],
    );
  }
}

class _SkinPreviewCard extends StatelessWidget {
  final String name;
  final List<Color> gradient;
  const _SkinPreviewCard({required this.name, required this.gradient});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 100,
      margin: const EdgeInsets.only(right: 12),
      decoration: BoxDecoration(
        gradient: LinearGradient(colors: gradient),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Center(
        child: Text(name, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
      ),
    );
  }
}
