import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../theme/app_theme.dart';
import '../services/api_service.dart';

class SkinStoreScreen extends StatefulWidget {
  const SkinStoreScreen({super.key});

  @override
  State<SkinStoreScreen> createState() => _SkinStoreScreenState();
}

class _SkinStoreScreenState extends State<SkinStoreScreen> {
  bool _loading = true;
  List<dynamic> _skins = [];

  @override
  void initState() {
    super.initState();
    _loadSkins();
  }

  Future<void> _loadSkins() async {
    final api = Provider.of<ApiService>(context, listen: false);
    await api.fetchSkins();
    setState(() {
      _skins = api.skins;
      _loading = false;
    });
  }

  // 内置皮肤数据（离线可用）
  final List<Map<String, dynamic>> _localSkins = [
    {'name': '极光紫', 'colors': [0xFF6C5CE7, 0xFFA29BFE], 'price': 0, 'owned': true},
    {'name': '樱花粉', 'colors': [0xFFFF9A9E, 0xFFFAD0C4], 'price': 0, 'owned': true},
    {'name': '海洋蓝', 'colors': [0xFF4FACFE, 0xFF00F2FE], 'price': 0, 'owned': true},
    {'name': '森林绿', 'colors': [0xFF43E97B, 0xFF38F9D7], 'price': 0, 'owned': true},
    {'name': '日落橙', 'colors': [0xFFFF6B6B, 0xFFFECA57], 'price': 1, 'owned': false},
    {'name': '星空黑', 'colors': [0xFF2D3436, 0xFF636E72], 'price': 1, 'owned': false},
    {'name': '薄荷青', 'colors': [0xFF00B894, 0xFF55EFC4], 'price': 1, 'owned': false},
    {'name': '玫瑰金', 'colors': [0xFFE17055, 0xFFFAB1A0], 'price': 1, 'owned': false},
    {'name': '薰衣草', 'colors': [0xFFA29BFE, 0xFFD6A2E8], 'price': 1, 'owned': false},
    {'name': '珊瑚红', 'colors': [0xFFFF7675, 0xFFFF9FF3], 'price': 1, 'owned': false},
    {'name': '翡翠绿', 'colors': [0xFF00CEC9, 0xFF81ECEC], 'price': 1, 'owned': false},
    {'name': '皇家蓝', 'colors': [0xFF0984E3, 0xFF74B9FF], 'price': 1, 'owned': false},
  ];

  @override
  Widget build(BuildContext context) {
    final api = Provider.of<ApiService>(context);
    final skins = _skins.isNotEmpty ? _skins : _localSkins;

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text('皮肤商店', style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold)),
              if (api.isLoggedIn)
                GlassContainer(
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                  borderRadius: 20,
                  child: Row(
                    children: [
                      const Icon(Icons.pets, color: AppTheme.accent, size: 18),
                      const SizedBox(width: 4),
                      Text('${api.meowCoins}', style: const TextStyle(fontWeight: FontWeight.bold)),
                    ],
                  ),
                ),
            ],
          ),
          const SizedBox(height: 8),
          Text('每日签到获得喵喵币，1喵喵币可购买一款高级皮肤', style: TextStyle(color: Colors.grey[600])),
          const SizedBox(height: 16),
          if (_loading)
            const Center(child: CircularProgressIndicator())
          else
            GridView.builder(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 2,
                crossAxisSpacing: 12,
                mainAxisSpacing: 12,
                childAspectRatio: 0.8,
              ),
              itemCount: skins.length,
              itemBuilder: (context, index) {
                final skin = skins[index];
                final colors = skin['colors'] as List<int>;
                final isOwned = skin['owned'] == true || skin['price'] == 0;
                return GlassContainer(
                  padding: const EdgeInsets.all(12),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      // 皮肤预览
                      Expanded(
                        child: Container(
                          decoration: BoxDecoration(
                            gradient: LinearGradient(
                              colors: colors.map((c) => Color(c)).toList(),
                            ),
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: Center(
                            child: Container(
                              width: 80,
                              height: 30,
                              decoration: BoxDecoration(
                                color: Colors.white.withOpacity(0.3),
                                borderRadius: BorderRadius.circular(6),
                              ),
                              child: Row(
                                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                                children: List.generate(5, (_) => Container(
                                  width: 10,
                                  height: 18,
                                  decoration: BoxDecoration(
                                    color: Colors.white.withOpacity(0.5),
                                    borderRadius: BorderRadius.circular(2),
                                  ),
                                )),
                              ),
                            ),
                          ),
                        ),
                      ),
                      const SizedBox(height: 8),
                      Text(skin['name'], style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
                      const SizedBox(height: 4),
                      Row(
                        children: [
                          if (isOwned)
                            const Text('已拥有', style: TextStyle(color: Colors.green, fontSize: 12))
                          else
                            Row(
                              children: [
                                const Icon(Icons.pets, color: AppTheme.accent, size: 14),
                                Text(' ${skin['price']}', style: const TextStyle(fontSize: 12)),
                              ],
                            ),
                          const Spacer(),
                          SizedBox(
                            height: 28,
                            child: ElevatedButton(
                              onPressed: isOwned
                                  ? () {
                                      ScaffoldMessenger.of(context).showSnackBar(
                                        SnackBar(content: Text('已应用 ${skin['name']} 皮肤')),
                                      );
                                    }
                                  : () async {
                                      if (!api.isLoggedIn) {
                                        ScaffoldMessenger.of(context).showSnackBar(
                                          const SnackBar(content: Text('请先登录')),
                                        );
                                        return;
                                      }
                                      final success = await api.buySkin(skin['name']);
                                      if (context.mounted) {
                                        ScaffoldMessenger.of(context).showSnackBar(
                                          SnackBar(content: Text(success ? '购买成功' : '喵喵币不足')),
                                        );
                                      }
                                    },
                              style: ElevatedButton.styleFrom(
                                padding: const EdgeInsets.symmetric(horizontal: 12),
                                backgroundColor: isOwned ? Colors.grey : AppTheme.primary,
                              ),
                              child: Text(isOwned ? '应用' : '购买', style: const TextStyle(fontSize: 11)),
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                );
              },
            ),
        ],
      ),
    );
  }
}
