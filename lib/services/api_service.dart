import 'dart:convert';
import 'dart:io';
import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

class ApiService extends ChangeNotifier {
  static const String _serverHost = '103.236.99.177';
  static const int _serverPort = 24512;

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

  // UDP请求
  Future<Map<String, dynamic>> _request(String action, Map<String, dynamic> data) async {
    try {
      final socket = await RawDatagramSocket.bind(InternetAddress.anyIPv4, 0);
      final request = jsonEncode({'action': action, 'data': data});
      socket.send(utf8.encode(request), InternetAddress(_serverHost), _serverPort);

      final completer = _UDPCompleter();
      socket.listen((event) {
        if (event == RawSocketEvent.read) {
          final datagram = socket.receive();
          if (datagram != null) {
            try {
              completer.complete(jsonDecode(utf8.decode(datagram.data)));
            } catch (_) {
              completer.complete({'error': '网络错误'});
            }
            socket.close();
          }
        }
      });

      final result = await completer.future.timeout(
        const Duration(seconds: 10),
        onTimeout: () {
          socket.close();
          return {'error': '网络错误'};
        },
      );
      return result;
    } catch (_) {
      return {'error': '网络错误'};
    }
  }

  // 注册
  Future<bool> register(String username, String password, String inviteCode) async {
    final result = await _request('register', {
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

  // 登录
  Future<bool> login(String username, String password) async {
    final result = await _request('login', {
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
    final result = await _request('signin', {'token': _token});
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
    final result = await _request('get_skins', {'token': _token});
    if (result['skins'] != null) {
      _skins = result['skins'];
      notifyListeners();
    }
  }

  // 购买皮肤
  Future<bool> buySkin(String skinId) async {
    final result = await _request('buy_skin', {'token': _token, 'skin_id': skinId});
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
    final result = await _request('get_dictionaries', {});
    if (result['dictionaries'] != null) {
      _dictionaries = result['dictionaries'];
      notifyListeners();
    }
  }

  // 统计字数
  Future<void> updateStats(int charCount) async {
    await _request('update_stats', {'token': _token, 'char_count': charCount});
  }

  // 获取用户统计
  Future<void> fetchUserStats() async {
    final result = await _request('get_stats', {'token': _token});
    if (result['stats'] != null) {
      _userStats = result['stats'];
      notifyListeners();
    }
  }

  // 检查更新
  Future<Map<String, dynamic>> checkUpdate() async {
    return await _request('check_update', {});
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

class _UDPCompleter {
  final _completer = _SafeCompleter<Map<String, dynamic>>();
  Future<Map<String, dynamic>> get future => _completer.future;
  void complete(Map<String, dynamic> value) => _completer.complete(value);
}

class _SafeCompleter<T> {
  T? _value;
  bool _completed = false;
  final List<Function(T)> _callbacks = [];

  Future<T> get future {
    return Future<T>(() {
      if (_completed) return _value as T;
      final c = _InnerCompleter<T>();
      _callbacks.add((v) => c.complete(v));
      return c.future;
    });
  }

  void complete(T value) {
    if (_completed) return;
    _completed = true;
    _value = value;
    for (final cb in _callbacks) {
      cb(value);
    }
  }
}

class _InnerCompleter<T> {
  late final Future<T> future;
  _InnerCompleter() {
    future = Future<T>(() => _value as T);
  }
  T? _value;
  void complete(T value) => _value = value;
}
