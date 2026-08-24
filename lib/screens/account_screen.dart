import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../theme/app_theme.dart';
import '../services/api_service.dart';

class AccountScreen extends StatefulWidget {
  const AccountScreen({super.key});

  @override
  State<AccountScreen> createState() => _AccountScreenState();
}

class _AccountScreenState extends State<AccountScreen> {
  final _usernameController = TextEditingController();
  final _passwordController = TextEditingController();
  final _inviteController = TextEditingController();
  bool _isRegister = false;
  bool _loading = false;

  @override
  void dispose() {
    _usernameController.dispose();
    _passwordController.dispose();
    _inviteController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final api = Provider.of<ApiService>(context);

    if (!api.isLoggedIn) {
      return SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('账号', style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold)),
            const SizedBox(height: 24),
            GlassContainer(
              child: Column(
                children: [
                  TextField(
                    controller: _usernameController,
                    decoration: const InputDecoration(
                      labelText: '用户名',
                      prefixIcon: Icon(Icons.person),
                    ),
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: _passwordController,
                    obscureText: true,
                    decoration: const InputDecoration(
                      labelText: '密码',
                      prefixIcon: Icon(Icons.lock),
                    ),
                  ),
                  if (_isRegister) ...[
                    const SizedBox(height: 12),
                    TextField(
                      controller: _inviteController,
                      decoration: const InputDecoration(
                        labelText: '邀请码',
                        prefixIcon: Icon(Icons.card_giftcard),
                      ),
                    ),
                  ],
                  const SizedBox(height: 20),
                  SizedBox(
                    width: double.infinity,
                    child: ElevatedButton(
                      onPressed: _loading ? null : () async {
                        setState(() => _loading = true);
                        bool success;
                        if (_isRegister) {
                          success = await api.register(
                            _usernameController.text,
                            _passwordController.text,
                            _inviteController.text,
                          );
                        } else {
                          success = await api.login(
                            _usernameController.text,
                            _passwordController.text,
                          );
                        }
                        if (context.mounted) {
                          setState(() => _loading = false);
                          ScaffoldMessenger.of(context).showSnackBar(
                            SnackBar(content: Text(success ? '成功' : '失败，请检查输入')),
                          );
                        }
                      },
                      child: _loading
                          ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2))
                          : Text(_isRegister ? '注册' : '登录'),
                    ),
                  ),
                  const SizedBox(height: 12),
                  TextButton(
                    onPressed: () => setState(() => _isRegister = !_isRegister),
                    child: Text(_isRegister ? '已有账号？去登录' : '没有账号？去注册'),
                  ),
                ],
              ),
            ),
          ],
        ),
      );
    }

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 用户信息卡片
          GlassContainer(
            child: Row(
              children: [
                Container(
                  width: 64,
                  height: 64,
                  decoration: BoxDecoration(
                    gradient: const LinearGradient(
                      colors: [AppTheme.primaryColor, AppTheme.secondaryColor],
                    ),
                    borderRadius: BorderRadius.circular(32),
                  ),
                  child: Center(
                    child: Text(
                      api.username?.substring(0, 1).toUpperCase() ?? 'U',
                      style: const TextStyle(fontSize: 28, color: Colors.white, fontWeight: FontWeight.bold),
                    ),
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(api.username ?? '', style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
                      const SizedBox(height: 4),
                      Row(
                        children: [
                          const Icon(Icons.pets, color: AppTheme.accentColor, size: 18),
                          const SizedBox(width: 4),
                          Text('${api.meowCoins} 喵喵币', style: const TextStyle(fontSize: 14)),
                        ],
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),

          // 签到
          GlassContainer(
            child: ListTile(
              leading: const Icon(Icons.calendar_today, color: AppTheme.primaryColor),
              title: const Text('每日签到'),
              subtitle: const Text('签到获得1喵喵币'),
              trailing: ElevatedButton(
                onPressed: () async {
                  final success = await api.signIn();
                  if (context.mounted) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(content: Text(success ? '签到成功，获得1喵喵币' : '今日已签到')),
                    );
                  }
                },
                child: const Text('签到'),
              ),
            ),
          ),
          const SizedBox(height: 12),

          // 输入统计
          GlassContainer(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('输入统计', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                const SizedBox(height: 12),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceAround,
                  children: const [
                    _StatColumn(value: '12,345', label: '总字数'),
                    _StatColumn(value: '567', label: '总次数'),
                    _StatColumn(value: '98%', label: '准确率'),
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(height: 12),

          // 已购皮肤
          GlassContainer(
            child: const ListTile(
              leading: Icon(Icons.palette, color: AppTheme.primaryColor),
              title: Text('已购皮肤'),
              subtitle: Text('查看已购买的皮肤'),
              trailing: Icon(Icons.chevron_right),
            ),
          ),
          const SizedBox(height: 12),

          // 退出登录
          GlassContainer(
            child: ListTile(
              leading: const Icon(Icons.logout, color: Colors.red),
              title: const Text('退出登录', style: TextStyle(color: Colors.red)),
              onTap: () async {
                await api.logout();
              },
            ),
          ),
        ],
      ),
    );
  }
}

class _StatColumn extends StatelessWidget {
  final String value;
  final String label;
  const _StatColumn({required this.value, required this.label});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Text(value, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: AppTheme.primaryColor)),
        const SizedBox(height: 4),
        Text(label, style: TextStyle(color: Colors.grey[600], fontSize: 12)),
      ],
    );
  }
}
