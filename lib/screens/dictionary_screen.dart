import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../theme/app_theme.dart';
import '../services/api_service.dart';

class DictionaryScreen extends StatefulWidget {
  const DictionaryScreen({super.key});

  @override
  State<DictionaryScreen> createState() => _DictionaryScreenState();
}

class _DictionaryScreenState extends State<DictionaryScreen> {
  bool _loading = true;
  List<dynamic> _dictionaries = [];
  Set<String> _downloaded = {};
  String? _downloadingId;
  double _downloadProgress = 0;

  // 内置词库数据
  final List<Map<String, dynamic>> _localDicts = [
    {'id': 'basic', 'name': '基础词库', 'desc': '常用汉字和词语', 'size': '2.3MB', 'words': 50000},
    {'id': 'internet', 'name': '网络流行词', 'desc': '最新网络用语和梗', 'size': '1.2MB', 'words': 20000},
    {'id': 'tech', 'name': '科技词库', 'desc': 'IT和科技专业词汇', 'size': '3.1MB', 'words': 80000},
    {'id': 'medical', 'name': '医学词库', 'desc': '医学专业术语', 'size': '4.5MB', 'words': 120000},
    {'id': 'legal', 'name': '法律词库', 'desc': '法律专业词汇', 'size': '2.8MB', 'words': 60000},
    {'id': 'finance', 'name': '金融词库', 'desc': '金融和经济词汇', 'size': '3.5MB', 'words': 90000},
    {'id': 'game', 'name': '游戏词库', 'desc': '游戏相关词汇', 'size': '1.8MB', 'words': 40000},
    {'id': 'anime', 'name': '动漫词库', 'desc': '动漫和二次元词汇', 'size': '2.1MB', 'words': 55000},
    {'id': 'food', 'name': '美食词库', 'desc': '美食和烹饪词汇', 'size': '1.5MB', 'words': 35000},
    {'id': 'travel', 'name': '旅游词库', 'desc': '旅游和出行词汇', 'size': '1.9MB', 'words': 45000},
    {'id': 'sports', 'name': '体育词库', 'desc': '体育和运动词汇', 'size': '2.2MB', 'words': 50000},
    {'id': 'music', 'name': '音乐词库', 'desc': '音乐和娱乐词汇', 'size': '1.7MB', 'words': 40000},
    {'id': 'art', 'name': '艺术词库', 'desc': '艺术和设计词汇', 'size': '2.0MB', 'words': 48000},
    {'id': 'history', 'name': '历史词库', 'desc': '历史和文化词汇', 'size': '3.2MB', 'words': 75000},
    {'id': 'geography', 'name': '地理词库', 'desc': '地理和地名词汇', 'size': '2.5MB', 'words': 60000},
    {'id': 'philosophy', 'name': '哲学词库', 'desc': '哲学和思想词汇', 'size': '1.8MB', 'words': 42000},
    {'id': 'psychology', 'name': '心理词库', 'desc': '心理学词汇', 'size': '2.3MB', 'words': 55000},
    {'id': 'education', 'name': '教育词库', 'desc': '教育和学习词汇', 'size': '2.6MB', 'words': 65000},
    {'id': 'business', 'name': '商业词库', 'desc': '商业和管理词汇', 'size': '3.0MB', 'words': 70000},
    {'id': 'agriculture', 'name': '农业词库', 'desc': '农业和种植词汇', 'size': '1.6MB', 'words': 38000},
  ];

  @override
  void initState() {
    super.initState();
    _loadDictionaries();
  }

  Future<void> _loadDictionaries() async {
    final api = Provider.of<ApiService>(context, listen: false);
    await api.fetchDictionaries();
    setState(() {
      _dictionaries = api.dictionaries.isNotEmpty ? api.dictionaries : _localDicts;
      _loading = false;
    });
  }

  Future<void> _downloadDictionary(Map<String, dynamic> dict) async {
    setState(() {
      _downloadingId = dict['id'];
      _downloadProgress = 0;
    });

    // 模拟下载进度
    for (int i = 0; i <= 100; i += 10) {
      await Future.delayed(const Duration(milliseconds: 200));
      if (mounted) setState(() => _downloadProgress = i / 100);
    }

    if (mounted) {
      setState(() {
        _downloaded.add(dict['id']);
        _downloadingId = null;
      });
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('${dict['name']} 下载完成')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('词库下载')),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: _dictionaries.length,
              itemBuilder: (context, index) {
                final dict = _dictionaries[index];
                final isDownloaded = _downloaded.contains(dict['id']);
                final isDownloading = _downloadingId == dict['id'];

                return GlassContainer(
                  margin: const EdgeInsets.only(bottom: 12),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
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
                            child: const Icon(Icons.menu_book, color: Colors.white),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(dict['name'], style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                                const SizedBox(height: 2),
                                Text(dict['desc'], style: TextStyle(color: Colors.grey[600], fontSize: 12)),
                                const SizedBox(height: 4),
                                Row(
                                  children: [
                                    Text('${dict['words']}词', style: const TextStyle(fontSize: 11, color: Colors.grey)),
                                    const SizedBox(width: 8),
                                    Text(dict['size'], style: const TextStyle(fontSize: 11, color: Colors.grey)),
                                  ],
                                ),
                              ],
                            ),
                          ),
                          if (isDownloading)
                            SizedBox(
                              width: 40,
                              height: 40,
                              child: CircularProgressIndicator(
                                value: _downloadProgress,
                                strokeWidth: 3,
                              ),
                            )
                          else if (isDownloaded)
                            const Icon(Icons.check_circle, color: Colors.green, size: 32)
                          else
                            IconButton(
                              icon: const Icon(Icons.download, color: AppTheme.primaryColor),
                              onPressed: () => _downloadDictionary(dict),
                            ),
                        ],
                      ),
                      if (isDownloading) ...[
                        const SizedBox(height: 8),
                        LinearProgressIndicator(value: _downloadProgress),
                      ],
                    ],
                  ),
                );
              },
            ),
    );
  }
}
