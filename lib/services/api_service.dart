import 'dart:convert';
import 'dart:io';
import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

class ApiService extends ChangeNotifier {
  static const String _serverUrl = 'https://chumian-ime.example.com';
  static const String _cdnUrl = 'https://cdn.chumian-ime.example.com';

  String? _token;
  String? _username;
  int _meowCoins = 0;
  bool _isLoggedIn = false;
  List<dynamic> _skins = [];
  List<dynamic> _dictionaries = [];
  Map<String, dynamic> _userStats = {};

  String? get token => _token;
  String? get username => _username;
  int get meowCoins => _meowCoins;
  bool get isLoggedIn => _isLoggedIn;
  List<dynamic> get skins => _skins;
  List<dynamic> get dictionaries => _dictionaries;
  Map<String, dynamic> get userStats => _userStats;

  ApiService() {
    _loadFromPrefs();
  }

  Future<void> _loadFromPrefs() async {
    final prefs = await SharedPreferences.getInstance();
    _token = prefs.getString('token');
    _username = prefs.getString('username');
    _meowCoins = prefs.getInt('meow_coins') ?? 0;
    _isLoggedIn = _token != null;
    notifyListeners();
  }

  Future<Map<String, dynamic>> _request(String endpoint,
      {Map<String, dynamic>? body, bool useCdn = false}) async {
    try {
      final baseUrl = useCdn ? _cdnUrl : _serverUrl;
      final url = Uri.parse('$baseUrl$endpoint');
      final headers = {
        'Content-Type': 'application/json',
        if (_token != null) 'Authorization': 'Bearer $_token',
      };

      http.Response response;
      if (body != null) {
        response = await http
            .post(url, headers: headers, body: jsonEncode(body))
            .timeout(const Duration(seconds: 10));
      } else {
        response = await http
            .get(url, headers: headers)
            .timeout(const Duration(seconds: 10));
      }

      if (response.statusCode == 200) {
        return jsonDecode(response.body);
      }
      return {'error': 'HTTP ${response.statusCode}'};
    } catch (e) {
      // CDN失败回源
      if (useCdn) {
        return _request(endpoint, body: body, useCdn: false);
      }
      return {'error': '网络错误'};
    }
  }

  // 注册
  Future<bool> register(String username, String password, String inviteCode) async {
    final result = await _request('/api/register', body: {
      'username': username,
      'password': password,
      'invite_code': inviteCode,
    });
    if (result['token'] != null) {
      _token = result['token'];
      _username = username;
      _isLoggedIn = true;
      final prefs = await SharedPreferences.getInstance();
      prefs.setString('token', _token!);
      prefs.setString('username', username);
      notifyListeners();
      return true;
    }
    return false;
  }

  // 登录
  Future<bool> login(String username, String password) async {
    final result = await _request('/api/login', body: {
      'username': username,
      'password': password,
    });
    if (result['token'] != null) {
      _token = result['token'];
      _username = username;
      _meowCoins = result['meow_coins'] ?? 0;
      _isLoggedIn = true;
      final prefs = await SharedPreferences.getInstance();
      prefs.setString('token', _token!);
      prefs.setString('username', username);
      prefs.setInt('meow_coins', _meowCoins);
      notifyListeners();
      return true;
    }
    return false;
  }

  // 签到
  Future<bool> signIn() async {
    final result = await _request('/api/signin');
    if (result['success'] == true) {
      _meowCoins = result['meow_coins'] ?? _meowCoins;
      final prefs = await SharedPreferences.getInstance();
      prefs.setInt('meow_coins', _meowCoins);
      notifyListeners();
      return true;
    }
    return false;
  }

  // 获取皮肤列表
  Future<void> fetchSkins() async {
    final result = await _request('/api/skins', useCdn: true);
    if (result['skins'] != null) {
      _skins = result['skins'];
      notifyListeners();
    }
  }

  // 购买皮肤
  Future<bool> buySkin(String skinId) async {
    final result = await _request('/api/skins/buy', body: {'skin_id': skinId});
    if (result['success'] == true) {
      _meowCoins = result['meow_coins'] ?? _meowCoins;
      final prefs = await SharedPreferences.getInstance();
      prefs.setInt('meow_coins', _meowCoins);
      notifyListeners();
      return true;
    }
    return false;
  }

  // 获取词库列表
  Future<void> fetchDictionaries() async {
    final result = await _request('/api/dictionaries', useCdn: true);
    if (result['dictionaries'] != null) {
      _dictionaries = result['dictionaries'];
      notifyListeners();
    }
  }

  // 下载词库
  Future<String?> downloadDictionary(String dictId) async {
    final result = await _request('/api/dictionaries/$dictId/download', useCdn: true);
    return result['url'];
  }

  // 统计字数
  Future<void> updateStats(int charCount) async {
    await _request('/api/stats/update', body: {'char_count': charCount});
  }

  // 获取用户统计
  Future<void> fetchUserStats() async {
    final result = await _request('/api/stats');
    if (result['stats'] != null) {
      _userStats = result['stats'];
      notifyListeners();
    }
  }

  // 检查更新
  Future<Map<String, dynamic>> checkUpdate() async {
    return await _request('/api/update', useCdn: true);
  }

  // 登出
  Future<void> logout() async {
    _token = null;
    _username = null;
    _meowCoins = 0;
    _isLoggedIn = false;
    final prefs = await SharedPreferences.getInstance();
    prefs.remove('token');
    prefs.remove('username');
    prefs.remove('meow_coins');
    notifyListeners();
  }
}
