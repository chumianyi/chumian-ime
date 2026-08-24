import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'theme/app_theme.dart';
import 'screens/home_screen.dart';
import 'services/api_service.dart';

void main() {
  runApp(const ChumianIMEApp());
}

class ChumianIMEApp extends StatelessWidget {
  const ChumianIMEApp({super.key});

  @override
  Widget build(BuildContext context) {
    return ChangeNotifierProvider(
      create: (_) => ApiService(),
      child: MaterialApp(
        title: '初眠输入法',
        debugShowCheckedModeBanner: false,
        theme: AppTheme.theme,
        home: const HomeScreen(),
      ),
    );
  }
}
